package com.marcusprado02.commons.spring.secrets;

import com.marcusprado02.commons.adapters.secrets.aws.AwsSecretsManagerSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

/**
 * Auto-configuration for AWS Secrets Manager adapter.
 *
 * <p>Activated when:
 *
 * <ul>
 *   <li>{@code commons.secrets.type=aws}
 *   <li>{@code AwsSecretsManagerSecretStoreAdapter} is on classpath
 *   <li>{@code commons.secrets.aws.enabled=true} (default)
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(AwsSecretsManagerSecretStoreAdapter.class)
@ConditionalOnProperty(
    prefix = "commons.secrets.aws",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(SecretsProperties.class)
public class AwsSecretsAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(AwsSecretsAutoConfiguration.class);

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  public SecretsManagerClient secretsManagerClient(SecretsProperties properties) {
    SecretsProperties.Aws aws = properties.getAws();

    SecretsManagerClientBuilder builder =
        SecretsManagerClient.builder()
            .region(Region.of(aws.getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.create());

    if (aws.getEndpoint() != null && !aws.getEndpoint().isBlank()) {
      builder.endpointOverride(URI.create(aws.getEndpoint()));
      log.info(
          "Configured AWS Secrets Manager: region={}, endpoint={}",
          aws.getRegion(),
          aws.getEndpoint());
    } else {
      log.info("Configured AWS Secrets Manager: region={}", aws.getRegion());
    }

    return builder.build();
  }

  @Bean
  @ConditionalOnMissingBean(SecretStorePort.class)
  @ConditionalOnProperty(prefix = "commons.secrets", name = "type", havingValue = "aws")
  public SecretStorePort secretStorePort(
      SecretsManagerClient client, SecretsProperties properties) {
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    log.info("Created AWS Secrets Manager secret store adapter");

    return adapter;
  }
}
