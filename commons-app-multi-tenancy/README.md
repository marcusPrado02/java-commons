# Commons Multi-Tenancy

A comprehensive multi-tenancy support library providing flexible tenant identification and data isolation strategies for Java applications.

## Features

- **Multiple Tenant Identification Strategies**
  - HTTP Header-based resolution
  - Subdomain-based resolution  
  - URL Path-based resolution
  - Composite resolver with priority ordering

- **Flexible Data Isolation Approaches**
  - Database-per-tenant with connection pooling
  - Schema-per-tenant with dynamic switching
  - Shared database with application-level filtering

- **Spring Framework Integration**
  - Servlet filters for tenant resolution
  - MVC interceptors for Spring applications
  - Auto-configuration for Spring Boot

- **Thread-Safe Context Management**
  - ThreadLocal tenant context storage
  - Context propagation utilities
  - Automatic cleanup and isolation

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-multi-tenancy</artifactId>
    <version>${commons.version}</version>
</dependency>
```

### Basic Usage

```java
// Set tenant context
TenantContext context = TenantContext.of("tenant123", "Acme Corp");
TenantContextHolder.setContext(context);

// Access current tenant
String tenantId = TenantContextHolder.getCurrentTenantId();
TenantContext current = TenantContextHolder.getContext();

// Run with specific tenant context
TenantContextHolder.runWithContext(context, () -> {
    // Code with tenant context
    doSomethingWithTenant();
});
```

## Tenant Identification

### Header-Based Resolution

Extracts tenant ID from HTTP headers (commonly `X-Tenant-ID`):

```java
TenantResolver<HttpServletRequest> resolver = new HeaderTenantResolver("X-Tenant-ID");

// Usage in servlet
Optional<TenantContext> tenant = resolver.resolve(request);
```

**Example HTTP Request:**
```
GET /api/users HTTP/1.1
Host: api.example.com
X-Tenant-ID: acme-corp
```

### Subdomain-Based Resolution

Extracts tenant ID from subdomain:

```java
// Default: extract first subdomain part
TenantResolver<HttpServletRequest> resolver = new SubdomainTenantResolver();

// With prefix/suffix stripping
TenantResolver<HttpServletRequest> resolver = new SubdomainTenantResolver(
    new String[]{"api-", "app-"},  // prefixes to strip
    new String[]{"-staging", "-dev"} // suffixes to strip
);
```

**Example HTTP Requests:**
```
Host: acme-corp.example.com          → tenant: "acme-corp"
Host: api-acme-corp.example.com      → tenant: "acme-corp" (with prefix stripping)
Host: acme-corp-staging.example.com  → tenant: "acme-corp" (with suffix stripping)
```

### Path-Based Resolution

Extracts tenant ID from URL path using regex patterns:

```java
// Default pattern: /([^/]+)/.* (first path segment)
TenantResolver<HttpServletRequest> resolver = new PathTenantResolver();

// Custom pattern
TenantResolver<HttpServletRequest> resolver = new PathTenantResolver("/api/v1/([^/]+)/.*");
```

**Example URLs:**
```
/acme-corp/api/users     → tenant: "acme-corp"
/api/v1/acme-corp/users  → tenant: "acme-corp" (with custom pattern)
```

### Composite Resolution

Combines multiple resolvers with priority ordering:

```java
CompositeTenantResolver<HttpServletRequest> composite = 
    CompositeTenantResolver.<HttpServletRequest>builder()
        .resolver(new HeaderTenantResolver("X-Tenant-ID"))      // Priority 10
        .resolver(new SubdomainTenantResolver())                // Priority 20  
        .resolver(new PathTenantResolver())                     // Priority 30
        .build();
```

Resolvers are tried in priority order (lower number = higher priority) until one successfully resolves a tenant.

## Data Isolation Strategies

### Database-per-Tenant

Creates separate database connections/pools for each tenant:

```java
// Configuration provider for each tenant
Function<String, HikariConfig> configProvider = tenantId -> {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:postgresql://localhost/" + tenantId + "_db");
    config.setUsername("user");
    config.setPassword("password");
    config.setMaximumPoolSize(10);
    return config;
};

TenantIsolationStrategy strategy = new DatabaseIsolationStrategy(configProvider);
DataSource dataSource = strategy.getDataSource(); // Returns tenant-specific DataSource
```

**Benefits:**
- Complete data isolation
- Independent scaling per tenant
- Tenant-specific configurations

**Trade-offs:**
- Higher resource usage
- Complex backup/maintenance
- Connection pool overhead

### Schema-per-Tenant

Uses single database with tenant-specific schemas:

```java
DataSource sharedDataSource = // your shared DataSource

// Default schema pattern: uses tenant ID as schema name
TenantIsolationStrategy strategy = new SchemaIsolationStrategy(sharedDataSource);

// Custom schema naming pattern
TenantIsolationStrategy strategy = new SchemaIsolationStrategy(
    sharedDataSource, 
    "app_{tenant}_schema"  // {tenant} replaced with actual tenant ID
);

DataSource dataSource = strategy.getDataSource(); // Automatically switches to tenant schema
```

**Benefits:**
- Moderate isolation
- Shared infrastructure
- Easier maintenance than database-per-tenant

**Trade-offs:**
- Schema-level security dependency
- Potential cross-tenant data leaks
- Database-specific implementation

### Shared Database with Application Filtering

Uses single database with application-level tenant filtering:

```java
DataSource sharedDataSource = // your shared DataSource
TenantIsolationStrategy strategy = new SharedDatabaseIsolationStrategy(sharedDataSource);

DataSource dataSource = strategy.getDataSource(); // Returns same shared DataSource
```

**Application Code Example:**
```java
// In your repository/DAO layer
public List<Order> findOrdersForCurrentTenant() {
    String tenantId = TenantContextHolder.getCurrentTenantId();
    return jdbcTemplate.query(
        "SELECT * FROM orders WHERE tenant_id = ?", 
        tenantId
    );
}
```

**Benefits:**
- Resource efficient
- Simple infrastructure
- Easy data analytics across tenants

**Trade-offs:**
- Requires careful application code
- Risk of data leakage bugs
- Complex queries for tenant filtering

## Spring Integration

### Servlet Filter (Recommended)

Automatically resolves tenant for all HTTP requests:

```java
@Bean
public FilterRegistrationBean<TenantFilter> tenantFilter(
        TenantResolver<HttpServletRequest> tenantResolver) {
    FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new TenantFilter(tenantResolver));
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
}
```

### Spring MVC Interceptor

Alternative to servlet filter for Spring MVC applications:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantInterceptor(tenantResolver))
                .addPathPatterns("/**");
    }
}
```

### Spring Boot Auto-Configuration

For Spring Boot applications, auto-configuration provides defaults:

```java
// Automatically configured when commons-app-multi-tenancy is on classpath
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Default Configuration:**
- CompositeTenantResolver with header → subdomain → path priority
- TenantFilter registered on all URLs
- Tenant context cleanup after each request

### Custom Configuration

Override defaults with your own beans:

```java
@Configuration
public class MultiTenancyConfig {
    
    @Bean
    @Primary
    public TenantResolver<HttpServletRequest> customTenantResolver() {
        return CompositeTenantResolver.<HttpServletRequest>builder()
            .resolver(new HeaderTenantResolver("X-Client-ID"))
            .resolver(new SubdomainTenantResolver("client-"))
            .build();
    }
    
    @Bean
    public TenantIsolationStrategy tenantIsolationStrategy() {
        return new DatabaseIsolationStrategy(this::createTenantConfig);
    }
    
    private HikariConfig createTenantConfig(String tenantId) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost/" + tenantId);
        // ... additional configuration
        return config;
    }
}
```

## Advanced Usage

### Tenant Context with Attributes

Store additional tenant-specific information:

```java
TenantContext context = TenantContext.builder()
    .tenantId("acme-corp")
    .name("Acme Corporation")  
    .domain("acme.example.com")
    .attribute("plan", "premium")
    .attribute("maxUsers", 1000)
    .attribute("region", "us-east-1")
    .attribute("features", List.of("analytics", "reporting"))
    .build();

// Access attributes with type safety
Optional<String> plan = context.getAttribute("plan", String.class);
Optional<Integer> maxUsers = context.getAttribute("maxUsers", Integer.class);
Optional<List> features = context.getAttribute("features", List.class);
```

### Custom Tenant Resolver

Implement custom tenant identification logic:

```java
public class DatabaseTenantResolver implements TenantResolver<HttpServletRequest> {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<TenantContext> resolve(HttpServletRequest request) {
        String userId = extractUserId(request);
        if (userId == null) {
            return Optional.empty();
        }
        
        return userRepository.findById(userId)
            .map(user -> TenantContext.builder()
                .tenantId(user.getTenantId())
                .name(user.getTenantName())
                .attribute("userId", userId)
                .build());
    }
    
    @Override
    public int getPriority() {
        return 5; // Higher priority than defaults
    }
}
```

### Async Context Propagation

Ensure tenant context is preserved across async operations:

```java
@Service
public class OrderService {
    
    @Async
    public CompletableFuture<Order> processOrderAsync(Long orderId) {
        TenantContext context = TenantContextHolder.getContext();
        
        return CompletableFuture.supplyAsync(() -> 
            TenantContextHolder.supplyWithContext(context, () -> {
                // Process order with tenant context
                return orderRepository.findById(orderId);
            })
        );
    }
}
```

### Database Migration per Tenant

Example using Flyway for schema-per-tenant:

```java
@Service
public class TenantMigrationService {
    
    public void migrateTenant(String tenantId) {
        TenantContextHolder.runWithContext(
            TenantContext.of(tenantId), 
            () -> {
                DataSource dataSource = isolationStrategy.getDataSource();
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas("tenant_" + tenantId)
                    .load();
                flyway.migrate();
            }
        );
    }
}
```

## Configuration Properties

When using Spring Boot auto-configuration:

```yaml
# application.yml
commons:
  multi-tenancy:
    resolver:
      header:
        enabled: true
        header-name: X-Tenant-ID
      subdomain:
        enabled: true
        prefixes: ["api-", "app-"]
        suffixes: ["-staging", "-dev"]  
      path:
        enabled: true
        pattern: "/([^/]+)/.*"
    filter:
      enabled: true
      url-patterns: ["/*"]
      order: -100
```

## Testing

### Unit Tests with Tenant Context

```java
@Test
void shouldProcessOrderForTenant() {
    TenantContext context = TenantContext.of("test-tenant");
    
    TenantContextHolder.runWithContext(context, () -> {
        // Test with tenant context
        Order order = orderService.createOrder(orderData);
        assertThat(order.getTenantId()).isEqualTo("test-tenant");
    });
}
```

### Integration Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "commons.multi-tenancy.resolver.header.enabled=true"
})
class MultiTenancyIntegrationTest {
    
    @Test
    void shouldResolveTenantFromHeader() {
        mockMvc.perform(get("/api/orders")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpected(status().isOk());
    }
}
```

## Security Considerations

### Tenant Isolation Validation

Always validate tenant access in your application:

```java
@PreAuthorize("hasAccessToTenant(#orderId)")
public Order getOrder(Long orderId) {
    String tenantId = TenantContextHolder.getCurrentTenantId();
    return orderRepository.findByIdAndTenant(orderId, tenantId);
}
```

### SQL Injection Prevention

When using shared database strategy, use parameterized queries:

```java
// ✅ Safe
jdbcTemplate.query(
    "SELECT * FROM orders WHERE tenant_id = ? AND status = ?",
    tenantId, status
);

// ❌ Dangerous - SQL injection risk  
jdbcTemplate.query(
    "SELECT * FROM orders WHERE tenant_id = '" + tenantId + "'"
);
```

### Input Validation

Validate tenant IDs to prevent directory traversal and injection:

```java
public class TenantValidator {
    private static final Pattern VALID_TENANT_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    public static boolean isValid(String tenantId) {
        return tenantId != null 
            && tenantId.length() <= 50
            && VALID_TENANT_ID.matcher(tenantId).matches();
    }
}
```

## Performance Considerations

### Connection Pool Sizing

For database-per-tenant strategy:

```java
private HikariConfig createConfig(String tenantId) {
    HikariConfig config = new HikariConfig();
    // Conservative pool sizing for many tenants
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    return config;
}
```

### Cache Tenant Context

Avoid repeated tenant resolution:

```java
@Component
public class CachingTenantResolver implements TenantResolver<HttpServletRequest> {
    
    @Cacheable(value = "tenant-cache", key = "#request.remoteAddr")
    public Optional<TenantContext> resolve(HttpServletRequest request) {
        // Expensive tenant resolution logic
    }
}
```

### Monitor Resource Usage

```java
@Component
public class TenantMetrics {
    
    @EventListener
    public void onTenantContextSet(TenantContextSetEvent event) {
        meterRegistry.counter("tenant.context.set", 
            "tenant", event.getTenantId()).increment();
    }
}
```

## Troubleshooting

### Common Issues

1. **Tenant context not available**
   ```
   java.lang.IllegalStateException: No tenant context available
   ```
   - Ensure tenant resolver is properly configured
   - Check filter/interceptor ordering
   - Verify tenant identification in requests

2. **Data source not found**
   ```
   javax.sql.SQLException: No suitable driver found for jdbc:postgresql://tenant123
   ```
   - Verify database exists for tenant
   - Check JDBC URL construction
   - Ensure database driver is available

3. **Cross-tenant data leakage**
   - Review all queries for tenant filtering
   - Use integration tests with multiple tenants
   - Implement tenant access validation

### Debug Logging

Enable debug logging to troubleshoot issues:

```yaml
logging:
  level:
    com.marcusprado02.commons.app.multitenancy: DEBUG
```

### Health Checks

Monitor tenant-specific resources:

```java
@Component
public class TenantHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check tenant data sources
            for (String tenantId : getActiveTenants()) {
                DataSource ds = getDataSourceForTenant(tenantId);
                ds.getConnection().close();
            }
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

## Best Practices

1. **Choose the Right Strategy**
   - Database-per-tenant: High isolation, regulatory requirements
   - Schema-per-tenant: Moderate isolation, shared infrastructure  
   - Shared database: High density, cost optimization

2. **Implement Proper Validation**
   - Always validate tenant access
   - Use parameterized queries
   - Sanitize tenant identifiers

3. **Monitor Performance**
   - Track connection pool usage
   - Monitor query performance per tenant
   - Set up alerting for tenant-specific issues

4. **Plan for Growth**
   - Start with shared database, migrate as needed
   - Implement tenant archival strategies
   - Consider sharding for large tenant counts

5. **Test Thoroughly**
   - Test tenant isolation
   - Validate data access controls  
   - Performance test with multiple tenants

## Dependencies

This library has minimal dependencies and uses optional dependencies for flexibility:

**Required:**
- `commons-kernel-errors` (error handling)
- `commons-kernel-result` (result types)

**Optional:**
- Spring Framework (for Spring integration)
- HikariCP (for connection pooling)
- Jakarta Servlet API (for servlet filters)

## License

This library is part of the Marcus Commons project and is licensed under the Apache License 2.0.
