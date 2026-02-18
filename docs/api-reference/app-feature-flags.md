# API Reference: commons-app-feature-flags

## Visão Geral

O módulo `commons-app-feature-flags` fornece infraestrutura para Feature Toggles, permitindo controle fino sobre funcionalidades em produção, rollout gradual e A/B testing.

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-feature-flags</artifactId>
</dependency>
```

---

## Core Concepts

### FeatureFlag

Representa uma feature flag com nome, status e contexto.

```java
public interface FeatureFlag {
    String name();
    boolean isEnabled();
    Map<String, String> context();
}
```

### FeatureFlagService

Serviço principal para consultar feature flags.

```java
public interface FeatureFlagService {
    
    /**
     * Verifica se feature está habilitada
     */
    boolean isEnabled(String featureName);
    
    /**
     * Verifica se feature está habilitada para contexto específico
     */
    boolean isEnabled(String featureName, FeatureContext context);
    
    /**
     * Retorna variação da feature (para A/B testing)
     */
    <T> T getVariation(String featureName, T defaultValue, Class<T> type);
    
    /**
     * Lista todas as features
     */
    List<FeatureFlag> getAllFeatures();
}
```

---

## Uso Básico

### 1. Verificação Simples

```java
@Service
public class OrderService {
    
    private final FeatureFlagService featureFlags;
    
    public Result<Order> createOrder(CreateOrderCommand command) {
        // Feature flag simples
        if (featureFlags.isEnabled("new-order-flow")) {
            return createOrderV2(command);  // Novo fluxo
        }
        
        return createOrderV1(command);  // Fluxo antigo
    }
}
```

### 2. Com Contexto

```java
@Service
public class DiscountService {
    
    private final FeatureFlagService featureFlags;
    
    public BigDecimal calculateDiscount(Order order, Customer customer) {
        // Contexto com informações do usuário
        FeatureContext context = FeatureContext.builder()
            .userId(customer.id().value())
            .tenantId(order.tenantId().value())
            .attribute("country", customer.country())
            .attribute("isPremium", customer.isPremium())
            .build();
        
        if (featureFlags.isEnabled("premium-discount", context)) {
            return calculatePremiumDiscount(order);
        }
        
        return calculateStandardDiscount(order);
    }
}
```

### 3. A/B Testing

```java
@Service
public class CheckoutService {
    
    private final FeatureFlagService featureFlags;
    
    public CheckoutPage getCheckoutPage(Customer customer) {
        FeatureContext context = FeatureContext.forUser(customer.id().value());
        
        // Retorna variação (A ou B)
        String variant = featureFlags.getVariation(
            "checkout-layout", 
            "control",  // default
            String.class,
            context
        );
        
        return switch (variant) {
            case "variant-a" -> new CheckoutPageVariantA();
            case "variant-b" -> new CheckoutPageVariantB();
            default -> new CheckoutPageControl();
        };
    }
}
```

---

## Feature Context

### Builder API

```java
public class FeatureContext {
    
    private String userId;
    private String tenantId;
    private String sessionId;
    private Map<String, Object> attributes;
    
    public static FeatureContextBuilder builder() {
        return new FeatureContextBuilder();
    }
    
    public static FeatureContext forUser(String userId) {
        return builder().userId(userId).build();
    }
    
    public static FeatureContext forTenant(String tenantId) {
        return builder().tenantId(tenantId).build();
    }
}

// Uso
FeatureContext context = FeatureContext.builder()
    .userId("user-123")
    .tenantId("tenant-abc")
    .sessionId("session-xyz")
    .attribute("country", "BR")
    .attribute("plan", "premium")
    .attribute("userAge", 25)
    .build();
```

---

## Estratégias de Avaliação

### 1. Boolean Strategy (On/Off)

```java
@Configuration
public class FeatureFlagConfig {
    
    @Bean
    public FeatureFlag newCheckoutEnabled() {
        return FeatureFlag.builder()
            .name("new-checkout")
            .strategy(BooleanStrategy.enabled())  // Sempre ligado
            .build();
    }
}
```

### 2. Percentage Rollout

```java
// Rollout gradual - 20% dos usuários
FeatureFlag betaFeature = FeatureFlag.builder()
    .name("beta-feature")
    .strategy(PercentageStrategy.of(20))  // 20% dos usuários
    .build();

// Distribui baseado em hash do userId
boolean enabled = featureFlags.isEnabled(
    "beta-feature",
    FeatureContext.forUser(userId)
);
```

### 3. User Targeting

```java
// Habilita para usuários específicos
FeatureFlag vipFeature = FeatureFlag.builder()
    .name("vip-feature")
    .strategy(UserTargetingStrategy.builder()
        .includeUsers("user-1", "user-2", "user-3")
        .build()
    )
    .build();
```

### 4. Attribute-Based

```java
// Habilita baseado em atributos
FeatureFlag premiumFeature = FeatureFlag.builder()
    .name("premium-feature")
    .strategy(AttributeStrategy.builder()
        .when("plan", equals("premium"))
        .when("country", in("US", "GB", "BR"))
        .build()
    )
    .build();
```

### 5. Time-Based

```java
// Habilita em período específico
FeatureFlag blackFridayFeature = FeatureFlag.builder()
    .name("black-friday-promotions")
    .strategy(TimeWindowStrategy.builder()
        .start(LocalDateTime.of(2026, 11, 25, 0, 0))
        .end(LocalDateTime.of(2026, 11, 30, 23, 59))
        .build()
    )
    .build();
```

---

## Providers

### 1. In-Memory Provider

```java
@Configuration
public class FeatureFlagConfig {
    
    @Bean
    public FeatureFlagProvider inMemoryProvider() {
        return InMemoryFeatureFlagProvider.builder()
            .feature("new-ui", true)
            .feature("beta-api", false)
            .feature("premium-discount", PercentageStrategy.of(50))
            .build();
    }
}
```

### 2. Environment Variables

```java
// application.yml
features:
  new-checkout: true
  beta-feature: false
  rollout-percentage: 25

// Código
@Bean
public FeatureFlagProvider environmentProvider() {
    return new EnvironmentFeatureFlagProvider();
}
```

### 3. Database Provider

```java
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {
    List<FeatureFlagEntity> findAllByTenantId(String tenantId);
}

@Service
public class DatabaseFeatureFlagProvider implements FeatureFlagProvider {
    
    private final FeatureFlagRepository repository;
    private final Cache<String, FeatureFlag> cache;
    
    @Override
    public Optional<FeatureFlag> getFeature(String name) {
        return cache.get(name, () -> {
            return repository.findById(name)
                .map(this::toFeatureFlag);
        });
    }
}
```

### 4. External Service (LaunchDarkly, Unleash, etc.)

```java
@Service
public class LaunchDarklyFeatureFlagProvider implements FeatureFlagProvider {
    
    private final LDClient ldClient;
    
    @Override
    public boolean isEnabled(String featureName, FeatureContext context) {
        LDUser user = new LDUser.Builder(context.userId())
            .custom("tenantId", context.tenantId())
            .custom("country", context.getAttribute("country"))
            .build();
        
        return ldClient.boolVariation(featureName, user, false);
    }
    
    @Override
    public <T> T getVariation(String featureName, T defaultValue, Class<T> type) {
        LDUser user = buildUser(context);
        
        if (type == String.class) {
            return (T) ldClient.stringVariation(featureName, user, (String) defaultValue);
        }
        
        if (type == Integer.class) {
            return (T) ldClient.intVariation(featureName, user, (Integer) defaultValue);
        }
        
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
}
```

---

## Annotations

### @FeatureGated

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureGated {
    String value();  // Feature flag name
    String fallback() default "";  // Fallback method
}

// Uso
@Service
public class PaymentService {
    
    @FeatureGated("new-payment-gateway")
    public Result<Payment> processPayment(PaymentCommand command) {
        // Novo gateway
        return processWithNewGateway(command);
    }
    
    // Fallback automático
    public Result<Payment> processPaymentFallback(PaymentCommand command) {
        // Gateway antigo
        return processWithOldGateway(command);
    }
}
```

### Aspect Implementation

```java
@Aspect
@Component
public class FeatureGatedAspect {
    
    private final FeatureFlagService featureFlags;
    
    @Around("@annotation(featureGated)")
    public Object checkFeatureFlag(ProceedingJoinPoint joinPoint, FeatureGated featureGated) 
            throws Throwable {
        
        String featureName = featureGated.value();
        
        if (featureFlags.isEnabled(featureName)) {
            // Feature habilitada - executa método
            return joinPoint.proceed();
        }
        
        // Feature desabilitada - executa fallback
        String fallbackMethod = featureGated.fallback();
        if (!fallbackMethod.isEmpty()) {
            return invokeFallback(joinPoint, fallbackMethod);
        }
        
        throw new FeatureNotEnabledException(featureName);
    }
}
```

---

## A/B Testing Completo

### Definir Experimento

```java
@Configuration
public class ExperimentConfig {
    
    @Bean
    public FeatureFlag checkoutExperiment() {
        return FeatureFlag.builder()
            .name("checkout-layout")
            .strategy(MultiVariateStrategy.builder()
                .variant("control", 40)      // 40% control
                .variant("variant-a", 30)    // 30% variante A
                .variant("variant-b", 30)    // 30% variante B
                .build()
            )
            .build();
    }
}
```

### Implementar Variantes

```java
@Service
public class CheckoutService {
    
    private final FeatureFlagService featureFlags;
    private final MetricsFacade metrics;
    
    public CheckoutPage showCheckout(Customer customer) {
        FeatureContext context = FeatureContext.forUser(customer.id().value());
        
        String variant = featureFlags.getVariation(
            "checkout-layout",
            "control",
            String.class,
            context
        );
        
        // Track experiment exposure
        metrics.incrementCounter(
            "experiment.checkout-layout.exposure",
            "variant", variant,
            "userId", customer.id().value()
        );
        
        return createCheckoutPage(variant);
    }
    
    public void trackConversion(Customer customer, Order order) {
        // Busca variante do experimento
        String variant = getAssignedVariant(customer);
        
        // Track conversão
        metrics.incrementCounter(
            "experiment.checkout-layout.conversion",
            "variant", variant,
            "revenue", order.totalAmount().value()
        );
    }
}
```

---

## REST API

### Admin Controller

```java
@RestController
@RequestMapping("/api/admin/features")
public class FeatureFlagAdminController {
    
    private final FeatureFlagService featureFlags;
    private final FeatureFlagRepository repository;
    
    @GetMapping
    public List<FeatureFlagDto> listAll() {
        return featureFlags.getAllFeatures()
            .stream()
            .map(FeatureFlagDto::from)
            .toList();
    }
    
    @PutMapping("/{name}")
    public ResponseEntity<?> updateFeature(
        @PathVariable String name,
        @RequestBody UpdateFeatureFlagRequest request
    ) {
        FeatureFlagEntity entity = repository.findById(name)
            .orElseThrow(() -> new FeatureFlagNotFoundException(name));
        
        entity.setEnabled(request.enabled());
        entity.setRolloutPercentage(request.rolloutPercentage());
        repository.save(entity);
        
        return ResponseEntity.ok(entity);
    }
    
    @PostMapping("/{name}/toggle")
    public ResponseEntity<?> toggle(@PathVariable String name) {
        FeatureFlagEntity entity = repository.findById(name)
            .orElseThrow(() -> new FeatureFlagNotFoundException(name));
        
        entity.setEnabled(!entity.isEnabled());
        repository.save(entity);
        
        return ResponseEntity.ok(entity);
    }
}
```

---

## Testing

### Mock Feature Flags em Testes

```java
@SpringBootTest
class OrderServiceTest {
    
    @MockBean
    private FeatureFlagService featureFlags;
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void shouldUseNewFlowWhenFeatureEnabled() {
        // Given
        when(featureFlags.isEnabled("new-order-flow")).thenReturn(true);
        
        // When
        Result<Order> result = orderService.createOrder(command);
        
        // Then
        verify(orderRepository).saveWithNewSchema(any());
    }
    
    @Test
    void shouldUseLegacyFlowWhenFeatureDisabled() {
        // Given
        when(featureFlags.isEnabled("new-order-flow")).thenReturn(false);
        
        // When
        Result<Order> result = orderService.createOrder(command);
        
        // Then
        verify(orderRepository).saveWithOldSchema(any());
    }
}
```

### Test Utilities

```java
public class FeatureFlagTestUtils {
    
    public static FeatureFlagService alwaysEnabled() {
        return mock(FeatureFlagService.class, invocation -> {
            if (invocation.getMethod().getName().equals("isEnabled")) {
                return true;
            }
            return invocation.callRealMethod();
        });
    }
    
    public static FeatureFlagService withFeatures(Map<String, Boolean> features) {
        return new InMemoryFeatureFlagService(features);
    }
}

// Uso
@Test
void test() {
    FeatureFlagService flags = FeatureFlagTestUtils.withFeatures(Map.of(
        "new-ui", true,
        "beta-api", false
    ));
}
```

---

## Monitoring

```java
@Component
public class FeatureFlagMetrics {
    
    private final FeatureFlagService featureFlags;
    private final MetricsFacade metrics;
    
    @EventListener
    public void onFeatureEvaluated(FeatureEvaluatedEvent event) {
        metrics.incrementCounter(
            "feature.evaluation",
            "feature", event.featureName(),
            "enabled", event.isEnabled(),
            "userId", event.context().userId()
        );
    }
    
    @Scheduled(fixedRate = 60000)
    public void reportFeatureStates() {
        featureFlags.getAllFeatures().forEach(feature -> {
            metrics.recordGauge(
                "feature.state",
                feature.isEnabled() ? 1 : 0,
                "feature", feature.name()
            );
        });
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use feature flags para dark launches
if (featureFlags.isEnabled("new-feature")) {
    newImplementation();
}

// ✅ Sempre forneça fallback
String variant = featureFlags.getVariation("feature", "control");

// ✅ Track metrics de experimentos
metrics.incrementCounter("experiment.exposure", "variant", variant);

// ✅ Remova feature flags após rollout completo
// (não acumule technical debt)

// ✅ Use contexto rico para targeting
FeatureContext.builder()
    .userId(userId)
    .attribute("country", country)
    .attribute("plan", plan)
    .build()
```

### ❌ DON'T

```java
// ❌ NÃO deixe feature flags para sempre no código
// ❌ NÃO use para configuração (use ConfigurationProvider)
// ❌ NÃO faça lógica complexa dentro de feature flags
// ❌ NÃO esqueça de documentar o propósito da flag
```

---

## Ver Também

- [Configuration Management](../guides/configuration.md)
- [A/B Testing Guide](../guides/ab-testing.md)
- [Observability](../guides/observability.md)
