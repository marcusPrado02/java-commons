package com.marcusprado02.commons.adapters.ml.tensorflow;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.ml.BatchPrediction;
import com.marcusprado02.commons.ports.ml.MLModel;
import com.marcusprado02.commons.ports.ml.ModelMetadata;
import com.marcusprado02.commons.ports.ml.Prediction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.types.TFloat32;

/**
 * TensorFlow implementation of MLModel.
 *
 * <p>Supports loading TensorFlow SavedModel format and performing inference using the Java
 * TensorFlow API.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TensorFlowModel model = TensorFlowModel.load(Path.of("model/saved_model"));
 *
 * Map<String, Object> features = Map.of(
 *     "input", new float[]{1.0f, 2.0f, 3.0f}
 * );
 *
 * Result<Prediction> result = model.predict(features);
 * model.close();  // Important: release resources
 * }</pre>
 */
public class TensorFlowModel implements MLModel, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(TensorFlowModel.class);

  private final SavedModelBundle model;
  private final ModelMetadata metadata;
  private final String inputTensorName;
  private final String outputTensorName;
  private volatile boolean closed = false;

  private TensorFlowModel(
      SavedModelBundle model,
      ModelMetadata metadata,
      String inputTensorName,
      String outputTensorName) {
    this.model = model;
    this.metadata = metadata;
    this.inputTensorName = inputTensorName;
    this.outputTensorName = outputTensorName;
  }

  /**
   * Loads a TensorFlow SavedModel from a directory.
   *
   * @param modelPath Path to the SavedModel directory
   * @return Loaded TensorFlow model wrapped in Result
   */
  public static Result<TensorFlowModel> load(Path modelPath) {
    return load(modelPath, "serving_default", "input", "output");
  }

  /**
   * Loads a TensorFlow SavedModel with custom tensor names.
   *
   * @param modelPath Path to the SavedModel directory
   * @param signatureDefKey Signature definition key (usually "serving_default")
   * @param inputTensorName Name of the input tensor
   * @param outputTensorName Name of the output tensor
   * @return Loaded TensorFlow model wrapped in Result
   */
  public static Result<TensorFlowModel> load(
      Path modelPath, String signatureDefKey, String inputTensorName, String outputTensorName) {
    try {
      if (!Files.exists(modelPath)) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("ML.MODEL_NOT_FOUND"),
                ErrorCategory.NOT_FOUND,
                Severity.ERROR,
                "Model not found at: " + modelPath));
      }

      logger.info("Loading TensorFlow model from: {}", modelPath);
      SavedModelBundle bundle = SavedModelBundle.load(modelPath.toString(), signatureDefKey);

      ModelMetadata metadata =
          ModelMetadata.builder()
              .name(modelPath.getFileName().toString())
              .version("1.0")
              .framework("tensorflow")
              .createdAt(Instant.now())
              .build();

      TensorFlowModel model =
          new TensorFlowModel(bundle, metadata, inputTensorName, outputTensorName);
      logger.info("Successfully loaded TensorFlow model: {}", metadata.name());

      return Result.ok(model);

    } catch (Exception e) {
      logger.error("Failed to load TensorFlow model: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("ML.MODEL_LOAD_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to load model: " + e.getMessage()));
    }
  }

  @Override
  public Result<Prediction> predict(Map<String, Object> features) {
    if (closed) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("ML.MODEL_CLOSED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Model has been closed"));
    }

    try {
      // Validate input
      Result<Void> validation = validateInput(features);
      if (validation.isFail()) {
        return Result.fail(validation.problemOrNull());
      }

      // Convert features to tensor
      Object inputData = features.get(inputTensorName);
      if (inputData == null) {
        inputData = features.values().iterator().next(); // Use first feature if no matching key
      }

      try (TFloat32 inputTensor = convertToTensor(inputData)) {
        // Run inference
        Session.Runner runner = model.session().runner();
        runner.feed(inputTensorName, inputTensor);

        // TensorFlow's Result is AutoCloseable and contains the output tensors
        try (org.tensorflow.Result outputs = runner.fetch(outputTensorName).run()) {
          if (outputs.size() == 0) {
            return Result.fail(
                Problem.of(
                    ErrorCode.of("ML.PREDICTION_FAILED"),
                    ErrorCategory.TECHNICAL,
                    Severity.ERROR,
                    "No output tensor returned"));
          }

          // Convert output to prediction
          try (Tensor outputTensor = outputs.get(0)) {
            Prediction prediction = tensorToPrediction(outputTensor, features);
            return Result.ok(prediction);
          }
        }
      }

    } catch (Exception e) {
      logger.error("Prediction failed: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("ML.PREDICTION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Prediction failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<BatchPrediction> predictBatch(List<Map<String, Object>> featureList) {
    Instant startTime = Instant.now();
    List<Prediction> predictions = new ArrayList<>();

    for (Map<String, Object> features : featureList) {
      Result<Prediction> result = predict(features);
      if (result.isOk()) {
        predictions.add(result.getOrNull());
      } else {
        logger.warn("Batch prediction failed for one item: {}", result.problemOrNull());
      }
    }

    Instant endTime = Instant.now();

    BatchPrediction batch =
        BatchPrediction.builder()
            .predictions(predictions)
            .startedAt(startTime)
            .completedAt(endTime)
            .build();

    return Result.ok(batch);
  }

  @Override
  public Result<ModelMetadata> getMetadata() {
    return Result.ok(metadata);
  }

  @Override
  public boolean isReady() {
    return !closed && model != null;
  }

  @Override
  public void close() {
    if (!closed) {
      try {
        model.close();
        closed = true;
        logger.info("TensorFlow model closed: {}", metadata.name());
      } catch (Exception e) {
        logger.error("Error closing model: {}", e.getMessage(), e);
      }
    }
  }

  @Override
  public Result<Void> warmup() {
    logger.info("Warming up model with dummy inference...");

    // Create dummy input based on metadata or defaults
    Map<String, Object> dummyInput = Map.of(inputTensorName, new float[] {0.0f});

    Result<Prediction> result = predict(dummyInput);
    if (result.isOk()) {
      logger.info("Model warmup successful");
      return Result.ok(null);
    } else {
      logger.warn("Model warmup failed: {}", result.problemOrNull());
      return Result.fail(result.problemOrNull());
    }
  }

  @Override
  public Result<Void> validateInput(Map<String, Object> features) {
    if (features == null || features.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("ML.INVALID_INPUT"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Features cannot be null or empty"));
    }

    // Basic validation - could be extended to check tensor shapes
    return Result.ok(null);
  }

  /**
   * Converts input data to TensorFlow tensor.
   *
   * @param data Input data (float[], double[], or list)
   * @return TensorFlow tensor
   */
  private TFloat32 convertToTensor(Object data) {
    float[] floatArray;

    if (data instanceof float[]) {
      floatArray = (float[]) data;
    } else if (data instanceof double[]) {
      double[] doubleArray = (double[]) data;
      floatArray = new float[doubleArray.length];
      for (int i = 0; i < doubleArray.length; i++) {
        floatArray[i] = (float) doubleArray[i];
      }
    } else if (data instanceof List) {
      List<?> list = (List<?>) data;
      floatArray = new float[list.size()];
      for (int i = 0; i < list.size(); i++) {
        floatArray[i] = ((Number) list.get(i)).floatValue();
      }
    } else {
      throw new IllegalArgumentException("Unsupported input data type: " + data.getClass());
    }

    // Create tensor with shape [1, length] for batch size 1
    FloatDataBuffer buffer = org.tensorflow.ndarray.buffer.DataBuffers.of(floatArray);
    return TFloat32.tensorOf(Shape.of(1, floatArray.length), buffer);
  }

  /**
   * Converts TensorFlow tensor output to Prediction.
   *
   * @param tensor Output tensor
   * @param features Input features used for prediction
   * @return Prediction object
   */
  private Prediction tensorToPrediction(Tensor tensor, Map<String, Object> features) {
    // Extract probabilities from output tensor
    // This is a simplified implementation - actual implementation depends on model output format
    TFloat32 floatTensor = (TFloat32) tensor;
    long[] shape = floatTensor.shape().asArray();

    // Assuming output is [1, num_classes] for classification
    int numClasses = (int) (shape.length > 1 ? shape[1] : shape[0]);
    float[] probabilities = new float[numClasses];

    FloatDataBuffer buffer = floatTensor.asRawTensor().data().asFloats();
    for (int i = 0; i < numClasses; i++) {
      probabilities[i] = buffer.getFloat(i);
    }

    // Find max probability and its index
    int maxIndex = 0;
    float maxProb = probabilities[0];
    for (int i = 1; i < probabilities.length; i++) {
      if (probabilities[i] > maxProb) {
        maxProb = probabilities[i];
        maxIndex = i;
      }
    }

    // Build probabilities map
    Map<String, Double> probMap = new HashMap<>();
    if (!metadata.labels().isEmpty()) {
      for (int i = 0; i < Math.min(numClasses, metadata.labels().size()); i++) {
        probMap.put(metadata.labels().get(i), (double) probabilities[i]);
      }
    } else {
      for (int i = 0; i < numClasses; i++) {
        probMap.put("class_" + i, (double) probabilities[i]);
      }
    }

    String predictedLabel;
    if (!metadata.labels().isEmpty() && maxIndex < metadata.labels().size()) {
      predictedLabel = metadata.labels().get(maxIndex);
    } else {
      predictedLabel = "class_" + maxIndex;
    }

    return Prediction.builder()
        .label(predictedLabel)
        .confidence(maxProb)
        .probabilities(probMap)
        .features(features)
        .modelVersion(metadata.version())
        .build();
  }
}
