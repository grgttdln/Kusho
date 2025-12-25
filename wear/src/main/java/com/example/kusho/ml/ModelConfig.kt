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
         * The model file must exist in assets folder.
         * 
         * Note: model.tflite appears to use Flex ops (TensorListReserve), 
         * so the app must include 'org.tensorflow:tensorflow-lite-select-tf-ops'.
         */
        val MODELS = listOf(
            ModelConfig(
                fileName = "model2.tflite",
                displayName = "CNN-LSTM v1",
                windowSize = 295,  // 3 seconds at 100Hz (Model expects 100Hz)
                channels = 6,      // ax, ay, az, gx, gy, gz
                labels = listOf("a", "e", "i", "o", "u"),
                description = "Main vowel recognition model"
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
