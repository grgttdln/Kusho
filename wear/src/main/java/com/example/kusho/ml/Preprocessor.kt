package com.example.kusho.ml

import com.example.kusho.sensors.SensorSample

/**
 * Converts recent [SensorSample]s into a normalized FloatArray suitable
 * for input to the TFLite CNN-LSTM model.
 */
class Preprocessor {

    fun hasEnoughData(samples: List<SensorSample>, requiredSamples: Int): Boolean =
        samples.size >= requiredSamples

    /**
     * Prepare a windowed, normalized input array matching the model's expected
     * windowSize and channel count.
     */
    fun prepareInput(samples: List<SensorSample>, windowSize: Int, channels: Int): FloatArray {
        val window = if (samples.size >= windowSize) {
            samples.takeLast(windowSize)
        } else {
            samples
        }

        val input = FloatArray(windowSize * channels)
        var idx = 0

        for (t in 0 until windowSize) {
            val s = if (t < window.size) window[t] else null

            val ax = s?.ax ?: 0f
            val ay = s?.ay ?: 0f
            val az = s?.az ?: 0f
            val gx = s?.gx ?: 0f
            val gy = s?.gy ?: 0f
            val gz = s?.gz ?: 0f

            input[idx++] = Normalization.normAcc(ax)
            input[idx++] = Normalization.normAcc(ay)
            input[idx++] = Normalization.normAcc(az)
            input[idx++] = Normalization.normGyro(gx)
            input[idx++] = Normalization.normGyro(gy)
            input[idx++] = Normalization.normGyro(gz)
        }

        return input
    }
}
