/**
 * Jaeger distributed tracing integration with OpenTelemetry.
 *
 * <p>This module provides utilities and configuration for integrating Jaeger distributed tracing
 * with OpenTelemetry. It offers high-level abstractions for:
 *
 * <ul>
 *   <li>Configuring Jaeger exporters (legacy gRPC and OTLP)
 *   <li>Sampling strategies (always on/off, probabilistic, rate limiting, parent-based)
 *   <li>Span attribute management following semantic conventions
 *   <li>Baggage propagation for contextual information
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <p>1. Configure Jaeger with OpenTelemetry:
 *
 * <pre>{@code
 * // Using OTLP (recommended for Jaeger 1.35+)
 * OpenTelemetry openTelemetry = JaegerConfiguration.builder()
 *     .serviceName("my-service")
 *     .serviceVersion("1.0.0")
 *     .otlpEndpoint("http://localhost:4317")
 *     .sampler(JaegerSamplers.probabilistic(0.1))  // Sample 10% of traces
 *     .build()
 *     .build();
 *
 * // Using legacy Jaeger gRPC
 * OpenTelemetry openTelemetry = JaegerConfiguration.builder()
 *     .serviceName("my-service")
 *     .jaegerEndpoint("http://localhost:14250")
 *     .build()
 *     .build();
 * }</pre>
 *
 * <p>2. Use with TracerFacade:
 *
 * <pre>{@code
 * TracerFacade tracer = new OtelTracerFacade("my-service");
 *
 * tracer.inSpan("process-order", () -> {
 *   // Business logic
 *   processOrder();
 * });
 * }</pre>
 *
 * <p>3. Add span attributes:
 *
 * <pre>{@code
 * Span span = tracer.spanBuilder("database-query").startSpan();
 *
 * SpanAttributes.builder()
 *     .component("postgresql")
 *     .dbSystem("postgresql")
 *     .dbStatement("SELECT * FROM orders WHERE id = ?")
 *     .dbName("ecommerce")
 *     .addTo(span);
 *
 * // ... perform query ...
 *
 * span.end();
 * }</pre>
 *
 * <p>4. Use baggage for context propagation:
 *
 * <pre>{@code
 * // Set baggage (propagated to downstream services)
 * BaggageManager.withBaggage(
 *     Map.of(
 *         "user.id", "12345",
 *         "tenant.id", "company-abc"
 *     ),
 *     () -> {
 *       // Call downstream services
 *       callUserService();
 *     });
 *
 * // Retrieve baggage in downstream service
 * Optional<String> userId = BaggageManager.get("user.id");
 * }</pre>
 *
 * <h2>Sampling Strategies</h2>
 *
 * <p>Choose the appropriate sampling strategy for your environment:
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.adapters.tracing.jaeger.JaegerSamplers#alwaysOn()
 *       alwaysOn()} - Sample all traces (development/low traffic)
 *   <li>{@link com.marcusprado02.commons.adapters.tracing.jaeger.JaegerSamplers#alwaysOff()
 *       alwaysOff()} - Sample no traces (disable tracing)
 *   <li>{@link
 *       com.marcusprado02.commons.adapters.tracing.jaeger.JaegerSamplers#probabilistic(double)
 *       probabilistic(ratio)} - Sample a percentage of traces (production)
 *   <li>{@link com.marcusprado02.commons.adapters.tracing.jaeger.JaegerSamplers#rateLimiting(int)
 *       rateLimiting(tracesPerSecond)} - Limit number of sampled traces per second
 *   <li>{@link com.marcusprado02.commons.adapters.tracing.jaeger.JaegerSamplers#parentBased()
 *       parentBased()} - Inherit parent's sampling decision (recommended for distributed systems)
 * </ul>
 *
 * <h2>Span Attributes</h2>
 *
 * <p>Follow OpenTelemetry semantic conventions for span attributes:
 *
 * <ul>
 *   <li>Database: {@code db.system}, {@code db.statement}, {@code db.name}
 *   <li>HTTP: {@code http.method}, {@code http.url}, {@code http.status_code}
 *   <li>Messaging: {@code messaging.system}, {@code messaging.destination}
 *   <li>RPC: {@code rpc.system}, {@code rpc.service}, {@code rpc.method}
 * </ul>
 *
 * <p>Use {@link com.marcusprado02.commons.adapters.tracing.jaeger.SpanAttributes.Builder
 * SpanAttributes.builder()} for type-safe attribute construction.
 *
 * <h2>Baggage</h2>
 *
 * <p>Baggage is contextual information propagated across service boundaries. Use it for:
 *
 * <ul>
 *   <li>User/tenant IDs for multi-tenant applications
 *   <li>Feature flags
 *   <li>Correlation/request IDs
 * </ul>
 *
 * <p><b>Warning:</b> Keep total baggage size under 4KB to avoid HTTP header limits. Avoid storing
 * sensitive information in baggage as it's propagated in HTTP headers.
 *
 * <h2>Integration with Spring</h2>
 *
 * <p>For Spring Boot applications, use {@code commons-spring-starter-otel} which provides
 * auto-configuration for OpenTelemetry. Configure Jaeger via properties:
 *
 * <pre>{@code
 * otel.exporter.otlp.endpoint=http://localhost:4317
 * otel.traces.exporter=otlp
 * otel.metrics.exporter=otlp
 * }</pre>
 *
 * @see com.marcusprado02.commons.adapters.tracing.jaeger.JaegerConfiguration
 * @see com.marcusprado02.commons.adapters.tracing.jaeger.JaegerSamplers
 * @see com.marcusprado02.commons.adapters.tracing.jaeger.SpanAttributes
 * @see com.marcusprado02.commons.adapters.tracing.jaeger.BaggageManager
 */
package com.marcusprado02.commons.adapters.tracing.jaeger;
