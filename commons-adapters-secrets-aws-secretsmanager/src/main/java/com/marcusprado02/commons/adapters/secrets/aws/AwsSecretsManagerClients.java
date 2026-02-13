package com.marcusprado02.commons.adapters.secrets.aws;

import java.util.Objects;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public final class AwsSecretsManagerClients {

  private AwsSecretsManagerClients() {}

  /** Builds a SecretsManagerClient using the default AWS credential provider chain (incl. IAM roles). */
  public static SecretsManagerClient secretsManagerClient(Region region) {
    Objects.requireNonNull(region, "region cannot be null");

    return SecretsManagerClient.builder().region(region).build();
  }
}
