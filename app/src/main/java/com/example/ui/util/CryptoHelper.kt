package com.example.ui.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    // We pad the key to have exactly 16 bytes for AES-128
    private val KEY_BYTES = "DuitKuSecureKeY_Key12".take(16).toByteArray(Charsets.UTF_8)
    private val ALT_KEY_BYTES = "DuitKuSecureKeY_".toByteArray(Charsets.UTF_8)

    fun encrypt(plainText: String): String {
        return try {
            val secretKey = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            try {
                val secretKey = SecretKeySpec(KEY_BYTES, ALGORITHM)
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            } catch (e2: Exception) {
                e2.printStackTrace()
                ""
            }
        }
    }

    fun decrypt(encryptedText: String): String {
        val trimmed = encryptedText.trim().removePrefix("\"").removeSuffix("\"").trim()
        if (trimmed.isEmpty()) return ""

        // If it is already plain JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed
        }

        val cleanedText = trimmed.replace("\r", "").replace("\n", "").replace(" ", "+")
        val keyCandidates = listOf(KEY_BYTES, ALT_KEY_BYTES)
        val transformations = listOf("AES/ECB/PKCS5Padding", "AES", "AES/ECB/PKCS7Padding")
        val base64Flags = listOf(Base64.DEFAULT, Base64.NO_WRAP, Base64.URL_SAFE)

        for (key in keyCandidates) {
            val secretKey = SecretKeySpec(key, ALGORITHM)
            for (trans in transformations) {
                for (flags in base64Flags) {
                    try {
                        val cipher = Cipher.getInstance(trans)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey)
                        val decodedBytes = Base64.decode(cleanedText, flags)
                        val decryptedBytes = cipher.doFinal(decodedBytes)
                        val result = String(decryptedBytes, Charsets.UTF_8).trim()
                        if (result.startsWith("{") || result.startsWith("[")) {
                            return result
                        }
                    } catch (_: Exception) {
                        // Continue trying other combinations
                    }
                }
            }
        }

        return ""
    }

    fun md5(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(input.lowercase().trim().toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }
}
