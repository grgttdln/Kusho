package com.example.kusho.ml

/**
 * Configuration for time-series windows used as model input.
 */
object WindowConfig {
    // Number of time steps expected by the model.
    // TODO: Set these to the actual values reported by the model at runtime.
    // For now, assume 200 timesteps and 6 channels (ax, ay, az, gx, gy, gz).
    const val WINDOW_SIZE: Int = 200

    // Number of channels: ax, ay, az, gx, gy, gz.
    const val CHANNELS: Int = 6

    // Minimum samples before we attempt inference.
    const val MIN_SAMPLES: Int = 100

    // Sliding step between inferences in terms of new samples.
    const val SLIDING_STEP: Int = 8
}
