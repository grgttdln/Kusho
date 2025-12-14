package com.example.kusho.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Wrapper around the cnn_lstm_watch.tflite model for air-writing
 * character recognition on Wear OS.
 */
class TFLiteAirWritingClassifier(
    context: Context,
    numThreads: Int = 2
) {

    data class PredictionResult(
        val label: String?,
        val confidence: Float,
        val probs: FloatArray
    )

    private val interpreter: Interpreter
    private val numClasses: Int
    val modelWindowSize: Int
    val modelChannels: Int

    init {
        val options = Interpreter.Options().apply {
            this.numThreads = numThreads
        }

        val modelBuffer = loadModelFile(context, MODEL_FILE_NAME)
        interpreter = try {
            Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create TFLite Interpreter: ${'$'}{e.message}", e)
        }

        // Read and log input shape.
        val inputShape: IntArray = try {
            interpreter.getInputTensor(0).shape()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to read input tensor shape: ${'$'}{e.message}", e)
        }

        val rank = inputShape.size
        if (rank != 3) {
            throw IllegalStateException("Unexpected input tensor rank ${'$'}rank, expected 3. Actual shape=${'$'}{inputShape.contentToString()}")
        }

        modelWindowSize = inputShape[1]
        modelChannels = inputShape[2]

        // Log mismatch instead of throwing so we can still initialize and see behavior.
        if (modelWindowSize != WindowConfig.WINDOW_SIZE || modelChannels != WindowConfig.CHANNELS) {
            Log.w(
                TAG,
                "Model expects WINDOW_SIZE=${'$'}modelWindowSize, CHANNELS=${'$'}modelChannels but WindowConfig is WINDOW_SIZE=${'$'}{WindowConfig.WINDOW_SIZE}, CHANNELS=${'$'}{WindowConfig.CHANNELS}"
            )
        }

        val outputShape: IntArray = try {
            interpreter.getOutputTensor(0).shape()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to read output tensor shape: ${'$'}{e.message}", e)
        }
        if (outputShape.size != 2 || outputShape[0] != 1) {
            throw IllegalStateException("Unexpected output tensor shape: ${'$'}{outputShape.contentToString()}")
        }
        numClasses = outputShape[1]
    }

    /**
     * Classify a preprocessed window.
     *
     * @param window FloatArray of size WINDOW_SIZE * CHANNELS.
     */
    fun classify(window: FloatArray): PredictionResult {
        // Use modelWindowSize/modelChannels instead of WindowConfig to avoid crashes if they differ.
        val expectedSize = modelWindowSize * modelChannels
        require(window.size == expectedSize) {
            "Unexpected input size: ${'$'}{window.size}, expected ${'$'}expectedSize (WINDOW_SIZE=${'$'}modelWindowSize, CHANNELS=${'$'}modelChannels})"
        }

        val t = modelWindowSize
        val c = modelChannels
        val input3d = Array(1) { Array(t) { FloatArray(c) } }

        var idx = 0
        for (i in 0 until t) {
            for (j in 0 until c) {
                input3d[0][i][j] = window[idx++]
            }
        }

        val output = Array(1) { FloatArray(numClasses) }
        interpreter.run(input3d, output)

        val probs = output[0]
        var maxIdx = 0
        var maxVal = Float.NEGATIVE_INFINITY
        for (i in probs.indices) {
            if (probs[i] > maxVal) {
                maxVal = probs[i]
                maxIdx = i
            }
        }

        val label = Labels.CHARACTERS.getOrNull(maxIdx)
        return PredictionResult(label = label, confidence = maxVal, probs = probs.copyOf())
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "cnn_lstm_watch.tflite"
        private const val TAG = "TFLiteClassifier"

        private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
            return try {
                val fileDescriptor = context.assets.openFd(modelFileName)
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel: FileChannel = inputStream.channel
                    fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileDescriptor.startOffset,
                        fileDescriptor.declaredLength
                    )
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load model file '$MODEL_FILE_NAME' from assets: ${'$'}{e.message}", e)
            }
        }
    }
}
