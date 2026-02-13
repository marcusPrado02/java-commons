package com.marcusprado02.commons.adapters.otel.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * Thin helpers for business metrics (counters, gauges, histograms) using the OpenTelemetry API.
 *
 * <p>Note: if no SDK is configured, instruments are no-ops.
 */
public final class OtelBusinessMetrics {

  private final Meter meter;

  public OtelBusinessMetrics(String instrumentationName) {
    String name =
        (instrumentationName == null || instrumentationName.isBlank())
            ? "com.marcusprado02.commons"
            : instrumentationName.trim();
    this.meter = GlobalOpenTelemetry.getMeter(name);
  }

  public LongCounter counter(String name, String description, String unit) {
    return meter
        .counterBuilder(Objects.requireNonNull(name, "name must not be null"))
        .setDescription(description == null ? "" : description)
        .setUnit(unit == null ? "1" : unit)
        .build();
  }

  public LongUpDownCounter upDownCounter(String name, String description, String unit) {
    return meter
        .upDownCounterBuilder(Objects.requireNonNull(name, "name must not be null"))
        .setDescription(description == null ? "" : description)
        .setUnit(unit == null ? "1" : unit)
        .build();
  }

  public DoubleHistogram histogram(String name, String description, String unit) {
    return meter
        .histogramBuilder(Objects.requireNonNull(name, "name must not be null"))
        .setDescription(description == null ? "" : description)
        .setUnit(unit == null ? "1" : unit)
        .build();
  }

  public ObservableLongGauge gauge(String name, String description, String unit, LongSupplier value) {
    Objects.requireNonNull(value, "value must not be null");
    return meter
        .gaugeBuilder(Objects.requireNonNull(name, "name must not be null"))
        .setDescription(description == null ? "" : description)
        .setUnit(unit == null ? "1" : unit)
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(value.getAsLong()));
  }

  public ObservableDoubleGauge gauge(
      String name, String description, String unit, DoubleSupplier value) {
    Objects.requireNonNull(value, "value must not be null");
    return meter
        .gaugeBuilder(Objects.requireNonNull(name, "name must not be null"))
        .setDescription(description == null ? "" : description)
        .setUnit(unit == null ? "1" : unit)
        .buildWithCallback(measurement -> measurement.record(value.getAsDouble()));
  }
}
