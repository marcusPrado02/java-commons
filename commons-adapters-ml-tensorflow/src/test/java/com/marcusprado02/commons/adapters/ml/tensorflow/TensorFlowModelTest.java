package com.marcusprado02.commons.adapters.ml.tensorflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.ml.ModelMetadata;
import com.marcusprado02.commons.ports.ml.Prediction;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TensorFlowModel.
 *
 * <p>Note: Most tests are @Disabled because they require an actual TensorFlow SavedModel file. To
 * run these tests:
 *
 * <ol>
 *   <li>Train and export a TensorFlow model as SavedModel format
 *   <li>Place the model in a directory (e.g., test-resources/test_model)
 *   <li>Update MODEL_PATH constant to point to your model
 *   <li>Remove @Disabled annotations
 * </ol>
 */
class TensorFlowModelTest {

  private static final String MODEL_PATH = "src/test/resources/test_model";

  @Test
  void shouldCreateTensorFlowModelInstance() {
    // Basic test - just verify class loads
    assertNotNull(TensorFlowModel.class);
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldLoadTensorFlowModel() {
    // Load model
    Result<TensorFlowModel> result = TensorFlowModel.load(Path.of(MODEL_PATH));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();

    try (TensorFlowModel model = result.getOrNull()) {
      assertThat(model.isReady()).isTrue();
    }
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldGetModelMetadata() {
    try (TensorFlowModel model = TensorFlowModel.load(Path.of(MODEL_PATH)).getOrNull()) {
      Result<ModelMetadata> result = model.getMetadata();

      assertThat(result.isOk()).isTrue();
      ModelMetadata metadata = result.getOrNull();

      assertThat(metadata.name()).isNotBlank();
      assertThat(metadata.framework()).isEqualTo("tensorflow");
      assertThat(metadata.version()).isNotBlank();
    }
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldMakePrediction() {
    try (TensorFlowModel model = TensorFlowModel.load(Path.of(MODEL_PATH)).getOrNull()) {
      // Example input - adjust based on your model
      Map<String, Object> features = Map.of("input", new float[] {1.0f, 2.0f, 3.0f, 4.0f});

      Result<Prediction> result = model.predict(features);

      assertThat(result.isOk()).isTrue();
      Prediction prediction = result.getOrNull();

      assertThat(prediction.label()).isNotBlank();
      assertThat(prediction.confidence()).isBetween(0.0, 1.0);
      assertThat(prediction.probabilities()).isNotEmpty();
      assertThat(prediction.timestamp()).isNotNull();
    }
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldMakeBatchPrediction() {
    try (TensorFlowModel model = TensorFlowModel.load(Path.of(MODEL_PATH)).getOrNull()) {
      List<Map<String, Object>> featureList =
          List.of(
              Map.of("input", new float[] {1.0f, 2.0f, 3.0f, 4.0f}),
              Map.of("input", new float[] {5.0f, 6.0f, 7.0f, 8.0f}),
              Map.of("input", new float[] {9.0f, 10.0f, 11.0f, 12.0f}));

      var result = model.predictBatch(featureList);

      assertThat(result.isOk()).isTrue();
      var batch = result.getOrNull();

      assertThat(batch.predictions()).hasSize(3);
      assertThat(batch.totalCount()).isEqualTo(3);
      assertThat(batch.duration()).isNotNull();
      assertThat(batch.averageConfidence()).isBetween(0.0, 1.0);
    }
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldWarmupModel() {
    try (TensorFlowModel model = TensorFlowModel.load(Path.of(MODEL_PATH)).getOrNull()) {
      Result<Void> result = model.warmup();

      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldValidateInput() {
    try (TensorFlowModel model = TensorFlowModel.load(Path.of(MODEL_PATH)).getOrNull()) {
      // Valid input
      Map<String, Object> validInput = Map.of("input", new float[] {1.0f, 2.0f});
      Result<Void> result = model.validateInput(validInput);
      assertThat(result.isOk()).isTrue();

      // Invalid input - null
      result = model.validateInput(null);
      assertThat(result.isFail()).isTrue();

      // Invalid input - empty
      result = model.validateInput(Map.of());
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldFailToLoadNonExistentModel() {
    Result<TensorFlowModel> result = TensorFlowModel.load(Path.of("/non/existent/model"));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).contains("ML.MODEL_NOT_FOUND");
  }

  @Test
  @Disabled("Requires actual TensorFlow SavedModel file")
  void shouldFailPredictionOnClosedModel() {
    TensorFlowModel model = TensorFlowModel.load(Path.of(MODEL_PATH)).getOrNull();
    model.close();

    Map<String, Object> features = Map.of("input", new float[] {1.0f, 2.0f});
    Result<Prediction> result = model.predict(features);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).contains("ML.MODEL_CLOSED");
  }
}
