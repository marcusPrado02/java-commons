# commons-app-configuration

Sistema de configuração dinâmica e feature flags com suporte a múltiplos provedores e refresh sem restart.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-configuration</artifactId>
</dependency>
```

## 🎯 Visão Geral

Este módulo fornece abstrações para gerenciar configuração dinâmica e feature flags em aplicações corporativas:

- **ConfigurationProvider**: Interface genérica para provedores de configuração
- **DynamicConfiguration**: Configuração com listeners e auto-refresh
- **FeatureFlags**: Service para feature toggles e A/B testing
- **Providers**: Implementações para Spring Cloud Config, Azure App Configuration, AWS AppConfig
- **Change Notification**: Sistema de eventos para mudanças de configuração
- **Auto-refresh**: Atualização automática sem restart da aplicação

## 🏗️ Componentes Principais

### ConfigurationProvider

Interface básica para acesso a valores de configuração.

```java
ConfigurationProvider provider = new InMemoryConfigurationProvider();

// Get typed values
Optional<String> dbUrl = provider.getString("database.url");
int timeout = provider.getInt("http.timeout").orElse(5000);
boolean enabled = provider.getBoolean("feature.enabled").orElse(false);

// Get all properties with prefix
Map<String, String> dbConfig = provider.getProperties("database");
// Returns: {url=..., username=..., password=...}

// Check existence
if (provider.containsKey("special.config")) {
    // ...
}

// Refresh from source
provider.refresh();
```

### DynamicConfiguration

Configuração dinâmica com suporte a listeners e auto-refresh.

```java
DynamicConfiguration config = new InMemoryConfigurationProvider();

// Listen for changes
config.addListener("database.max-connections", change -> {
    logger.info("Max connections changed from {} to {}",
        change.oldValue(), change.newValue());
    connectionPool.resize(Integer.parseInt(change.newValue()));
});

// Listen for all changes
config.addListener(change -> {
    logger.info("Configuration changed: {} = {}",
        change.key(), change.newValue());
});

// Enable auto-refresh every 5 minutes
config.enableAutoRefresh(Duration.ofMinutes(5));

// Later: disable auto-refresh
config.disableAutoRefresh();
```

### FeatureFlags

Service para feature toggles e A/B testing.

#### Boolean Flags

```java
FeatureFlags flags = new InMemoryFeatureFlags();

// Enable/disable features
flags.enable("new-checkout-flow");
flags.disable("old-legacy-feature");

// Check flags
if (flags.isEnabled("new-checkout-flow")) {
    return newCheckoutService.process(order);
} else {
    return legacyCheckoutService.process(order);
}
```

#### Context-Aware Flags

```java
// Percentage-based rollout
flags.setRolloutPercentage("beta-feature", 50); // 50% of users

// Check with user context
Map<String, Object> context = Map.of(
    "userId", "user-123",
    "region", "us-east",
    "subscriptionTier", "premium"
);

if (flags.isEnabled("beta-feature", context)) {
    return betaFeatureService.execute();
}
```

#### Variant Flags (A/B/C Testing)

```java
// Configure variants
flags.setVariants("button-color", Map.of(
    "control", 40,  // 40% get control (original)
    "red", 30,      // 30% get red button
    "blue", 30      // 30% get blue button
));

// Get variant for user
String variant = flags.getVariant("button-color", "control",
    Map.of("userId", "user-123"));

switch (variant) {
    case "red" -> renderRedButton();
    case "blue" -> renderBlueButton();
    default -> renderControlButton();
}
```

#### Flag Metadata

```java
// Set metadata
flags.setMetadata("new-feature", Map.of(
    "description", "New advanced analytics dashboard",
    "owner", "analytics-team",
    "jiraTicket", "PROJ-123"
));

// Get metadata
Map<String, Object> metadata = flags.getFlagMetadata("new-feature");
// Returns: {enabled=true, percentage=100, description=..., ...}

// List all flags
Set<String> allFlags = flags.getAllFlags();
```

### ConfigurationChangeEvent

Evento que representa uma mudança de configuração.

```java
ConfigurationChangeEvent event = ConfigurationChangeEvent.updated(
    "database.url",
    "jdbc:old",
    "jdbc:new",
    "azure-app-config"
);

// Access event properties
String key = event.key();                    // "database.url"
String oldValue = event.oldValue();          // "jdbc:old"
String newValue = event.newValue();          // "jdbc:new"
ChangeType type = event.changeType();        // UPDATED
Instant when = event.timestamp();
String source = event.source();              // "azure-app-config"

// Factory methods
ConfigurationChangeEvent added = ConfigurationChangeEvent.added(key, value, source);
ConfigurationChangeEvent updated = ConfigurationChangeEvent.updated(key, oldVal, newVal, source);
ConfigurationChangeEvent removed = ConfigurationChangeEvent.removed(key, oldVal, source);
```

## 📋 Providers

### InMemoryConfigurationProvider

Provider in-memory para testes e desenvolvimento.

```java
InMemoryConfigurationProvider provider = new InMemoryConfigurationProvider();

// Set properties
provider.setProperty("app.name", "my-service");
provider.setProperty("app.timeout", "5000");

// Set multiple properties
provider.setProperties(Map.of(
    "database.url", "jdbc:postgresql://localhost/db",
    "database.username", "admin",
    "database.password", "secret"
));

// Remove property
provider.removeProperty("old.config");

// Clear all
provider.clear();
```

### CompositeConfigurationProvider

Combina múltiplos providers com ordem de precedência.

```java
// Higher priority providers are queried first
ConfigurationProvider composite = new CompositeConfigurationProvider(
    environmentProvider,      // Highest priority
    azureAppConfigProvider,
    springCloudConfigProvider,
    defaultsProvider          // Lowest priority
);

// Query checks providers in order
Optional<String> value = composite.getString("database.url");
// Returns first non-empty value from: env > azure > spring > defaults
```

### SpringCloudConfigProvider

Integra com Spring Cloud Config Server.

```java
// In Spring Boot application
@Bean
public ConfigurationProvider springCloudConfig(ConfigurableEnvironment environment) {
    return new SpringCloudConfigProvider(environment);
}

// Use the provider
@Autowired
private ConfigurationProvider configProvider;

public void someMethod() {
    Optional<String> value = configProvider.getString("my.config.key");
}
```

**Requirements:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

**application.yml:**
```yaml
spring:
  cloud:
    config:
      uri: http://config-server:8888
      name: my-application
      profile: production
```

### AzureAppConfigurationProvider

Integra com Azure App Configuration.

```java
// Create provider
AzureAppConfigurationProvider provider = new AzureAppConfigurationProvider(
    "Endpoint=https://myappconfig.azconfig.io;Id=xxx;Secret=yyy"
);

// Optional: set label for environment-specific config
provider.setLabel("production");

// Use the provider
Optional<String> value = provider.getString("my.config.key");

// Refresh from Azure
provider.refresh();
```

**Requirements:**
```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-data-appconfiguration</artifactId>
</dependency>
```

**Azure Setup:**
```bash
# Create Azure App Configuration
az appconfig create --name myappconfig --resource-group mygroup --location eastus

# Add configuration values
az appconfig kv set --name myappconfig --key database.url --value "jdbc:..."
az appconfig kv set --name myappconfig --key database.url --value "jdbc:production..." --label production
```

### AwsAppConfigProvider

Integra com AWS AppConfig.

```java
// Create provider
AwsAppConfigProvider provider = new AwsAppConfigProvider(
    "my-application",       // Application name
    "production",           // Environment
    "feature-flags"         // Configuration profile
);

// Use the provider
Optional<String> value = provider.getString("my.config.key");

// Refresh from AWS
provider.refresh();
```

**Requirements:**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>appconfig</artifactId>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>appconfigdata</artifactId>
</dependency>
```

**AWS Setup:**
```bash
# Create application
aws appconfig create-application --name my-application

# Create environment
aws appconfig create-environment \
    --application-id <app-id> \
    --name production

# Create configuration profile
aws appconfig create-configuration-profile \
    --application-id <app-id> \
    --name feature-flags \
    --location-uri hosted
```

## 📋 Exemplos Práticos

### Exemplo 1: Configuração Multi-Source

```java
// Priority: Environment > Azure > Defaults
ConfigurationProvider composite = new CompositeConfigurationProvider(
    new InMemoryConfigurationProvider() {{
        // Environment variables
        System.getenv().forEach((k, v) -> setProperty(k.toLowerCase().replace('_', '.'), v));
    }},
    new AzureAppConfigurationProvider(azureConnectionString),
    new InMemoryConfigurationProvider() {{
        // Defaults
        setProperty("database.pool-size", "10");
        setProperty("http.timeout", "5000");
    }}
);

// Query checks all sources in priority order
int poolSize = composite.getInt("database.pool-size").orElse(10);
```

### Exemplo 2: Dynamic Database Connection Pool

```java
public class DatabaseConnectionManager {
    private final DynamicConfiguration config;
    private HikariDataSource dataSource;

    public DatabaseConnectionManager(DynamicConfiguration config) {
        this.config = config;

        // Initialize pool
        reconfigurePool();

        // Listen for pool size changes
        config.addListener("database.pool-size", event -> {
            logger.info("Pool size changed from {} to {}",
                event.oldValue(), event.newValue());
            reconfigurePool();
        });

        // Listen for URL changes
        config.addListener("database.url", event -> {
            logger.warn("Database URL changed - recreating pool");
            recreatePool();
        });

        // Enable auto-refresh every 5 minutes
        config.enableAutoRefresh(Duration.ofMinutes(5));
    }

    private void reconfigurePool() {
        int poolSize = config.getInt("database.pool-size").orElse(10);
        dataSource.setMaximumPoolSize(poolSize);
    }

    private void recreatePool() {
        if (dataSource != null) {
            dataSource.close();
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getString("database.url").orElseThrow());
        hikariConfig.setUsername(config.getString("database.username").orElseThrow());
        hikariConfig.setPassword(config.getString("database.password").orElseThrow());
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size").orElse(10));

        dataSource = new HikariDataSource(hikariConfig);
    }
}
```

### Exemplo 3: Feature Rollout Strategy

```java
public class FeatureRolloutService {
    private final FeatureFlags flags;

    public void setupGradualRollout() {
        // Week 1: Internal team only (0%)
        flags.disable("new-analytics");
        flags.setMetadata("new-analytics", Map.of(
            "stage", "internal-testing",
            "week", 1
        ));

        // Week 2: 5% rollout
        flags.setRolloutPercentage("new-analytics", 5);

        // Week 3: 25% rollout
        flags.setRolloutPercentage("new-analytics", 25);

        // Week 4: 50% rollout
        flags.setRolloutPercentage("new-analytics", 50);

        // Week 5: 100% rollout
        flags.enable("new-analytics");
    }

    public void renderAnalytics(String userId) {
        Map<String, Object> context = Map.of("userId", userId);

        if (flags.isEnabled("new-analytics", context)) {
            return newAnalyticsService.render(userId);
        } else {
            return legacyAnalyticsService.render(userId);
        }
    }
}
```

### Exemplo 4: A/B Testing

```java
public class ABTestingService {
    private final FeatureFlags flags;

    public void setupPricingTest() {
        // Test 3 different pricing displays
        flags.setVariants("pricing-display", Map.of(
            "control", 34,      // Current design
            "variant-a", 33,    // Emphasize discount
            "variant-b", 33     // Emphasize savings
        ));
    }

    public PricingView renderPricing(String userId, Product product) {
        Map<String, Object> context = Map.of("userId", userId);

        String variant = flags.getVariant("pricing-display", "control", context);

        return switch (variant) {
            case "variant-a" -> new DiscountEmphasisView(product);
            case "variant-b" -> new SavingsEmphasisView(product);
            default -> new StandardPricingView(product);
        };
    }
}
```

### Exemplo 5: Circuit Breaker Configuration

```java
public class CircuitBreakerService {
    private final DynamicConfiguration config;
    private CircuitBreaker circuitBreaker;

    public CircuitBreakerService(DynamicConfiguration config) {
        this.config = config;

        // Initial setup
        recreateCircuitBreaker();

        // Listen for threshold changes
        config.addListener(event -> {
            if (event.key().startsWith("circuit-breaker.")) {
                logger.info("Circuit breaker config changed: {} = {}",
                    event.key(), event.newValue());
                recreateCircuitBreaker();
            }
        });
    }

    private void recreateCircuitBreaker() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(
                config.getDouble("circuit-breaker.failure-rate-threshold").orElse(50.0).floatValue())
            .slowCallRateThreshold(
                config.getDouble("circuit-breaker.slow-call-rate-threshold").orElse(100.0).floatValue())
            .slowCallDurationThreshold(
                Duration.ofMillis(config.getLong("circuit-breaker.slow-call-duration-ms").orElse(60000L)))
            .permittedNumberOfCallsInHalfOpenState(
                config.getInt("circuit-breaker.half-open-calls").orElse(10))
            .slidingWindowSize(
                config.getInt("circuit-breaker.sliding-window-size").orElse(100))
            .build();

        circuitBreaker = CircuitBreaker.of("my-service", cbConfig);
    }
}
```

## 🎯 Best Practices

### ✅ DO

```java
// Use CompositeProvider for layered configuration
ConfigurationProvider config = new CompositeConfigurationProvider(
    environmentProvider,    // Override everything
    cloudProvider,
    defaultsProvider
);

// Use typed getters with defaults
int timeout = config.getInt("http.timeout").orElse(5000);

// Use listeners for reactive configuration
config.addListener("pool.size", event -> pool.resize(parseInt(event.newValue())));

// Enable auto-refresh for cloud providers
config.enableAutoRefresh(Duration.ofMinutes(5));

// Use context for feature flags
Map<String, Object> context = Map.of("userId", userId, "region", region);
if (flags.isEnabled("feature", context)) { ... }

// Use percentage rollout for gradual feature deployment
flags.setRolloutPercentage("new-feature", 10);  // Start at 10%

// Add metadata for documentation
flags.setMetadata("feature", Map.of(
    "description", "New advanced search",
    "owner", "search-team",
    "jiraTicket", "PROJ-123"
));
```

### ❌ DON'T

```java
// Don't poll configuration in a loop
while (true) {
    String value = provider.getString("key").orElse("");  // ❌
    Thread.sleep(1000);
}
// Instead: Use listeners or auto-refresh

// Don't ignore refresh failures
config.refresh();  // ❌ No error handling
// Instead: Handle Result
config.refresh().orElseThrow(problem ->
    new ConfigurationException(problem));

// Don't hardcode feature flags
if (true) {  // ❌ Hardcoded
    return newFeature();
}
// Instead: Use FeatureFlags service
if (flags.isEnabled("new-feature")) {
    return newFeature();
}

// Don't use random for percentage rollout without user ID
if (new Random().nextInt(100) < 50) {  // ❌ Inconsistent
    return newFeature();
}
// Instead: Use context with userId
if (flags.isEnabled("feature", Map.of("userId", userId))) {
    return newFeature();
}

// Don't store secrets in configuration
provider.setProperty("password", "secret123");  // ❌ Security risk
// Instead: Use secrets management (Vault, Azure Key Vault, AWS Secrets Manager)
```

## 🔗 Integração com Spring Boot

### Auto-Configuration

```java
@Configuration
@ConditionalOnProperty(name = "commons.config.enabled", havingValue = "true")
public class ConfigurationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationProvider configurationProvider(
            ConfigurableEnvironment environment,
            @Value("${azure.appconfig.connection-string:}") String azureConnectionString) {

        List<ConfigurationProvider> providers = new ArrayList<>();

        // Add Spring Environment provider
        providers.add(new SpringCloudConfigProvider(environment));

        // Add Azure if configured
        if (!azureConnectionString.isBlank()) {
            providers.add(new AzureAppConfigurationProvider(azureConnectionString));
        }

        return new CompositeConfigurationProvider(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public FeatureFlags featureFlags(ConfigurationProvider configProvider) {
        // Feature flags from configuration
        return new InMemoryFeatureFlags();
    }
}
```

## 📚 Referências

- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)
- [Azure App Configuration](https://learn.microsoft.com/azure/azure-app-configuration/)
- [AWS AppConfig](https://docs.aws.amazon.com/appconfig/latest/userguide/what-is-appconfig.html)
- [Feature Toggles (Martin Fowler)](https://martinfowler.com/articles/feature-toggles.html)

## 📄 Licença

Este projeto está sob a licença definida no arquivo raiz do repositório.
