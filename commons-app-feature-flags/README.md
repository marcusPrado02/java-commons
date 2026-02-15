# Commons App :: Feature Flags

Production-ready feature flags abstraction with multiple provider implementations (LaunchDarkly, Unleash, in-memory).

## Features

- 🎯 **Provider abstraction** - Swap providers without changing code
- 🚀 **Multiple providers** - LaunchDarkly, Unleash, in-memory (for testing)
- 🎭 **Multi-variant flags** - Boolean, string, number, JSON values
- 🎯 **User targeting** - Target specific users and segments
- 📊 **Percentage rollouts** - Gradual feature rollouts
- 🌸 **Spring integration** - `@FeatureFlag` annotation support
- 🧪 **Testing-friendly** - In-memory provider for tests

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-feature-flags</artifactId>
</dependency>
```

### Basic Usage

```java
// 1. Create a provider
FeatureFlagProvider provider = InMemoryFeatureFlagProvider.builder()
    .flag("new-checkout", true)
    .flag("max-items", 100)
    .flag("theme", "dark")
    .build();

// 2. Create service
FeatureFlagService featureFlags = new FeatureFlagService(provider);

// 3. Check feature flags
if (featureFlags.isEnabled("new-checkout")) {
    processNewCheckout();
} else {
    processOldCheckout();
}

// Get values
int maxItems = featureFlags.getValue("max-items").asInt();
String theme = featureFlags.getValue("theme").asString();
```

## Providers

### In-Memory Provider (for testing and development)

```java
InMemoryFeatureFlagProvider provider = InMemoryFeatureFlagProvider.builder()
    // Simple boolean flags
    .flag("new-ui", true)
    .flag("beta-feature", false)
    
    // Multi-variant flags
    .flag("max-items", 100)
    .flag("theme", "dark")
    
    // User targeting
    .flagWithTargeting("premium-feature",
        context -> "premium".equals(context.getAttribute("plan").orElse(null)),
        FeatureFlagValue.of(true),
        FeatureFlagValue.of(false))
    
    // Percentage rollouts
    .flagWithPercentageRollout("gradual-rollout",
        50, // 50% enabled
        FeatureFlagValue.of(true),
        FeatureFlagValue.of(false))
    
    .build();

// Dynamic updates
provider.updateFlag("new-ui", FeatureFlagValue.of(false));
provider.removeFlag("beta-feature");
provider.clear();
```

### LaunchDarkly Provider

```java
// Add dependency
<dependency>
    <groupId>com.launchdarkly</groupId>
    <artifactId>launchdarkly-java-server-sdk</artifactId>
</dependency>

// Create provider
LaunchDarklyFeatureFlagProvider provider =
    new LaunchDarklyFeatureFlagProvider("sdk-key-123");

// Use with context
FeatureFlagContext context = FeatureFlagContext.builder()
    .userId("user123")
    .attribute("email", "user@example.com")
    .attribute("plan", "premium")
    .build();

boolean enabled = provider.isEnabled("new-feature", context);

// Shutdown
provider.shutdown();
```

### Unleash Provider

```java
// Add dependency
<dependency>
    <groupId>io.getunleash</groupId>
    <artifactId>unleash-client-java</artifactId>
</dependency>

// Create Unleash client
UnleashConfig config = UnleashConfig.builder()
    .appName("my-app")
    .instanceId("instance-1")
    .unleashAPI("https://unleash.example.com/api/")
    .apiKey("*:*.unleash-api-key")
    .build();

Unleash unleash = new DefaultUnleash(config);

// Create provider
UnleashFeatureFlagProvider provider = new UnleashFeatureFlagProvider(unleash);

// Use
boolean enabled = provider.isEnabled("feature-key", context);
```

## Feature Flag Context

Context provides user identification and attributes for targeting:

```java
// Anonymous context
FeatureFlagContext context = FeatureFlagContext.anonymous();

// User context
FeatureFlagContext context = FeatureFlagContext.forUser("user123");

// Full context with attributes
FeatureFlagContext context = FeatureFlagContext.builder()
    .userId("user123")
    .sessionId("session456")
    .attribute("email", "user@example.com")
    .attribute("plan", "premium")
    .attribute("region", "US")
    .attribute("beta_tester", true)
    .build();

// Access attributes
Optional<String> userId = context.getUserId();
Optional<Object> plan = context.getAttribute("plan");
```

## Feature Flag Values

Supports multiple value types:

```java
// Boolean flags
FeatureFlagValue value = provider.getValue("enabled-flag", context);
boolean enabled = value.asBoolean();

// String flags (e.g., theme, variant, message)
String theme = provider.getValue("theme", context).asString();

// Number flags (e.g., limits, timeouts, percentages)
int maxItems = provider.getValue("max-items", context).asInt();
long timeout = provider.getValue("timeout-ms", context).asLong();
double percentage = provider.getValue("discount-rate", context).asDouble();

// JSON flags (complex configuration)
FeatureFlagValue value = provider.getValue("config", context);
Optional<JsonNode> json = value.asJson();

if (json.isPresent()) {
    int retries = json.get().get("retries").asInt();
    String endpoint = json.get().get("endpoint").asText();
}
```

## Spring Integration

### Configuration

```java
@Configuration
@Import(FeatureFlagAutoConfiguration.class)
public class AppConfig {

    @Bean
    public FeatureFlagProvider featureFlagProvider() {
        return InMemoryFeatureFlagProvider.builder()
            .flag("new-payment-flow", true)
            .flag("discount-feature", false)
            .build();
    }
}
```

### Using @FeatureFlag Annotation

```java
@Service
public class PaymentService {

    // Throw exception if disabled
    @FeatureFlag(key = "new-payment-flow", fallback = FallbackStrategy.THROW_EXCEPTION)
    public PaymentResult processPayment(Payment payment) {
        // New payment flow
        return newPaymentProcessor.process(payment);
    }

    // Return null if disabled
    @FeatureFlag(key = "discount-feature", fallback = FallbackStrategy.RETURN_NULL)
    public Discount calculateDiscount(Order order) {
        // Calculate discount
        return discountCalculator.calculate(order);
    }

    // Return default value if disabled
    @FeatureFlag(key = "max-items-feature", fallback = FallbackStrategy.RETURN_DEFAULT)
    public int getMaxItems() {
        return 100; // Returns 0 if disabled
    }

    // Call fallback method if disabled
    @FeatureFlag(
        key = "new-checkout",
        fallback = FallbackStrategy.CALL_METHOD,
        fallbackMethod = "processOldCheckout"
    )
    public CheckoutResult processNewCheckout(Cart cart) {
        // New checkout logic
        return newCheckoutService.process(cart);
    }

    private CheckoutResult processOldCheckout(Cart cart) {
        // Old checkout logic
        return oldCheckoutService.process(cart);
    }
}
```

### User Targeting with SpEL

```java
@Service
public class UserService {

    // Get user ID from method parameter
    @FeatureFlag(key = "premium-feature", userId = "#userId")
    public UserProfile getPremiumProfile(String userId) {
        return premiumProfileService.get(userId);
    }

    // Get user ID from object property
    @FeatureFlag(key = "discount-feature", userId = "#order.userId")
    public Discount getDiscount(Order order) {
        return discountService.calculate(order);
    }

    // Get user ID from bean method
    @FeatureFlag(key = "beta-feature", userId = "@securityContext.currentUserId()")
    public BetaFeature getBetaFeature() {
        return betaFeatureService.get();
    }
}
```

## Advanced Patterns

### A/B Testing

```java
// In-memory provider with percentage rollout
InMemoryFeatureFlagProvider provider = InMemoryFeatureFlagProvider.builder()
    .flagWithPercentageRollout("variant-a",
        50, // 50% of users see variant A
        FeatureFlagValue.of("variant-a"),
        FeatureFlagValue.of("variant-b"))
    .build();

// Use
FeatureFlagContext context = FeatureFlagContext.forUser(userId);
String variant = provider.getValue("variant-a", context).asString();

if ("variant-a".equals(variant)) {
    showVariantA();
} else {
    showVariantB();
}
```

### Gradual Rollout

```java
// Week 1: 10% rollout
provider.updateFlag("new-feature",
    FeatureFlagValue.of(true)); // with 10% targeting in LaunchDarkly

// Week 2: 25% rollout
// Update in LaunchDarkly dashboard

// Week 3: 50% rollout
// Update in LaunchDarkly dashboard

// Week 4: 100% rollout
// Update in LaunchDarkly dashboard
```

### Kill Switch

```java
@Service
public class OrderService {

    private final FeatureFlagService featureFlags;

    public void processOrder(Order order) {
        // Check kill switch
        if (featureFlags.isEnabled("order-processing-enabled")) {
            internalProcessOrder(order);
        } else {
            throw new ServiceUnavailableException("Order processing is temporarily disabled");
        }
    }
}
```

### Feature-based Configuration

```java
@Service
public class EmailService {

    private final FeatureFlagService featureFlags;

    public void sendEmail(Email email) {
        // Get configuration from feature flag
        FeatureFlagValue config = featureFlags.getValue("email-config");
        JsonNode json = config.asJson().orElseThrow();

        int retries = json.get("retries").asInt();
        int timeout = json.get("timeout").asInt();
        String template = json.get("template").asText();

        emailSender.send(email, retries, timeout, template);
    }
}
```

## Testing

### Unit Testing with In-Memory Provider

```java
class OrderServiceTest {

    private InMemoryFeatureFlagProvider provider;
    private FeatureFlagService featureFlags;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        provider = InMemoryFeatureFlagProvider.builder()
            .flag("new-pricing", false)
            .flag("discount-enabled", true)
            .flag("max-items", 100)
            .build();

        featureFlags = new FeatureFlagService(provider);
        orderService = new OrderService(featureFlags);
    }

    @Test
    void shouldUseOldPricingWhenFlagDisabled() {
        Order order = orderService.createOrder();

        assertThat(order.getPricingVersion()).isEqualTo("v1");
    }

    @Test
    void shouldUseNewPricingWhenFlagEnabled() {
        provider.updateFlag("new-pricing", FeatureFlagValue.of(true));

        Order order = orderService.createOrder();

        assertThat(order.getPricingVersion()).isEqualTo("v2");
    }
}
```

### Integration Testing with Spring

```java
@SpringBootTest
@Import(FeatureFlagAutoConfiguration.class)
class PaymentControllerIntegrationTest {

    @Autowired
    private InMemoryFeatureFlagProvider provider;

    @Autowired
    private PaymentController controller;

    @Test
    void shouldUseNewPaymentFlowWhenEnabled() {
        provider.updateFlag("new-payment-flow", FeatureFlagValue.of(true));

        PaymentResult result = controller.processPayment(payment);

        assertThat(result.getProcessor()).isEqualTo("new");
    }
}
```

## Best Practices

### 1. Use Descriptive Feature Keys

```java
// Good
"new-checkout-flow"
"premium-features-enabled"
"max-cart-items"

// Bad
"feature1"
"flag"
"config"
```

### 2. Always Provide Default Values

```java
// Good
FeatureFlagValue value = provider.getValue("max-items", context,
    FeatureFlagValue.of(50)); // Default: 50

// Or
int maxItems = featureFlags.getValueOrDefault("max-items",
    FeatureFlagValue.of(50)).asInt();
```

### 3. Clean Up Old Flags

```java
// Remove flags that have been 100% rolled out for > 30 days
// This keeps your feature flag list manageable
```

### 4. Document Flag Purpose

```java
@Service
public class OrderService {

    /**
     * Feature flag: "new-pricing"
     * Purpose: A/B test new pricing algorithm
     * Owner: pricing-team
     * Rollout: 2024-01-15
     * Expected removal: 2024-02-15
     */
    @FeatureFlag(key = "new-pricing")
    public void calculatePrice(Order order) {
        // ...
    }
}
```

### 5. Use Typed Wrappers

```java
@Service
public class FeatureFlagConfig {

    private final FeatureFlagService featureFlags;

    public boolean isNewCheckoutEnabled() {
        return featureFlags.isEnabled("new-checkout");
    }

    public int getMaxCartItems() {
        return featureFlags.getValueOrDefault("max-cart-items",
            FeatureFlagValue.of(50)).asInt();
    }
}

// Usage
if (featureFlagConfig.isNewCheckoutEnabled()) {
    // ...
}
```

### 6. Monitor Flag Usage

```java
@Aspect
@Component
public class FeatureFlagMonitoringAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(featureFlag)")
    public Object monitor(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag)
        throws Throwable {

        String key = featureFlag.key();
        boolean enabled = // evaluate flag

        meterRegistry.counter("feature_flag.checked",
            "key", key,
            "enabled", String.valueOf(enabled)).increment();

        return joinPoint.proceed();
    }
}
```

## Architecture

```
commons-app-feature-flags/
├── FeatureFlagContext          - Evaluation context (user, attributes)
├── FeatureFlagValue            - Multi-type flag value (boolean, string, number, JSON)
├── FeatureFlagProvider         - Provider abstraction
├── FeatureFlagService          - High-level service
│
├── provider/
│   ├── inmemory/
│   │   └── InMemoryFeatureFlagProvider    - For testing
│   ├── launchdarkly/
│   │   └── LaunchDarklyFeatureFlagProvider
│   └── unleash/
│       └── UnleashFeatureFlagProvider
│
└── spring/
    ├── @FeatureFlag            - Method-level annotation
    ├── FeatureFlagAspect       - AOP interceptor
    └── FeatureFlagAutoConfiguration
```

## Dependencies

Core module has no external dependencies except:
- `commons-kernel-errors`
- `commons-kernel-result`
- `jackson-databind` (for JSON values)

Optional dependencies:
- `spring-context` + `spring-aop` + `aspectjweaver` (for @FeatureFlag annotation)
- `launchdarkly-java-server-sdk` (for LaunchDarkly provider)
- `unleash-client-java` (for Unleash provider)

## Migration from Provider to Provider

### From In-Memory to LaunchDarkly

```java
// Before
@Bean
public FeatureFlagProvider featureFlagProvider() {
    return InMemoryFeatureFlagProvider.builder()
        .flag("new-feature", true)
        .build();
}

// After
@Bean
public FeatureFlagProvider featureFlagProvider() {
    return new LaunchDarklyFeatureFlagProvider(
        environment.getProperty("launchdarkly.sdk-key"));
}

// No code changes needed - just swap the provider!
```

## Troubleshooting

### Flag Not Updating

```java
// LaunchDarkly caches flag values
// Force refresh:
ldClient.flush();

// Or configure cache TTL in LaunchDarkly config
```

### Performance Issues

```java
// Use caching wrapper
@Bean
public FeatureFlagProvider cachedProvider(FeatureFlagProvider provider) {
    return new CachingFeatureFlagProvider(provider,
        Duration.ofMinutes(5)); // Cache for 5 minutes
}
```

### Testing Percentage Rollouts

```java
// Use deterministic user IDs
for (int i = 0; i < 100; i++) {
    FeatureFlagContext context = FeatureFlagContext.forUser("user" + i);
    boolean enabled = provider.isEnabled("feature", context);
    // ...
}
```

## License

This module is part of the Commons Platform project.
