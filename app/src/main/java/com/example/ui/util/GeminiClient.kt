package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val TAG = "GeminiClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ScanResult(
        val amount: Double,
        val type: String, // "EXPENSE" or "INCOME"
        val note: String
    )

    suspend fun scanMultipleReceipts(context: Context, uris: List<Uri>): List<ScanResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or still a placeholder.")
            throw Exception("API Key Gemini belum disetel. Silakan masukkan kunci API Gemini di Panel Rahasia (Secrets).")
        }

        val allResults = mutableListOf<ScanResult>()

        for (uri in uris) {
            val bitmap = loadOptimizedBitmap(context, uri) ?: continue
            val base64Image = bitmap.toResizedBase64()

            val payload = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val textPart = JSONObject().apply {
                                put("text", """
                                    Analisis foto ini. Foto ini dapat berisi satu atau beberapa nota/kwitansi belanja sekaligus secara bersamaan (misal ditata berdampingan atau bertumpuk). Temukan semua struk/kwitansi belanja yang terdeteksi di dalam foto ini. Untuk setiap struk yang teratur, ekstrak:
                                    - amount: total belanja (Double)
                                    - type: tipe transaksi ("EXPENSE" atau "INCOME", biasanya EXPENSE)
                                    - note: nama toko dan barang utama yang dibeli (String).

                                    Kembalikan tanggapan hanya dalam format JSON ARRAY seperti contoh berikut:
                                    [
                                      {
                                        "amount": 45000.0,
                                        "type": "EXPENSE",
                                        "note": "Kopi Susu di Kopi Kenangan"
                                      },
                                      {
                                        "amount": 120000.0,
                                        "type": "EXPENSE",
                                        "note": "Bahan Pokok di Indomaret"
                                      }
                                    ]
                                    Harap pastikan jumlah/amount berupa nilai numerik biasa murni tanpa format Rp atau titik ribu.
                                    Kembalikan HANYA teks JSON ARRAY tersebut tanpa prefiks markdown penjelasan lainnya.
                                """.trimIndent())
                            }
                            val imagePart = JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineData)
                            }
                            put(textPart)
                            put(imagePart)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are an expert Indonesian receipt analyzer. You detect all receipts in an image and return them as a raw, valid JSON Array containing objects with amount (Double), type (String), and note (String).")
                        })
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)

                val generationConfigObj = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1f)
                }
                put("generationConfig", generationConfigObj)
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Response failed for URI $uri: $errorBody")
                    continue
                }

                val responseBody = response.body?.string() ?: continue
                Log.d(TAG, "Raw response of multi-scan: $responseBody")

                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) continue

                val textResult = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val firstBracket = textResult.indexOf('[')
                val lastBracket = textResult.lastIndexOf(']')
                val cleanedJson = if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                    textResult.substring(firstBracket, lastBracket + 1).trim()
                } else {
                    textResult.trim()
                }

                Log.d(TAG, "Cleaned JSON Array extracted: $cleanedJson")

                val jsonArray = JSONArray(cleanedJson)
                for (i in 0 until jsonArray.length()) {
                    val parsedOutput = jsonArray.getJSONObject(i)
                    val amountRaw = parsedOutput.opt("amount")
                    val amount = when (amountRaw) {
                        is Number -> amountRaw.toDouble()
                        is String -> {
                            val cleaned = amountRaw.replace(Regex("[^0-9.,]"), "")
                            if (cleaned.contains(",") && cleaned.contains(".")) {
                                val firstComma = cleaned.indexOf(",")
                                val firstDot = cleaned.indexOf(".")
                                if (firstComma < firstDot) {
                                    cleaned.replace(",", "").toDoubleOrNull() ?: 0.0
                                } else {
                                    cleaned.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                                }
                            } else if (cleaned.contains(",")) {
                                val parts = cleaned.split(",")
                                if (parts.size == 2 && parts[1].length == 3) {
                                    cleaned.replace(",", "").toDoubleOrNull() ?: 0.0
                                } else {
                                    cleaned.replace(",", ".").toDoubleOrNull() ?: 0.0
                                }
                            } else if (cleaned.contains(".")) {
                                val parts = cleaned.split(".")
                                if (parts.size == 2 && parts[1].length == 3) {
                                    cleaned.replace(".", "").toDoubleOrNull() ?: 0.0
                                } else {
                                    cleaned.toDoubleOrNull() ?: 0.0
                                }
                            } else {
                                cleaned.toDoubleOrNull() ?: 0.0
                            }
                        }
                        else -> 0.0
                    }

                    val type = parsedOutput.optString("type", "EXPENSE").uppercase()
                    val noteText = parsedOutput.optString("note", "Pindaan Nota")
                    allResults.add(ScanResult(amount, type, noteText))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning URI $uri", e)
            }
        }
        allResults
    }

    suspend fun scanReceipt(context: Context, uri: Uri): ScanResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or still a placeholder.")
            throw Exception("API Key Gemini belum disetel. Sila masukkan kunci API Gemini di Panel Rahasia (Secrets).")
        }

        // 1. Get Bitmap and optimize it (max 1024 size)
        val bitmap = loadOptimizedBitmap(context, uri) ?: throw Exception("Gagal memuat gambar nota.")
        val base64Image = bitmap.toResizedBase64()

        // 2. Prepare JSON payload matching standard Direct REST API specifications
        val payload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        // Instruction Prompt
                        val textPart = JSONObject().apply {
                            put("text", """
                                Analisis foto nota / resi ini. Ekstrak nilai total belanja (amount), jenis transaksi (type: "EXPENSE" atau "INCOME", biasanya EXPENSE untuk belanja), dan catatan ringkas nama toko serta nama barang/jasa utama yang dibeli (note).
                                Sila kembalikan tanggapan hanya dalam format objek JSON seperti ini:
                                {
                                  "amount": 45000.0,
                                  "type": "EXPENSE",
                                  "note": "Kopi Susu dan Donat di Kopi Kenangan"
                                }
                                Harap pastikan jumlah/amount diekstrak sebagai angka numerik biasa tanpa tanda titik ribu atau mata uang (misal 50000.0 bukannya Rp 50.000).
                                Kembalikan HANYA teks JSON tersebut tanpa prefiks ```json atau format markdown penjelasan lainnya.
                            """.trimIndent())
                        }
                        // Image raw inline data Part
                        val imagePart = JSONObject().apply {
                            val inlineData = JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }
                            put("inlineData", inlineData)
                        }
                        put(textPart)
                        put(imagePart)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Use systemInstruction for best system-level schema alignment
            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are an expert Indonesian receipt analyzer. You return raw, valid JSON only containing amount (Double), type (String), and note (String).")
                    })
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemInstructionObj)
            
            // Explicitly request JSON Response schema via responseMimeType
            val generationConfigObj = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1f) // low temperature for precise factual extraction
            }
            put("generationConfig", generationConfigObj)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Response failed: $errorBody")
                throw Exception("Gagal memanggil Gemini API: Kode ${response.code}. Sila periksa kunci API Anda.")
            }

            val responseBody = response.body?.string() ?: throw Exception("Respons Gemini kosong.")
            Log.d(TAG, "Raw response: $responseBody")

            // Parse response
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw Exception("Tidak ada kandidat teks dari Gemini.")
            }

            val textResult = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            // Extremely robust JSON block extraction using brace detection
            val firstBrace = textResult.indexOf('{')
            val lastBrace = textResult.lastIndexOf('}')
            val cleanedJson = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                textResult.substring(firstBrace, lastBrace + 1).trim()
            } else {
                textResult.trim()
            }

            Log.d(TAG, "Cleaned JSON extracted: $cleanedJson")

            val parsedOutput = JSONObject(cleanedJson)
            
            // Robust parsing of amount (can be Double, Int, or String like "22,500" or "Rp 22.500")
            val amountRaw = parsedOutput.opt("amount")
            val amount = when (amountRaw) {
                is Number -> amountRaw.toDouble()
                is String -> {
                    // Extract numeric parts and convert
                    val cleaned = amountRaw.replace(Regex("[^0-9.,]"), "")
                    if (cleaned.contains(",") && cleaned.contains(".")) {
                        val firstComma = cleaned.indexOf(",")
                        val firstDot = cleaned.indexOf(".")
                        if (firstComma < firstDot) {
                            cleaned.replace(",", "").toDoubleOrNull() ?: 0.0
                        } else {
                            cleaned.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                        }
                    } else if (cleaned.contains(",")) {
                        val parts = cleaned.split(",")
                        if (parts.size == 2 && parts[1].length == 3) {
                            cleaned.replace(",", "").toDoubleOrNull() ?: 0.0
                        } else {
                            cleaned.replace(",", ".").toDoubleOrNull() ?: 0.0
                        }
                    } else if (cleaned.contains(".")) {
                        val parts = cleaned.split(".")
                        if (parts.size == 2 && parts[1].length == 3) {
                            // Dot is thousand separator (e.g. 22.500)
                            cleaned.replace(".", "").toDoubleOrNull() ?: 0.0
                        } else {
                            cleaned.toDoubleOrNull() ?: 0.0
                        }
                    } else {
                        cleaned.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> 0.0
            }
            
            val type = parsedOutput.optString("type", "EXPENSE").uppercase()
            val noteText = parsedOutput.optString("note", "Scan Nota")

            ScanResult(amount, type, noteText)

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning receipt code", e)
            throw e
        }
    }

    private fun loadOptimizedBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun Bitmap.toResizedBase64(): String {
        val maxDimension = 1024
        val width = this.width
        val height = this.height
        val (newWidth, newHeight) = if (width > height) {
            if (width > maxDimension) {
                val ratio = maxDimension.toFloat() / width
                (maxDimension to (height * ratio).toInt())
            } else {
                (width to height)
            }
        } else {
            if (height > maxDimension) {
                val ratio = maxDimension.toFloat() / height
                ((width * ratio).toInt() to maxDimension)
            } else {
                (width to height)
            }
        }

        val resized = Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
