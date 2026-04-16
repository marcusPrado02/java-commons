package com.marcusprado02.commons.ports.ml;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import java.util.Map;

/**
 * Port interface for machine learning model operations.
 *
 * <p>Provides abstraction over different ML frameworks (TensorFlow, PyTorch, ONNX, etc.) for model
 * loading, inference, and metadata retrieval.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MLModel model = TensorFlowModel.load("model.pb");
 *
 * Map<String, Object> features = Map.of(
 *     "feature1", 0.5,
 *     "feature2", 1.2
 * );
 *
 * Result<Prediction> result = model.predict(features);
 * if (result.isOk()) {
 *     Prediction prediction = result.get();
 *     System.out.println("Predicted: " + prediction.label() +
 *                        " (confidence: " + prediction.confidence() + ")");
 * }
 * }</pre>
 */
public interface MLModel {

  /**
   * Makes a single prediction using the model.
   *
   * @param features Input features as key-value pairs
   * @return Prediction result
   */
  Result<Prediction> predict(Map<String, Object> features);

  /**
   * Makes predictions for a batch of inputs.
   *
   * @param featureList List of input feature maps
   * @return Batch prediction result
   */
  Result<BatchPrediction> predictBatch(List<Map<String, Object>> featureList);

  /**
   * Gets metadata about the model.
   *
   * @return Model metadata
   */
  Result<ModelMetadata> getMetadata();

  /**
   * Checks if the model is loaded and ready for inference.
   *
   * @return true if model is ready
   */
  boolean isReady();

  /**
   * Closes the model and releases resources.
   *
   * <p>This should be called when the model is no longer needed to free up memory and GPU
   * resources.
   */
  void close();

  /**
   * Warms up the model by running a dummy inference.
   *
   * <p>This is useful for JIT compilation and GPU initialization to reduce latency for the first
   * real prediction.
   *
   * @return Result indicating success or failure
   */
  Result<Void> warmup();

  /**
   * Validates input features against the expected model input.
   *
   * @param features Input features to validate
   * @return Validation result with details if validation fails
   */
  Result<Void> validateInput(Map<String, Object> features);
}
