package com.example.app.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Utility object for secure password hashing using PBKDF2.
 *
 * PASSWORD SECURITY BEST PRACTICES:
 *
 * This implementation uses PBKDF2WithHmacSHA256
 *
 * For even better security in production, consider:
 * - Using Android Keystore for key management
 * - Implementing bcrypt or Argon2 via a library
 * - Adding pepper (application-level secret) in addition to salt
 */
object PasswordUtils {

    // Algorithm for password hashing
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    // Number of iterations - higher is more secure but slower
    // OWASP recommends at least 600,000 iterations for PBKDF2-SHA256 as of 2023
    // Using 120,000 as a balance between security and mobile performance
    private const val ITERATIONS = 120_000

    // Key length in bits
    private const val KEY_LENGTH = 256

    // Salt length in bytes (16 bytes = 128 bits)
    private const val SALT_LENGTH = 16

    /**
     * Generate a random salt for password hashing.
     * Each user should have a unique salt.
     *
     * @return Base64-encoded salt string
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Hash a password using PBKDF2 with the provided salt.
     *
     * @param password The plain text password to hash
     * @param salt The Base64-encoded salt
     * @return Base64-encoded password hash
     */
    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)

        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            ITERATIONS,
            KEY_LENGTH
        )

        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = factory.generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } finally {
            spec.clearPassword() // Clear sensitive data from memory
        }
    }

    /**
     * Verify a password against a stored hash.
     *
     * @param password The plain text password to verify
     * @param storedHash The stored password hash
     * @param salt The stored salt
     * @return true if the password matches, false otherwise
     */
    fun verifyPassword(password: String, storedHash: String, salt: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return constantTimeEquals(computedHash, storedHash)
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * A timing attack could otherwise reveal information about
     * the password by measuring how long the comparison takes.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) {
            return false
        }

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Validate password strength.
     *
     * @param password The password to validate
     * @return Pair of (isValid, errorMessage)
     */
    fun validatePasswordStrength(password: String): Pair<Boolean, String?> {
        return when {
            password.length < 8 -> false to "Password must be at least 8 characters"
            !password.any { it.isDigit() } -> false to "Password must contain at least one number"
            !password.any { it.isLetter() } -> false to "Password must contain at least one letter"
            else -> true to null
        }
    }
}

