# Commons Adapters - ML TensorFlow

TensorFlow adapter for machine learning operations in the Commons Platform.

## Overview

This module provides a **TensorFlow** implementation of the `MLModel` port interface, enabling machine learning inference using the TensorFlow Java API. It supports loading SavedModel formats and performing single and batch predictions with comprehensive error handling.

## Features

- ✅ **SavedModel Loading**: Load TensorFlow SavedModel format
- ✅ **Single Prediction**: Inference for individual inputs
- ✅ **Batch Prediction**: Efficient batch inference
- ✅ **Model Metadata**: Access model information (version, framework, labels)
- ✅ **Input Validation**: Validate features before prediction
- ✅ **Model Warmup**: JIT compilation and initialization
- ✅ **Resource Management**: AutoCloseable for proper cleanup
- ✅ **Result Pattern**: Type-safe error handling
- ⏳ **GPU Support**: Planned (requires appropriate TensorFlow native libraries)
- ⏳ **ONNX Support**: Planned (separate adapter module)

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-ml-tensorflow</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Export a TensorFlow SavedModel

First, train and export your model in Python:

```python
import tensorflow as tf

# Train your model
model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu', input_shape=(4,)),
    tf.keras.layers.Dense(32, activation='relu'),
    tf.keras.layers.Dense(3, activation='softmax')  # 3 classes
])

model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])
# ... train your model ...

# Export as SavedModel
model.save('saved_model/my_model')
```

### 2. Load and Use in Java

```java
import com.marcusprado02.commons.adapters.ml.tensorflow.TensorFlowModel;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.ml.Prediction;

import java.nio.file.Path;
import java.util.Map;

public class MLExample {
    public static void main(String[] args) {
        // Load the model
        Result<TensorFlowModel> modelResult = TensorFlowModel.load(
            Path.of("saved_model/my_model")
        );

        if (modelResult.isFail()) {
            System.err.println("Failed to load model: " + modelResult.problemOrNull());
            return;
        }

        try (TensorFlowModel model = modelResult.get()) {
            // Warmup (optional but recommended)
            model.warmup();

            // Prepare input features
            Map<String, Object> features = Map.of(
                "input", new float[]{5.1f, 3.5f, 1.4f, 0.2f}  // Example: Iris dataset
            );

            // Make prediction
            Result<Prediction> predictionResult = model.predict(features);

            if (predictionResult.isOk()) {
                Prediction prediction = predictionResult.get();

                System.out.println("Predicted class: " + prediction.label());
                System.out.println("Confidence: " + prediction.confidence());
                System.out.println("All probabilities: " + prediction.probabilities());

                // Get top 3 most likely classes
                List<String> topClasses = prediction.topNClasses(3);
                System.out.println("Top 3 classes: " + topClasses);
            } else {
                System.err.println("Prediction failed: " + predictionResult.problemOrNull());
            }
        }
    }
}
```

## API Usage

### Loading Models

#### Default Loading

```java
// Uses default tensor names: "input" and "output"
Result<TensorFlowModel> model = TensorFlowModel.load(Path.of("model_path"));
```

#### Custom Tensor Names

```java
// Specify custom tensor names for your model
Result<TensorFlowModel> model = TensorFlowModel.load(
    Path.of("model_path"),
    "serving_default",        // Signature definition key
    "dense_input",            // Input tensor name
    "dense_2"                 // Output tensor name
);
```

### Single Prediction

```java
try (TensorFlowModel model = TensorFlowModel.load(modelPath).get()) {
    Map<String, Object> features = Map.of(
        "input", new float[]{1.0f, 2.0f, 3.0f, 4.0f}
    );

    Result<Prediction> result = model.predict(features);

    if (result.isOk()) {
        Prediction prediction = result.get();
        String label = prediction.label();
        double confidence = prediction.confidence();
        Map<String, Double> probabilities = prediction.probabilities();
    }
}
```

### Batch Prediction

```java
List<Map<String, Object>> batch = List.of(
    Map.of("input", new float[]{1.0f, 2.0f, 3.0f, 4.0f}),
    Map.of("input", new float[]{5.0f, 6.0f, 7.0f, 8.0f}),
    Map.of("input", new float[]{9.0f, 10.0f, 11.0f, 12.0f})
);

Result<BatchPrediction> result = model.predictBatch(batch);

if (result.isOk()) {
    BatchPrediction batchPred = result.get();
    System.out.println("Total predictions: " + batchPred.totalCount());
    System.out.println("Average confidence: " + batchPred.averageConfidence());
    System.out.println("Throughput: " + batchPred.throughput() + " pred/s");

    // Get predictions above threshold
    List<Prediction> confident = batchPred.predictionsAboveThreshold(0.8);
}
```

### Model Metadata

```java
Result<ModelMetadata> metaResult = model.getMetadata();

if (metaResult.isOk()) {
    ModelMetadata meta = metaResult.get();
    System.out.println("Model: " + meta.name());
    System.out.println("Version: " + meta.version());
    System.out.println("Framework: " + meta.framework());
    System.out.println("Classes: " + meta.labels());
    System.out.println("Is classification: " + meta.isClassificationModel());
}
```

### Input Validation

```java
Map<String, Object> features = Map.of("input", new float[]{1.0f, 2.0f});

Result<Void> validation = model.validateInput(features);

if (validation.isFail()) {
    System.err.println("Invalid input: " + validation.problemOrNull());
}
```

### Model Warmup

```java
// Warmup model with dummy inference (reduces first-call latency)
Result<Void> warmupResult = model.warmup();

if (warmupResult.isOk()) {
    System.out.println("Model warmed up and ready");
}
```

## Input Data Formats

The TensorFlowModel supports multiple input data formats:

### Float Array

```java
Map<String, Object> features = Map.of(
    "input", new float[]{1.0f, 2.0f, 3.0f, 4.0f}
);
```

### Double Array

```java
Map<String, Object> features = Map.of(
    "input", new double[]{1.0, 2.0, 3.0, 4.0}
);
// Automatically converted to float
```

### List

```java
Map<String, Object> features = Map.of(
    "input", List.of(1.0, 2.0, 3.0, 4.0)
);
```

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<Prediction> result = model.predict(features);

if (result.isOk()) {
    Prediction prediction = result.get();
    // Handle successful prediction
} else {
    Problem problem = result.problemOrNull();
    System.err.println("Error: " + problem.message());
    System.err.println("Error code: " + problem.code().value());
    System.err.println("Category: " + problem.category());
}
```

### Error Codes

| Error Code | Category | Description |
|------------|----------|-------------|
| `ML.MODEL_NOT_FOUND` | NOT_FOUND | Model file not found at specified path |
| `ML.MODEL_LOAD_FAILED` | TECHNICAL | Failed to load TensorFlow model |
| `ML.MODEL_CLOSED` | TECHNICAL | Attempting to use a closed model |
| `ML.PREDICTION_FAILED` | TECHNICAL | Prediction execution failed |
| `ML.INVALID_INPUT` | BUSINESS | Invalid input features |

## Spring Boot Integration

### Configuration

```java
@Configuration
public class MLConfiguration {

    @Bean
    public TensorFlowModel irisClassifier() {
        Path modelPath = Path.of("models/iris_classifier");
        return TensorFlowModel.load(modelPath)
            .getOrThrow(() -> new IllegalStateException("Failed to load model"));
    }

    @PreDestroy
    public void cleanup() {
        // Models are AutoCloseable, but you can also close explicitly
        irisClassifier().close();
    }
}
```

### Service Usage

```java
@Service
@RequiredArgsConstructor
public class IrisClassificationService {

    private final TensorFlowModel irisClassifier;

    public Result<String> classifyIris(double sepalLength, double sepalWidth,
                                        double petalLength, double petalWidth) {
        Map<String, Object> features = Map.of(
            "input", new float[]{
                (float) sepalLength,
                (float) sepalWidth,
                (float) petalLength,
                (float) petalWidth
            }
        );

        return irisClassifier.predict(features)
            .map(Prediction::label);
    }
}
```

### REST Controller

```java
@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MLController {

    private final IrisClassificationService classificationService;

    @PostMapping("/classify-iris")
    public ResponseEntity<?> classifyIris(@RequestBody IrisRequest request) {
        Result<String> result = classificationService.classifyIris(
            request.sepalLength(),
            request.sepalWidth(),
            request.petalLength(),
            request.petalWidth()
        );

        if (result.isOk()) {
            return ResponseEntity.ok(Map.of("species", result.get()));
        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("error", result.problemOrNull().message()));
        }
    }
}

record IrisRequest(double sepalLength, double sepalWidth,
                   double petalLength, double petalWidth) {}
```

## Performance Considerations

### Model Loading

- Model loading is **expensive** - load once at startup, not per-request
- Use singleton beans in Spring Boot
- Consider lazy loading for rarely-used models

### Warmup

```java
// Warmup reduces first-call latency (JIT compilation, GPU initialization)
model.warmup();
```

### Batch Inference

```java
// Batch predictions are more efficient than multiple single predictions
model.predictBatch(featureList);  // Better
// vs
featureList.forEach(features -> model.predict(features));  // Slower
```

### Resource Cleanup

```java
// Always close models to free native resources
try (TensorFlowModel model = TensorFlowModel.load(path).get()) {
    // Use model
}  // Automatically closed

// Or manually
TensorFlowModel model = TensorFlowModel.load(path).get();
try {
    // Use model
} finally {
    model.close();
}
```

## Dependencies

- **TensorFlow Java**: 0.5.0
- **commons-ports-ml**: ML port interfaces
- **commons-kernel-result**: Result pattern
- **commons-kernel-errors**: Error handling

## TensorFlow Version Compatibility

| TensorFlow Java Version | TensorFlow Python Version | Java Version |
|-------------------------|---------------------------|--------------|
| 0.5.0 | 2.10.x - 2.15.x | Java 11+ |

## Limitations

### Current Limitations

- **SavedModel Only**: Only supports TensorFlow SavedModel format (not Keras H5, Checkpoint, etc.)
- **Float Tensors**: Primarily supports float32 tensors (int, string tensors require custom conversion)
- **Single Input/Output**: Simplified API assumes single input and output tensor
- **No Training**: Inference only - no training or fine-tuning support
- **CPU Only**: GPU support requires additional TensorFlow native libraries

### Workarounds

**Multiple Inputs/Outputs**: For advanced models, use custom tensor names:

```java
Result<TensorFlowModel> model = TensorFlowModel.load(
    path,
    "serving_default",
    "input_layer_name",
    "output_layer_name"
);
```

**Non-Float Data**: Convert to float arrays before prediction:

```java
// Convert integers to floats
int[] intArray = {1, 2, 3, 4};
float[] floatArray = IntStream.of(intArray)
    .mapToDouble(i -> i)
    .toArray();
```

## Testing

### Unit Tests

Most tests require an actual TensorFlow SavedModel:

```java
@Test
@Disabled("Requires actual TensorFlow SavedModel file")
void shouldMakePrediction() {
    try (TensorFlowModel model = TensorFlowModel.load(Path.of("test_model")).get()) {
        Map<String, Object> features = Map.of("input", new float[]{1.0f, 2.0f});
        Result<Prediction> result = model.predict(features);
        assertThat(result.isOk()).isTrue();
    }
}
```

### Creating Test Models

Python script to create a simple test model:

```python
import tensorflow as tf
import numpy as np

# Create simple model
model = tf.keras.Sequential([
    tf.keras.layers.Dense(10, activation='relu', input_shape=(4,)),
    tf.keras.layers.Dense(3, activation='softmax')
])

# Compile
model.compile(optimizer='adam', loss='categorical_crossentropy')

# Dummy training
X = np.random.rand(100, 4)
y = np.random.randint(0, 3, 100)
y_one_hot = tf.keras.utils.to_categorical(y, 3)
model.fit(X, y_one_hot, epochs=5, verbose=0)

# Save
model.save('test-resources/test_model')
print("Model saved for Java testing")
```

## Troubleshooting

### "Model not found" Error

```
Error: ML.MODEL_NOT_FOUND - Model not found at: /path/to/model
```

**Solution**: Ensure the path points to the SavedModel directory (containing `saved_model.pb` and `variables/`)

### "Failed to load TensorFlow library"

```
java.lang.UnsatisfiedLinkError: no tensorflow_jni in java.library.path
```

**Solution**: TensorFlow Java includes native libraries for common platforms. If your platform is unsupported, build TensorFlow from source.

### "Prediction failed" with Tensor Shape Mismatch

```
Error: ML.PREDICTION_FAILED - Shape mismatch
```

**Solution**: Ensure input features match the model's expected input shape:

```java
// Check model metadata for expected shape
Result<ModelMetadata> meta = model.getMetadata();
List<Integer> expectedShape = meta.get().inputShape();
```

### High Latency on First Prediction

**Solution**: Use warmup to trigger JIT compilation:

```java
model.warmup();  // Run before handling real requests
```

## Future Enhancements

- GPU support configuration
- Multi-input/multi-output models
- TensorFlow Lite integration for mobile
- Model quantization support
- TensorFlow Serving integration
- Advanced tensor operations
- Custom metrics and monitoring

## References

- [TensorFlow Java API](https://www.tensorflow.org/jvm)
- [TensorFlow SavedModel Guide](https://www.tensorflow.org/guide/saved_model)
- [Commons ML Port Documentation](../commons-ports-ml/README.md)

## License

See the [LICENSE](../LICENSE) file for details.
