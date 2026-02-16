# Commons Adapters - Tracing - Jaeger

Jaeger distributed tracing integration with OpenTelemetry.

This module provides utilities and configuration for integrating [Jaeger](https://www.jaegertracing.io/) distributed tracing with [OpenTelemetry](https://opentelemetry.io/). It offers high-level abstractions for configuring exporters, sampling strategies, span attributes, and baggage propagation.

## Features

- ✅ **Jaeger Configuration** - Builder for configuring Jaeger with OpenTelemetry SDK
- ✅ **Multiple Exporters** - Legacy Jaeger gRPC and modern OTLP support
- ✅ **Sampling Strategies** - Always on/off, probabilistic, rate limiting, parent-based
- ✅ **Span Attributes** - Type-safe builders following semantic conventions
- ✅ **Baggage Propagation** - Context propagation across service boundaries
- ✅ **Batch Processing** - Configurable batch processors for efficiency
- ✅ **Production Ready** - Sensible defaults with full customization

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-tracing-jaeger</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Quick Start

### 1. Configure Jaeger with OpenTelemetry

#### Using OTLP (Recommended for Jaeger 1.35+)

```java
import com.marcusprado02.commons.adapters.tracing.jaeger.*;
import io.opentelemetry.api.OpenTelemetry;

OpenTelemetry openTelemetry = JaegerConfiguration.builder()
    .serviceName("my-service")
    .serviceVersion("1.0.0")
    .otlpEndpoint("http://localhost:4317")
    .sampler(JaegerSamplers.probabilistic(0.1))  // Sample 10% of traces
    .build()
    .build();
```

#### Using Legacy Jaeger gRPC

```java
OpenTelemetry openTelemetry = JaegerConfiguration.builder()
    .serviceName("my-service")
    .serviceVersion("1.0.0")
    .jaegerEndpoint("http://localhost:14250")
    .sampler(JaegerSamplers.alwaysOn())
    .build()
    .build();
```

### 2. Use with TracerFacade

```java
import com.marcusprado02.commons.adapters.otel.OtelTracerFacade;
import com.marcusprado02.commons.app.observability.TracerFacade;

TracerFacade tracer = new OtelTracerFacade("my-service");

// Simple span
tracer.inSpan("process-order", () -> {
    // Business logic
    processOrder();
});

// Span with return value
String result = tracer.inSpan("fetch-user", () -> {
    return userService.getUser("12345");
});
```

### 3. Add Span Attributes

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

Tracer tracer = GlobalOpenTelemetry.getTracer("my-service");
Span span = tracer.spanBuilder("database-query").startSpan();

try {
    SpanAttributes.builder()
        .component("postgresql")
        .dbSystem("postgresql")
        .dbStatement("SELECT * FROM orders WHERE id = ?")
        .dbName("ecommerce")
        .dbUser("app_user")
        .addTo(span);

    // Perform database query
    executeQuery();

} catch (Exception e) {
    SpanAttributes.recordError(span, e);
    throw e;
} finally {
    span.end();
}
```

### 4. Use Baggage for Context Propagation

```java
import java.util.Map;

// Set baggage (propagated to downstream services)
BaggageManager.withBaggage(
    Map.of(
        "user.id", "12345",
        "tenant.id", "company-abc"
    ),
    () -> {
        // Call downstream services
        userService.getUser();
        orderService.getOrders();
    });

// Retrieve baggage in downstream service
Optional<String> userId = BaggageManager.get("user.id");
if (userId.isPresent()) {
    logger.info("Processing request for user: {}", userId.get());
}
```

## Configuration Options

### JaegerConfiguration

```java
JaegerConfiguration config = JaegerConfiguration.builder()
    // Required
    .serviceName("my-service")                      // Service name (required)

    // Optional
    .serviceVersion("1.0.0")                        // Service version (default: "unknown")
    .otlpEndpoint("http://localhost:4317")          // OTLP endpoint
    .sampler(JaegerSamplers.probabilistic(0.1))     // Sampling strategy (default: alwaysOn)
    .batchScheduleDelay(Duration.ofSeconds(5))      // Batch delay (default: 5s)
    .maxQueueSize(2048)                             // Queue size (default: 2048)
    .maxExportBatchSize(512)                        // Batch size (default: 512)
    .exportTimeout(Duration.ofSeconds(30))          // Export timeout (default: 30s)
    .build();

OpenTelemetry openTelemetry = config.build();
```

### Default Endpoints

- **Jaeger gRPC**: `http://localhost:14250` (legacy collector)
- **OTLP gRPC**: `http://localhost:4317` (Jaeger OTLP receiver)
- **Jaeger UI**: `http://localhost:16686` (for viewing traces)

## Sampling Strategies

### Development

```java
// Sample all traces for complete visibility
Sampler sampler = JaegerSamplers.alwaysOn();
```

### Production - Low Traffic

```java
// Sample all traces if traffic is manageable
Sampler sampler = JaegerSamplers.alwaysOn();
```

### Production - High Traffic

```java
// Sample 10% of traces to reduce overhead
Sampler sampler = JaegerSamplers.probabilistic(0.1);

// Or use rate limiting (approximate 100 traces/second)
Sampler sampler = JaegerSamplers.rateLimiting(100);
```

### Distributed Systems

```java
// Inherit parent's sampling decision (recommended)
Sampler sampler = JaegerSamplers.parentBased();

// With custom root sampler
Sampler sampler = JaegerSamplers.parentBased(
    JaegerSamplers.probabilistic(0.1)
);
```

### Custom Sampling

```java
Sampler customSampler = JaegerSamplers.parentBasedCustom(
    JaegerSamplers.probabilistic(0.1),      // Root spans: 10% sampling
    JaegerSamplers.alwaysOn(),               // Remote parent sampled: always on
    JaegerSamplers.alwaysOff(),              // Remote parent not sampled: always off
    JaegerSamplers.alwaysOn(),               // Local parent sampled: always on
    JaegerSamplers.alwaysOff()               // Local parent not sampled: always off
);
```

## Span Attributes

### Database Operations

```java
SpanAttributes.builder()
    .component("postgresql")
    .dbSystem("postgresql")
    .dbName("ecommerce")
    .dbStatement("SELECT * FROM orders WHERE user_id = ?")
    .dbOperation("SELECT")
    .dbUser("app_user")
    .addTo(span);
```

### HTTP Requests

```java
SpanAttributes.builder()
    .httpMethod("GET")
    .httpUrl("https://api.example.com/users/12345")
    .httpStatusCode(200)
    .httpUserAgent("MyApp/1.0")
    .addTo(span);
```

### Messaging

```java
SpanAttributes.builder()
    .messagingSystem("kafka")
    .messagingDestination("order-events")
    .messagingOperation("send")
    .messagingMessageId("msg-12345")
    .addTo(span);
```

### RPC/gRPC

```java
SpanAttributes.builder()
    .rpcSystem("grpc")
    .rpcService("UserService")
    .rpcMethod("GetUser")
    .addTo(span);
```

### Custom Attributes

```java
SpanAttributes.builder()
    .attr("order.id", "order-12345")
    .attr("order.total", 99.99)
    .attr("order.items_count", 3L)
    .attr("order.paid", true)
    .addTo(span);
```

### Error Recording

```java
try {
    performOperation();
} catch (Exception e) {
    // Records exception with stack trace
    SpanAttributes.recordError(span, e);
    throw e;
}

// Or with custom error message
SpanAttributes.recordError(span, "Operation failed: invalid input");
```

## Baggage Propagation

### Basic Usage

```java
// Set single baggage item
try (Scope scope = BaggageManager.set("user.id", "12345")) {
    callDownstreamService();
}

// Set multiple items
try (Scope scope = BaggageManager.setAll(Map.of(
    "user.id", "12345",
    "tenant.id", "company-abc"
))) {
    callDownstreamService();
}

// Get baggage
Optional<String> userId = BaggageManager.get("user.id");

// Get all baggage
Map<String, String> allBaggage = BaggageManager.getAll();
```

### Functional API

```java
// Execute with baggage (Runnable)
BaggageManager.withBaggage("correlation.id", "abc-123", () -> {
    processRequest();
});

// Execute with baggage (Supplier)
String result = BaggageManager.withBaggage("user.id", "12345", () -> {
    return getUserData();
});

// Multiple baggage items
String result = BaggageManager.withBaggage(
    Map.of("user.id", "12345", "tenant.id", "company-abc"),
    () -> {
        return processMultiTenantRequest();
    });
```

### Checking Baggage

```java
// Check if key exists
if (BaggageManager.contains("user.id")) {
    String userId = BaggageManager.get("user.id").orElseThrow();
}

// Check if empty
if (BaggageManager.isEmpty()) {
    logger.warn("No baggage found");
}

// Get size
int count = BaggageManager.size();
```

### Removing Baggage

```java
// Remove specific key
try (Scope scope = BaggageManager.remove("user.id")) {
    // user.id is not propagated
}

// Clear all baggage
try (Scope scope = BaggageManager.clear()) {
    // No baggage is propagated
}
```

### ⚠️ Baggage Best Practices

1. **Keep it small**: Total baggage should be under 4KB to avoid HTTP header limits
2. **Avoid sensitive data**: Baggage is propagated in HTTP headers
3. **Use for contextual info**: User IDs, tenant IDs, correlation IDs, feature flags
4. **Don't use for large data**: Use span attributes or logging instead
5. **Clean up when done**: Remove baggage when no longer needed

## Spring Integration

### Option 1: Manual Configuration

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        return JaegerConfiguration.builder()
            .serviceName("my-spring-app")
            .serviceVersion("1.0.0")
            .otlpEndpoint("http://localhost:4317")
            .sampler(JaegerSamplers.probabilistic(0.1))
            .build()
            .build();
    }

    @Bean
    public TracerFacade tracerFacade() {
        return new OtelTracerFacade("my-spring-app");
    }
}
```

### Option 2: Use commons-spring-starter-otel

For Spring Boot applications, use the starter which provides auto-configuration:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-spring-starter-otel</artifactId>
  <version>${commons.version}</version>
</dependency>
```

Configure via `application.yml`:

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
  traces:
    exporter: otlp
    sampler:
      probability: 0.1
  resource:
    attributes:
      service.name: my-spring-app
      service.version: 1.0.0
```

## Running Jaeger Locally

### Using Docker

```bash
# All-in-one Jaeger with OTLP support (Jaeger 1.35+)
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  -p 14250:14250 \
  jaegertracing/all-in-one:latest
```

### Using Docker Compose

```yaml
version: '3'
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "16686:16686"  # Jaeger UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP
      - "14250:14250"  # Jaeger gRPC
```

### Access Jaeger UI

Open http://localhost:16686 in your browser to view traces.

## Example: Complete E-Commerce Service

```java
import com.marcusprado02.commons.adapters.tracing.jaeger.*;
import com.marcusprado02.commons.adapters.otel.OtelTracerFacade;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class OrderService {

    private final Tracer tracer;
    private final TracerFacade tracerFacade;

    public OrderService() {
        // Configure Jaeger
        JaegerConfiguration.builder()
            .serviceName("order-service")
            .serviceVersion("1.0.0")
            .otlpEndpoint("http://localhost:4317")
            .sampler(JaegerSamplers.parentBased(
                JaegerSamplers.probabilistic(0.1)
            ))
            .build()
            .build();

        this.tracer = GlobalOpenTelemetry.getTracer("order-service");
        this.tracerFacade = new OtelTracerFacade("order-service");
    }

    public Order createOrder(CreateOrderRequest request) {
        return tracerFacade.inSpan("create-order", () -> {
            // Set baggage for downstream services
            return BaggageManager.withBaggage(
                Map.of(
                    "user.id", request.userId(),
                    "tenant.id", request.tenantId()
                ),
                () -> {
                    // Validate inventory
                    validateInventory(request);

                    // Process payment
                    String paymentId = processPayment(request);

                    // Create order in database
                    Order order = saveOrder(request, paymentId);

                    // Publish event
                    publishOrderCreatedEvent(order);

                    return order;
                });
        });
    }

    private void validateInventory(CreateOrderRequest request) {
        Span span = tracer.spanBuilder("validate-inventory").startSpan();
        try {
            SpanAttributes.builder()
                .component("inventory-service")
                .httpMethod("POST")
                .httpUrl("http://inventory-service/api/validate")
                .attr("items_count", (long) request.items().size())
                .addTo(span);

            inventoryClient.validate(request.items());

            SpanAttributes.builder()
                .httpStatusCode(200)
                .addTo(span);

        } catch (Exception e) {
            SpanAttributes.recordError(span, e);
            throw e;
        } finally {
            span.end();
        }
    }

    private String processPayment(CreateOrderRequest request) {
        Span span = tracer.spanBuilder("process-payment").startSpan();
        try {
            SpanAttributes.builder()
                .component("payment-service")
                .attr("amount", request.totalAmount())
                .attr("currency", "USD")
                .attr("payment_method", request.paymentMethod())
                .addTo(span);

            String paymentId = paymentClient.charge(request);

            SpanAttributes.builder()
                .attr("payment.id", paymentId)
                .attr("payment.status", "SUCCESS")
                .addTo(span);

            return paymentId;

        } catch (PaymentFailedException e) {
            SpanAttributes.recordError(span, e);
            throw e;
        } finally {
            span.end();
        }
    }

    private Order saveOrder(CreateOrderRequest request, String paymentId) {
        Span span = tracer.spanBuilder("save-order").startSpan();
        try {
            SpanAttributes.builder()
                .component("postgresql")
                .dbSystem("postgresql")
                .dbName("orders")
                .dbStatement("INSERT INTO orders (user_id, payment_id, total) VALUES (?, ?, ?)")
                .dbOperation("INSERT")
                .addTo(span);

            Order order = orderRepository.save(request, paymentId);

            SpanAttributes.builder()
                .attr("order.id", order.id())
                .attr("order.status", order.status().toString())
                .addTo(span);

            return order;

        } catch (Exception e) {
            SpanAttributes.recordError(span, e);
            throw e;
        } finally {
            span.end();
        }
    }

    private void publishOrderCreatedEvent(Order order) {
        Span span = tracer.spanBuilder("publish-order-created").startSpan();
        try {
            SpanAttributes.builder()
                .component("kafka")
                .messagingSystem("kafka")
                .messagingDestination("order-events")
                .messagingOperation("send")
                .attr("event.type", "OrderCreated")
                .attr("order.id", order.id())
                .addTo(span);

            eventPublisher.publish("order-events", new OrderCreatedEvent(order));

        } catch (Exception e) {
            SpanAttributes.recordError(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## Performance Considerations

### Sampling

- **Development**: Use `alwaysOn()` for complete visibility
- **Production low traffic**: Use `alwaysOn()` if manageable
- **Production high traffic**: Use `probabilistic(0.01)` to `probabilistic(0.1)` (1-10% sampling)
- **Distributed systems**: Use `parentBased()` to maintain trace continuity

### Batch Processing

Default batch settings are optimized for most use cases:

- **Schedule delay**: 5 seconds (balance between latency and throughput)
- **Queue size**: 2048 spans (sufficient for most traffic)
- **Batch size**: 512 spans (optimal network usage)
- **Export timeout**: 30 seconds (allow time for large batches)

Adjust for high-traffic services:

```java
JaegerConfiguration.builder()
    .batchScheduleDelay(Duration.ofSeconds(1))  // Export more frequently
    .maxQueueSize(4096)                         // Larger queue
    .maxExportBatchSize(1024)                   // Larger batches
    .exportTimeout(Duration.ofSeconds(60))      // Longer timeout
    .build();
```

### Baggage

- Keep total size under 4KB
- Limit to 5-10 key-value pairs
- Avoid large strings or complex data
- Remove baggage when no longer needed

## Troubleshooting

### Traces not appearing in Jaeger

1. **Check Jaeger is running**: `curl http://localhost:16686`
2. **Verify endpoint**: Ensure correct endpoint in configuration
3. **Check sampling**: Ensure sampler is not `alwaysOff()`
4. **Review logs**: Enable OpenTelemetry debug logging

### High memory usage

1. **Reduce queue size**: Lower `maxQueueSize` (e.g., 1024)
2. **Increase batch frequency**: Lower `batchScheduleDelay` (e.g., 1 second)
3. **Adjust sampling**: Lower sampling ratio (e.g., 0.01 for 1%)

### Slow performance

1. **Use async export**: Batch processor exports asynchronously by default
2. **Optimize sampling**: Lower sampling ratio for high-traffic endpoints
3. **Monitor queue**: Ensure queue doesn't fill up (indicates export bottleneck)

## Best Practices

1. **Use parent-based sampling** in distributed systems
2. **Sample less in production** (1-10%) for high-traffic services
3. **Add meaningful span attributes** following semantic conventions
4. **Use baggage sparingly** - keep under 4KB
5. **Record errors** with `SpanAttributes.recordError()`
6. **Set component names** for easy filtering in Jaeger UI
7. **Close scopes** when using baggage manually
8. **Use try-with-resources** for scope management

## Links

- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [Jaeger OTLP Support](https://www.jaegertracing.io/docs/latest/apis/#opentelemetry-protocol-stable)

## License

This module is part of the Commons library and follows the same license.
