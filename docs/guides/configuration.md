# Guia: Configuration Management

## Visão Geral

O módulo `commons-app-configuration` implementa o **Factor III (Config)** dos 12 factors, fornecendo configuração externalizada, feature flags e refresh dinâmico.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-configuration</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🎯 ConfigurationProvider

Interface unificada para acessar configurações de múltiplas fontes.

### Interface

```java
public interface ConfigurationProvider {
    Optional<String> getString(String key);
    Optional<Integer> getInt(String key);
    Optional<Long> getLong(String key);
    Optional<Boolean> getBoolean(String key);
    Optional<Double> getDouble(String key);
    
    Map<String, String> getProperties(String prefix);
    
    void addListener(String key, ConfigurationChangeListener listener);
    void enableAutoRefresh(Duration interval);
}
```

---

## 📚 Providers

### InMemoryConfigurationProvider

Para desenvolvimento e testes:

```java
InMemoryConfigurationProvider config = new InMemoryConfigurationProvider();

// Set valores
config.setProperty("database.url", "jdbc:postgresql://localhost/mydb");
config.setProperty("database.username", "admin");
config.setProperty("database.pool.size", "10");

// Leitura
String url = config.getString("database.url").orElse("default");
int poolSize = config.getInt("database.pool.size").orElse(5);

// Múltiplos valores
Map<String, String> dbProps = config.getProperties("database");
// {url=jdbc:..., username=admin, pool.size=10}
```

### EnvironmentConfigurationProvider

Lê variáveis de ambiente (12-factor):

```java
ConfigurationProvider config = new EnvironmentConfigurationProvider();

// Lê DATABASE_URL environment variable
String dbUrl = config.getString("database.url").orElse("default");

// Mapeia automaticamente:
// DATABASE__URL → database.url
// DATABASE__POOL__SIZE → database.pool.size
```

### SpringCloudConfigProvider

Para Spring Cloud Config Server:

```java
@Configuration
public class ConfigProviderConfig {
    
    @Bean
    public ConfigurationProvider springCloudConfig(Environment environment) {
        return new SpringCloudConfigProvider(environment);
    }
}

// Lê configurações do Config Server remoto
String feature = config.getString("features.new-checkout").orElse("false");
```

### ConsulConfigurationProvider

Para Consul KV Store:

```java
Consul consul = Consul.builder()
    .withUrl("http://consul:8500")
    .build();

ConfigurationProvider config = new ConsulConfigurationProvider(
    consul,
    "config/myapp"  // Prefix do Consul KV
);

// Lê config/myapp/database/url do Consul
String dbUrl = config.getString("database.url").orElse("default");
```

---

## 🔀 CompositeConfigurationProvider

Múltiplas fontes com prioridade:

```java
ConfigurationProvider config = new CompositeConfigurationProvider(
    new EnvironmentConfigurationProvider(),    // Prioridade 1 (maior)
    new ConsulConfigurationProvider(consul),   // Prioridade 2
    new InMemoryConfigurationProvider() {{     // Prioridade 3 (defaults)
        setProperty("database.pool.size", "10");
        setProperty("http.timeout", "5000");
    }}
);

// Busca na ordem: Environment → Consul → Defaults
int poolSize = config.getInt("database.pool.size").orElse(5);
```

### Exemplo Real

```java
@Configuration
public class AppConfigurationSetup {
    
    @Bean
    public ConfigurationProvider configurationProvider(
            Environment springEnv,
            Consul consul) {
        
        // Defaults
        InMemoryConfigurationProvider defaults = new InMemoryConfigurationProvider();
        defaults.setProperty("server.port", "8080");
        defaults.setProperty("database.pool.min", "5");
        defaults.setProperty("database.pool.max", "20");
        defaults.setProperty("cache.ttl", "300");
        
        // Composite com prioridade
        return new CompositeConfigurationProvider(
            // 1. Environment variables (maior prioridade)
            new EnvironmentConfigurationProvider(),
            
            // 2. Spring Cloud Config (externa)
            new SpringCloudConfigProvider(springEnv),
            
            // 3. Consul KV (service discovery)
            new ConsulConfigurationProvider(consul, "config/myapp"),
            
            // 4. Defaults (menor prioridade)
            defaults
        );
    }
}
```

---

## 🔄 Dynamic Refresh

### Auto-refresh

```java
ConfigurationProvider config = new ConsulConfigurationProvider(consul, "config");

// Recarrega a cada 30 segundos
config.enableAutoRefresh(Duration.ofSeconds(30));

// A cada refresh, detecta mudanças e notifica listeners
```

### Change Listeners

```java
config.addListener("database.pool.size", event -> {
    logger.info("Pool size changed from {} to {}", 
        event.oldValue(), 
        event.newValue());
    
    // Reconfigura pool de conexões em runtime
    int newSize = Integer.parseInt(event.newValue());
    connectionPool.resize(newSize);
});

config.addListener("feature.*", event -> {
    logger.info("Feature flag changed: {}", event.key());
    featureManager.reload();
});
```

### Exemplo Completo

```java
@Service
public class DynamicConnectionPool {
    
    private final DataSource dataSource;
    private final ConfigurationProvider config;
    
    @PostConstruct
    public void init() {
        // Configura pool inicial
        configurePool();
        
        // Listener para mudanças
        config.addListener("database.pool.max", event -> {
            int newMax = Integer.parseInt(event.newValue());
            logger.info("Reconfiguring pool max size to: {}", newMax);
            
            HikariDataSource hikari = (HikariDataSource) dataSource;
            hikari.setMaximumPoolSize(newMax);
        });
        
        config.addListener("database.pool.min", event -> {
            int newMin = Integer.parseInt(event.newValue());
            logger.info("Reconfiguring pool min size to: {}", newMin);
            
            HikariDataSource hikari = (HikariDataSource) dataSource;
            hikari.setMinimumIdle(newMin);
        });
        
        // Auto-refresh a cada 1 minuto
        config.enableAutoRefresh(Duration.ofMinutes(1));
    }
    
    private void configurePool() {
        int maxSize = config.getInt("database.pool.max").orElse(20);
        int minSize = config.getInt("database.pool.min").orElse(5);
        
        HikariDataSource hikari = (HikariDataSource) dataSource;
        hikari.setMaximumPoolSize(maxSize);
        hikari.setMinimumIdle(minSize);
    }
}
```

---

## 🚩 Feature Flags

### FeatureFlags

```java
public interface FeatureFlags {
    boolean isEnabled(String featureName);
    boolean isEnabled(String featureName, Map<String, Object> context);
    
    void enable(String featureName);
    void disable(String featureName);
    
    void setRolloutPercentage(String featureName, int percentage);
    void setVariants(String featureName, Map<String, Integer> weights);
    
    String getVariant(String featureName, String defaultVariant, 
                     Map<String, Object> context);
}
```

### Uso Básico

```java
FeatureFlags flags = new InMemoryFeatureFlags();

// Liga/desliga features
flags.enable("new-checkout");
flags.disable("experimental-search");

// Verifica se está ligada
if (flags.isEnabled("new-checkout")) {
    return newCheckoutFlow();
} else {
    return legacyCheckoutFlow();
}
```

### Rollout Gradual

```java
// Começa com 10% dos usuários
flags.setRolloutPercentage("new-dashboard", 10);

// Contexto com userId para consistência
Map<String, Object> context = Map.of(
    "userId", currentUser.id().value(),
    "region", currentUser.region()
);

if (flags.isEnabled("new-dashboard", context)) {
    return newDashboard();  // 10% verão isso
} else {
    return oldDashboard();  // 90% verão isso
}

// Depois de testar, aumenta gradualmente
flags.setRolloutPercentage("new-dashboard", 25);  // 25%
flags.setRolloutPercentage("new-dashboard", 50);  // 50%
flags.setRolloutPercentage("new-dashboard", 100); // 100% (todos)
```

### A/B Testing com Variants

```java
// Define variantes com pesos
flags.setVariants("button-color", Map.of(
    "control", 40,  // 40% veem botão original
    "red", 30,      // 30% veem botão vermelho
    "blue", 30      // 30% veem botão azul
));

// Obtém variante para usuário (consistente)
String variant = flags.getVariant(
    "button-color",
    "control",      // Default
    Map.of("userId", user.id().value())
);

switch (variant) {
    case "red" -> renderRedButton();
    case "blue" -> renderBlueButton();
    default -> renderControlButton();
}
```

### Context-Based Flags

```java
Map<String, Object> context = Map.of(
    "userId", user.id().value(),
    "email", user.email().value(),
    "accountType", user.accountType().name(),
    "createdAt", user.createdAt().toString(),
    "region", user.region()
);

// Feature só para premium
if (flags.isEnabled("premium-analytics", context)) {
    return premiumAnalytics();
}

// Feature por região
if (flags.isEnabled("eu-gdpr-banner", context)) {
    return gdprBanner();
}
```

### Metadata

```java
flags.setMetadata("new-search", Map.of(
    "description", "New Elasticsearch-based search",
    "owner", "search-team",
    "jira", "PROJ-1234",
    "targetDate", "2026-03-01",
    "rolloutStrategy", "gradual-10-25-50-100"
));

// Útil para documentação e debugging
Map<String, String> meta = flags.getMetadata("new-search");
logger.info("Feature owner: {}", meta.get("owner"));
```

---

## 🎯 Padrões de Uso

### Database Connection com Config Externalizada

```java
@Configuration
public class DatabaseConfig {
    
    private final ConfigurationProvider config;
    
    @Bean
    public DataSource dataSource() {
        HikariConfig hikari = new HikariConfig();
        
        // Configurações via environment variables
        hikari.setJdbcUrl(config.getString("database.url")
            .orElseThrow(() -> new IllegalStateException("database.url not configured")));
            
        hikari.setUsername(config.getString("database.username")
            .orElse("postgres"));
            
        hikari.setPassword(config.getString("database.password")
            .orElse(""));
        
        // Pool sizing
        hikari.setMaximumPoolSize(config.getInt("database.pool.max").orElse(20));
        hikari.setMinimumIdle(config.getInt("database.pool.min").orElse(5));
        hikari.setConnectionTimeout(config.getLong("database.timeout").orElse(30000L));
        
        return new HikariDataSource(hikari);
    }
}

// Environment variables:
// DATABASE__URL=jdbc:postgresql://prod-db:5432/mydb
// DATABASE__USERNAME=app_user
// DATABASE__PASSWORD=<secret>
// DATABASE__POOL__MAX=50
```

### Feature Flag Service

```java
@Service
public class CheckoutService {
    
    private final FeatureFlags features;
    private final PaymentGateway paymentGateway;
    
    public CheckoutResult checkout(Cart cart, User user) {
        Map<String, Object> context = Map.of(
            "userId", user.id().value(),
            "accountType", user.accountType().name()
        );
        
        // Feature toggle
        if (features.isEnabled("express-checkout", context)) {
            return expressCheckout(cart, user);
        }
        
        // Variants (A/B test)
        String paymentFlow = features.getVariant(
            "payment-flow",
            "classic",
            context
        );
        
        return switch (paymentFlow) {
            case "one-click" -> oneClickCheckout(cart, user);
            case "saved-cards" -> savedCardsCheckout(cart, user);
            default -> classicCheckout(cart, user);
        };
    }
}
```

### Circuit Breaker Config Dinamica

```java
@Service
public class DynamicCircuitBreakerService {
    
    private final ConfigurationProvider config;
    private final ResilienceExecutor resilience;
    
    public Order fetchOrder(OrderId orderId) {
        // Configurações dinâmicas
        float failureThreshold = config.getDouble("circuit-breaker.failure-threshold")
            .orElse(50.0)
            .floatValue();
            
        int minCalls = config.getInt("circuit-breaker.min-calls").orElse(20);
        
        CircuitBreakerPolicy cb = new CircuitBreakerPolicy(
            failureThreshold,
            minCalls
        );
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, cb, null, null, null
        );
        
        return resilience.supply("order.fetch", policies, () -> 
            orderClient.getOrder(orderId)
        );
    }
    
    @PostConstruct
    public void setupListeners() {
        // Reconfigura circuit breaker em runtime
        config.addListener("circuit-breaker.*", event -> {
            logger.info("Circuit breaker config changed: {} = {}", 
                event.key(), event.newValue());
        });
    }
}
```

---

## 🔐 Secrets Management

### Integração com Secrets Providers

```java
@Configuration
public class SecretsConfig {
    
    @Bean
    public ConfigurationProvider secretsProvider(
            @Value("${vault.address}") String vaultAddress,
            @Value("${vault.token}") String vaultToken) {
        
        VaultClient vault = VaultClient.builder()
            .address(vaultAddress)
            .token(vaultToken)
            .build();
        
        return new VaultConfigurationProvider(
            vault,
            "secret/myapp"  // Path no Vault
        );
    }
    
    @Bean
    public DataSource dataSource(ConfigurationProvider secrets) {
        HikariConfig config = new HikariConfig();
        
        // URL vem de variável de ambiente
        config.setJdbcUrl(System.getenv("DATABASE_URL"));
        
        // Credenciais vêm do Vault
        config.setUsername(secrets.getString("database.username")
            .orElseThrow());
        config.setPassword(secrets.getString("database.password")
            .orElseThrow());
        
        return new HikariDataSource(config);
    }
}
```

---

## Testing

### Mock Configuration

```java
@Test
void shouldUseConfiguredTimeout() {
    // Given
    InMemoryConfigurationProvider config = new InMemoryConfigurationProvider();
    config.setProperty("http.timeout", "3000");
    
    HttpService service = new HttpService(config);
    
    // When
    int timeout = service.getTimeout();
    
    // Then
    assertThat(timeout).isEqualTo(3000);
}

@Test
void shouldFallbackToDefault() {
    // Given
    InMemoryConfigurationProvider config = new InMemoryConfigurationProvider();
    // Não configura timeout
    
    HttpService service = new HttpService(config);
    
    // When
    int timeout = service.getTimeout();
    
    // Then
    assertThat(timeout).isEqualTo(5000);  // Default
}
```

### Feature Flag Testing

```java
@Test
void shouldEnableFeatureForUser() {
    // Given
    InMemoryFeatureFlags flags = new InMemoryFeatureFlags();
    flags.setRolloutPercentage("feature-x", 50);
    
    // When/Then
    Map<String, Object> context = Map.of("userId", "user-123");
    boolean enabled = flags.isEnabled("feature-x", context);
    
    // Deve ser consistente para mesmo usuário
    assertThat(flags.isEnabled("feature-x", context)).isEqualTo(enabled);
}

@Test
void shouldReturnCorrectVariant() {
    // Given
    InMemoryFeatureFlags flags = new InMemoryFeatureFlags();
    flags.setVariants("test", Map.of(
        "control", 50,
        "variant-a", 50
    ));
    
    // When
    String variant = flags.getVariant("test", "control", 
        Map.of("userId", "user-456"));
    
    // Then
    assertThat(variant).isIn("control", "variant-a");
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use variáveis de ambiente para config sensível
String dbPassword = System.getenv("DATABASE_PASSWORD");

// ✅ Forneça defaults razoáveis
int timeout = config.getInt("http.timeout").orElse(5000);

// ✅ Use listeners para reconfiguração dinâmica
config.addListener("pool.size", event -> pool.resize(...));

// ✅ Valide configurações obrigatórias no startup
config.getString("api.key")
    .orElseThrow(() -> new IllegalStateException("api.key required"));

// ✅ Use feature flags para rollout gradual
flags.setRolloutPercentage("new-feature", 10);  // Começa baixo
```

### ❌ DON'T

```java
// ❌ NÃO hardcode configurações
String dbUrl = "jdbc:postgresql://localhost/db";  // ❌

// ❌ NÃO commite secrets no código
String apiKey = "sk_live_abc123...";  // ❌ NUNCA!

// ❌ NÃO use feature flags como config
if (flags.isEnabled("database-url")) {  // ❌ Errado
    // Flags são booleanas, não valores
}

// ❌ NÃO ignore erros de configuração
try {
    config.getString("critical.setting").orElseThrow();
} catch (Exception ignored) {}  // ❌
```

---

## Ver Também

- [12-Factor App: Config](https://12factor.net/config)
- [commons-ports-secrets](../api-reference/ports-secrets.md) - Vault, KeyVault, etc.
- [Feature Toggles (Martin Fowler)](https://martinfowler.com/articles/feature-toggles.html)
