package com.example.ui.util

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    // Primary key: 16 bytes for AES-128
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

        // 1. If it is already plain JSON
        if (isJson(trimmed)) {
            return trimmed
        }

        // 2. Try unescaping JSON string quotes (e.g. \"wallets\": [...])
        val unescaped = unescapeJsonString(trimmed)
        if (isJson(unescaped)) {
            return unescaped
        }

        // 3. Try decrypting from Base64 or Hex String
        val candidateStrings = listOf(
            trimmed,
            trimmed.replace("\r", "").replace("\n", "").replace(" ", "+").replace("\t", ""),
            unescaped
        ).distinct()

        for (cand in candidateStrings) {
            val dec = decryptStringCandidates(cand)
            if (dec.isNotEmpty() && isJson(dec)) {
                return dec
            }
        }

        return ""
    }

    fun decryptBytes(rawBytes: ByteArray): String {
        if (rawBytes.isEmpty()) return ""

        // 1. Try parsing as UTF-8 string first
        try {
            val str = String(rawBytes, Charsets.UTF_8).trim()
            if (isJson(str)) {
                return str
            }
            val fromStr = decrypt(str)
            if (fromStr.isNotEmpty() && isJson(fromStr)) {
                return fromStr
            }
        } catch (_: Exception) {}

        // 2. Try GZIP / Deflate decompression
        try {
            val decomp = decompress(rawBytes)
            if (decomp != null) {
                val str = String(decomp, Charsets.UTF_8).trim()
                if (isJson(str)) return str
                val fromStr = decrypt(str)
                if (fromStr.isNotEmpty() && isJson(fromStr)) return fromStr
            }
        } catch (_: Exception) {}

        // 3. Try Raw Cipher Decryption on the bytes directly
        val rawDecrypted = decryptRawBytes(rawBytes)
        if (rawDecrypted.isNotEmpty() && isJson(rawDecrypted)) {
            return rawDecrypted
        }

        return ""
    }

    private fun isJson(text: String): Boolean {
        val t = text.trim()
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))
    }

    private fun unescapeJsonString(s: String): String {
        return try {
            if (s.contains("\\\"") || s.contains("\\n") || s.contains("\\r")) {
                s.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
            } else {
                s
            }
        } catch (e: Exception) {
            s
        }
    }

    private fun decryptStringCandidates(input: String): String {
        val base64Flags = listOf(Base64.DEFAULT, Base64.NO_WRAP, Base64.URL_SAFE)
        
        // Try decoding Base64
        for (flag in base64Flags) {
            try {
                val decodedBytes = Base64.decode(input, flag)
                if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                    // Check if decoded bytes is plain JSON or GZIP
                    val asUtf8 = try { String(decodedBytes, Charsets.UTF_8).trim() } catch (_: Exception) { "" }
                    if (isJson(asUtf8)) return asUtf8
                    
                    val decomp = decompress(decodedBytes)
                    if (decomp != null) {
                        val dStr = String(decomp, Charsets.UTF_8).trim()
                        if (isJson(dStr)) return dStr
                    }

                    val rawDec = decryptRawBytes(decodedBytes)
                    if (rawDec.isNotEmpty() && isJson(rawDec)) return rawDec
                }
            } catch (_: Exception) {}
        }

        // Try Hex decoding
        try {
            val hexBytes = hexStringToByteArray(input)
            if (hexBytes != null && hexBytes.isNotEmpty()) {
                val rawDec = decryptRawBytes(hexBytes)
                if (rawDec.isNotEmpty() && isJson(rawDec)) return rawDec
            }
        } catch (_: Exception) {}

        return ""
    }

    private fun decryptRawBytes(bytes: ByteArray): String {
        val keyCandidates = buildKeyCandidates()
        val transformations = listOf(
            "AES/ECB/PKCS5Padding",
            "AES",
            "AES/ECB/PKCS7Padding",
            "AES/CBC/PKCS5Padding",
            "AES/CTR/NoPadding"
        )

        for (key in keyCandidates) {
            val secretKey = SecretKeySpec(key, ALGORITHM)
            
            // 1. ECB / Default modes
            for (trans in transformations) {
                if (trans.contains("/CBC/")) {
                    // CBC with Zero IV
                    try {
                        val cipher = Cipher.getInstance(trans)
                        val ivSpec = IvParameterSpec(ByteArray(16))
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                        val decrypted = cipher.doFinal(bytes)
                        val res = parseDecryptedBytes(decrypted)
                        if (res.isNotEmpty()) return res
                    } catch (_: Exception) {}

                    // CBC with first 16 bytes as IV
                    if (bytes.size > 16) {
                        try {
                            val iv = bytes.copyOfRange(0, 16)
                            val cipherBytes = bytes.copyOfRange(16, bytes.size)
                            val cipher = Cipher.getInstance(trans)
                            val ivSpec = IvParameterSpec(iv)
                            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                            val decrypted = cipher.doFinal(cipherBytes)
                            val res = parseDecryptedBytes(decrypted)
                            if (res.isNotEmpty()) return res
                        } catch (_: Exception) {}
                    }
                } else if (trans.contains("/CTR/")) {
                    try {
                        val cipher = Cipher.getInstance(trans)
                        val ivSpec = IvParameterSpec(ByteArray(16))
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                        val decrypted = cipher.doFinal(bytes)
                        val res = parseDecryptedBytes(decrypted)
                        if (res.isNotEmpty()) return res
                    } catch (_: Exception) {}
                } else {
                    // ECB modes
                    try {
                        val cipher = Cipher.getInstance(trans)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey)
                        val decrypted = cipher.doFinal(bytes)
                        val res = parseDecryptedBytes(decrypted)
                        if (res.isNotEmpty()) return res
                    } catch (_: Exception) {}
                }
            }

            // 2. GCM Mode
            if (bytes.size > 12) {
                try {
                    val iv = bytes.copyOfRange(0, 12)
                    val cipherBytes = bytes.copyOfRange(12, bytes.size)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    val decrypted = cipher.doFinal(cipherBytes)
                    val res = parseDecryptedBytes(decrypted)
                    if (res.isNotEmpty()) return res
                } catch (_: Exception) {}
            }
        }

        return ""
    }

    private fun parseDecryptedBytes(decrypted: ByteArray): String {
        // Try plain UTF-8
        try {
            val str = String(decrypted, Charsets.UTF_8).trim()
            if (isJson(str)) return str
        } catch (_: Exception) {}

        // Try decompressing GZIP / Deflate
        try {
            val decomp = decompress(decrypted)
            if (decomp != null) {
                val str = String(decomp, Charsets.UTF_8).trim()
                if (isJson(str)) return str
            }
        } catch (_: Exception) {}

        return ""
    }

    private fun buildKeyCandidates(): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(KEY_BYTES)
        list.add(ALT_KEY_BYTES)

        val rawKeyStrings = listOf(
            "DuitKuSecureKeY_",
            "DuitKuSecureKey!",
            "DuitKuSecureKey",
            "DuitKuSecretKey!",
            "DuitKuSecretKey",
            "DuitKuKey2024!",
            "DuitKuKey2025!",
            "DuitKuKey2026!",
            "DuitKuMasterKey!",
            "DuitKuMasterKey",
            "DuitKuBackupKey!",
            "DuitKuBackupKey",
            "DuitKu_Key_2024",
            "DuitKu_Key_2025",
            "DuitKu_Key_2026",
            "DuitKuAppKey2024",
            "DuitKuAppKey2025",
            "DuitKuAppKey2026",
            "com.example.duitku",
            "com.example",
            "DuitKuFinanceApp",
            "DuitKu"
        )

        for (s in rawKeyStrings) {
            val b = s.toByteArray(Charsets.UTF_8)
            // 16 bytes
            if (b.size >= 16) {
                list.add(b.copyOfRange(0, 16))
            } else {
                val padded = ByteArray(16)
                System.arraycopy(b, 0, padded, 0, b.size)
                list.add(padded)
            }
            // 24 bytes
            if (b.size >= 24) {
                list.add(b.copyOfRange(0, 24))
            }
            // 32 bytes
            if (b.size >= 32) {
                list.add(b.copyOfRange(0, 32))
            }

            // MD5 of string (16 bytes)
            try {
                val md5 = MessageDigest.getInstance("MD5").digest(b)
                list.add(md5)
            } catch (_: Exception) {}

            // SHA-256 of string (32 bytes)
            try {
                val sha = MessageDigest.getInstance("SHA-256").digest(b)
                list.add(sha)
            } catch (_: Exception) {}
        }

        return list.distinctBy { it.toList() }
    }

    private fun decompress(bytes: ByteArray): ByteArray? {
        // Try GZIP
        try {
            ByteArrayInputStream(bytes).use { bis ->
                GZIPInputStream(bis).use { gis ->
                    return gis.readBytes()
                }
            }
        } catch (_: Exception) {}

        // Try Inflater / ZLIB
        try {
            ByteArrayInputStream(bytes).use { bis ->
                InflaterInputStream(bis).use { iis ->
                    return iis.readBytes()
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun hexStringToByteArray(s: String): ByteArray? {
        val clean = s.trim().replace(" ", "").replace(":", "")
        if (clean.length % 2 != 0) return null
        val len = clean.length
        val data = ByteArray(len / 2)
        try {
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
                i += 2
            }
            return data
        } catch (_: Exception) {
            return null
        }
    }

    fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.lowercase().trim().toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }
}
