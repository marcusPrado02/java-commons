# commons-adapters-secrets-vault

HashiCorp Vault adapter for `SecretStorePort`.

## 📋 Overview

This adapter provides integration with [HashiCorp Vault](https://www.vaultproject.io/), enabling secure storage and retrieval of secrets in distributed systems.

## ✨ Features

- ✅ **KV Secrets Engine v2** support with automatic versioning
- ✅ **Dynamic secrets** retrieval
- ✅ **Secret rotation** compatible
- ✅ **Metadata tracking** (version, created_at)
- ✅ **Nested paths** support (e.g., `app/prod/db/password`)
- ✅ **Thread-safe** operations
- ✅ **Auto-closeable** SecretValue for secure memory handling

## 🚀 Quick Start

### Add Dependency

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-secrets-vault</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Configuration

```java
@Configuration
public class VaultConfig {

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.create("vault.example.com", 8200);
        endpoint.setScheme("https");

        TokenAuthentication auth = new TokenAuthentication("your-vault-token");

        return new VaultTemplate(endpoint, auth);
    }

    @Bean
    public SecretStorePort secretStore(VaultTemplate vaultTemplate) {
        return new VaultSecretStoreAdapter(vaultTemplate);
    }
}
```

### Using with Spring Cloud Vault

```yaml
spring:
  cloud:
    vault:
      uri: https://vault.example.com:8200
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        default-context: application
```

```java
@Bean
public SecretStorePort secretStore(VaultTemplate vaultTemplate) {
    return new VaultSecretStoreAdapter(vaultTemplate, "secret"); // default mount
}
```

## 💻 Usage Examples

### Store and Retrieve Simple Secret

```java
@Autowired
private SecretStorePort secretStore;

public void storeApiKey() {
    SecretKey key = SecretKey.of("api/stripe/key");
    SecretValue value = SecretValue.of("sk_test_123456");

    String version = secretStore.put(key, value);
    System.out.println("Stored secret version: " + version);
}

public String getApiKey() {
    SecretKey key = SecretKey.of("api/stripe/key");

    return secretStore.get(key)
        .map(SecretValue::asString)
        .orElseThrow(() -> new RuntimeException("API key not found"));
}
```

### Store Structured Secrets (Database Credentials)

```java
public void storeDbCredentials() {
    SecretKey key = SecretKey.of("db/production/postgres");

    Map<String, String> credentials = Map.of(
        "username", "admin",
        "password", "super-secret-pass",
        "host", "db.prod.example.com",
        "port", "5432",
        "database", "myapp"
    );

    secretStore.put(key, credentials);
}

public Map<String, String> getDbCredentials() {
    SecretKey key = SecretKey.of("db/production/postgres");

    String serialized = secretStore.get(key)
        .map(SecretValue::asString)
        .orElseThrow();

    // Parse the serialized format: {key1=value1,key2=value2}
    // In production, use proper JSON deserialization
    return parseCredentials(serialized);
}
```

### Auto-Close for Security

```java
public void processSecret() {
    SecretKey key = SecretKey.of("sensitive/token");

    try (SecretValue secret = secretStore.get(key).orElseThrow()) {
        String token = secret.asString();
        // Use token...
    } // Secret is zeroed out in memory here
}
```

### Check if Secret Exists

```java
public boolean hasApiKey(String service) {
    SecretKey key = SecretKey.of("api/" + service + "/key");
    return secretStore.exists(key);
}
```

### List Secrets with Prefix

```java
public List<String> listDatabaseSecrets() {
    return secretStore.list("db/")
        .stream()
        .map(SecretKey::value)
        .collect(Collectors.toList());
}
```

### Delete Secret

```java
public void revokeApiKey(String service) {
    SecretKey key = SecretKey.of("api/" + service + "/key");

    boolean deleted = secretStore.delete(key);
    if (deleted) {
        log.info("API key revoked for service: {}", service);
    }
}
```

### Secret Rotation

```java
@Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
public void rotateApiKeys() {
    List<SecretKey> apiKeys = secretStore.list("api/");

    for (SecretKey key : apiKeys) {
        // Generate new key
        String newKey = generateNewApiKey();

        // Update in Vault
        secretStore.put(key, SecretValue.of(newKey));

        log.info("Rotated secret: {}", key);
    }
}
```

## 🔧 Vault Setup

### Enable KV v2 Secrets Engine

```bash
vault secrets enable -path=secret kv-v2
```

### Store a Secret

```bash
vault kv put secret/api/stripe/key value=sk_test_123456
```

### Read a Secret

```bash
vault kv get secret/api/stripe/key
```

### List Secrets

```bash
vault kv list secret/api/
```

## 🏗️ Architecture

### Path Resolution

- Input: `SecretKey.of("db/postgres/password")`
- Resolved: `/v1/secret/data/db/postgres/password`
- Mount path: `secret` (configurable)
- KV v2 requirement: `/data/` in path

### Data Format

**Writing to Vault (KV v2):**
```json
{
  "data": {
    "value": "my-secret"
  }
}
```

**Reading from Vault (KV v2 Response):**
```json
{
  "data": {
    "data": {
      "value": "my-secret"
    },
    "metadata": {
      "created_time": "2026-02-13T14:20:00Z",
      "version": 1
    }
  }
}
```

## 🧪 Testing

11 integration tests with Testcontainers Vault:

```bash
./mvnw test -pl commons-adapters-secrets-vault
```

**Test Coverage:**
- ✓ Put and get simple secrets
- ✓ Put and get secret maps (structured data)
- ✓ Non-existent secrets (empty Optional)
- ✓ Delete secrets
- ✓ Check existence
- ✓ Update secrets (versioning)
- ✓ Versioned secrets metadata
- ✓ Multiple secrets
- ✓ Nested paths
- ✓ Special characters in values
- ✓ Auto-close resource management

**Test Duration:** ~3.5s with Testcontainers

## ⚙️ Configuration Options

### Custom Mount Path

```java
@Bean
public SecretStorePort secretStore(VaultTemplate vaultTemplate) {
    return new VaultSecretStoreAdapter(vaultTemplate, "custom-secrets");
}
```

### Different Vault Namespace

```java
VaultEndpoint endpoint = VaultEndpoint.create("vault.example.com", 8200);
endpoint.setScheme("https");

ClientAuthentication auth = new TokenAuthentication("token");

VaultTemplate template = new VaultTemplate(endpoint, auth);
template.setNamespace("prod"); // Enterprise feature

SecretStorePort secretStore = new VaultSecretStoreAdapter(template);
```

### AppRole Authentication

```java
@Bean
public SecretStorePort secretStoreWithAppRole() {
    VaultEndpoint endpoint = VaultEndpoint.create("vault.example.com", 8200);

    AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions
        .builder()
        .roleId(AppRoleAuthenticationOptions.RoleId.provided("role-id"))
        .secretId(AppRoleAuthenticationOptions.SecretId.provided("secret-id"))
        .build();

    AppRoleAuthentication auth = new AppRoleAuthentication(options, new RestTemplate());

    VaultTemplate template = new VaultTemplate(endpoint, auth);

    return new VaultSecretStoreAdapter(template);
}
```

## 🔒 Security Best Practices

1. **Never log secret values**
   ```java
   log.info("Retrieved secret: {}", key); // ✅ Good
   log.info("Secret value: {}", value.asString()); // ❌ Bad
   ```

2. **Use try-with-resources**
   ```java
   try (SecretValue secret = secretStore.get(key).orElseThrow()) {
       // Use secret
   } // Auto-zeroed
   ```

3. **Rotate secrets regularly**
   - Use Vault's dynamic secrets when possible
   - Implement automatic rotation policies
   - Audit secret access

4. **Least privilege access**
   - Use AppRole with minimal policies
   - Avoid root tokens in production
   - Enable audit logging

5. **Secure transport**
   - Always use HTTPS for Vault
   - Validate TLS certificates
   - Use Vault namespaces for multi-tenancy

## 📊 Performance

- **Write latency**: ~10-50ms (depends on network + Vault)
- **Read latency**: ~5-30ms (faster with caching)
- **Vault KV v2**: Automatic versioning overhead is minimal
- **Recommendation**: Cache frequently accessed secrets locally with TTL

## 🐛 Troubleshooting

### "400 Bad Request: no data provided"

**Cause:** Secret payload not wrapped in `data` object for KV v2.

**Solution:** This adapter handles it automatically. Ensure you're using KV v2:
```bash
vault secrets enable -version=2 kv
```

### "403 Permission Denied"

**Cause:** Vault token/role doesn't have permission.

**Solution:** Grant appropriate policy:
```hcl
path "secret/data/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
```

### Connection timeout

**Cause:** Vault unreachable or network issue.

**Solution:** Check Vault health:
```bash
vault status
```

## 📚 References

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Spring Vault Reference](https://docs.spring.io/spring-vault/docs/current/reference/html/)
- [KV Secrets Engine v2](https://www.vaultproject.io/docs/secrets/kv/kv-v2)
- [Vault API Documentation](https://www.vaultproject.io/api-docs)

## 🔜 Future Enhancements

- [ ] Support for KV v1 engine
- [ ] Dynamic database credentials integration
- [ ] AWS IAM authentication
- [ ] Kubernetes authentication
- [ ] Secret caching with TTL
- [ ] Batch operations
- [ ] Transit encryption engine support
