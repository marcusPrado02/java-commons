# Port: HTTP Client

## Visão Geral

`commons-ports-http` define contratos para clientes HTTP, abstraindo implementações como OkHttp, WebClient, ou HttpClient.

**Quando usar:**
- Chamadas REST para APIs externas
- Service-to-service communication
- Integração com third-party services
- HTTP requests com retry, timeout, circuit breaker

**Implementações disponíveis:**
- `commons-adapters-http-okhttp` - OkHttp (blocking)
- `commons-adapters-http-webclient` - WebClient (reactive)

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-http</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (implementação) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-http-okhttp</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Interfaces

### HttpClient

Cliente HTTP básico.

```java
public interface HttpClient {
    
    /**
     * Executa request HTTP.
     */
    Result<HttpResponse> execute(HttpRequest request);
    
    /**
     * Executa request de forma assíncrona.
     */
    CompletableFuture<Result<HttpResponse>> executeAsync(HttpRequest request);
}
```

### HttpRequest

Representa uma requisição HTTP.

```java
public class HttpRequest {
    
    private final HttpMethod method;
    private final String url;
    private final HttpHeaders headers;
    private final Optional<HttpBody> body;
    private final Duration timeout;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder method(HttpMethod method);
        public Builder url(String url);
        public Builder header(String name, String value);
        public Builder headers(HttpHeaders headers);
        public Builder body(HttpBody body);
        public Builder timeout(Duration timeout);
        public HttpRequest build();
    }
}

public enum HttpMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
}
```

### HttpResponse

Representa uma resposta HTTP.

```java
public class HttpResponse {
    
    private final int statusCode;
    private final HttpHeaders headers;
    private final Optional<byte[]> body;
    
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    public String bodyAsString() {
        return body.map(bytes -> new String(bytes, StandardCharsets.UTF_8))
            .orElse("");
    }
    
    public <T> Result<T> bodyAs(Class<T> type, ObjectMapper mapper) {
        if (body.isEmpty()) {
            return Result.fail(Problem.of("HTTP.EMPTY_BODY", "Response body is empty"));
        }
        
        try {
            T value = mapper.readValue(body.get(), type);
            return Result.ok(value);
        } catch (IOException e) {
            return Result.fail(Problem.of("HTTP.PARSE_ERROR", e.getMessage()));
        }
    }
}
```

---

## 💡 Usage Examples

### Simple GET Request

```java
@Service
public class WeatherService {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public Result<Weather> getCurrentWeather(String city) {
        HttpRequest request = HttpRequest.builder()
            .method(HttpMethod.GET)
            .url("https://api.weather.com/v1/current?city=" + city)
            .header("X-API-Key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .build();
        
        return httpClient.execute(request)
            .flatMap(response -> {
                if (!response.isSuccessful()) {
                    return Result.fail(Problem.of(
                        "WEATHER.API_ERROR",
                        "API returned status: " + response.statusCode()
                    ));
                }
                
                return response.bodyAs(Weather.class, objectMapper);
            });
    }
}
```

### POST Request with JSON Body

```java
@Service
public class PaymentService {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public Result<PaymentResponse> processPayment(PaymentRequest paymentRequest) {
        // Serialize request to JSON
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(paymentRequest);
        } catch (JsonProcessingException e) {
            return Result.fail(Problem.of("PAYMENT.SERIALIZATION_ERROR", e.getMessage()));
        }
        
        HttpRequest request = HttpRequest.builder()
            .method(HttpMethod.POST)
            .url("https://api.payment-gateway.com/v1/payments")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .body(HttpBody.json(jsonBody))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        return httpClient.execute(request)
            .flatMap(response -> {
                if (response.statusCode() == 401) {
                    return Result.fail(Problem.of("PAYMENT.UNAUTHORIZED", "Invalid token"));
                }
                
                if (response.statusCode() == 402) {
                    return Result.fail(Problem.of("PAYMENT.INSUFFICIENT_FUNDS", "Payment declined"));
                }
                
                if (!response.isSuccessful()) {
                    return Result.fail(Problem.of(
                        "PAYMENT.API_ERROR",
                        "Payment failed with status: " + response.statusCode()
                    ));
                }
                
                return response.bodyAs(PaymentResponse.class, objectMapper);
            });
    }
}
```

---

## 🔄 RestClient

Cliente REST de alto nível.

```java
public interface RestClient {
    
    /**
     * GET request.
     */
    <T> Result<T> get(String url, Class<T> responseType);
    
    /**
     * POST request.
     */
    <T, R> Result<R> post(String url, T body, Class<R> responseType);
    
    /**
     * PUT request.
     */
    <T, R> Result<R> put(String url, T body, Class<R> responseType);
    
    /**
     * PATCH request.
     */
    <T, R> Result<R> patch(String url, T body, Class<R> responseType);
    
    /**
     * DELETE request.
     */
    Result<Void> delete(String url);
    
    /**
     * Builder para configuração avançada.
     */
    RequestBuilder<T> request();
}
```

### RestClient Usage

```java
@Service
public class UserService {
    
    private final RestClient restClient;
    
    public Result<User> getUser(UserId userId) {
        return restClient.get(
            "https://api.users.com/v1/users/" + userId.value(),
            User.class
        );
    }
    
    public Result<User> createUser(CreateUserRequest request) {
        return restClient.post(
            "https://api.users.com/v1/users",
            request,
            User.class
        );
    }
    
    public Result<Void> deleteUser(UserId userId) {
        return restClient.delete(
            "https://api.users.com/v1/users/" + userId.value()
        );
    }
}
```

### Advanced Request Configuration

```java
public class OrderService {
    
    private final RestClient restClient;
    
    public Result<Order> getOrder(OrderId orderId) {
        return restClient.request()
            .method(HttpMethod.GET)
            .url("https://api.orders.com/v1/orders/" + orderId.value())
            .header("Accept", "application/json")
            .header("X-Tenant-Id", tenantId)
            .timeout(Duration.ofSeconds(10))
            .retryPolicy(RetryPolicy.exponentialBackoff(3))
            .responseType(Order.class)
            .execute();
    }
}
```

---

## 🔧 Resilience Integration

### With Circuit Breaker

```java
@Service
public class ResilientPaymentService {
    
    private final HttpClient httpClient;
    private final CircuitBreakerPolicy circuitBreaker;
    
    public ResilientPaymentService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.circuitBreaker = CircuitBreakerPolicy.builder()
            .failureThreshold(5)
            .successThreshold(2)
            .timeout(Duration.ofSeconds(60))
            .build();
    }
    
    public Result<PaymentResponse> processPayment(PaymentRequest request) {
        return circuitBreaker.execute(() -> {
            HttpRequest httpRequest = buildPaymentRequest(request);
            
            return httpClient.execute(httpRequest)
                .flatMap(response -> response.bodyAs(
                    PaymentResponse.class,
                    objectMapper
                ));
        });
    }
}
```

### With Retry Policy

```java
@Service
public class RetryableHttpService {
    
    private final HttpClient httpClient;
    private final RetryPolicy retryPolicy;
    
    public RetryableHttpService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.retryPolicy = RetryPolicy.builder()
            .maxAttempts(3)
            .backoff(Duration.ofSeconds(1))
            .exponential(true)
            .retryOn(IOException.class, TimeoutException.class)
            .build();
    }
    
    public Result<Data> fetchData(String url) {
        return retryPolicy.execute(() -> {
            HttpRequest request = HttpRequest.builder()
                .method(HttpMethod.GET)
                .url(url)
                .timeout(Duration.ofSeconds(10))
                .build();
            
            return httpClient.execute(request)
                .flatMap(response -> response.bodyAs(Data.class, objectMapper));
        });
    }
}
```

---

## 🌐 Multi-Service Client

### Service Registry Pattern

```java
@Configuration
public class HttpClientConfiguration {
    
    @Bean
    public RestClient userServiceClient(HttpClient httpClient) {
        return RestClient.builder()
            .httpClient(httpClient)
            .baseUrl("https://api.users.com")
            .defaultHeader("X-API-Version", "1.0")
            .defaultTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Bean
    public RestClient orderServiceClient(HttpClient httpClient) {
        return RestClient.builder()
            .httpClient(httpClient)
            .baseUrl("https://api.orders.com")
            .defaultHeader("X-API-Version", "2.0")
            .defaultTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    @Bean
    public RestClient paymentServiceClient(
        HttpClient httpClient,
        CircuitBreakerPolicy circuitBreaker
    ) {
        return RestClient.builder()
            .httpClient(httpClient)
            .baseUrl("https://api.payment.com")
            .defaultHeader("X-API-Version", "1.0")
            .defaultTimeout(Duration.ofSeconds(30))
            .circuitBreaker(circuitBreaker)
            .retryPolicy(RetryPolicy.exponentialBackoff(3))
            .build();
    }
}
```

### Typed Service Clients

```java
public interface UserServiceClient {
    Result<User> getUser(UserId userId);
    Result<List<User>> listUsers(UserFilter filter);
    Result<User> createUser(CreateUserRequest request);
    Result<User> updateUser(UserId userId, UpdateUserRequest request);
    Result<Void> deleteUser(UserId userId);
}

@Service
public class RestUserServiceClient implements UserServiceClient {
    
    private final RestClient restClient;
    
    @Override
    public Result<User> getUser(UserId userId) {
        return restClient.get("/users/" + userId.value(), User.class);
    }
    
    @Override
    public Result<List<User>> listUsers(UserFilter filter) {
        String queryParams = filter.toQueryString();
        
        return restClient.get("/users?" + queryParams, UserList.class)
            .map(UserList::users);
    }
    
    @Override
    public Result<User> createUser(CreateUserRequest request) {
        return restClient.post("/users", request, User.class);
    }
    
    @Override
    public Result<User> updateUser(UserId userId, UpdateUserRequest request) {
        return restClient.put("/users/" + userId.value(), request, User.class);
    }
    
    @Override
    public Result<Void> deleteUser(UserId userId) {
        return restClient.delete("/users/" + userId.value());
    }
}
```

---

## 🧪 Testing

### Mock HTTP Client

```java
public class MockHttpClient implements HttpClient {
    
    private final Map<String, HttpResponse> mockResponses = new HashMap<>();
    private final List<HttpRequest> capturedRequests = new ArrayList<>();
    
    public void mockResponse(String url, HttpResponse response) {
        mockResponses.put(url, response);
    }
    
    public void mockResponse(String url, int status, String body) {
        HttpResponse response = HttpResponse.builder()
            .statusCode(status)
            .body(body.getBytes(StandardCharsets.UTF_8))
            .build();
        
        mockResponses.put(url, response);
    }
    
    @Override
    public Result<HttpResponse> execute(HttpRequest request) {
        capturedRequests.add(request);
        
        HttpResponse response = mockResponses.get(request.url());
        
        if (response == null) {
            return Result.fail(Problem.of(
                "HTTP.NOT_MOCKED",
                "No mock response for: " + request.url()
            ));
        }
        
        return Result.ok(response);
    }
    
    public List<HttpRequest> getCapturedRequests() {
        return Collections.unmodifiableList(capturedRequests);
    }
    
    public void reset() {
        mockResponses.clear();
        capturedRequests.clear();
    }
}
```

### Test Example

```java
class WeatherServiceTest {
    
    private MockHttpClient httpClient;
    private WeatherService weatherService;
    
    @BeforeEach
    void setUp() {
        httpClient = new MockHttpClient();
        weatherService = new WeatherService(httpClient, new ObjectMapper());
    }
    
    @Test
    void shouldReturnWeatherSuccessfully() {
        // Given
        String responseJson = """
            {
                "city": "London",
                "temperature": 15.5,
                "condition": "Cloudy"
            }
            """;
        
        httpClient.mockResponse(
            "https://api.weather.com/v1/current?city=London",
            200,
            responseJson
        );
        
        // When
        Result<Weather> result = weatherService.getCurrentWeather("London");
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        Weather weather = result.getOrThrow();
        assertThat(weather.city()).isEqualTo("London");
        assertThat(weather.temperature()).isEqualTo(15.5);
        
        // Verify request
        List<HttpRequest> requests = httpClient.getCapturedRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).method()).isEqualTo(HttpMethod.GET);
    }
    
    @Test
    void shouldHandleApiError() {
        // Given: API returns 500
        httpClient.mockResponse(
            "https://api.weather.com/v1/current?city=London",
            500,
            "Internal Server Error"
        );
        
        // When
        Result<Weather> result = weatherService.getCurrentWeather("London");
        
        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.problemOrNull().type()).isEqualTo("WEATHER.API_ERROR");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use timeouts
HttpRequest request = HttpRequest.builder()
    .url(url)
    .timeout(Duration.ofSeconds(10))  // ✅ Sempre configurar!
    .build();

// ✅ Trate erros HTTP
if (!response.isSuccessful()) {
    return Result.fail(Problem.of(
        "API.ERROR",
        "Status: " + response.statusCode()
    ));
}

// ✅ Use circuit breaker para serviços externos
CircuitBreakerPolicy circuitBreaker = CircuitBreakerPolicy.builder()
    .failureThreshold(5)
    .timeout(Duration.ofSeconds(60))
    .build();

// ✅ Reutilize HttpClient (connection pooling)
@Bean
public HttpClient httpClient() {
    return OkHttpClientAdapter.builder()
        .connectionPool(50, Duration.ofMinutes(5))
        .build();
}

// ✅ Log requests/responses para debugging
log.info("HTTP request")
    .field("method", request.method())
    .field("url", request.url())
    .log();
```

### ❌ DON'T

```java
// ❌ NÃO crie novo client para cada request
HttpClient client = new OkHttpClientAdapter();  // ❌ Cada request!
client.execute(request);

// ❌ NÃO ignore timeouts
HttpRequest request = HttpRequest.builder()
    .url(url)
    .build();  // ❌ Sem timeout!

// ❌ NÃO exponha exceptions HTTP
public Weather getWeather(String city) throws IOException {  // ❌
    // Use Result pattern!
}

// ❌ NÃO ignore status codes
return response.bodyAs(Weather.class);  // ❌ E se 404? 500?

// ❌ NÃO hardcode URLs
String url = "https://api.service.com";  // ❌ Use configuration!
```

---

## Ver Também

- [Resilience Guide](../../guides/resilience.md) - Circuit breaker, retry
- [OkHttp Adapter](../../../commons-adapters-http-okhttp/) - Blocking implementation
- [WebClient Adapter](../../../commons-adapters-http-webclient/) - Reactive implementation
- [Observability Guide](../../guides/observability.md) - HTTP metrics
