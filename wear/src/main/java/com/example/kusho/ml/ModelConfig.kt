package com.example.kusho.ml

/**
 * Configuration for different model versions.
 * Add new model configurations here to support different models.
 */
data class ModelConfig(
    val fileName: String,
    val displayName: String,
    val windowSize: Int,
    val channels: Int,
    val labels: List<String>,
    val description: String = ""
) {
    companion object {
        /**
         * Available model configurations.
         * The model file must exist in assets folder
         */
        val MODELS = listOf(
            ModelConfig(
                fileName = "complete_model_1.tflite",
                displayName = "Complete Alphabet Model",
                windowSize = 295,  // 3 seconds at 100Hz (Model expects 100Hz)
                channels = 6,      // ax, ay, az, gx, gy, gz
                labels = listOf(
                    // Lowercase letters a-z (indices 0-25)
                    "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                    "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                    // Uppercase letters A-Z (indices 26-51)
                    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                    "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
                ),
                description = "Complete uppercase and lowercase alphabet recognition model"
            )
        )

        /**
         * Get the default/primary model configuration
         */
        fun getDefault(): ModelConfig = MODELS.first()

        /**
         * Find a model config by filename
         */
        fun findByFileName(fileName: String): ModelConfig? =
            MODELS.find { it.fileName == fileName }
    }
}
