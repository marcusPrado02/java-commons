# Port: Secrets Management

## Visão Geral

`commons-ports-secrets` define contratos para gerenciamento seguro de secrets (passwords, API keys, certificates), abstraindo implementações como AWS Secrets Manager, Azure Key Vault, ou HashiCorp Vault.

**Quando usar:**
- Armazenar senhas de banco de dados
- API keys de serviços externos
- Certificados TLS/SSL
- Encryption keys
- Credentials rotation

**Implementações disponíveis:**
- `commons-adapters-secrets-aws-secretsmanager` - AWS Secrets Manager
- `commons-adapters-secrets-azure-keyvault` - Azure Key Vault
- `commons-adapters-secrets-vault` - HashiCorp Vault

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-secrets</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (implementação) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-secrets-aws-secretsmanager</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Interfaces

### SecretsProvider

Interface principal para secrets.

```java
public interface SecretsProvider {
    
    /**
     * Recupera secret por nome.
     */
    Result<String> getSecret(String secretName);
    
    /**
     * Recupera secret como JSON e deserializa.
     */
    <T> Result<T> getSecret(String secretName, Class<T> type);
    
    /**
     * Cria ou atualiza secret.
     */
    Result<Void> putSecret(String secretName, String secretValue);
    
    /**
     * Cria ou atualiza secret JSON.
     */
    <T> Result<Void> putSecret(String secretName, T secretValue);
    
    /**
     * Deleta secret.
     */
    Result<Void> deleteSecret(String secretName);
    
    /**
     * Lista todos os secrets.
     */
    Result<List<String>> listSecrets();
    
    /**
     * Rotaciona secret (se suportado).
     */
    Result<Void> rotateSecret(String secretName);
}
```

### Secret

Modelo de secret com metadados.

```java
public record Secret(
    String name,
    String value,
    Map<String, String> metadata,
    Instant createdAt,
    Instant lastRotated,
    Optional<Instant> expiresAt
) {
    public boolean isExpired() {
        return expiresAt
            .map(expiry -> Instant.now().isAfter(expiry))
            .orElse(false);
    }
    
    public Duration age() {
        return Duration.between(createdAt, Instant.now());
    }
    
    public boolean needsRotation(Duration maxAge) {
        return age().compareTo(maxAge) > 0;
    }
}
```

---

## 💡 Basic Usage

### Database Credentials

```java
@Configuration
public class DatabaseConfiguration {
    
    private final SecretsProvider secretsProvider;
    
    @Bean
    public DataSource dataSource() {
        // Retrieve database credentials
        Result<DatabaseCredentials> credentialsResult = 
            secretsProvider.getSecret(
                "database/postgres/credentials",
                DatabaseCredentials.class
            );
        
        if (credentialsResult.isFail()) {
            throw new RuntimeException(
                "Failed to load database credentials",
                credentialsResult.problemOrNull()
            );
        }
        
        DatabaseCredentials credentials = credentialsResult.getOrThrow();
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(credentials.url());
        config.setUsername(credentials.username());
        config.setPassword(credentials.password());
        config.setMaximumPoolSize(20);
        
        return new HikariDataSource(config);
    }
}

public record DatabaseCredentials(
    String url,
    String username,
    String password
) {}
```

### API Keys

```java
@Service
public class PaymentService {
    
    private final SecretsProvider secretsProvider;
    private final String apiKey;
    
    public PaymentService(SecretProvider secretsProvider) {
        this.secretsProvider = secretsProvider;
        
        // Lazy load API key
        this.apiKey = secretsProvider
            .getSecret("payment-gateway/api-key")
            .getOrElse(() -> {
                throw new RuntimeException("Payment API key not found");
            });
    }
    
    public Result<PaymentResponse> processPayment(PaymentRequest request) {
        HttpRequest httpRequest = HttpRequest.builder()
            .method(HttpMethod.POST)
            .url("https://api.payment.com/v1/charge")
            .header("Authorization", "Bearer " + apiKey)
            .body(request)
            .build();
        
        return httpClient.execute(httpRequest)
            .flatMap(response -> response.bodyAs(
                PaymentResponse.class,
                objectMapper
            ));
    }
}
```

---

## 🔄 Cached Secrets Provider

### Caching Wrapper

```java
public class CachedSecretsProvider implements SecretsProvider {
    
    private final SecretsProvider delegate;
    private final CacheProvider cache;
    private final Duration cacheTtl;
    
    public CachedSecretsProvider(
        SecretsProvider delegate,
        CacheProvider cache,
        Duration cacheTtl
    ) {
        this.delegate = delegate;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }
    
    @Override
    public Result<String> getSecret(String secretName) {
        String cacheKey = "secret:" + secretName;
        
        // Try cache first
        Optional<String> cached = cache.get(cacheKey, String.class);
        
        if (cached.isPresent()) {
            return Result.ok(cached.get());
        }
        
        // Load from secrets provider
        return delegate.getSecret(secretName)
            .andThen(secretValue -> {
                // Cache for short duration
                cache.set(cacheKey, secretValue, cacheTtl);
            });
    }
    
    @Override
    public <T> Result<T> getSecret(String secretName, Class<T> type) {
        String cacheKey = "secret:" + secretName + ":" + type.getName();
        
        Optional<T> cached = cache.get(cacheKey, type);
        
        if (cached.isPresent()) {
            return Result.ok(cached.get());
        }
        
        return delegate.getSecret(secretName, type)
            .andThen(secretValue -> {
                cache.set(cacheKey, secretValue, cacheTtl);
            });
    }
    
    @Override
    public Result<Void> putSecret(String secretName, String secretValue) {
        // Invalidate cache
        String cacheKey = "secret:" + secretName;
        cache.delete(cacheKey);
        
        return delegate.putSecret(secretName, secretValue);
    }
}
```

---

## 🔐 Encryption Service

### EncryptionService Interface

```java
public interface EncryptionService {
    
    /**
     * Encrypta dados.
     */
    Result<String> encrypt(String plaintext);
    
    /**
     * Decripta dados.
     */
    Result<String> decrypt(String ciphertext);
    
    /**
     * Encrypta com key específica.
     */
    Result<String> encrypt(String plaintext, String keyId);
    
    /**
     * Decripta com key específica.
     */
    Result<String> decrypt(String ciphertext, String keyId);
    
    /**
     * Gera data key.
     */
    Result<DataKey> generateDataKey(String keyId);
}

public record DataKey(
    String keyId,
    byte[] plaintext,
    byte[] encrypted
) {
    /**
     * Zera plaintext da memória.
     */
    public void destroy() {
        Arrays.fill(plaintext, (byte) 0);
    }
}
```

### Envelope Encryption

```java
@Service
public class SecureStorageService {
    
    private final EncryptionService encryptionService;
    private final FileStorage fileStorage;
    
    public Result<Void> storeSecurely(String fileName, byte[] data) {
        // Generate data encryption key
        Result<DataKey> dataKeyResult = encryptionService
            .generateDataKey("master-key");
        
        if (dataKeyResult.isFail()) {
            return Result.fail(dataKeyResult.problemOrNull());
        }
        
        DataKey dataKey = dataKeyResult.getOrThrow();
        
        try {
            // Encrypt data with data key
            byte[] encryptedData = encryptWithAes(data, dataKey.plaintext());
            
            // Store encrypted data + encrypted data key
            EnvelopeData envelope = new EnvelopeData(
                encryptedData,
                dataKey.encrypted()
            );
            
            return fileStorage.store(fileName, serialize(envelope));
            
        } finally {
            // Destroy plaintext key from memory
            dataKey.destroy();
        }
    }
    
    public Result<byte[]> retrieveSecurely(String fileName) {
        return fileStorage.retrieve(fileName)
            .flatMap(envelopeBytes -> {
                EnvelopeData envelope = deserialize(envelopeBytes);
                
                // Decrypt data key
                return encryptionService.decrypt(
                    Base64.getEncoder().encodeToString(envelope.encryptedDataKey())
                ).flatMap(dataKeyPlaintext -> {
                    // Decrypt data
                    byte[] dataKey = Base64.getDecoder().decode(dataKeyPlaintext);
                    byte[] decryptedData = decryptWithAes(
                        envelope.encryptedData(),
                        dataKey
                    );
                    
                    // Clean up
                    Arrays.fill(dataKey, (byte) 0);
                    
                    return Result.ok(decryptedData);
                });
            });
    }
}
```

---

## 🔄 Secret Rotation

### Rotation Strategy

```java
@Service
public class SecretRotationService {
    
    private final SecretsProvider secretsProvider;
    private final StructuredLog log;
    
    public Result<Void> rotateSecret(
        String secretName,
        SecretGenerator generator
    ) {
        log.info("Rotating secret")
            .field("secretName", secretName)
            .log();
        
        // Generate new secret value
        String newValue = generator.generate();
        
        // Store new secret
        Result<Void> putResult = secretsProvider.putSecret(
            secretName,
            newValue
        );
        
        if (putResult.isFail()) {
            log.error("Failed to rotate secret")
                .field("secretName", secretName)
                .field("error", putResult.problemOrNull().detail())
                .log();
            
            return putResult;
        }
        
        log.info("Secret rotated successfully")
            .field("secretName", secretName)
            .log();
        
        return Result.ok();
    }
}

@FunctionalInterface
public interface SecretGenerator {
    String generate();
}
```

### Scheduled Rotation

```java
@Component
public class DatabasePasswordRotationJob implements ScheduledTask {
    
    private final SecretRotationService rotationService;
    private final DataSourceManager dataSourceManager;
    
    @Override
    public String name() {
        return "rotate-database-password";
    }
    
    @Override
    public String cronExpression() {
        return "0 0 2 1 * ?"; // 1st day of month at 2 AM
    }
    
    @Override
    public Result<Void> execute() {
        String secretName = "database/postgres/credentials";
        
        // Generate new password
        SecretGenerator passwordGenerator = () -> {
            return SecureRandom.getInstanceStrong()
                .ints(32, 33, 126)
                .collect(
                    StringBuilder::new,
                    StringBuilder::appendCodePoint,
                    StringBuilder::append
                )
                .toString();
        };
        
        return rotationService.rotateSecret(secretName, passwordGenerator)
            .andThen(() -> {
                // Update database password
                return dataSourceManager.updatePassword(newPassword);
            })
            .andThen(() -> {
                // Restart connection pool
                return dataSourceManager.restartPool();
            });
    }
}
```

---

## 🎯 Multi-Environment Secrets

### Environment-Aware Provider

```java
@Configuration
public class SecretsConfiguration {
    
    @Value("${spring.profiles.active}")
    private String environment;
    
    @Bean
    public SecretsProvider secretsProvider() {
        SecretsProvider baseProvider = createProvider();
        
        return new EnvironmentSecretsProvider(
            baseProvider,
            environment
        );
    }
    
    private SecretsProvider createProvider() {
        // Choose provider based on environment
        return switch (environment) {
            case "prod" -> new AwsSecretsManagerProvider();
            case "staging" -> new AzureKeyVaultProvider();
            default -> new LocalSecretsProvider();
        };
    }
}

public class EnvironmentSecretsProvider implements SecretsProvider {
    
    private final SecretsProvider delegate;
    private final String environment;
    
    @Override
    public Result<String> getSecret(String secretName) {
        // Prefix with environment
        String fullName = environment + "/" + secretName;
        return delegate.getSecret(fullName);
    }
}
```

---

## 🧪 Testing

### Mock Secrets Provider

```java
public class MockSecretsProvider implements SecretsProvider {
    
    private final Map<String, String> secrets = new HashMap<>();
    
    public void mockSecret(String name, String value) {
        secrets.put(name, value);
    }
    
    public <T> void mockSecret(String name, T value, ObjectMapper mapper) {
        try {
            String json = mapper.writeValueAsString(value);
            secrets.put(name, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Result<String> getSecret(String secretName) {
        String value = secrets.get(secretName);
        
        if (value == null) {
            return Result.fail(Problem.of(
                "SECRETS.NOT_FOUND",
                "Secret not found: " + secretName
            ));
        }
        
        return Result.ok(value);
    }
    
    @Override
    public <T> Result<T> getSecret(String secretName, Class<T> type) {
        return getSecret(secretName)
            .flatMap(json -> {
                try {
                    T value = new ObjectMapper().readValue(json, type);
                    return Result.ok(value);
                } catch (JsonProcessingException e) {
                    return Result.fail(Problem.of(
                        "SECRETS.PARSE_ERROR",
                        e.getMessage()
                    ));
                }
            });
    }
    
    @Override
    public Result<Void> putSecret(String secretName, String secretValue) {
        secrets.put(secretName, secretValue);
        return Result.ok();
    }
}
```

### Test Example

```java
class PaymentServiceTest {
    
    private MockSecretsProvider secretsProvider;
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        secretsProvider = new MockSecretsProvider();
        
        // Mock API key
        secretsProvider.mockSecret(
            "payment-gateway/api-key",
            "test-api-key-12345"
        );
        
        paymentService = new PaymentService(secretsProvider);
    }
    
    @Test
    void shouldLoadApiKeyFromSecrets() {
        // Given: Secret mocked in setUp
        
        // When: Process payment
        PaymentRequest request = new PaymentRequest(
            Money.of(100.00, "USD"),
            "4111111111111111"
        );
        
        Result<PaymentResponse> result = paymentService.processPayment(request);
        
        // Then: API key used in request
        assertThat(result.isOk()).isTrue();
        // Verify HTTP request contains Authorization header
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Cache secrets com TTL curto
CachedSecretsProvider cached = new CachedSecretsProvider(
    secretsProvider,
    cache,
    Duration.ofMinutes(5)  // ✅ Curto para permitir rotation
);

// ✅ Use try-finally para limpar secrets da memória
String password = secretsProvider.getSecret("db-password").getOrThrow();
try {
    // Use password
} finally {
    password = null;  // ✅ Ajuda GC
}

// ✅ Rotacione secrets regularmente
@Scheduled(cron = "0 0 2 1 * ?")  // Monthly
public void rotateSecrets() { }

// ✅ Use envelope encryption para dados grandes
DataKey dataKey = encryptionService.generateDataKey("master-key");
try {
    byte[] encrypted = encrypt(data, dataKey.plaintext());
} finally {
    dataKey.destroy();  // ✅ Limpar plaintext
}

// ✅ Log acesso a secrets (audit)
log.info("Secret accessed")
    .field("secretName", secretName)
    .field("userId", userId)
    .log();
```

### ❌ DON'T

```java
// ❌ NÃO logue secrets
log.info("Password: " + password);  // ❌ NUNCA!

// ❌ NÃO cache secrets por muito tempo
cache.set("api-key", key, Duration.ofDays(30));  // ❌ Muito longo!

// ❌ NÃO hardcode secrets
String apiKey = "sk_live_abc123";  // ❌ Usar secrets provider!

// ❌ NÃO retorne secrets em APIs
@GetMapping("/config")
public Config getConfig() {
    return new Config(secretsProvider.getSecret("api-key"));  // ❌ Exposto!
}

// ❌ NÃO ignore falhas de secrets
String key = secretsProvider.getSecret("key")
    .getOrElse("default");  // ❌ Sem fallback para secrets!
```

---

## Ver Também

- [AWS Secrets Manager Adapter](../../../commons-adapters-secrets-aws-secretsmanager/) - AWS implementation
- [Azure Key Vault Adapter](../../../commons-adapters-secrets-azure-keyvault/) - Azure implementation
- [HashiCorp Vault Adapter](../../../commons-adapters-secrets-vault/) - Vault implementation
- [Configuration Management](../../guides/configuration.md) - External configuration
