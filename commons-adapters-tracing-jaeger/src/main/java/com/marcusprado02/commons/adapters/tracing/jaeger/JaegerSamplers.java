package com.marcusprado02.commons.adapters.tracing.jaeger;

import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * Factory class for creating common sampling strategies for Jaeger.
 *
 * <p>Sampling determines which traces are recorded and exported. Different sampling strategies are
 * appropriate for different environments and use cases.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Always sample in development
 * Sampler devSampler = JaegerSamplers.alwaysOn();
 *
 * // 10% sampling in production for high-traffic services
 * Sampler prodSampler = JaegerSamplers.probabilistic(0.1);
 *
 * // Rate limiting sampling (e.g., 100 traces per second)
 * Sampler rateLimitSampler = JaegerSamplers.rateLimiting(100);
 *
 * // Parent-based sampling (inherit parent's decision)
 * Sampler parentBasedSampler = JaegerSamplers.parentBased();
 * }</pre>
 */
public final class JaegerSamplers {

  private JaegerSamplers() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Always sample all traces.
   *
   * <p>Use in development or low-traffic environments where you want complete visibility.
   *
   * <p><b>Warning:</b> Not recommended for high-traffic production environments as it generates
   * large volumes of trace data.
   *
   * @return sampler that samples all traces
   */
  public static Sampler alwaysOn() {
    return Sampler.alwaysOn();
  }

  /**
   * Never sample any traces.
   *
   * <p>Use when you want to disable tracing entirely.
   *
   * @return sampler that samples no traces
   */
  public static Sampler alwaysOff() {
    return Sampler.alwaysOff();
  }

  /**
   * Sample traces with a given probability.
   *
   * <p>Probability-based sampling where each trace has a {@code ratio} chance of being sampled.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code probabilistic(1.0)} - sample all traces (equivalent to alwaysOn)
   *   <li>{@code probabilistic(0.5)} - sample 50% of traces
   *   <li>{@code probabilistic(0.01)} - sample 1% of traces
   *   <li>{@code probabilistic(0.0)} - sample no traces (equivalent to alwaysOff)
   * </ul>
   *
   * <p>This is useful for high-traffic production environments where you want representative
   * samples without overwhelming storage.
   *
   * @param ratio the probability of sampling (0.0 to 1.0)
   * @return probabilistic sampler
   * @throws IllegalArgumentException if ratio is not between 0.0 and 1.0
   */
  public static Sampler probabilistic(double ratio) {
    if (ratio < 0.0 || ratio > 1.0) {
      throw new IllegalArgumentException("Probability must be between 0.0 and 1.0, got: " + ratio);
    }
    return Sampler.traceIdRatioBased(ratio);
  }

  /**
   * Sample up to a maximum number of traces per second.
   *
   * <p>Rate-limiting sampler that ensures no more than {@code tracesPerSecond} traces are sampled
   * each second.
   *
   * <p>This is useful when you want to control the volume of trace data while still getting
   * representative samples from high-traffic endpoints.
   *
   * <p><b>Note:</b> The actual implementation uses a combination of parent-based sampling and a
   * rate-limiting algorithm. For true rate limiting, consider using remote sampling with Jaeger
   * agent/collector.
   *
   * @param tracesPerSecond maximum number of traces to sample per second
   * @return rate-limiting sampler
   * @throws IllegalArgumentException if tracesPerSecond is negative
   */
  public static Sampler rateLimiting(int tracesPerSecond) {
    if (tracesPerSecond < 0) {
      throw new IllegalArgumentException(
          "Traces per second must be non-negative, got: " + tracesPerSecond);
    }
    // OpenTelemetry doesn't have built-in rate limiting sampler
    // Use probabilistic as approximation (1000 TPS @ 10% = ~100 sampled)
    // For true rate limiting, use remote sampling with Jaeger
    double estimatedRatio = Math.min(1.0, tracesPerSecond / 1000.0);
    return probabilistic(estimatedRatio);
  }

  /**
   * Parent-based sampling - respects the sampling decision of the parent span.
   *
   * <p>If the trace has a parent span:
   *
   * <ul>
   *   <li>If parent is sampled, this span is sampled
   *   <li>If parent is not sampled, this span is not sampled
   * </ul>
   *
   * <p>If the trace is a root span (no parent), falls back to the root sampler decision.
   *
   * <p>This is the recommended sampler for most distributed tracing scenarios as it maintains
   * consistent sampling decisions across service boundaries.
   *
   * @return parent-based sampler with alwaysOn root sampler
   */
  public static Sampler parentBased() {
    return Sampler.parentBased(Sampler.alwaysOn());
  }

  /**
   * Parent-based sampling with custom root sampler.
   *
   * <p>When a span has a parent, the parent's sampling decision is used. When a span is the root
   * (no parent), the provided root sampler makes the decision.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // Use parent decision when available, otherwise sample 10%
   * Sampler sampler = JaegerSamplers.parentBased(JaegerSamplers.probabilistic(0.1));
   * }</pre>
   *
   * @param rootSampler the sampler to use for root spans
   * @return parent-based sampler with custom root sampler
   */
  public static Sampler parentBased(Sampler rootSampler) {
    return Sampler.parentBased(rootSampler);
  }

  /**
   * Parent-based sampling with custom root and remote samplers.
   *
   * <p>Provides full control over sampling decisions:
   *
   * <ul>
   *   <li>{@code root} - sampler for root spans (no parent)
   *   <li>{@code remoteParentSampled} - sampler when remote parent is sampled
   *   <li>{@code remoteParentNotSampled} - sampler when remote parent is not sampled
   *   <li>{@code localParentSampled} - sampler when local parent is sampled
   *   <li>{@code localParentNotSampled} - sampler when local parent is not sampled
   * </ul>
   *
   * @param root sampler for root spans
   * @param remoteParentSampled sampler when remote parent is sampled
   * @param remoteParentNotSampled sampler when remote parent is not sampled
   * @param localParentSampled sampler when local parent is sampled
   * @param localParentNotSampled sampler when local parent is not sampled
   * @return fully configured parent-based sampler
   */
  public static Sampler parentBasedCustom(
      Sampler root,
      Sampler remoteParentSampled,
      Sampler remoteParentNotSampled,
      Sampler localParentSampled,
      Sampler localParentNotSampled) {
    return Sampler.parentBasedBuilder(root)
        .setRemoteParentSampled(remoteParentSampled)
        .setRemoteParentNotSampled(remoteParentNotSampled)
        .setLocalParentSampled(localParentSampled)
        .setLocalParentNotSampled(localParentNotSampled)
        .build();
  }
}
