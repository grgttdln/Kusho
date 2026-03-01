# Model Integration

## Model Deployment

To integrate the air-writing recognition models, the TensorFlow models were converted into TensorFlow Lite (TFLite) models using the TFLiteConverter class from the tensorflow.lite package. Four specialized TFLite models were produced to accommodate different combinations of letter case and hand dominance: a Temporal Convolutional Network (TCN) architecture for right-hand models and a CNN-LSTM architecture for left-hand models. Each model was written into a separate .tflite file and moved into the assets directory of the Wear OS app. The build configuration specifies that TFLite files must not be compressed during packaging to preserve memory-mapped file access at runtime. The TFLite dependencies used are tensorflow-lite version 2.17.0 and tensorflow-lite-select-tf-ops version 2.16.1. The mapping between model file, architecture, target hand, and letter case can be seen in [Table X].

[Table X]
Mapping Between Model File and Configuration

| Model File                                      | Architecture | Hand  | Letter Case |
|-------------------------------------------------|-------------|-------|-------------|
| tcn_multihead_model_CAPITAL_right.tflite        | TCN         | Right | Uppercase   |
| tcn_multihead_model_small_right.tflite          | TCN         | Right | Lowercase   |
| complete_cnn-lstm_model_CAPITAL_left.tflite     | CNN-LSTM    | Left  | Uppercase   |
| complete_cnn-lstm_model_small_left.tflite       | CNN-LSTM    | Left  | Lowercase   |

At runtime, the appropriate model is selected automatically based on the user's dominant hand and the letter case of the current learning session. This selection is performed by the getModelForSession method of the ModelConfig class, which constructs the target filename from the hand and case parameters and retrieves the corresponding model configuration. If the parameters do not match any specialized model, the system falls back to a default complete alphabet model capable of recognizing all 52 letter classes.

Subsequently, an interface called AirWritingClassifier was created, and it was responsible for defining the contract for interfacing with the TFLite models. The interface exposes the model's expected window size (number of timesteps), number of input channels, number of output classes, class labels, and two classification methods: one that accepts raw sensor samples and one that accepts a preprocessed float array. The concrete implementation of this interface is the SimpleTFLiteClassifier class. To use the class, the users need to provide the application context and a ModelConfig object specifying the model file and its parameters. The users then could pass in sensor data into the classify function to get the predicted letter. The full implementation of the SimpleTFLiteClassifier class can be seen in [Appendix X].

The SimpleTFLiteClassifier loads the model file from the Wear OS assets directory using a FileInputStream and memory-maps it through a FileChannel for efficient access. The TFLite Interpreter is initialized from the memory-mapped buffer with an options configuration that allocates two threads for inference. Upon initialization, the classifier reads the actual input and output tensor shapes directly from the loaded model to verify they match the expected configuration. The input tensor is resized to match the shape [1, 295, 6] and tensors are reallocated accordingly. The model configuration also defines an index offset for specialized models, which is used during post-processing to map output indices to the correct letter labels.

Each model accepts an input tensor of shape [1, 295, 6] and produces an output tensor of shape [1, 52]. The input consists of a single batch containing 295 timesteps, each with 6 sensor channels representing three-axis accelerometer readings (ax, ay, az) and three-axis gyroscope readings (gx, gy, gz). The output contains 52 probability values corresponding to 52 letter classes. The mapping between the output float array indices and letter labels can be seen in [Table X].

[Table X]
Mapping Between Output Float Array Index and Letter Label

| Index Range | Letter Labels       |
|-------------|---------------------|
| 0–25        | a, b, c, ..., z (lowercase) |
| 26–51       | A, B, C, ..., Z (uppercase) |

For specialized models that recognize only uppercase or only lowercase letters, an index offset is applied during post-processing. Uppercase models use an offset of 26, meaning the model's output index is subtracted by 26 to map to the uppercase label array (A–Z at indices 0–25 in the labels list). Lowercase models use no offset, as their output indices 0–25 directly correspond to the lowercase labels (a–z).

## Model Execution

### Sensor Data Collection

The sensor data collection is managed by the MotionSensorManager class, which implements the Android SensorEventListener interface. The class registers listeners for both the accelerometer and the gyroscope on the Wear OS device, each configured at a sampling rate of 100 Hz (a sampling period of 10,000 microseconds). The recording is initiated when the user begins a gesture attempt and continues for a fixed duration of 3 seconds, yielding approximately 295 data points.

The accelerometer serves as the sampling driver for the data collection pipeline. Each time a new accelerometer event is received, a data row is created by pairing the accelerometer reading with the most recent gyroscope reading. This approach produces synchronized six-channel tuples of the form (ax, ay, az, gx, gy, gz), where ax, ay, and az represent the three-axis accelerometer values in m/s², and gx, gy, and gz represent the three-axis gyroscope values in rad/s. Each data row is stored as a SensorSample object with a nanosecond-precision timestamp. The collected samples are stored in a thread-safe mutable list and can be retrieved via the getCollectedSamples method.

### Motion Validation

Before classification, the system performs a motion validation check to ensure that the user performed a meaningful writing gesture. The hasSignificantMotion method computes two metrics from the collected gyroscope data: the variance of the gyroscope magnitude and the range (maximum minus minimum) of the gyroscope magnitude. The gyroscope magnitude for each sample is calculated as the Euclidean norm of the three gyroscope axes:

    |ω| = √(gx² + gy² + gz²)

The variance threshold is set at 0.3 and the range threshold at 1.0 rad/s. Both conditions must be satisfied for the gesture to be considered valid. If the motion is deemed insufficient, the system returns an unknown prediction ("?") instead of running model inference, preventing false predictions from stationary or minimal hand movements.

### Input Preprocessing

The prepareInput method of the SimpleTFLiteClassifier class transforms the collected sensor samples into the format expected by the model. The preprocessing pipeline consists of three stages: sample alignment, interleaving, and normalization.

In the sample alignment stage, the method adjusts the number of samples to match the model's expected window size of 295 timesteps. If more than 295 samples were collected, the method takes only the last 295 samples, aligning to the end of the gesture (post-gesture alignment). If fewer than 295 samples were collected, the remaining positions are zero-padded to fill the expected window size.

In the interleaving stage, the sensor samples are flattened into a single-dimension float array with 1,770 values (295 timesteps × 6 channels). The values are interleaved in the order [ax₀, ay₀, az₀, gx₀, gy₀, gz₀, ax₁, ay₁, az₁, gx₁, gy₁, gz₁, ...], where each group of six consecutive values represents one timestep.

In the normalization stage, per-channel Z-score standardization is applied using the normalizePerSample method. For each of the six sensor channels, the method computes the channel mean (μ) and standard deviation (σ) across all 295 timesteps, then transforms each value using the formula:

    x' = (x - μ) / (σ + ε)

where x is the original sensor value, μ is the mean of that channel across all timesteps, σ is the standard deviation of that channel computed as:

    σ = √((1/N) × Σ(xᵢ - μ)² + ε)

and ε = 1 × 10⁻⁶ is a small constant added for numerical stability to prevent division by zero. N represents the number of timesteps (295). This normalization ensures that each sensor channel has zero mean and unit variance, accounting for differences in scale between accelerometer values (m/s²) and gyroscope values (rad/s).

### Model Inference

The classifyRaw method of the SimpleTFLiteClassifier class performs model inference on the preprocessed input. The method first validates that the input float array contains exactly 1,770 values (295 × 6). The normalized float array is then loaded into a direct ByteBuffer allocated with 7,080 bytes (1,770 floats × 4 bytes each), configured in native byte order for compatibility with the TFLite runtime. A corresponding output ByteBuffer is allocated for 52 float values (208 bytes). The run method of the TFLite Interpreter is then called with the input and output ByteBuffers as arguments.

### Post-Processing and Prediction

To get the predicted letter from the output buffer, the method reads the 52 probability values from the output ByteBuffer into a float array. The method then finds the index of the probability with the highest value (argmax). For specialized models, the index offset defined in the model configuration is subtracted from this index to map it to the correct position in the labels list. For example, an uppercase model with an offset of 26 would subtract 26 from the raw output index, so output index 26 (representing "A" in the full 52-class output space) becomes label index 0, which maps to "A" in the uppercase labels list. If the adjusted index falls outside the valid label range, the method returns "?" as the predicted label.

The result is encapsulated in a PredictionResult object containing the predicted letter label, the raw label index, the confidence value (the maximum probability from the output array), the full probability array for all 52 classes, and a success flag. The full structure of the PredictionResult class can be seen in [Appendix X].

### Correctness Evaluation

The predicted letter is compared against the expected letter for the current learning task. The expected letter is determined by the target letter and the current letter case setting of the session. The comparison logic includes special handling for letters that have similar shapes in their uppercase and lowercase forms. The set of similar-shape letters can be seen in [Table X].

[Table X]
Letters with Similar Shapes in Uppercase and Lowercase Forms

| Letter | Uppercase | Lowercase |
|--------|-----------|-----------|
| C      | C         | c         |
| K      | K         | k         |
| O      | O         | o         |
| P      | P         | p         |
| S      | S         | s         |
| V      | V         | v         |
| W      | W         | w         |
| X      | X         | x         |
| Z      | Z         | z         |

For letters in the similar-shape set, the comparison is performed case-insensitively, accepting either case as correct. For all other letters, the comparison is case-sensitive, requiring the predicted letter to match the exact case of the expected letter. The correctness result, along with the predicted letter, is transmitted from the Wear OS device to the phone application, which serves as the authoritative source for providing feedback to the user.

### Integration with Learning Modes

The model is integrated into two learning modes on the Wear OS device: Tutorial Mode and Learn Mode. Both modes follow the same state progression for gesture recognition: IDLE → COUNTDOWN → RECORDING → PROCESSING → SHOWING_PREDICTION.

In Tutorial Mode, managed by the TutorialModeViewModel, the user is guided through a sequence of individual letters. A 3-second countdown prepares the user, followed by a 3-second recording window during which the sensor data is collected. After recording, the collected samples are classified and the result is evaluated against the target letter. The result is displayed on the watch and transmitted to the phone for authoritative feedback.

In Learn Mode, managed by the LearnModeViewModel, the user practices air-writing in the context of word-based exercises such as fill-in-the-blank and write-the-word activities. The recording and classification pipeline is identical to Tutorial Mode, but the phone application determines correctness based on the current word and masked letter position. Learn Mode also includes a phone feedback timeout of 8 seconds, after which the watch proceeds independently if no response is received.

In both modes, the model is loaded when the screen is entered using the ModelLoader class, which validates the model file exists in the assets folder before instantiating the SimpleTFLiteClassifier. The model resources are released when the screen is disposed to prevent memory leaks. The model selection is performed at screen entry based on the session's letter case and the user's dominant hand preference.
