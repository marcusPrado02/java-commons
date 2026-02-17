package com.marcusprado02.commons.ports.ml;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a batch prediction result containing multiple individual predictions.
 *
 * @param predictions List of individual predictions
 * @param totalCount Total number of predictions made
 * @param duration Time taken for batch prediction
 * @param startedAt When batch prediction started
 * @param completedAt When batch prediction completed
 * @param metadata Batch-level metadata
 */
public record BatchPrediction(
    List<Prediction> predictions,
    int totalCount,
    Duration duration,
    Instant startedAt,
    Instant completedAt,
    Map<String, String> metadata) {

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets average confidence across all predictions.
   *
   * @return Average confidence score (0.0 to 1.0)
   */
  public double averageConfidence() {
    return predictions.stream().mapToDouble(Prediction::confidence).average().orElse(0.0);
  }

  /**
   * Gets predictions that meet a confidence threshold.
   *
   * @param threshold Minimum confidence threshold
   * @return Filtered list of predictions
   */
  public List<Prediction> predictionsAboveThreshold(double threshold) {
    return predictions.stream().filter(p -> p.meetsThreshold(threshold)).toList();
  }

  /**
   * Gets the throughput in predictions per second.
   *
   * @return Predictions per second
   */
  public double throughput() {
    if (duration.isZero()) {
      return 0.0;
    }
    return totalCount / duration.toSeconds();
  }

  public static class Builder {
    private List<Prediction> predictions = List.of();
    private int totalCount;
    private Duration duration = Duration.ZERO;
    private Instant startedAt = Instant.now();
    private Instant completedAt = Instant.now();
    private Map<String, String> metadata = Map.of();

    public Builder predictions(List<Prediction> predictions) {
      this.predictions = predictions != null ? predictions : List.of();
      this.totalCount = this.predictions.size();
      return this;
    }

    public Builder prediction(Prediction prediction) {
      if (this.predictions.isEmpty()) {
        this.predictions = new java.util.ArrayList<>();
      }
      if (!(this.predictions instanceof java.util.ArrayList)) {
        this.predictions = new java.util.ArrayList<>(this.predictions);
      }
      this.predictions.add(prediction);
      this.totalCount = this.predictions.size();
      return this;
    }

    public Builder duration(Duration duration) {
      this.duration = duration;
      return this;
    }

    public Builder startedAt(Instant startedAt) {
      this.startedAt = startedAt;
      return this;
    }

    public Builder completedAt(Instant completedAt) {
      this.completedAt = completedAt;
      if (startedAt != null && completedAt != null) {
        this.duration = Duration.between(startedAt, completedAt);
      }
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

    public BatchPrediction build() {
      if (startedAt != null && completedAt != null && duration.isZero()) {
        duration = Duration.between(startedAt, completedAt);
      }
      return new BatchPrediction(
          predictions, totalCount, duration, startedAt, completedAt, metadata);
    }
  }
}
