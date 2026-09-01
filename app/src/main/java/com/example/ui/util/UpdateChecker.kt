package com.example.ui.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
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

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    
    // Default repository, match user's details
    private const val DEFAULT_OWNER = "martinusgary"
    private const val DEFAULT_REPO = "DuitKu"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
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
}
