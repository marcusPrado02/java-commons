package com.marcusprado02.commons.spring.secrets;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Commons Secrets.
 *
 * <p>Properties prefix: {@code commons.secrets}
 */
@ConfigurationProperties(prefix = "commons.secrets")
public class SecretsProperties {

  /** Secrets provider type: vault, aws, azure, or none. */
  private SecretsType type = SecretsType.VAULT;

  /** Vault-specific configuration. */
  private final Vault vault = new Vault();

  /** AWS Secrets Manager configuration. */
  private final Aws aws = new Aws();

  /** Azure Key Vault configuration. */
  private final Azure azure = new Azure();

  /** Cache configuration for secrets. */
  private final Cache cache = new Cache();

  public SecretsType getType() {
    return type;
  }

  public void setType(SecretsType type) {
    this.type = type;
  }

  public Vault getVault() {
    return vault;
  }

  public Aws getAws() {
    return aws;
  }

  public Azure getAzure() {
    return azure;
  }

  public Cache getCache() {
    return cache;
  }

  public enum SecretsType {
    VAULT,
    AWS,
    AZURE,
    NONE
  }

  /** HashiCorp Vault configuration. */
  public static class Vault {

    /** Vault server URI. */
    private String uri = "http://localhost:8200";

    /** Authentication token. */
    private String token;

    /** KV secrets engine mount path. */
    private String kvPath = "secret";

    /** KV secrets engine version (1 or 2). */
    private int kvVersion = 2;

    /** Enable Vault secrets. */
    private boolean enabled = true;

    /** Connection timeout. */
    private Duration timeout = Duration.ofSeconds(5);

    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getKvPath() {
      return kvPath;
    }

    public void setKvPath(String kvPath) {
      this.kvPath = kvPath;
    }

    public int getKvVersion() {
      return kvVersion;
    }

    public void setKvVersion(int kvVersion) {
      this.kvVersion = kvVersion;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }

  /** AWS Secrets Manager configuration. */
  public static class Aws {

    /** AWS region. */
    private String region = "us-east-1";

    /** AWS endpoint override (for LocalStack). */
    private String endpoint;

    /** Enable AWS Secrets Manager. */
    private boolean enabled = true;

    public String getRegion() {
      return region;
    }

    public void setRegion(String region) {
      this.region = region;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  /** Azure Key Vault configuration. */
  public static class Azure {

    /** Key Vault URL. */
    private String vaultUrl;

    /** Enable Azure Key Vault. */
    private boolean enabled = true;

    public String getVaultUrl() {
      return vaultUrl;
    }

    public void setVaultUrl(String vaultUrl) {
      this.vaultUrl = vaultUrl;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  /** Cache configuration for secrets. */
  public static class Cache {

    /** Enable caching of secrets. */
    private boolean enabled = true;

    /** Cache TTL. */
    private Duration ttl = Duration.ofMinutes(5);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      this.ttl = ttl;
    }
  }
}
