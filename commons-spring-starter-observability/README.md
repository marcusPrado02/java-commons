# Commons Spring Starter - Observability

Starter Spring Boot para observabilidade (contexto + tracing) com foco em integração com Micrometer Tracing.

## O que o starter faz

- Registra filtros servlet do `commons-adapters-web-spring`:
  - `CorrelationIdFilter`
  - `RequestContextFilter`
- Se existir um bean Micrometer `io.micrometer.tracing.Tracer` no contexto, publica um bean `TracerFacade` que adapta Micrometer para a abstração do `commons-app-observability`.

## Micrometer Tracing (bridge)

Este starter adiciona:

- `io.micrometer:micrometer-tracing`
- `io.micrometer:micrometer-tracing-bridge-otel` (bridge padrão para OpenTelemetry)

E oferece suporte opcional para Brave/Zipkin (dependências marcadas como `optional`):

- `io.micrometer:micrometer-tracing-bridge-brave`
- `io.zipkin.reporter2:zipkin-reporter-brave`

A criação/configuração do bean `io.micrometer.tracing.Tracer` geralmente é feita pelo seu stack Spring Boot (ex: Actuator/Observability auto-config) ou explicitamente pela aplicação.

## Quando usar OTel vs Micrometer

- **Preferir OpenTelemetry direto** (ex: `commons-spring-starter-otel`) quando:
  - você quer um caminho mais “vendor neutral” e padrão de mercado para tracing/exporters
  - você já está usando o `opentelemetry-spring-boot-starter` e quer consistência com spans/propagators OTel

- **Preferir Micrometer Tracing** quando:
  - você quer integrar com a abordagem de observabilidade do ecossistema Spring (observations)
  - você quer flexibilidade de backend via bridges (OTel ou Brave/Zipkin) com API Micrometer

Dica prática: se sua aplicação já usa métricas via Micrometer e Observations, Micrometer Tracing tende a encaixar melhor; se você está padronizando em OTel end-to-end, use o starter OTel.
