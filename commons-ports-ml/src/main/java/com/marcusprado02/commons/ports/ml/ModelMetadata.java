package com.marcusprado02.commons.ports.ml;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata about a machine learning model.
 *
 * @param name Model name/identifier
 * @param version Model version
 * @param framework ML framework (tensorflow, pytorch, onnx, etc.)
 * @param inputShape Expected input tensor shape
 * @param outputShape Expected output tensor shape
 * @param labels List of class labels (for classification)
 * @param createdAt When model was created/trained
 * @param accuracy Model accuracy metric
 * @param description Model description
 * @param metadata Additional custom metadata
 */
public record ModelMetadata(
    String name,
    String version,
    String framework,
    List<Integer> inputShape,
    List<Integer> outputShape,
    List<String> labels,
    Instant createdAt,
    Optional<Double> accuracy,
    Optional<String> description,
    Map<String, String> metadata) {

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if this is a classification model.
   *
   * @return true if model has labels defined
   */
  public boolean isClassificationModel() {
    return !labels.isEmpty();
  }

  /**
   * Gets the number of classes for classification models.
   *
   * @return Number of classes, or 0 if not a classification model
   */
  public int numberOfClasses() {
    return labels.size();
  }

  public static class Builder {
    private String name;
    private String version;
    private String framework;
    private List<Integer> inputShape = List.of();
    private List<Integer> outputShape = List.of();
    private List<String> labels = List.of();
    private Instant createdAt = Instant.now();
    private Optional<Double> accuracy = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Map<String, String> metadata = Map.of();

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder framework(String framework) {
      this.framework = framework;
      return this;
    }

    public Builder inputShape(List<Integer> inputShape) {
      this.inputShape = inputShape != null ? inputShape : List.of();
      return this;
    }

    public Builder outputShape(List<Integer> outputShape) {
      this.outputShape = outputShape != null ? outputShape : List.of();
      return this;
    }

    public Builder labels(List<String> labels) {
      this.labels = labels != null ? labels : List.of();
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder accuracy(Double accuracy) {
      this.accuracy = Optional.ofNullable(accuracy);
      return this;
    }

    public Builder description(String description) {
      this.description = Optional.ofNullable(description);
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata != null ? metadata : Map.of();
      return this;
    }

    public Builder metadata(String key, String value) {
      if (this.metadata.isEmpty()) {
        this.metadata = new java.util.HashMap<>();
      }
      if (!(this.metadata instanceof java.util.HashMap)) {
        this.metadata = new java.util.HashMap<>(this.metadata);
      }
      this.metadata.put(key, value);
      return this;
    }

    public ModelMetadata build() {
      return new ModelMetadata(
          name,
          version,
          framework,
          inputShape,
          outputShape,
          labels,
          createdAt,
          accuracy,
          description,
          metadata);
    }
  }
}
