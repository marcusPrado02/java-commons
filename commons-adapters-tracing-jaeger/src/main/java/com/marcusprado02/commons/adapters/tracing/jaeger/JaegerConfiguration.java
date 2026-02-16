package com.marcusprado02.commons.adapters.tracing.jaeger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration builder for Jaeger tracing with OpenTelemetry.
 *
 * <p>Provides a fluent API to configure Jaeger exporters, samplers, and batch processors.
 *
 * <p>Jaeger 1.35+ supports OTLP natively, which is the recommended export protocol.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * OpenTelemetry openTelemetry = JaegerConfiguration.builder()
 *     .serviceName("my-service")
 *     .serviceVersion("1.0.0")
 *     .endpoint("http://localhost:4317")  // Jaeger OTLP gRPC receiver
 *     .sampler(JaegerSamplers.probabilistic(0.5))
 *     .build();
 * }</pre>
 */
public final class JaegerConfiguration {

  private final String serviceName;
  private final String serviceVersion;
  private final String endpoint;
  private final Sampler sampler;
  private final Duration batchScheduleDelay;
  private final int maxQueueSize;
  private final int maxExportBatchSize;
  private final Duration exportTimeout;

  private JaegerConfiguration(Builder builder) {
    this.serviceName = Objects.requireNonNull(builder.serviceName, "serviceName is required");
    this.serviceVersion = builder.serviceVersion != null ? builder.serviceVersion : "unknown";
    this.endpoint = Objects.requireNonNull(builder.endpoint, "endpoint is required");
    this.sampler = builder.sampler != null ? builder.sampler : JaegerSamplers.alwaysOn();
    this.batchScheduleDelay = builder.batchScheduleDelay;
    this.maxQueueSize = builder.maxQueueSize;
    this.maxExportBatchSize = builder.maxExportBatchSize;
    this.exportTimeout = builder.exportTimeout;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builds and returns a configured {@link OpenTelemetry} instance.
   *
   * @return configured OpenTelemetry instance with Jaeger OTLP exporter
   */
  public OpenTelemetry build() {
    Resource resource =
        Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, serviceName)
                        .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                        .build()));

    SpanExporter spanExporter =
        OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).setTimeout(exportTimeout).build();

    BatchSpanProcessor batchProcessor =
        BatchSpanProcessor.builder(spanExporter)
            .setScheduleDelay(batchScheduleDelay)
            .setMaxQueueSize(maxQueueSize)
            .setMaxExportBatchSize(maxExportBatchSize)
            .setExporterTimeout(exportTimeout)
            .build();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(batchProcessor)
            .setResource(resource)
            .setSampler(sampler)
            .build();

    return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
  }

  public static final class Builder {
    private String serviceName;
    private String serviceVersion;
    private String endpoint = "http://localhost:4317"; // Default Jaeger OTLP gRPC endpoint
    private Sampler sampler;
    private Duration batchScheduleDelay = Duration.ofSeconds(5);
    private int maxQueueSize = 2048;
    private int maxExportBatchSize = 512;
    private Duration exportTimeout = Duration.ofSeconds(30);

    private Builder() {}

    /**
     * Sets the service name (required).
     *
     * @param serviceName the service name
     * @return this builder
     */
    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    /**
     * Sets the service version.
     *
     * @param serviceVersion the service version
     * @return this builder
     */
    public Builder serviceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
      return this;
    }

    /**
     * Sets the Jaeger OTLP endpoint.
     *
     * <p>Default is http://localhost:4317 (Jaeger OTLP gRPC receiver)
     *
     * <p>Jaeger 1.35+ supports OTLP natively. This is the recommended export protocol.
     *
     * @param endpoint the Jaeger OTLP endpoint URL
     * @return this builder
     */
    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    /**
     * Sets the sampling strategy.
     *
     * @param sampler the sampler
     * @return this builder
     */
    public Builder sampler(Sampler sampler) {
      this.sampler = sampler;
      return this;
    }

    /**
     * Sets the batch schedule delay (default: 5 seconds).
     *
     * @param batchScheduleDelay the delay between batch exports
     * @return this builder
     */
    public Builder batchScheduleDelay(Duration batchScheduleDelay) {
      this.batchScheduleDelay = batchScheduleDelay;
      return this;
    }

    /**
     * Sets the maximum queue size (default: 2048).
     *
     * @param maxQueueSize the maximum queue size
     * @return this builder
     */
    public Builder maxQueueSize(int maxQueueSize) {
      this.maxQueueSize = maxQueueSize;
      return this;
    }

    /**
     * Sets the maximum export batch size (default: 512).
     *
     * @param maxExportBatchSize the maximum batch size
     * @return this builder
     */
    public Builder maxExportBatchSize(int maxExportBatchSize) {
      this.maxExportBatchSize = maxExportBatchSize;
      return this;
    }

    /**
     * Sets the export timeout (default: 30 seconds).
     *
     * @param exportTimeout the export timeout
     * @return this builder
     */
    public Builder exportTimeout(Duration exportTimeout) {
      this.exportTimeout = exportTimeout;
      return this;
    }

    /**
     * Builds the {@link JaegerConfiguration}.
     *
     * @return the Jaeger configuration
     */
    public JaegerConfiguration build() {
      return new JaegerConfiguration(this);
    }
  }
}
