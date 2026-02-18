# API Reference: commons-app-multi-tenancy

## Visão Geral

O módulo `commons-app-multi-tenancy` fornece infraestrutura para aplicações SaaS multi-tenant, com suporte a isolamento de dados em múltiplos níveis.

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-multi-tenancy</artifactId>
</dependency>
```

---

## Core Concepts

### TenantId

Identificador tipado de tenant.

```java
public class TenantId extends Identifier {
    
    public static TenantId of(String value) {
        return new TenantId(value);
    }
    
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID().toString());
    }
}
```

### TenantContext

Contexto thread-local que armazena o tenant atual.

```java
public class TenantContext {
    
    private static final ThreadLocal<TenantId> currentTenant = new ThreadLocal<>();
    
    /**
     * Define o tenant atual
     */
    public static void setCurrentTenant(TenantId tenantId) {
        currentTenant.set(tenantId);
    }
    
    /**
     * Retorna o tenant atual
     */
    public static Optional<TenantId> getCurrentTenant() {
        return Optional.ofNullable(currentTenant.get());
    }
    
    /**
     * Retorna o tenant atual ou lança exception
     */
    public static TenantId requireCurrentTenant() {
        return getCurrentTenant()
            .orElseThrow(() -> new TenantNotFoundException("No tenant context"));
    }
    
    /**
     * Limpa o contexto
     */
    public static void clear() {
        currentTenant.remove();
    }
}
```

---

## Isolation Strategies

### 1. Database per Tenant

Cada tenant tem seu próprio banco de dados.

```java
@Configuration
public class MultiTenantDataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        AbstractRoutingDataSource dataSource = new TenantRoutingDataSource();
        
        // Map de tenant → datasource
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("tenant-1", createDataSource("tenant1_db"));
        targetDataSources.put("tenant-2", createDataSource("tenant2_db"));
        
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(createDefaultDataSource());
        
        return dataSource;
    }
}

public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant()
            .map(TenantId::value)
            .orElse(null);
    }
}
```

### 2. Schema per Tenant

Mesmo banco, schemas diferentes.

```java
@Component
public class TenantSchemaResolver {
    
    private final JdbcTemplate jdbcTemplate;
    
    public void setTenantSchema() {
        TenantId tenantId = TenantContext.requireCurrentTenant();
        String schema = toSchemaName(tenantId);
        
        // PostgreSQL
        jdbcTemplate.execute("SET search_path TO " + schema);
        
        // MySQL
        // jdbcTemplate.execute("USE " + schema);
    }
    
    private String toSchemaName(TenantId tenantId) {
        return "tenant_" + tenantId.value().replaceAll("-", "_");
    }
}

@Aspect
@Component
public class TenantSchemaAspect {
    
    private final TenantSchemaResolver schemaResolver;
    
    @Before("@annotation(Transactional)")
    public void setSchema() {
        schemaResolver.setTenantSchema();
    }
}
```

### 3. Row-Level Security (Discriminator Column)

Mesma tabela, coluna `tenant_id` discrimina.

```java
@Entity
@Table(name = "users")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User extends Entity<UserId> {
    
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;
    
    // Getters/setters
}

@Component
public class TenantFilterAspect {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Before("execution(* com.example..*Repository.*(..))")
    public void enableTenantFilter() {
        TenantId tenantId = TenantContext.requireCurrentTenant();
        
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId.value());
    }
}
```

---

## Tenant Resolution

### TenantResolver

Interface para resolução de tenant a partir de request.

```java
public interface TenantResolver {
    
    /**
     * Resolve o tenant ID a partir do contexto atual
     */
    Result<TenantId> resolveTenant();
}
```

### Implementações

#### 1. Header-Based

```java
@Component
public class HeaderTenantResolver implements TenantResolver {
    
    private final HttpServletRequest request;
    
    @Override
    public Result<TenantId> resolveTenant() {
        String tenantHeader = request.getHeader("X-Tenant-ID");
        
        if (tenantHeader == null || tenantHeader.isBlank()) {
            return Result.fail(Problem.of(
                "TENANT.MISSING_HEADER",
                "Missing X-Tenant-ID header"
            ));
        }
        
        return Result.ok(TenantId.of(tenantHeader));
    }
}
```

#### 2. Subdomain-Based

```java
@Component
public class SubdomainTenantResolver implements TenantResolver {
    
    private final HttpServletRequest request;
    
    @Override
    public Result<TenantId> resolveTenant() {
        String host = request.getServerName();  // ex: acme.myapp.com
        
        String[] parts = host.split("\\.");
        if (parts.length < 3) {
            return Result.fail(Problem.of(
                "TENANT.INVALID_SUBDOMAIN",
                "Cannot resolve tenant from host: " + host
            ));
        }
        
        String subdomain = parts[0];  // "acme"
        return Result.ok(TenantId.of(subdomain));
    }
}
```

#### 3. JWT-Based

```java
@Component
public class JwtTenantResolver implements TenantResolver {
    
    private final JwtDecoder jwtDecoder;
    private final HttpServletRequest request;
    
    @Override
    public Result<TenantId> resolveTenant() {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail(Problem.of("TENANT.MISSING_TOKEN", "Missing JWT token"));
        }
        
        String token = authHeader.substring(7);
        
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String tenantClaim = jwt.getClaimAsString("tenant_id");
            
            if (tenantClaim == null) {
                return Result.fail(Problem.of("TENANT.MISSING_CLAIM", "JWT missing tenant_id claim"));
            }
            
            return Result.ok(TenantId.of(tenantClaim));
            
        } catch (Exception e) {
            return Result.fail(Problem.of("TENANT.INVALID_TOKEN", "Invalid JWT token"));
        }
    }
}
```

---

## Web Integration

### Tenant Filter

```java
@Component
public class TenantFilter implements Filter {
    
    private final TenantResolver tenantResolver;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        try {
            // Resolve tenant
            Result<TenantId> result = tenantResolver.resolveTenant();
            
            if (result.isFail()) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
                httpResponse.getWriter().write("Tenant resolution failed");
                return;
            }
            
            // Set context
            TenantId tenantId = result.getOrThrow();
            TenantContext.setCurrentTenant(tenantId);
            
            // Continue chain
            chain.doFilter(request, response);
            
        } finally {
            // Clear context
            TenantContext.clear();
        }
    }
}
```

### Spring Configuration

```java
@Configuration
public class MultiTenancyConfig {
    
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
```

---

## Messaging Integration

### Tenant Propagation

```java
@Component
public class TenantPropagatingMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Override
    public void publish(Message message) {
        // Adiciona tenant-id ao header
        TenantContext.getCurrentTenant().ifPresent(tenantId -> {
            message.headers().put("tenant-id", tenantId.value());
        });
        
        kafkaTemplate.send(
            message.topic(),
            message.key(),
            message.payload()
        );
    }
}

@Component
public class TenantRestoringMessageListener {
    
    @KafkaListener(topics = "orders.created")
    public void handleOrderCreated(
        @Payload OrderCreated event,
        @Header("tenant-id") String tenantId
    ) {
        try {
            // Restaura contexto
            TenantContext.setCurrentTenant(TenantId.of(tenantId));
            
            // Processa mensagem
            processOrder(event);
            
        } finally {
            TenantContext.clear();
        }
    }
}
```

---

## Observability Integration

### Request Context

```java
@Component
public class TenantRequestContextContributor implements RequestContextContributor {
    
    @Override
    public void contribute(RequestContextBuilder builder) {
        TenantContext.getCurrentTenant().ifPresent(tenantId -> {
            builder.withTenantId(tenantId.value());
        });
    }
}

// Logs incluem automaticamente tenant-id
log.info("Processing order");
// {"level":"INFO","message":"Processing order","tenantId":"acme","correlationId":"..."}
```

---

## Testing

### Test Utilities

```java
public class TenantTestUtils {
    
    public static void runAsTenant(TenantId tenantId, Runnable action) {
        try {
            TenantContext.setCurrentTenant(tenantId);
            action.run();
        } finally {
            TenantContext.clear();
        }
    }
    
    public static <T> T getAsTenant(TenantId tenantId, Supplier<T> supplier) {
        try {
            TenantContext.setCurrentTenant(tenantId);
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }
}
```

### Integration Test

```java
@SpringBootTest
class MultiTenancyIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    private final TenantId tenant1 = TenantId.of("tenant-1");
    private final TenantId tenant2 = TenantId.of("tenant-2");
    
    @Test
    void shouldIsolateDataBetweenTenants() {
        // Tenant 1 cria usuário
        TenantTestUtils.runAsTenant(tenant1, () -> {
            User user = new User(...);
            userRepository.save(user);
        });
        
        // Tenant 2 não vê usuário do Tenant 1
        List<User> tenant2Users = TenantTestUtils.getAsTenant(tenant2, () -> {
            return userRepository.findAll();
        });
        
        assertThat(tenant2Users).isEmpty();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Sempre valide tenant antes de operações críticas
TenantId tenantId = TenantContext.requireCurrentTenant();

// ✅ Limpe contexto em finally
try {
    TenantContext.setCurrentTenant(tenantId);
    process();
} finally {
    TenantContext.clear();
}

// ✅ Propague tenant em mensagens
message.headers().put("tenant-id", tenantId.value());

// ✅ Use filters com ordem alta
registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
```

### ❌ DON'T

```java
// ❌ NÃO confie no tenant do client sem validação
// ❌ NÃO permita cross-tenant queries
// ❌ NÃO esqueça de limpar contexto
// ❌ NÃO hardcode tenant IDs
```

---

## Ver Também

- [Observability](../guides/observability.md)
- [Domain Events](../guides/domain-events.md)
- [Testing Strategies](../guides/testing.md)
