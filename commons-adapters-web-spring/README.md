# Commons Adapters - Web Spring

Adaptadores Spring MVC para aplicações REST com suporte completo a observabilidade, validação, rate limiting, CORS e muito mais.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-web-spring</artifactId>
    <version>${commons.version}</version>
</dependency>
```

## 🎯 Funcionalidades

### 1. Exception Handlers Globais (RFC 7807)

Handler global de exceções com suporte a **Problem Details (RFC 7807)**.

#### Exceções Tratadas

- **DomainException**: Exceções de domínio
- **MethodArgumentNotValidException**: Erros de validação de campos
- **HttpMessageNotReadableException**: JSON malformado
- **HttpRequestMethodNotSupportedException**: Método HTTP não permitido
- **NoResourceFoundException**: Recurso não encontrado (404)
- **IllegalArgumentException**: Argumentos inválidos
- **Exception**: Fallback para erros inesperados (500)

#### Configuração

O handler é registrado automaticamente via `@RestControllerAdvice`:

```java
@Configuration
public class WebConfig {

    @Bean
    public HttpProblemMapper problemMapper() {
        return new DefaultHttpProblemMapper(); // Implementação do commons-adapters-web
    }
}
```

#### Exemplo de Resposta

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed for one or more fields",
  "details": {
    "errors": {
      "email": "must be a valid email address",
      "age": "must be greater than 18"
    }
  },
  "meta": {
    "correlationId": "abc-123",
    "timestamp": "2026-02-13T20:00:00Z"
  }
}
```

---

### 2. Request/Response Logging

Logging detalhado de requisições e respostas HTTP.

#### Configuração

```java
@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestResponseLoggingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestResponseLoggingFilter());
        bean.setOrder(10); // Após CorrelationIdFilter
        return bean;
    }
}
```

#### application.yml

```yaml
# Ativar logs DEBUG para ver requisições/respostas
logging:
  level:
    com.marcusprado02.commons.adapters.web.spring.filter.RequestResponseLoggingFilter: DEBUG
```

#### Exemplo de Log

```
>>> HTTP Request: GET /api/users?page=0&size=20 | Headers: X-Correlation-Id=abc-123, Content-Type=application/json
<<< HTTP Response: 200 OK | Duration: 45ms | Headers: X-Correlation-Id=abc-123
```

---

### 3. Correlation ID Injection

Propagação automática de `X-Correlation-Id` entre requisições.

#### Funcionalidades

- Gera `X-Correlation-Id` automaticamente se não existir
- Propaga o ID em chamadas downstream (RestTemplate, WebClient)
- Adiciona correlation ID ao MDC (SLF4J)
- Retorna o ID no response header

#### Já Configurado

O filtro `CorrelationIdFilter` já está registrado via `commons-spring-starter-observability`.

---

### 4. Rate Limiting

Rate limiting configurável por IP, usuário ou API key usando **token bucket algorithm**.

#### Configuração Programática

```java
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        RateLimitFilter filter = RateLimitFilter.builder()
            .limit(100)                          // 100 requisições
            .window(Duration.ofMinutes(1))       // por minuto
            .keyExtractor(request -> request.getRemoteAddr()) // por IP
            .build();

        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.setUrlPatterns(List.of("/api/*"));
        bean.setOrder(5);
        return bean;
    }
}
```

#### Configuração via Propriedades

```yaml
commons:
  web:
    rate-limit:
      enabled: true
      limit: 100
      window: 1m
      key-type: IP_ADDRESS # IP_ADDRESS | USER | API_KEY
      api-key-header: X-API-Key # Apenas para key-type: API_KEY
```

```java
@Bean
@ConditionalOnProperty(prefix = "commons.web.rate-limit", name = "enabled", havingValue = "true")
public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties props) {
    Function<HttpServletRequest, String> keyExtractor = switch (props.getKeyType()) {
        case IP_ADDRESS -> HttpServletRequest::getRemoteAddr;
        case USER -> req -> req.getUserPrincipal() != null
            ? req.getUserPrincipal().getName()
            : req.getRemoteAddr();
        case API_KEY -> req -> req.getHeader(props.getApiKeyHeader());
    };

    RateLimitFilter filter = RateLimitFilter.builder()
        .limit(props.getLimit())
        .window(props.getWindow())
        .keyExtractor(keyExtractor)
        .build();

    FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(filter);
    bean.setUrlPatterns(List.of("/api/*"));
    bean.setOrder(5);
    return bean;
}
```

#### Response Headers

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1676314800
```

#### Resposta de Rate Limit Excedido

```json
{
  "status": 429,
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please try again later."
}
```

**⚠️ Nota**: Esta implementação é **in-memory** e adequada para single instances. Para ambientes distribuídos, use **Redis-based rate limiting** (ex: Bucket4j com Redis).

---

### 5. CORS Configuration Helpers

Helpers para configuração fácil de CORS.

#### Configuração via Propriedades

```yaml
commons:
  web:
    cors:
      enabled: true
      allowed-origins:
        - http://localhost:3000
        - https://app.example.com
      allowed-origin-patterns:
        - https://*.example.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
      allowed-headers:
        - "*"
      exposed-headers:
        - X-Correlation-Id
        - X-Total-Count
      allow-credentials: true
      max-age: 3600
      path-patterns:
        - /api/**
```

```java
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(CorsProperties corsProperties) {
        return CorsConfigurationHelper.fromProperties(corsProperties);
    }
}
```

#### Configuração Programática

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsConfigurationHelper.builder()
            .allowedOrigins("http://localhost:3000", "https://app.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .exposedHeaders("X-Correlation-Id", "X-Total-Count")
            .allowCredentials(true)
            .maxAge(3600)
            .applyTo(registry, "/api/**");
    }
}
```

---

## 📁 Estrutura do Módulo

```
commons-adapters-web-spring/
├── exception/
│   └── ProblemExceptionHandler.java      # Handler global de exceções (RFC 7807)
├── filter/
│   ├── CorrelationIdFilter.java          # Propagação de correlation ID
│   ├── RequestContextFilter.java         # Limpeza de contexto
│   ├── RequestResponseLoggingFilter.java # Logging de req/res
│   └── WebHeaders.java                   # Constantes de headers
├── ratelimit/
│   ├── RateLimitFilter.java              # Rate limiting (token bucket)
│   └── RateLimitProperties.java          # Propriedades de rate limiting
├── cors/
│   ├── CorsProperties.java               # Propriedades de CORS
│   └── CorsConfigurationHelper.java      # Helper para CORS
├── result/                               # Mappers Result<T> → ResponseEntity
├── envelope/                             # API response envelopes
├── context/                              # Request context (tenant, actor)
├── client/                               # RestTemplate context propagation
└── config/                               # Auto-configurations
```

---

## 🔧 Configuração Completa

### application.yml

```yaml
commons:
  web:
    # CORS
    cors:
      enabled: true
      allowed-origins:
        - http://localhost:3000
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers:
        - "*"
      exposed-headers:
        - X-Correlation-Id
        - X-Total-Count
      allow-credentials: true
      max-age: 3600

    # Rate Limiting
    rate-limit:
      enabled: true
      limit: 100
      window: 1m
      key-type: IP_ADDRESS

    # Context (tenant, actor)
    context:
      tenant-header: X-Tenant-Id
      actor-header: X-Actor-Id

# Logging
logging:
  level:
    com.marcusprado02.commons.adapters.web.spring.filter.RequestResponseLoggingFilter: DEBUG
```

### @Configuration

```java
@Configuration
@EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class})
public class WebSecurityConfig {

    // CORS
    @Bean
    public WebMvcConfigurer corsConfigurer(CorsProperties props) {
        return CorsConfigurationHelper.fromProperties(props);
    }

    // Rate Limiting
    @Bean
    @ConditionalOnProperty(prefix = "commons.web.rate-limit", name = "enabled")
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties props) {
        RateLimitFilter filter = RateLimitFilter.builder()
            .limit(props.getLimit())
            .window(props.getWindow())
            .keyExtractor(HttpServletRequest::getRemoteAddr)
            .build();

        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.setUrlPatterns(List.of("/api/*"));
        bean.setOrder(5);
        return bean;
    }

    // Request/Response Logging
    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestResponseLoggingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestResponseLoggingFilter());
        bean.setOrder(10);
        return bean;
    }

    // Exception Handler (já auto-registrado via @RestControllerAdvice)
    @Bean
    public HttpProblemMapper problemMapper() {
        return new DefaultHttpProblemMapper();
    }
}
```

---

## 🧪 Testing

### Exception Handler Test

```java
@WebMvcTest
class ProblemExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn400ForValidationError() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"invalid\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.errors.email").exists());
    }
}
```

### Rate Limit Test

```java
@Test
void shouldReturn429AfterRateLimitExceeded() {
    RateLimitFilter filter = RateLimitFilter.builder()
        .limit(5)
        .window(Duration.ofMinutes(1))
        .keyExtractor(HttpServletRequest::getRemoteAddr)
        .build();

    // 5 requisições OK
    for (int i = 0; i < 5; i++) {
        // assert 200
    }

    // 6ª requisição: 429
    // assert 429 Too Many Requests
}
```

---

## 🔗 Dependências

- `commons-adapters-web` → Interfaces e DTOs base
- `commons-kernel-errors` → DomainException, Problem
- `commons-app-observability` → CorrelationId, RequestContext
- `commons-adapters-otel` → OpenTelemetry baggage
- Spring Boot Starter Web → Spring MVC

---

## 📚 Veja Também

- [commons-adapters-web-spring-webflux](../commons-adapters-web-spring-webflux) → Reactive (WebFlux)
- [commons-spring-starter-observability](../commons-spring-starter-observability) → Auto-config de observabilidade
- [commons-kernel-errors](../commons-kernel-errors) → Error handling DDD

---

## 📝 Licença

MIT License - veja [LICENSE](../LICENSE)
