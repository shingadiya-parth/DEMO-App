package com.example.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object AuthCryptoHelper {

    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash == expectedHash
    }

    fun generateResetToken(): String {
        return (100000 + secureRandom.nextInt(900000)).toString()
    }

    fun generateReferralCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
        return "PLAY$code"
    }

    fun generateUserId(): String {
        return "usr_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    }
}
