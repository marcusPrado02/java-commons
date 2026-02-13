# Commons Adapters - OpenTelemetry (OTel)

Este módulo fornece integrações leves com OpenTelemetry para a plataforma:

- `OtelTracerFacade`: implementação de `TracerFacade`
- Decorators para tracing + propagação de contexto nos ports:
  - HTTP: `OtelHttpClientPortDecorator`
  - Messaging: `OtelMessagePublisherPortDecorator`, `OtelMessageConsumerPortDecorator`
- Métricas de negócio (counters, gauges, histograms): `OtelBusinessMetrics`

> Observação: este módulo usa apenas APIs do OTel por padrão. Se o SDK não estiver configurado,
> as operações são no-op.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-otel</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Tracing em use cases

Use a `TracerFacade` (via `commons-spring-starter-otel` ou instanciando `OtelTracerFacade`):

```java
TracerFacade tracer = new OtelTracerFacade("my-service");

tracer.inSpan("CreateOrderUseCase", () -> {
  // lógica do caso de uso
});
```

## Auto-instrumentation (ports)

### HTTP client

```java
HttpClientPort base = /* seu adapter: OkHttpClientAdapter/WebClientHttpClientAdapter/etc */;
HttpClientPort traced = new OtelHttpClientPortDecorator(base, "my-service");

traced.execute(HttpRequest.builder().method(HttpMethod.GET).uri(URI.create("https://example.com")).build());
```

O decorator cria spans CLIENT e injeta `traceparent`/`tracestate` nos headers.

### Messaging

```java
MessagePublisherPort publisher = /* adapter */;
MessagePublisherPort tracedPublisher = new OtelMessagePublisherPortDecorator(publisher, "my-service");

MessageConsumerPort consumer = /* adapter */;
MessageConsumerPort tracedConsumer = new OtelMessageConsumerPortDecorator(consumer, "my-service");
```

O publisher injeta contexto OTel nos `MessageHeaders` e o consumer extrai e cria span CONSUMER.

## Métricas de negócio

```java
OtelBusinessMetrics metrics = new OtelBusinessMetrics("my-service");

var orders = metrics.counter("orders.created", "Orders created", "1");
orders.add(1);

var latency = metrics.histogram("orders.latency", "Order latency", "ms");
latency.record(42.0);
```

## Exporters: Jaeger / Zipkin / Tempo (exemplos)

Os exemplos abaixo assumem uso do `opentelemetry-spring-boot-starter` (via `commons-spring-starter-otel`).

### OTLP (Tempo ou Jaeger via OTLP)

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
  traces:
    exporter: otlp
  metrics:
    exporter: otlp
```

### Zipkin

```yaml
otel:
  exporter:
    zipkin:
      endpoint: http://localhost:9411/api/v2/spans
  traces:
    exporter: zipkin
```

> Dependendo do runtime, exporters podem requerer dependências adicionais ou configuração via variáveis
> de ambiente (`OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_TRACES_EXPORTER`, etc.).
