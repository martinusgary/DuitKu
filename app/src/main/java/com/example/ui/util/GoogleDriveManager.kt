package com.example.ui.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object GoogleDriveManager {
    private const val TAG = "GoogleDriveManager"
    private const val BACKUP_FILE_NAME = "duitku_cloud_backup.duitku"
    private const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                val androidAccount = account.account
                if (androidAccount != null) {
                    val scopeString = "oauth2:https://www.googleapis.com/auth/drive.appdata email profile"
                    GoogleAuthUtil.getToken(context, androidAccount, scopeString)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting OAuth access token", e)
                null
            }
        }
    }

    suspend fun uploadBackupToDrive(
        context: Context,
        account: GoogleSignInAccount,
        encryptedBackupData: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context, account)
            if (token == null) {
                return@withContext Result.failure(Exception("Gagal memperoleh token otentikasi Google Drive OAuth."))
            }

            // 1. Check if backup file already exists in appDataFolder
            val existingFileId = findBackupFileId(token)

            val timeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            val currentTimeStr = timeFormat.format(Date())
            val payload = "$currentTimeStr:::$encryptedBackupData"

            if (existingFileId != null) {
                // 2. Update existing file
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val mediaBody = payload.toRequestBody("text/plain; charset=utf-8".toMediaType())
                val updateRequest = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(mediaBody)
                    .build()

                val response = httpClient.newCall(updateRequest).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully updated backup in Google Drive appDataFolder: $existingFileId")
                    Result.success(currentTimeStr)
                } else {
                    val err = response.body?.string() ?: "Update failed"
                    Log.e(TAG, "Drive update error: $err")
                    Result.failure(Exception("Gagal memperbarui berkas di Google Drive: $err"))
                }
            } else {
                // 3. Create new multipart file in appDataFolder
                val metadataJson = JSONObject().apply {
                    put("name", BACKUP_FILE_NAME)
                    put("parents", org.json.JSONArray().apply { put("appDataFolder") })
                    put("description", "Cadangan data transaksi terenkripsi DuitKu")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "metadata",
                        null,
                        metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                    )
                    .addFormDataPart(
                        "file",
                        BACKUP_FILE_NAME,
                        payload.toRequestBody("text/plain; charset=utf-8".toMediaType())
                    )
                    .build()

                val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val createRequest = Request.Builder()
                    .url(createUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()

                val response = httpClient.newCall(createRequest).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully created new backup in Google Drive appDataFolder")
                    Result.success(currentTimeStr)
                } else {
                    val err = response.body?.string() ?: "Create failed"
                    Log.e(TAG, "Drive create error: $err")
                    Result.failure(Exception("Gagal membuat berkas baru di Google Drive: $err"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Drive upload exception", e)
            Result.failure(e)
        }
    }

    suspend fun fetchBackupFromDrive(
        context: Context,
        account: GoogleSignInAccount
    ): Result<Pair<String, String>?> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context, account)
            if (token == null) {
                return@withContext Result.failure(Exception("Gagal memperoleh token otentikasi Google Drive OAuth."))
            }

            val fileId = findBackupFileId(token)
            if (fileId == null) {
                return@withContext Result.success(null)
            }

            // Download file content
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val downloadRequest = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(downloadRequest).execute()
            if (response.isSuccessful) {
                val content = response.body?.string() ?: ""
                if (content.contains(":::")) {
                    val parts = content.split(":::", limit = 2)
                    Result.success(Pair(parts[0], parts[1]))
                } else {
                    Result.success(Pair("Tanggal Tidak Diketahui", content))
                }
            } else {
                val err = response.body?.string() ?: "Download failed"
                Result.failure(Exception("Gagal mengunduh cadangan dari Google Drive: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Drive fetch exception", e)
            Result.failure(e)
        }
    }

    private fun findBackupFileId(token: String): String? {
        try {
            val queryUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='$BACKUP_FILE_NAME'+and+trashed=false&fields=files(id,name,modifiedTime)"
            val searchRequest = Request.Builder()
                .url(queryUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(searchRequest).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: return null
                val root = JSONObject(jsonStr)
                val files = root.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val first = files.getJSONObject(0)
                    return first.optString("id")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup file in Drive", e)
        }
        return null
    }
}
