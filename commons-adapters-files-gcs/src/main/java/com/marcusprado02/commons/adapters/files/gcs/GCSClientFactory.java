package com.marcusprado02.commons.adapters.files.gcs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.util.Objects;

/** Factory for creating GCS Storage clients. */
public final class GCSClientFactory {

  private GCSClientFactory() {
    // Utility class
  }

  /**
   * Create a Storage client from configuration.
   *
   * @param config GCS configuration
   * @return configured Storage client
   * @throws IOException if credentials cannot be loaded
   */
  public static Storage createStorage(GCSConfiguration config) throws IOException {
    Objects.requireNonNull(config, "config must not be null");

    var credentials = config.loadCredentials();
    var builder =
        StorageOptions.newBuilder().setProjectId(config.projectId()).setCredentials(credentials);

    // Use custom endpoint if configured (for testing with fake-gcs-server)
    if (config.hasCustomEndpoint()) {
      builder.setHost(config.endpoint());
    }

    return builder.build().getService();
  }
}
