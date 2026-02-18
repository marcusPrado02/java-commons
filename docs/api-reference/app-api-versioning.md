# API Reference: API Versioning

## Visão Geral

`commons-app-api-versioning` fornece estratégias robustas para versionamento de APIs REST, incluindo URI versioning, header versioning, content negotiation e API deprecation.

**Quando usar:**
- Evoluir APIs sem quebrar clientes existentes
- Manter múltiplas versões simultaneamente
- Deprecar versões antigas gradualmente
- Suportar diferentes contratos por cliente
- Gerenciar breaking changes

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-api-versioning</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### ApiVersion

Representa uma versão de API.

```java
public class ApiVersion implements Comparable<ApiVersion> {
    
    private final int major;
    private final int minor;
    private final int patch;
    
    public static ApiVersion of(String version) {
        // Parse "v1", "v1.2", "1.2.3"
        return new ApiVersion(major, minor, patch);
    }
    
    public boolean isCompatibleWith(ApiVersion other) {
        return this.major == other.major;
    }
    
    public boolean isNewerThan(ApiVersion other) {
        return this.compareTo(other) > 0;
    }
}
```

### VersioningStrategy

Estratégia para extrair versão da request.

```java
public interface VersioningStrategy {
    
    /**
     * Extrai versão da HTTP request.
     */
    Optional<ApiVersion> extractVersion(HttpServletRequest request);
    
    /**
     * Nome da estratégia.
     */
    String name();
}
```

### ApiVersionResolver

Resolve versão do endpoint para request.

```java
public interface ApiVersionResolver {
    
    /**
     * Resolve melhor versão disponível para request.
     */
    Result<ApiVersion> resolveVersion(
        HttpServletRequest request,
        List<ApiVersion> availableVersions
    );
    
    /**
     * Versão padrão quando não especificada.
     */
    ApiVersion defaultVersion();
}
```

---

## 💡 Estratégias de Versionamento

### 1. URI Versioning

Versão na URL (`/api/v1/orders`).

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseV1> getOrder(@PathVariable String id) {
        // Implementação V1
        return ResponseEntity.ok(new OrderResponseV1(...));
    }
    
    @PostMapping
    public ResponseEntity<OrderResponseV1> createOrder(
        @RequestBody CreateOrderRequestV1 request
    ) {
        // Implementação V1
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrderResponseV1(...));
    }
}

@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 {
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseV2> getOrder(@PathVariable String id) {
        // Implementação V2 com novos campos
        return ResponseEntity.ok(new OrderResponseV2(...));
    }
    
    @PostMapping
    public ResponseEntity<OrderResponseV2> createOrder(
        @RequestBody CreateOrderRequestV2 request
    ) {
        // Implementação V2
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrderResponseV2(...));
    }
}
```

**Vantagens:**
- ✅ Explícito e fácil de entender
- ✅ Fácil de testar (URLs diferentes)
- ✅ Funciona com cache HTTP

**Desvantagens:**
- ❌ Polui namespace de URLs
- ❌ Versão em múltiplos lugares

### 2. Header Versioning

Versão no header HTTP.

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
        @PathVariable String id,
        @RequestHeader(value = "Api-Version", required = false) String version
    ) {
        ApiVersion apiVersion = version != null 
            ? ApiVersion.of(version) 
            : ApiVersion.LATEST;
        
        return switch (apiVersion.major()) {
            case 1 -> getOrderV1(id);
            case 2 -> getOrderV2(id);
            default -> ResponseEntity
                .status(HttpStatus.NOT_ACCEPTABLE)
                .body(Problem.of(
                    "API.VERSION_NOT_SUPPORTED",
                    "API version not supported: " + version
                ));
        };
    }
}
```

**Custom Annotation:**

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    String value();
    String deprecatedSince() default "";
    String removedIn() default "";
}
```

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping("/{id}")
    @ApiVersion("1.0")
    @Deprecated
    public ResponseEntity<OrderResponseV1> getOrderV1(@PathVariable String id) {
        // V1 implementation
    }
    
    @GetMapping("/{id}")
    @ApiVersion("2.0")
    public ResponseEntity<OrderResponseV2> getOrderV2(@PathVariable String id) {
        // V2 implementation
    }
}
```

**Vantagens:**
- ✅ URL limpa e estável
- ✅ Versionamento granular (por endpoint)
- ✅ Fácil adicionar metadados de versão

**Desvantagens:**
- ❌ Menos explícito
- ❌ Difícil testar no browser
- ❌ Cache HTTP mais complexo

### 3. Content Negotiation

Versão no `Accept` ou `Content-Type`.

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping(
        value = "/{id}",
        produces = "application/vnd.myapp.order.v1+json"
    )
    public ResponseEntity<OrderResponseV1> getOrderV1(@PathVariable String id) {
        // V1
    }
    
    @GetMapping(
        value = "/{id}",
        produces = "application/vnd.myapp.order.v2+json"
    )
    public ResponseEntity<OrderResponseV2> getOrderV2(@PathVariable String id) {
        // V2
    }
}
```

**Request:**
```bash
# V1
curl -H "Accept: application/vnd.myapp.order.v1+json" \
  http://api.example.com/api/orders/123

# V2
curl -H "Accept: application/vnd.myapp.order.v2+json" \
  http://api.example.com/api/orders/123
```

**Vantagens:**
- ✅ Segue padrão HTTP (Content Negotiation)
- ✅ URL limpa
- ✅ Suporta múltiplos formatos (JSON, XML, etc.)

**Desvantagens:**
- ❌ Complexo de implementar
- ❌ Difícil de testar
- ❌ Menos conhecido

### 4. Query Parameter Versioning

Versão como query parameter.

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
        @PathVariable String id,
        @RequestParam(value = "version", defaultValue = "1") int version
    ) {
        return switch (version) {
            case 1 -> getOrderV1(id);
            case 2 -> getOrderV2(id);
            default -> ResponseEntity.badRequest().build();
        };
    }
}
```

**Request:**
```bash
# V1
GET /api/orders/123?version=1

# V2
GET /api/orders/123?version=2
```

**Vantagens:**
- ✅ Fácil de testar
- ✅ Simples de implementar

**Desvantagens:**
- ❌ Query parameters para dados, não controle
- ❌ Polui URL com parâmetros técnicos

---

## 🎯 Implementação Completa

### Version Interceptor

```java
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private final ApiVersionResolver versionResolver;
    private final DeprecationService deprecationService;
    
    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        
        // Extrair versões suportadas do endpoint
        ApiVersion annotation = handlerMethod.getMethodAnnotation(ApiVersion.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType()
                .getAnnotation(ApiVersion.class);
        }
        
        if (annotation == null) {
            return true;  // Sem versionamento
        }
        
        // Resolver versão da request
        List<ApiVersion> supportedVersions = 
            parseSupportedVersions(annotation.value());
        
        Result<ApiVersion> versionResult = 
            versionResolver.resolveVersion(request, supportedVersions);
        
        if (versionResult.isFail()) {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                toProblemJson(versionResult.problemOrNull())
            );
            return false;
        }
        
        ApiVersion resolvedVersion = versionResult.getOrThrow();
        request.setAttribute("api.version", resolvedVersion);
        
        // Verificar deprecation
        if (deprecationService.isDeprecated(resolvedVersion)) {
            DeprecationInfo info = deprecationService.getInfo(resolvedVersion);
            response.addHeader("Deprecation", "true");
            response.addHeader("Sunset", info.sunsetDate().toString());
            response.addHeader("Link", 
                "<" + info.migrationGuide() + ">; rel=\"deprecation\"");
        }
        
        return true;
    }
}
```

### Version Resolver

```java
@Component
public class DefaultApiVersionResolver implements ApiVersionResolver {
    
    private final List<VersioningStrategy> strategies;
    private final ApiVersion defaultVersion;
    
    public DefaultApiVersionResolver(
        List<VersioningStrategy> strategies,
        @Value("${api.version.default:1.0.0}") String defaultVersion
    ) {
        this.strategies = strategies;
        this.defaultVersion = ApiVersion.of(defaultVersion);
    }
    
    @Override
    public Result<ApiVersion> resolveVersion(
        HttpServletRequest request,
        List<ApiVersion> availableVersions
    ) {
        // Tentar extrair versão usando estratégias
        for (VersioningStrategy strategy : strategies) {
            Optional<ApiVersion> extracted = strategy.extractVersion(request);
            if (extracted.isPresent()) {
                ApiVersion requestedVersion = extracted.get();
                
                // Verificar se versão é suportada
                if (isSupported(requestedVersion, availableVersions)) {
                    return Result.ok(requestedVersion);
                }
                
                return Result.fail(Problem.of(
                    "API.VERSION_NOT_SUPPORTED",
                    "API version not supported: " + requestedVersion
                ));
            }
        }
        
        // Sem versão especificada, usar default
        if (isSupported(defaultVersion, availableVersions)) {
            return Result.ok(defaultVersion);
        }
        
        // Default não suportado, usar versão mais recente
        ApiVersion latest = availableVersions.stream()
            .max(Comparator.naturalOrder())
            .orElse(defaultVersion);
        
        return Result.ok(latest);
    }
    
    private boolean isSupported(
        ApiVersion requested,
        List<ApiVersion> available
    ) {
        return available.stream()
            .anyMatch(v -> v.isCompatibleWith(requested));
    }
}
```

---

## 📦 Response Transformation

### Version-Specific DTOs

```java
// V1 Response
public record OrderResponseV1(
    String orderId,
    String customerId,
    BigDecimal total,
    String status
) {}

// V2 Response (novos campos)
public record OrderResponseV2(
    String orderId,
    CustomerInfoV2 customer,  // Expandido
    List<OrderItemV2> items,  // Novo
    MoneyV2 total,            // Estruturado
    OrderStatusV2 status,     // Enum estruturado
    Instant createdAt,        // Novo
    Instant updatedAt         // Novo
) {}
```

### Adapter Pattern

```java
@Service
public class OrderResponseAdapter {
    
    public Object adapt(Order order, ApiVersion version) {
        return switch (version.major()) {
            case 1 -> toV1(order);
            case 2 -> toV2(order);
            default -> throw new UnsupportedVersionException(version);
        };
    }
    
    private OrderResponseV1 toV1(Order order) {
        return new OrderResponseV1(
            order.id().value(),
            order.customerId().value(),
            order.total().amount(),
            order.status().name()
        );
    }
    
    private OrderResponseV2 toV2(Order order) {
        return new OrderResponseV2(
            order.id().value(),
            new CustomerInfoV2(
                order.customerId().value(),
                order.customerName(),
                order.customerEmail()
            ),
            order.items().stream()
                .map(this::toItemV2)
                .toList(),
            new MoneyV2(
                order.total().amount(),
                order.total().currency()
            ),
            new OrderStatusV2(
                order.status().name(),
                order.status().description()
            ),
            order.createdAt(),
            order.updatedAt()
        );
    }
}
```

---

## 🚫 API Deprecation

### Deprecation Info

```java
public record DeprecationInfo(
    ApiVersion version,
    LocalDate deprecatedSince,
    LocalDate sunsetDate,
    String reason,
    String migrationGuide
) {}
```

### Deprecation Service

```java
@Service
public class DeprecationService {
    
    private final Map<ApiVersion, DeprecationInfo> deprecations;
    
    public DeprecationService(
        @Value("${api.deprecations}") List<DeprecationConfig> configs
    ) {
        this.deprecations = configs.stream()
            .collect(Collectors.toMap(
                c -> ApiVersion.of(c.version()),
                c -> new DeprecationInfo(
                    ApiVersion.of(c.version()),
                    c.deprecatedSince(),
                    c.sunsetDate(),
                    c.reason(),
                    c.migrationGuide()
                )
            ));
    }
    
    public boolean isDeprecated(ApiVersion version) {
        return deprecations.containsKey(version);
    }
    
    public boolean isRemoved(ApiVersion version) {
        DeprecationInfo info = deprecations.get(version);
        return info != null && LocalDate.now().isAfter(info.sunsetDate());
    }
    
    public Optional<DeprecationInfo> getInfo(ApiVersion version) {
        return Optional.ofNullable(deprecations.get(version));
    }
}
```

### Configuration

```yaml
api:
  deprecations:
    - version: "1.0"
      deprecated-since: "2024-01-01"
      sunset-date: "2024-06-01"
      reason: "Replaced by v2 with improved data model"
      migration-guide: "https://docs.example.com/migration/v1-to-v2"
```

### HTTP Response Headers

```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 01 Jun 2024 00:00:00 GMT
Link: <https://docs.example.com/migration/v1-to-v2>; rel="deprecation"
Warning: 299 - "API version 1.0 is deprecated and will be removed on 2024-06-01"
Content-Type: application/json

{
  "orderId": "order-123",
  "customerId": "customer-456",
  "total": 99.99,
  "status": "COMPLETED"
}
```

---

## 📊 Monitoring

### Version Usage Metrics

```java
@Component
public class ApiVersionMetricsCollector implements HandlerInterceptor {
    
    private final MetricsFacade metrics;
    
    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        ApiVersion version = (ApiVersion) request.getAttribute("api.version");
        if (version != null) {
            metrics.incrementCounter(
                "api.requests",
                "version", version.toString(),
                "endpoint", request.getRequestURI(),
                "method", request.getMethod(),
                "status", String.valueOf(response.getStatus())
            );
        }
    }
}
```

---

## 🧪 Testing

### Version Tests

```java
@WebMvcTest(OrderController.class)
class ApiVersioningTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void shouldReturnV1ResponseWhenV1Requested() throws Exception {
        when(orderService.findOrder(any()))
            .thenReturn(Result.ok(createTestOrder()));
        
        mockMvc.perform(get("/api/v1/orders/order-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("order-123"))
            .andExpect(jsonPath("$.total").value(99.99))
            .andExpect(jsonPath("$.items").doesNotExist());  // Not in V1
    }
    
    @Test
    void shouldReturnV2ResponseWhenV2Requested() throws Exception {
        when(orderService.findOrder(any()))
            .thenReturn(Result.ok(createTestOrder()));
        
        mockMvc.perform(get("/api/v2/orders/order-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("order-123"))
            .andExpect(jsonPath("$.total.amount").value(99.99))
            .andExpect(jsonPath("$.total.currency").value("USD"))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.createdAt").exists());
    }
    
    @Test
    void shouldReturnDeprecationHeadersForV1() throws Exception {
        mockMvc.perform(get("/api/v1/orders/order-123"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Deprecation"))
            .andExpect(header().string("Deprecation", "true"))
            .andExpect(header().exists("Sunset"))
            .andExpect(header().exists("Link"));
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use semantic versioning
ApiVersion.of("1.2.3")  // MAJOR.MINOR.PATCH

// ✅ Versione apenas breaking changes
// v1 -> v2: mudança na estrutura de dados
// v1.1 -> v1.2: novos campos opcionais (não quebra)

// ✅ Avise deprecation com antecedência
response.addHeader("Deprecation", "true");
response.addHeader("Sunset", "2024-06-01");

// ✅ Forneça guia de migração
Link: <https://docs.example.com/migration/v1-to-v2>; rel="deprecation"

// ✅ Monitore uso das versões
metrics.incrementCounter("api.requests", "version", "1.0");
```

### ❌ DON'T

```java
// ❌ NÃO remova versões sem aviso
// Mantenha pelo menos 6 meses após deprecation

// ❌ NÃO misture estratégias de versionamento
// Escolha uma e seja consistente

// ❌ NÃO versione para mudanças não-quebráveis
// Adicione campos opcionais sem mudar versão

// ❌ NÃO use versões muito granulares
// /api/v1.2.3/orders  ❌
// /api/v1/orders      ✅

// ❌ NÃO mude contratos sem mudar versão
public record OrderResponse(
    String id,
    BigDecimal total  // ❌ Era String, mudou para BigDecimal
) {}
```

---

## Ver Também

- [Web Adapters](adapters/README.md#web) - REST controllers
- [Testing Guide](../guides/testing.md) - Contract testing
- [API Gateway](app-api-gateway.md) - API Gateway patterns
