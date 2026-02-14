package com.marcusprado02.commons.spring.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.adapters.secrets.aws.AwsSecretsManagerSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Testcontainers
class AwsSecretsAutoConfigurationTest {

  @Container
  private static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
          .withServices(LocalStackContainer.Service.SECRETSMANAGER);

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(AwsSecretsAutoConfiguration.class));

  @Test
  void shouldAutoConfigureAwsSecrets() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=aws",
            "commons.secrets.aws.region=us-east-1",
            "commons.secrets.aws.endpoint=" + localstack.getEndpoint())
        .run(
            context -> {
              assertThat(context).hasSingleBean(SecretsManagerClient.class);
              assertThat(context).hasSingleBean(SecretStorePort.class);

              SecretStorePort secretStore = context.getBean(SecretStorePort.class);
              assertThat(secretStore).isInstanceOf(AwsSecretsManagerSecretStoreAdapter.class);

              // Test secret operations
              SecretKey key = SecretKey.of("test-key");
              SecretValue value = SecretValue.of("test-value");
              secretStore.put(key, value);
              assertThat(secretStore.get(key).map(SecretValue::asString)).hasValue("test-value");
            });
  }

  @Test
  void shouldNotAutoConfigureWhenDisabled() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.aws.enabled=false",
            "commons.secrets.aws.region=us-east-1",
            "commons.secrets.aws.endpoint=" + localstack.getEndpoint())
        .run(context -> assertThat(context).doesNotHaveBean(SecretStorePort.class));
  }

  @Test
  void shouldNotAutoConfigureWhenTypeIsNotAws() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=vault",
            "commons.secrets.aws.region=us-east-1",
            "commons.secrets.aws.endpoint=" + localstack.getEndpoint())
        .run(context -> assertThat(context).doesNotHaveBean(SecretStorePort.class));
  }
}
