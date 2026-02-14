# Commons Adapters - Azure Key Vault Secret Store

Azure Key Vault implementation of `SecretStorePort` for secure secret management in Azure cloud environments.

## Features

- ✅ **Managed Identity Support**: Authenticate without storing credentials in code
- ✅ **Secret Versioning**: Track and retrieve different versions of secrets
- ✅ **Automatic Name Normalization**: Converts path-like names to Azure-compliant format
- ✅ **Expiration Support**: Set and track secret expiration dates
- ✅ **Tags & Metadata**: Store key-value pairs as tags (up to 15)
- ✅ **Soft Delete & Recovery**: Protect against accidental deletion
- ✅ **Secure Memory Handling**: Auto-closeable SecretValue with memory zeroing

## Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-secrets-azure-keyvault</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Quick Start

### With Managed Identity (Recommended for Azure)

```java
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.marcusprado02.commons.adapters.secrets.azure.AzureKeyVaultSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.*;

// Create SecretClient with Managed Identity
String vaultUrl = "https://my-keyvault.vault.azure.net/";

SecretClient secretClient = new SecretClientBuilder()
    .vaultUrl(vaultUrl)
    .credential(new DefaultAzureCredentialBuilder().build())
    .buildClient();

// Create adapter
SecretStorePort secretStore = new AzureKeyVaultSecretStoreAdapter(secretClient);

// Optional: add in-memory cache with TTL and optional background refresh
// (refreshInterval = null disables background refresh)
try (CachedSecretStorePort cached =
        new CachedSecretStorePort(secretStore, Duration.ofSeconds(30))) {
    // use cached as SecretStorePort
}

// Store a secret
SecretKey apiKey = SecretKey.of("stripe/api-key");
SecretValue value = SecretValue.of("sk_live_...");
String version = secretStore.put(apiKey, value);

// Retrieve secret
try (SecretValue retrieved = secretStore.get(apiKey).orElseThrow()) {
    String apiKeyValue = retrieved.asString();
    // Use the secret
}
```

### Storing Structured Secrets

```java
// Database credentials as a map
Map<String, String> dbConfig = Map.of(
    "host", "mydb.database.azure.com",
    "port", "5432",
    "database", "myapp",
    "username", "admin"
);

SecretKey dbKey = SecretKey.of("database/credentials");
String version = secretStore.put(dbKey, dbConfig);

// The map is serialized and stored as "{host=..., port=..., database=..., username=...}"
// Tags are automatically added for each key-value pair (max 15 items)
```

### Versioning

```java
// Store multiple versions
SecretKey key = SecretKey.of("api-token");
String v1 = secretStore.put(key, SecretValue.of("old-token"));
String v2 = secretStore.put(key, SecretValue.of("new-token"));

// Get latest version
SecretValue latest = secretStore.get(key).orElseThrow();

// Get specific version
SecretValue oldVersion = secretStore.get(key, v1).orElseThrow();
```

### Secret Expiration

```java
Instant expiresAt = Instant.now().plus(Duration.ofDays(30));
SecretValue tempSecret = SecretValue.of(
    "temporary-key".getBytes(),
    null, // version (auto-generated)
    Instant.now(), // created at
    expiresAt
);

secretStore.put(SecretKey.of("temp/api-key"), tempSecret);

// Later, check if expired
try (SecretValue retrieved = secretStore.get(SecretKey.of("temp/api-key")).orElseThrow()) {
    if (retrieved.isExpired()) {
        // Rotate secret
    }
}
```

## Azure Setup

### 1. Create Key Vault

```bash
# Create resource group
az group create --name myapp-rg --location eastus

# Create Key Vault
az keyvault create \
    --name myapp-keyvault \
    --resource-group myapp-rg \
    --location eastus
```

### 2. Configure Managed Identity

#### System-Assigned Identity (recommended for single app)

```bash
# Enable System-Assigned Identity on App Service
az webapp identity assign \
    --name myapp \
    --resource-group myapp-rg

# Get the object ID
IDENTITY_OBJECT_ID=$(az webapp identity show \
    --name myapp \
    --resource-group myapp-rg \
    --query principalId \
    --output tsv)

# Grant Key Vault access
az keyvault set-policy \
    --name myapp-keyvault \
    --resource-group myapp-rg \
    --object-id $IDENTITY_OBJECT_ID \
    --secret-permissions get list set delete
```

#### User-Assigned Identity (recommended for multiple apps)

```bash
# Create User-Assigned Identity
az identity create \
    --name myapp-identity \
    --resource-group myapp-rg

# Get client ID and object ID
CLIENT_ID=$(az identity show \
    --name myapp-identity \
    --resource-group myapp-rg \
    --query clientId \
    --output tsv)

OBJECT_ID=$(az identity show \
    --name myapp-identity \
    --resource-group myapp-rg \
    --query principalId \
    --output tsv)

# Assign to App Service
az webapp identity assign \
    --name myapp \
    --resource-group myapp-rg \
    --identities /subscriptions/{sub-id}/resourcegroups/myapp-rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/myapp-identity

# Grant Key Vault access
az keyvault set-policy \
    --name myapp-keyvault \
    --resource-group myapp-rg \
    --object-id $OBJECT_ID \
    --secret-permissions get list set delete
```

### 3. RBAC Alternative (Recommended)

Instead of access policies, use Azure RBAC for finer-grained control:

```bash
# Enable RBAC on Key Vault
az keyvault update \
    --name myapp-keyvault \
    --resource-group myapp-rg \
    --enable-rbac-authorization true

# Assign Key Vault Secrets User role (read-only)
az role assignment create \
    --assignee $IDENTITY_OBJECT_ID \
    --role "Key Vault Secrets User" \
    --scope /subscriptions/{subscription-id}/resourceGroups/myapp-rg/providers/Microsoft.KeyVault/vaults/myapp-keyvault

# Or assign Key Vault Secrets Officer (read-write-delete)
az role assignment create \
    --assignee $IDENTITY_OBJECT_ID \
    --role "Key Vault Secrets Officer" \
    --scope /subscriptions/{subscription-id}/resourceGroups/myapp-rg/providers/Microsoft.KeyVault/vaults/myapp-keyvault
```

## Local Development

For local development without Managed Identity, use Azure CLI authentication:

```bash
# Login with Azure CLI
az login

# Set subscription
az account set --subscription "My Subscription"
```

The `DefaultAzureCredential` will automatically use Azure CLI credentials when running locally.

Alternative: Use environment variables for Service Principal:

```bash
export AZURE_CLIENT_ID="..."
export AZURE_TENANT_ID="..."
export AZURE_CLIENT_SECRET="..."
```

## Secret Name Normalization

Azure Key Vault has strict naming requirements. The adapter automatically normalizes secret names:

- **Allowed**: Alphanumeric and hyphens only
- **Max length**: 127 characters
- **Cannot start with**: Numbers

```java
// These are automatically normalized:
SecretKey.of("config/database")      → "config-database"
SecretKey.of("123-secret")           → "s-123-secret"
SecretKey.of("my_secret_key")        → "my-secret-key"
SecretKey.of("UPPER_CASE")           → "UPPER-CASE"
```

## Certificate Support

While this adapter focuses on secrets, Azure Key Vault also supports certificates.

This module includes a small helper to build SDK clients with Managed Identity:

```java
SecretClient secretClient = AzureKeyVaultClients.secretClient(vaultUrl);
CertificateClient certificateClient = AzureKeyVaultClients.certificateClient(vaultUrl);
```

## When to Use

### Use Azure Key Vault Adapter When:

- ✅ Running on Azure (App Service, AKS, Azure Functions, Azure VMs)
- ✅ Need Managed Identity authentication (no credentials in code)
- ✅ Require HSM-backed secrets for compliance
- ✅ Want Azure-native integration (Azure Monitor, RBAC, etc.)
- ✅ Need certificate lifecycle management

### Use Vault Adapter When:

- ✅ Running on-premises or multi-cloud
- ✅ Need dynamic secret generation
- ✅ Require advanced secret engines (database, PKI, SSH)
- ✅ Want more complex access control policies
- ✅ Need secret leasing and renewal

## Comparison: Azure Key Vault vs HashiCorp Vault

| Feature | Azure Key Vault | HashiCorp Vault |
|---------|----------------|-----------------|
| **Authentication** | Managed Identity | Token/AppRole |
| **Versioning** | Automatic | Manual (KV v2) |
| **Dynamic Secrets** | No | Yes |
| **Secret Engines** | Secrets, Keys, Certs | 20+ engines |
| **HSM Support** | Yes (Premium tier) | Yes (Enterprise) |
| **Secret Leasing** | No | Yes |
| **Cost Model** | Per-operation | Cluster-based |
| **Hosting** | Azure-only | Any infrastructure |

## Spring Boot Integration

```java
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.marcusprado02.commons.adapters.secrets.azure.AzureKeyVaultSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureKeyVaultConfig {

    @Bean
    public SecretClient azureSecretClient(@Value("${azure.keyvault.vault-url}") String vaultUrl) {
        return new SecretClientBuilder()
            .vaultUrl(vaultUrl)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }

    @Bean
    public SecretStorePort secretStorePort(SecretClient secretClient) {
        return new AzureKeyVaultSecretStoreAdapter(secretClient);
    }
}
```

`application.yml`:

```yaml
azure:
  keyvault:
    vault-url: https://myapp-keyvault.vault.azure.net/
```

## Best Practices

### 1. Use Managed Identity

Always prefer Managed Identity over Service Principal credentials:

```java
// ✅ Good - Managed Identity
new DefaultAzureCredentialBuilder().build()

// ❌ Avoid - Client Secret in code
new ClientSecretCredentialBuilder()
    .clientId("...")
    .clientSecret("...") // Secret in code!
    .tenantId("...")
    .build()
```

### 2. Implement Secret Rotation

```java
public void rotateApiKey() {
    SecretKey key = SecretKey.of("api-key");

    // Generate new secret
    String newSecret = generateNewApiKey();

    // Store new version
    String newVersion = secretStore.put(key, SecretValue.of(newSecret));

    // Old versions remain accessible for graceful migration
    log.info("Rotated secret to version {}", newVersion);
}
```

### 3. Use Try-With-Resources

Always use try-with-resources to ensure memory is zeroed:

```java
// ✅ Good - Memory is automatically zeroed
try (SecretValue secret = secretStore.get(key).orElseThrow()) {
    String value = secret.asString();
    // Use secret
}
// Secret data is now zeroed in memory

// ❌ Avoid - Secret remains in memory
SecretValue secret = secretStore.get(key).orElseThrow();
String value = secret.asString();
// Secret data still in memory!
```

### 4. Separate Secrets by Environment

```java
String environment = System.getenv("ENVIRONMENT"); // dev, staging, prod

SecretKey dbPassword = SecretKey.of(environment + "/database/password");
SecretValue password = secretStore.get(dbPassword).orElseThrow();
```

### 5. Monitor Secret Access

Enable Azure Monitor diagnostic settings:

```bash
az monitor diagnostic-settings create \
    --name keyvault-logs \
    --resource /subscriptions/{sub-id}/resourceGroups/myapp-rg/providers/Microsoft.KeyVault/vaults/myapp-keyvault \
    --logs '[{"category": "AuditEvent", "enabled": true}]' \
    --workspace /subscriptions/{sub-id}/resourceGroups/myapp-rg/providers/Microsoft.OperationalInsights/workspaces/myapp-logs
```

## Troubleshooting

### Error: "Caller is not authorized"

**Cause**: Managed Identity doesn't have Key Vault access

**Solution**: Grant access policy or RBAC role:

```bash
az keyvault set-policy \
    --name myapp-keyvault \
    --object-id $IDENTITY_OBJECT_ID \
    --secret-permissions get list
```

### Error: "Secret not found"

**Cause**: Secret name was normalized differently

**Solution**: List secrets to see actual names:

```java
List<SecretKey> allSecrets = secretStore.list(null);
allSecrets.forEach(k -> System.out.println(k.value()));
```

### Error: "Request failed with 429"

**Cause**: Rate limiting (too many requests)

**Solution**: Implement exponential backoff or cache secrets locally:

```java
import java.time.Duration;

RetryOptions retryOptions = new RetryOptions(new ExponentialBackoff(
    3, // max retries
    Duration.ofSeconds(1),
    Duration.ofSeconds(10)
));

SecretClient client = new SecretClientBuilder()
    .vaultUrl(vaultUrl)
    .credential(credential)
    .retryOptions(retryOptions)
    .buildClient();
```

### Local Development: "ERROR AADSTS700016"

**Cause**: Azure CLI not logged in

**Solution**: Login with Azure CLI:

```bash
az login
az account set --subscription "My Subscription"
```

## Security Considerations

### Private Endpoint

For production, use Private Endpoint to keep traffic within Azure VNet:

```bash
az network vnet create \
    --resource-group myapp-rg \
    --name myapp-vnet \
    --subnet-name subnet1

az network private-endpoint create \
    --name myapp-keyvault-pe \
    --resource-group myapp-rg \
    --vnet-name myapp-vnet \
    --subnet subnet1 \
    --private-connection-resource-id /subscriptions/{sub-id}/resourceGroups/myapp-rg/providers/Microsoft.KeyVault/vaults/myapp-keyvault \
    --group-id vault \
    --connection-name myapp-keyvault-pc
```

### Firewall Rules

Restrict access to specific IP addresses:

```bash
az keyvault network-rule add \
    --name myapp-keyvault \
    --ip-address 203.0.113.10

az keyvault update \
    --name myapp-keyvault \
    --default-action Deny
```

### Soft Delete & Purge Protection

Enable soft delete and purge protection for production:

```bash
az keyvault update \
    --name myapp-keyvault \
    --enable-soft-delete true \
    --enable-purge-protection true \
    --retention-days 90
```

## License

This project is licensed under the same terms as the commons-parent project.
