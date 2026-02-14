# Commons Spring Starter Secrets

[![Maven Central](https://img.shields.io/maven-central/v/com.marcusprado02/commons-spring-starter-secrets.svg)](https://central.sonatype.com/artifact/com.marcusprado02/commons-spring-starter-secrets)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot auto-configuration starter for Commons secrets management adapters, providing zero-configuration integration with **HashiCorp Vault**, **AWS Secrets Manager**, and **Azure Key Vault**.

## Features

- ✅ **Zero-configuration** secrets management with sensible defaults
- 🔐 **Multiple backends**: Vault, AWS Secrets Manager, Azure Key Vault
- ⚙️ **Flexible configuration** via Spring Boot properties
- 💪 **Type-safe** configuration with `SecretsProperties`
- 🏥 **Health checks** via Spring Boot Actuator
- 🎯 **Conditional auto-configuration** based on classpath
- 🔄 **Caching support** (optional TTL-based caching)
- 📦 **Minimal dependencies** (only brings what you need)

## Installation

Add the starter dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-spring-starter-secrets</artifactId>
  <version>${commons.version}</version>
</dependency>
```

### Backend Dependencies

Choose **one** secrets backend:

#### HashiCorp Vault (recommended)

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-secrets-vault</artifactId>
</dependency>
```

#### AWS Secrets Manager

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-secrets-aws-secrets-manager</artifactId>
</dependency>
```

#### Azure Key Vault

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-secrets-azure-keyvault</artifactId>
</dependency>
```

### Optional Dependencies

Enable health indicators:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Quick Start

### HashiCorp Vault (default)

With **zero configuration**, the starter will connect to `http://localhost:8200`:

```java
@Autowired
private SecretStorePort secretStore;

public void example() {
  // Store secret
  secretStore.store("database/password", "super-secret");

  // Retrieve secret
  String password = secretStore.retrieve("database/password");

  // Delete secret
  secretStore.delete("database/password");

  // Check if exists
  boolean exists = secretStore.exists("database/password");

  // List secrets by prefix
  List<String> keys = secretStore.listKeys("database/");
}
```

### AWS Secrets Manager

To use AWS Secrets Manager, set the secrets type:

```yaml
commons:
  secrets:
    type: aws
    aws:
      region: us-east-1
```

### Azure Key Vault

To use Azure Key Vault:

```yaml
commons:
  secrets:
    type: azure
    azure:
      vault-url: https://my-vault.vault.azure.net/
```

## Configuration

### HashiCorp Vault Configuration

```yaml
commons:
  secrets:
    type: vault  # default
    vault:
      enabled: true  # default
      uri: http://localhost:8200  # default
      token: ${VAULT_TOKEN}  # required
      kv-path: secret  # default
      kv-version: 2  # default (1 or 2)
      timeout: 5s  # connection timeout
```

### AWS Secrets Manager Configuration

```yaml
commons:
  secrets:
    type: aws
    aws:
      enabled: true  # default
      region: us-east-1  # default
      endpoint: http://localhost:4566  # optional (for LocalStack)
```

**Authentication**: Uses AWS Default Credential Chain (IAM roles, environment variables, etc.)

### Azure Key Vault Configuration

```yaml
commons:
  secrets:
    type: azure
    azure:
      enabled: true  # default
      vault-url: https://my-vault.vault.azure.net/  # required
```

**Authentication**: Uses Azure Default Credential (Managed Identity, environment variables, etc.)

### Disable Secrets Management

```yaml
commons:
  secrets:
    type: none
```

Or disable specific backends:

```yaml
commons:
  secrets:
    vault:
      enabled: false
    aws:
      enabled: false
    azure:
      enabled: false
```

## Health Indicators

When Spring Boot Actuator is on the classpath, a health indicator is automatically registered:

```bash
curl http://localhost:8080/actuator/health/secrets
```

Response:

```json
{
  "status": "UP",
  "details": {
    "type": "VaultSecretStoreAdapter",
    "status": "Secret store read/write successful"
  }
}
```

## Advanced Usage

### Custom VaultTemplate

Override the auto-configured `VaultTemplate`:

```java
@Configuration
public class SecretsConfig {

  @Bean
  public VaultTemplate vaultTemplate() {
    VaultEndpoint endpoint = VaultEndpoint.from(URI.create("https://vault.example.com"));
    ClientAuthentication auth = new AppRoleAuthentication(
      AppRoleAuthenticationOptions.builder()
        .roleId("my-role")
        .secretId("my-secret")
        .build()
    );

    return new VaultTemplate(endpoint, auth);
  }
}
```

### Custom AWS Secrets Manager Client

Override the auto-configured client:

```java
@Configuration
public class SecretsConfig {

  @Bean
  public SecretsManagerClient secretsManagerClient() {
    return SecretsManagerClient.builder()
      .region(Region.US_WEST_2)
      .credentialsProvider(InstanceProfileCredentialsProvider.create())
      .build();
  }
}
```

### Custom Azure Secret Client

Override the auto-configured client:

```java
@Configuration
public class SecretsConfig {

  @Bean
  public SecretClient secretClient() {
    return new SecretClientBuilder()
      .vaultUrl("https://my-vault.vault.azure.net/")
      .credential(new ManagedIdentityCredentialBuilder().build())
      .buildClient();
  }
}
```

### Environment-Specific Secrets

Use different backends per environment:

```yaml
# application-dev.yml
commons:
  secrets:
    type: vault
    vault:
      uri: http://localhost:8200
      token: dev-token

# application-prod.yml
commons:
  secrets:
    type: aws
    aws:
      region: us-east-1
```

## How It Works

### Auto-Configuration

The starter provides three auto-configuration classes:

1. **`VaultSecretsAutoConfiguration`**
   - Activated when `VaultSecretStoreAdapter` is on classpath
   - Configured via `commons.secrets.vault.*` properties
   - Creates: `VaultTemplate`, `SecretStorePort`

2. **`AwsSecretsAutoConfiguration`**
   - Activated when `AwsSecretsManagerSecretStoreAdapter` is on classpath
   - Configured via `commons.secrets.aws.*` properties
   - Creates: `SecretsManagerClient`, `SecretStorePort`

3. **`AzureSecretsAutoConfiguration`**
   - Activated when `AzureKeyVaultSecretStoreAdapter` is on classpath
   - Configured via `commons.secrets.azure.*` properties
   - Creates: `SecretClient`, `SecretStorePort`

### Conditional Bean Creation

Beans are created **only when not already defined**, allowing full customization:

```java
@ConditionalOnMissingBean(SecretStorePort.class)
public SecretStorePort secretStorePort(...) {
  // Auto-configured only if user didn't provide one
}
```

### Type Selection

The `commons.secrets.type` property controls which `SecretStorePort` bean is created:

- `vault` (default): Creates Vault-backed secret store
- `aws`: Creates AWS Secrets Manager-backed secret store
- `azure`: Creates Azure Key Vault-backed secret store
- `none`: Disables secrets auto-configuration

## Testing

The starter includes comprehensive tests for Vault and AWS using Testcontainers:

### Example Test

```java
@SpringBootTest
@Testcontainers
class SecretsIntegrationTest {

  @Container
  static GenericContainer<?> vault =
    new GenericContainer<>("hashicorp/vault:1.15")
      .withExposedPorts(8200)
      .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "test-token");

  @DynamicPropertySource
  static void vaultProperties(DynamicPropertyRegistry registry) {
    registry.add("commons.secrets.vault.uri",
      () -> "http://" + vault.getHost() + ":" + vault.getFirstMappedPort());
    registry.add("commons.secrets.vault.token", () -> "test-token");
  }

  @Autowired
  private SecretStorePort secretStore;

  @Test
  void shouldStoreAndRetrieveSecret() {
    secretStore.store("key", "value");
    assertThat(secretStore.retrieve("key")).isEqualTo("value");
  }
}
```

## Configuration Properties Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `commons.secrets.type` | `SecretsType` | `vault` | Secrets backend: `vault`, `aws`, `azure`, `none` |
| `commons.secrets.vault.enabled` | `boolean` | `true` | Enable Vault secrets |
| `commons.secrets.vault.uri` | `String` | `http://localhost:8200` | Vault server URI |
| `commons.secrets.vault.token` | `String` | - | Vault authentication token (required) |
| `commons.secrets.vault.kv-path` | `String` | `secret` | KV secrets engine mount path |
| `commons.secrets.vault.kv-version` | `int` | `2` | KV secrets engine version (1 or 2) |
| `commons.secrets.vault.timeout` | `Duration` | `5s` | Connection timeout |
| `commons.secrets.aws.enabled` | `boolean` | `true` | Enable AWS Secrets Manager |
| `commons.secrets.aws.region` | `String` | `us-east-1` | AWS region |
| `commons.secrets.aws.endpoint` | `String` | - | AWS endpoint override (for LocalStack) |
| `commons.secrets.azure.enabled` | `boolean` | `true` | Enable Azure Key Vault |
| `commons.secrets.azure.vault-url` | `String` | - | Azure Key Vault URL (required) |
| `commons.secrets.cache.enabled` | `boolean` | `true` | Enable secret caching |
| `commons.secrets.cache.ttl` | `Duration` | `5m` | Cache TTL |

## IDE Support

Enable auto-completion for configuration properties in IntelliJ IDEA / VS Code:

1. Ensure `spring-boot-configuration-processor` is on classpath (already included as optional dependency)
2. IDE will provide auto-completion for `commons.secrets.*` properties

## Architecture

The starter follows the **Port-Adapter pattern** (Hexagonal Architecture):

```
┌──────────────────────────────────────┐
│   Spring Boot Application            │
├──────────────────────────────────────┤
│   commons-spring-starter-secrets     │ ← Auto-configuration
│   ├─ VaultSecretsAutoConfiguration   │
│   ├─ AwsSecretsAutoConfiguration     │
│   ├─ AzureSecretsAutoConfiguration   │
│   └─ SecretsHealthIndicator          │
├──────────────────────────────────────┤
│   SecretStorePort                    │ ← Port (Interface)
├──────────────────────────────────────┤
│   Adapters                           │
│   ├─ VaultSecretStoreAdapter         │ ← Vault Implementation
│   ├─ AwsSecretsManagerAdapter        │ ← AWS Implementation
│   └─ AzureKeyVaultAdapter            │ ← Azure Implementation
└──────────────────────────────────────┘
```

## Comparison with Spring Cloud Vault

| Feature | Commons Secrets Starter | Spring Cloud Vault |
|---------|------------------------|-------------------|
| **Programming Model** | Imperative (manual control) | Declarative (PropertySource) |
| **Use Case** | Direct secret access | Application configuration |
| **Flexibility** | Full control over secret operations | Limited to property injection |
| **Backend Support** | Vault, AWS, Azure | Vault only |
| **Port-Adapter** | ✅ Follows Hexagonal Architecture | ❌ Spring-specific |
| **Testing** | Easy to mock `SecretStorePort` | Requires Vault setup |
| **Dynamic Secrets** | ✅ Runtime secret operations | ❌ Startup-only |

**When to use Commons Secrets:**
- ✅ Need runtime secret access (not just startup config)
- ✅ Want port-adapter pattern for testability
- ✅ Need multiple backends (Vault, AWS, Azure)
- ✅ Prefer imperative over declarative

**When to use Spring Cloud Vault:**
- ✅ Simple property injection from Vault
- ✅ Prefer declarative `@Value` approach
- ✅ Already using Spring Cloud ecosystem

## Examples

### Database Password Management

```java
@Service
public class DatabaseService {

  private final SecretStorePort secretStore;
  private final DataSource dataSource;

  @PostConstruct
  public void rotatePasswordIfNeeded() {
    String currentPassword = secretStore.retrieve("database/password");

    if (isPasswordExpired(currentPassword)) {
      String newPassword = generateSecurePassword();
      secretStore.store("database/password", newPassword);
      updateDatabasePassword(newPassword);
    }
  }
}
```

### API Key Storage

```java
@Service
public class ExternalApiClient {

  private final SecretStorePort secretStore;
  private final RestTemplate restTemplate;

  public String callExternalApi() {
    String apiKey = secretStore.retrieve("external-api/key");

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-API-Key", apiKey);

    return restTemplate.exchange(
      "https://api.example.com/data",
      HttpMethod.GET,
      new HttpEntity<>(headers),
      String.class
    ).getBody();
  }
}
```

### Secret Rotation

```java
@Service
public class SecretRotationService {

  private final SecretStorePort secretStore;

  @Scheduled(cron = "0 0 0 * * *")  // Daily at midnight
  public void rotateSecrets() {
    List<String> secrets = secretStore.listKeys("rotatable/");

    for (String secretKey : secrets) {
      String oldValue = secretStore.retrieve(secretKey);
      String newValue = generateNewSecret();

      secretStore.store(secretKey, newValue);
      notifyDependentServices(secretKey, newValue);
    }
  }
}
```

## Troubleshooting

### Secrets not auto-configured

**Symptom:** No `SecretStorePort` bean available

**Solutions:**
1. Ensure backend dependency is on classpath:
   ```xml
   <dependency>
     <groupId>com.marcusprado02</groupId>
     <artifactId>commons-adapters-secrets-vault</artifactId>
   </dependency>
   ```

2. Check configuration:
   ```yaml
   commons:
     secrets:
       type: vault  # or aws/azure
       vault:
         enabled: true
         token: ${VAULT_TOKEN}
   ```

3. Enable auto-configuration debug:
   ```yaml
   logging:
     level:
       com.marcusprado02.commons.spring.secrets: DEBUG
   ```

### Connection refused (Vault)

**Symptom:** Health check fails with connection error

**Solutions:**
1. Verify Vault is running:
   ```bash
   docker run -d -p 8200:8200 \
     -e VAULT_DEV_ROOT_TOKEN_ID=mytoken \
     hashicorp/vault:1.15
   ```

2. Check URI configuration:
   ```yaml
   commons:
     secrets:
       vault:
         uri: http://localhost:8200
         token: mytoken
   ```

### AWS Authentication failed

**Symptom:** `Unable to load credentials from any provider`

**Solutions:**
1. Configure AWS credentials:
   ```bash
   export AWS_ACCESS_KEY_ID=your-key
   export AWS_SECRET_ACCESS_KEY=your-secret
   export AWS_REGION=us-east-1
   ```

2. Use IAM roles (recommended in production):
   ```yaml
   commons:
     secrets:
       type: aws
       aws:
         region: us-east-1
   ```

### Azure Authentication failed

**Symptom:** `Authentication failed`

**Solutions:**
1. Configure Managed Identity in Azure
2. Or use environment variables:
   ```bash
   export AZURE_CLIENT_ID=your-client-id
   export AZURE_CLIENT_SECRET=your-secret
   export AZURE_TENANT_ID=your-tenant-id
   ```

## Migration Guide

### From Spring Cloud Vault

Replace PropertySource:

```java
// Before (Spring Cloud Vault)
@Value("${database.password}")
private String databasePassword;

// After (Commons Secrets)
@Autowired
private SecretStorePort secretStore;

@PostConstruct
public void init() {
  String databasePassword = secretStore.retrieve("database/password");
}
```

### From direct Vault client

Replace client usage:

```java
// Before (direct VaultTemplate)
@Autowired
private VaultTemplate vaultTemplate;

public String getSecret(String path) {
  VaultResponse response = vaultTemplate.read("secret/data/" + path);
  return response.getData().get("value").toString();
}

// After (SecretStorePort)
@Autowired
private SecretStorePort secretStore;

public String getSecret(String path) {
  return secretStore.retrieve(path);
}
```

## Security Best Practices

1. **Never commit secrets** to version control
2. **Use environment variables** for tokens/credentials
3. **Enable audit logging** in Vault/AWS/Azure
4. **Rotate secrets regularly** using scheduled jobs
5. **Use least-privilege** IAM/Azure roles
6. **Enable encryption in transit** (HTTPS for Vault)
7. **Monitor secret access** via health checks and logs

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## License

Apache License 2.0 - see [LICENSE](../LICENSE) for details.

## See Also

- [commons-adapters-secrets-vault](../commons-adapters-secrets-vault/README.md) - Vault adapter implementation
- [commons-adapters-secrets-aws-secrets-manager](../commons-adapters-secrets-aws-secrets-manager/README.md) - AWS adapter
- [commons-adapters-secrets-azure-keyvault](../commons-adapters-secrets-azure-keyvault/README.md) - Azure adapter
- [commons-ports-secrets](../commons-ports-secrets/README.md) - SecretStorePort interface
- [Spring Boot Starters](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters) - Official documentation
