package com.marcusprado02.commons.app.observability;

import java.util.Map;

final class NoopMetricsFacade implements MetricsFacade {

  static final NoopMetricsFacade INSTANCE = new NoopMetricsFacade();

  private NoopMetricsFacade() {}

  @Override
  public void incrementCounter(String name, long delta, Map<String, String> attributes) {
    // no-op
  }

  @Override
  public void recordHistogram(String name, double value, Map<String, String> attributes) {
    // no-op
  }

  @Override
  public void recordGauge(String name, double value, Map<String, String> attributes) {
    // no-op
  }
}
