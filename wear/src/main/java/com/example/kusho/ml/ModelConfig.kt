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
    val description: String = "",
    /**
     * Index offset for mapping model output indices to labels.
     * For models that output 52 classes but only use a subset:
     * - CAPITAL model: offset 0 (uses indices 0-25 for A-Z)
     * - small model: offset 26 (uses indices 26-51 mapped to labels 0-25)
     */
    val indexOffset: Int = 0
) {
    companion object {
        /**
         * Available model configurations.
         * The model file must exist in assets folder
         */
        val MODELS = listOf(
            ModelConfig(
                fileName = "tcn_multihead_model.tflite",
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
            ),
            ModelConfig(
                fileName = "tcn_multihead_model_CAPITAL_right.tflite",
                displayName = "Uppercase Alphabet Model",
                windowSize = 295,
                channels = 6,
                labels = listOf(
                    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                    "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
                ),
                description = "Specialized model for uppercase letters",
                indexOffset = 26  // Model outputs 52 classes, uppercase is at indices 26-51
            ),
            ModelConfig(
                fileName = "tcn_multihead_model_small_right.tflite",
                displayName = "Lowercase Alphabet Model",
                windowSize = 295,
                channels = 6,
                labels = listOf(
                    "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                    "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
                ),
                description = "Specialized model for lowercase letters"
                // No offset needed - this model outputs indices 0-25 for a-z
            )
        )

        /**
         * Get the default/primary model configuration
         */
        fun getDefault(): ModelConfig = MODELS.first()

        /**
         * Get model for Tutorial Mode based on letter case
         */
        fun getTutorialModeModel(letterCase: String): ModelConfig {
            return when (letterCase.lowercase()) {
                "capital", "uppercase" -> MODELS.find { it.fileName == "tcn_multihead_model_CAPITAL_right.tflite" } ?: getDefault()
                "small", "lowercase" -> MODELS.find { it.fileName == "tcn_multihead_model_small_right.tflite" } ?: getDefault()
                else -> getDefault()
            }
        }

        /**
         * Find a model config by filename
         */
        fun findByFileName(fileName: String): ModelConfig? =
            MODELS.find { it.fileName == fileName }
    }
}
