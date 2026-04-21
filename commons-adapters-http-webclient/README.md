# commons-adapters-http-webclient

Implementação de `HttpClientPort` usando Spring WebClient (Project Reactor). Reativo por natureza com suporte a backpressure.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-http-webclient</artifactId>
</dependency>
```

## Setup

```java
WebClient webClient = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .build();

HttpClientPort client = WebClientAdapter.builder()
    .webClient(webClient)
    .resilienceExecutor(resilience)   // opcional
    .resiliencePolicies(policies)     // opcional
    .build();
```

## Requisições reativas

```java
// GET reativo
Mono<UserDto> user = client.get(
    "/users/{id}", Map.of("id", userId),
    UserDto.class
);

// POST reativo
Mono<OrderDto> order = client.post(
    "/orders",
    orderRequest,
    OrderDto.class
);

// Processar sem bloquear thread
client.get("/users/" + userId, UserDto.class)
    .map(UserDto::from)
    .flatMap(dto -> cacheService.cache(dto))
    .subscribe(
        dto -> log.info("Cached user {}", dto.id()),
        err -> log.error("Failed", err)
    );
```

## Bloqueante (interop com código imperativo)

```java
// Bloqueia — use somente fora de contexto reativo
HttpResponse response = client.execute(
    HttpRequest.builder()
        .method(HttpMethod.GET)
        .url("/users/42")
        .build()
);
UserDto user = response.bodyAs(UserDto.class);
```

## Spring Boot

```java
@Bean
public HttpClientPort webClient(WebClient.Builder builder, ResilienceExecutor resilience) {
    WebClient wc = builder
        .baseUrl("${services.user-service.url}")
        .build();

    return WebClientAdapter.builder()
        .webClient(wc)
        .resilienceExecutor(resilience)
        .build();
}
```

## Diferença vs OkHttp

| | `commons-adapters-http-okhttp` | `commons-adapters-http-webclient` |
|---|---|---|
| Modelo | Bloqueante | Reativo (Reactor) |
| Ideal para | Apps Spring MVC / non-reactive | Apps Spring WebFlux |
| Backpressure | Não | Sim |
| Streaming | Manual | Nativo (`Flux<DataBuffer>`) |
