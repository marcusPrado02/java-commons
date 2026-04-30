package com.marcusprado02.commons.adapters.files.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3BranchTest {

  // --- S3Configuration compact constructor throw branches ---

  @Test
  void constructor_nonPositiveMultipartThreshold_throws() {
    assertThatThrownBy(() -> new S3Configuration("us-east-1", null, false, 0, 5 * 1024 * 1024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("multipartThresholdBytes");
  }

  @Test
  void constructor_nonPositiveMultipartChunkSize_throws() {
    assertThatThrownBy(() -> new S3Configuration("us-east-1", null, false, 5 * 1024 * 1024, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("multipartChunkSizeBytes");
  }

  // --- S3Configuration.localStack() method ---

  @Test
  void localStack_setsEndpointAndPathStyle() {
    S3Configuration config = S3Configuration.localStack("http://localhost:4566");
    assertThat(config.endpoint()).isNotNull();
    assertThat(config.pathStyleAccessEnabled()).isTrue();
    assertThat(config.region()).isEqualTo("us-east-1");
  }

  // --- S3ClientFactory.createClient() endpoint + pathStyle branches ---

  @Test
  void createClient_withEndpointAndPathStyle_hitsAllBranches() {
    S3Configuration config = S3Configuration.localStack("http://localhost:4566");
    S3Client client = S3ClientFactory.createClient(config, AnonymousCredentialsProvider.create());
    assertThat(client).isNotNull();
    client.close();
  }

  // --- S3ClientFactory.createPresigner() endpoint branch ---

  @Test
  void createPresigner_withEndpoint_hitsEndpointBranch() {
    S3Configuration config = S3Configuration.localStack("http://localhost:4566");
    S3Presigner presigner =
        S3ClientFactory.createPresigner(config, AnonymousCredentialsProvider.create());
    assertThat(presigner).isNotNull();
    presigner.close();
  }

  // --- S3ClientFactory.createClient() no-endpoint branch (defaults) ---

  @Test
  void createClient_withDefaults_noEndpointNoPathStyle() {
    S3Configuration config = S3Configuration.defaults("us-east-1");
    S3Client client = S3ClientFactory.createClient(config, AnonymousCredentialsProvider.create());
    assertThat(client).isNotNull();
    client.close();
  }

  // --- S3ClientFactory.createAdapter() convenience method ---

  @Test
  void createAdapter_withCredentials_returnsAdapter() {
    S3Configuration config = S3Configuration.localStack("http://localhost:4566");
    S3FileStoreAdapter adapter =
        S3ClientFactory.createAdapter(config, AnonymousCredentialsProvider.create());
    assertThat(adapter).isNotNull();
  }
}
