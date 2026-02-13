# Commons Adapters - AWS Secrets Manager Secret Store

AWS Secrets Manager implementation of `SecretStorePort` for secure secret management on AWS.

## Features

- ✅ **IAM roles / Default credential chain**: Works with ECS/EKS/EC2 roles and local AWS profiles
- ✅ **Secret versioning**: Returns `versionId` on writes and exposes it on reads
- ✅ **Rotation-friendly reads**: Supports `AWSCURRENT`, `AWSPREVIOUS`, `AWSPENDING` via `get(key, version)`
- ✅ **Binary and string secrets**: Reads from `secretString` and `secretBinary`
- ✅ **Optional in-memory cache**: TTL cache decorator with optional background refresh

## Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-secrets-aws-secretsmanager</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Quick Start

```java
import com.marcusprado02.commons.adapters.secrets.aws.AwsSecretsManagerClients;
import com.marcusprado02.commons.adapters.secrets.aws.AwsSecretsManagerSecretStoreAdapter;
import com.marcusprado02.commons.adapters.secrets.aws.CachedSecretStorePort;
import com.marcusprado02.commons.ports.secrets.*;
import java.time.Duration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

SecretsManagerClient client = AwsSecretsManagerClients.secretsManagerClient(Region.US_EAST_1);
SecretStorePort store = new AwsSecretsManagerSecretStoreAdapter(client);

// Optional: TTL cache (refreshInterval = null disables background refresh)
try (CachedSecretStorePort cached = new CachedSecretStorePort(store, Duration.ofSeconds(30))) {
  SecretKey key = SecretKey.of("myapp/api-key");
  store.put(key, SecretValue.of("secret-value"));

  try (SecretValue value = cached.get(key).orElseThrow()) {
    String s = value.asString();
  }
}
```

## Versioning and Rotation

### Write returns `versionId`

```java
SecretKey key = SecretKey.of("myapp/token");
String v1 = store.put(key, SecretValue.of("old"));
String v2 = store.put(key, SecretValue.of("new"));

// Fetch a specific version by versionId
SecretValue old = store.get(key, v1).orElseThrow();
```

### Read by rotation stage

AWS rotations typically label versions with stages like `AWSCURRENT` and `AWSPREVIOUS`.
This adapter accepts these stage labels in `get(key, version)`:

```java
SecretKey key = SecretKey.of("myapp/token");

// Current active version
SecretValue current = store.get(key, "AWSCURRENT").orElseThrow();

// Previous version
SecretValue previous = store.get(key, "AWSPREVIOUS").orElseThrow();

// Pending (during rotation)
SecretValue pending = store.get(key, "AWSPENDING").orElseThrow();
```

## Credentials (IAM roles)

`AwsSecretsManagerClients.secretsManagerClient(region)` uses the AWS SDK v2 default credential provider chain.
In AWS environments, that typically means IAM roles (EC2 instance profile, ECS task role, EKS IRSA).
Locally, it typically uses `~/.aws/credentials` and `AWS_PROFILE`.
