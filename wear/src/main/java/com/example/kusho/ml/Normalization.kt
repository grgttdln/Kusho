package com.example.kusho.ml

/**
 * Simple normalization helpers. Replace constants with those used during training
 * if available to maximize model accuracy.
 */
object Normalization {
    private const val ACC_MAX_ABS = 20f  // m/s^2, example value
    private const val GYRO_MAX_ABS = 500f // deg/s, example value

    fun normAcc(v: Float): Float = (v / ACC_MAX_ABS).coerceIn(-1f, 1f)

    fun normGyro(v: Float): Float = (v / GYRO_MAX_ABS).coerceIn(-1f, 1f)
}

