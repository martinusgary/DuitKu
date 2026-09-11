package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class UpdateResult {
    object NoUpdate : UpdateResult()
    data class NewUpdate(
        val latestVersionName: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val pageUrl: String
    ) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    
    // Default repository, match user's details
    private const val DEFAULT_OWNER = "martinusgary"
    private const val DEFAULT_REPO = "DuitKu"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun cleanVersion(version: String): String {
        return version.trim().lowercase()
            .removePrefix("release")
            .removePrefix("v")
            .removePrefix("-")
            .trim()
    }

    /**
     * Compares two semantic version strings.
     * Returns positive if version1 > version2, negative if version1 < version2, 0 if equal.
     */
    fun compareVersions(version1: String, version2: String): Int {
        val clean1 = cleanVersion(version1).split("-")[0]
        val clean2 = cleanVersion(version2).split("-")[0]

        val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            // 1. Try releases/latest
            val latestUrl = "https://api.github.com/repos/$DEFAULT_OWNER/$DEFAULT_REPO/releases/latest"
            val request = Request.Builder()
                .url(latestUrl)
                .header("User-Agent", "DuitKu-Android-Updater")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrEmpty()) {
                            val json = JSONObject(bodyString)
                            val tagName = json.optString("tag_name", "")
                            if (tagName.isNotEmpty() && compareVersions(tagName, currentVersion) > 0) {
                                val htmlUrl = json.optString("html_url", "https://github.com/$DEFAULT_OWNER/$DEFAULT_REPO/releases")
                                val bodyNotes = json.optString("body", "Pembaruan versi baru telah tersedia di GitHub.")
                                
                                var downloadUrl = htmlUrl
                                val assets = json.optJSONArray("assets")
                                if (assets != null && assets.length() > 0) {
                                    for (i in 0 until assets.length()) {
                                        val asset = assets.getJSONObject(i)
                                        val name = asset.optString("name", "")
                                        if (name.endsWith(".apk", ignoreCase = true)) {
                                            val assetUrl = asset.optString("browser_download_url", "")
                                            if (assetUrl.isNotEmpty()) {
                                                downloadUrl = assetUrl
                                                break
                                            }
                                        }
                                    }
                                }

                                return@withContext UpdateResult.NewUpdate(
                                    latestVersionName = tagName,
                                    downloadUrl = downloadUrl,
                                    releaseNotes = bodyNotes,
                                    pageUrl = htmlUrl
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "releases/latest check failed, trying tags: ${e.message}")
            }

            // 2. Fallback: Check tags
            val tagsUrl = "https://api.github.com/repos/$DEFAULT_OWNER/$DEFAULT_REPO/tags"
            val tagsRequest = Request.Builder()
                .url(tagsUrl)
                .header("User-Agent", "DuitKu-Android-Updater")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(tagsRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val tagsArray = JSONArray(bodyString)
                        if (tagsArray.length() > 0) {
                            val latestTagObj = tagsArray.getJSONObject(0)
                            val tagName = latestTagObj.optString("name", "")
                            if (tagName.isNotEmpty() && compareVersions(tagName, currentVersion) > 0) {
                                val htmlUrl = "https://github.com/$DEFAULT_OWNER/$DEFAULT_REPO/releases/tag/$tagName"
                                return@withContext UpdateResult.NewUpdate(
                                    latestVersionName = tagName,
                                    downloadUrl = htmlUrl,
                                    releaseNotes = "Pembaruan versi $tagName telah dirilis di repositori GitHub.",
                                    pageUrl = htmlUrl
                                )
                            }
                        }
                    }
                }
            }

            return@withContext UpdateResult.NoUpdate
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            return@withContext UpdateResult.Error(e.message ?: "Gagal memeriksa pembaruan dari GitHub")
        }
    }

    /**
     * Downloads the APK file directly to the application cache with progress callback.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (DownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val safeVersion = versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "DuitKu-$safeVersion.apk"
            val targetDir = context.getExternalFilesDir("updates") ?: context.cacheDir
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "DuitKu-Android-Updater")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = "HTTP error code ${response.code}"
                    withContext(Dispatchers.Main) {
                        onProgress(DownloadState.Error(errMsg))
                    }
                    return@withContext null
                }

                val body = response.body ?: run {
                    withContext(Dispatchers.Main) {
                        onProgress(DownloadState.Error("Respon kosong dari server"))
                    }
                    return@withContext null
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead: Long = 0
                var lastPercent = -1

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            val percent = if (totalBytes > 0) {
                                ((totalBytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            if (percent != lastPercent) {
                                lastPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress(DownloadState.Downloading(percent, totalBytesRead, totalBytes))
                                }
                            }
                        }
                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    onProgress(DownloadState.Completed(targetFile))
                }
                return@withContext targetFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onProgress(DownloadState.Error(e.message ?: "Gagal mengunduh berkas APK"))
            }
            return@withContext null
        }
    }

    /**
     * Triggers the Android package installer for the downloaded APK file.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching installer: ${e.message}", e)
            // Fallback: try opening with generic intent
            try {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = apkUri
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback installer failed: ${fallbackError.message}", fallbackError)
            }
        }
    }
}
