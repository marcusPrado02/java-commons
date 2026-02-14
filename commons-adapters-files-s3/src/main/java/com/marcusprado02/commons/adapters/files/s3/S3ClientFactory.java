package com.marcusprado02.commons.adapters.files.s3;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Objects;

/**
 * Factory for creating S3 clients and adapters.
 */
public final class S3ClientFactory {

  private S3ClientFactory() {
  }

  /**
   * Create an S3 client with the given configuration.
   */
  public static S3Client createClient(S3Configuration config) {
    return createClient(config, DefaultCredentialsProvider.create());
  }

  /**
   * Create an S3 client with custom credentials provider.
   */
  public static S3Client createClient(S3Configuration config, AwsCredentialsProvider credentialsProvider) {
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(credentialsProvider, "credentialsProvider must not be null");

    S3ClientBuilder builder = S3Client.builder()
        .region(Region.of(config.region()))
        .credentialsProvider(credentialsProvider);

    if (config.endpoint() != null) {
      builder.endpointOverride(config.endpoint());
    }

    if (config.pathStyleAccessEnabled()) {
      builder.forcePathStyle(true);
    }

    return builder.build();
  }

  /**
   * Create an S3 presigner with the given configuration.
   */
  public static S3Presigner createPresigner(S3Configuration config) {
    return createPresigner(config, DefaultCredentialsProvider.create());
  }

  /**
   * Create an S3 presigner with custom credentials provider.
   */
  public static S3Presigner createPresigner(S3Configuration config, AwsCredentialsProvider credentialsProvider) {
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(credentialsProvider, "credentialsProvider must not be null");

    S3Presigner.Builder builder = S3Presigner.builder()
        .region(Region.of(config.region()))
        .credentialsProvider(credentialsProvider);

    if (config.endpoint() != null) {
      builder.endpointOverride(config.endpoint());
    }

    return builder.build();
  }

  /**
   * Create a fully configured S3FileStoreAdapter.
   */
  public static S3FileStoreAdapter createAdapter(S3Configuration config) {
    return createAdapter(config, DefaultCredentialsProvider.create());
  }

  /**
   * Create a fully configured S3FileStoreAdapter with custom credentials provider.
   */
  public static S3FileStoreAdapter createAdapter(S3Configuration config, AwsCredentialsProvider credentialsProvider) {
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(credentialsProvider, "credentialsProvider must not be null");

    S3Client client = createClient(config, credentialsProvider);
    S3Presigner presigner = createPresigner(config, credentialsProvider);

    return new S3FileStoreAdapter(client, presigner, config);
  }
}
