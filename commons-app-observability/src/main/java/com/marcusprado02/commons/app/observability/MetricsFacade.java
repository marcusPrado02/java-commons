package com.marcusprado02.commons.app.observability;

import java.util.Map;

/** Framework-agnostic metrics facade (SLI/SLO recording). */
public interface MetricsFacade {

  void incrementCounter(String name, long delta, Map<String, String> attributes);

  void recordHistogram(String name, double value, Map<String, String> attributes);

  void recordGauge(String name, double value, Map<String, String> attributes);

  static MetricsFacade noop() {
    return NoopMetricsFacade.INSTANCE;
  }
}
