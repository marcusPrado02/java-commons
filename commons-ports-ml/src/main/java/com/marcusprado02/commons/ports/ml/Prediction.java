package com.marcusprado02.commons.ports.ml;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a machine learning prediction result.
 *
 * @param label Predicted label/class
 * @param confidence Confidence score (0.0 to 1.0)
 * @param probabilities Class probabilities for multi-class prediction
 * @param features Input features used for prediction
 * @param metadata Additional prediction metadata
 * @param timestamp When the prediction was made
 * @param modelVersion Model version used
 */
public record Prediction(
    String label,
    double confidence,
    Map<String, Double> probabilities,
    Map<String, Object> features,
    Map<String, String> metadata,
    Instant timestamp,
    Optional<String> modelVersion) {

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if prediction confidence meets a threshold.
   *
   * @param threshold Minimum confidence threshold (0.0 to 1.0)
   * @return true if confidence >= threshold
   */
  public boolean meetsThreshold(double threshold) {
    return confidence >= threshold;
  }

  /**
   * Gets the top N most likely classes with their probabilities.
   *
   * @param n Number of top classes to return
   * @return List of class names sorted by probability (descending)
   */
  public List<String> topNClasses(int n) {
    return probabilities.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .limit(n)
        .map(Map.Entry::getKey)
        .toList();
  }

  public static class Builder {
    private String label;
    private double confidence;
    private Map<String, Double> probabilities = Map.of();
    private Map<String, Object> features = Map.of();
    private Map<String, String> metadata = Map.of();
    private Instant timestamp = Instant.now();
    private Optional<String> modelVersion = Optional.empty();

    public Builder label(String label) {
      this.label = label;
      return this;
    }

    public Builder confidence(double confidence) {
      this.confidence = confidence;
      return this;
    }

    public Builder probabilities(Map<String, Double> probabilities) {
      this.probabilities = probabilities != null ? probabilities : Map.of();
      return this;
    }

    public Builder probability(String label, double probability) {
      if (this.probabilities.isEmpty()) {
        this.probabilities = new java.util.HashMap<>();
      }
      if (!(this.probabilities instanceof java.util.HashMap)) {
        this.probabilities = new java.util.HashMap<>(this.probabilities);
      }
      this.probabilities.put(label, probability);
      return this;
    }

    public Builder features(Map<String, Object> features) {
      this.features = features != null ? features : Map.of();
      return this;
    }

    public Builder feature(String key, Object value) {
      if (this.features.isEmpty()) {
        this.features = new java.util.HashMap<>();
      }
      if (!(this.features instanceof java.util.HashMap)) {
        this.features = new java.util.HashMap<>(this.features);
      }
      this.features.put(key, value);
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

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder modelVersion(String modelVersion) {
      this.modelVersion = Optional.ofNullable(modelVersion);
      return this;
    }

    public Prediction build() {
      return new Prediction(
          label, confidence, probabilities, features, metadata, timestamp, modelVersion);
    }
  }
}
