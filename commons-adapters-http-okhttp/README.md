# commons-adapters-http-okhttp

Implementação de `HttpClientPort` usando OkHttp 4.x com suporte a resiliência, tracing e interceptors.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-http-okhttp</artifactId>
</dependency>
```

## Setup

```java
OkHttpClient okHttp = new OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(3))
    .readTimeout(Duration.ofSeconds(10))
    .build();

HttpClientPort client = OkHttpClientAdapter.builder()
    .client(okHttp)
    .resilienceExecutor(resilience)          // opcional
    .resiliencePolicies(policies)            // opcional
    .tracer(tracerFacade)                    // opcional
    .interceptor(new AuthInterceptor(token)) // opcional
    .build();
```

## Requisições

```java
// GET
HttpRequest req = HttpRequest.builder()
    .method(HttpMethod.GET)
    .url("https://api.example.com/users/42")
    .header("Accept", "application/json")
    .build();

HttpResponse response = client.execute(req);

if (response.isSuccess()) {
    UserDto user = response.bodyAs(UserDto.class);
}

// POST com corpo JSON
HttpRequest post = HttpRequest.builder()
    .method(HttpMethod.POST)
    .url("https://api.example.com/orders")
    .header("Content-Type", "application/json")
    .body(HttpBody.json(objectMapper.writeValueAsBytes(orderRequest)))
    .build();

HttpResponse created = client.execute(post);
```

## Streaming

```java
HttpResponse stream = client.execute(
    HttpRequest.builder().method(HttpMethod.GET).url(largeFileUrl).build()
);

try (HttpStreamingResponse streaming = client.executeStreaming(req)) {
    InputStream body = streaming.body();
    // processar em chunks
}
```

## Interceptor customizado

```java
public class AuthInterceptor implements HttpInterceptor {

    private final String token;

    @Override
    public HttpRequest intercept(HttpRequest request) {
        return request.withHeader("Authorization", "Bearer " + token);
    }
}
```

## Upload multipart

```java
HttpRequest upload = HttpRequest.builder()
    .method(HttpMethod.POST)
    .url("https://api.example.com/files")
    .body(HttpBody.multipart()
        .part("file", filename, fileBytes, "application/pdf")
        .part("metadata", metadataJson)
        .build())
    .build();

HttpResponse response = client.execute(upload);
```

## Spring Boot

```java
@Bean
public HttpClientPort httpClient(ResilienceExecutor resilience) {
    OkHttpClient okHttp = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(10))
        .build();

    return OkHttpClientAdapter.builder()
        .client(okHttp)
        .resilienceExecutor(resilience)
        .build();
}
```
