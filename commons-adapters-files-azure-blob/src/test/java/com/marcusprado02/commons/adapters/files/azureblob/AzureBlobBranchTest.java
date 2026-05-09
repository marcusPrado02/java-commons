package com.marcusprado02.commons.adapters.files.azureblob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.adapters.files.azureblob.AzureBlobConfiguration.AuthenticationType;
import org.junit.jupiter.api.Test;

class AzureBlobBranchTest {

  // --- AzureBlobConfiguration compact constructor throw branches ---

  @Test
  void constructor_nullAuthenticationType_throws() {
    assertThatThrownBy(
            () -> new AzureBlobConfiguration(null, null, null, "conn", null, false, 3, 30))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("authenticationType");
  }

  @Test
  void constructor_negativeMaxRetries_throws() {
    assertThatThrownBy(
            () ->
                new AzureBlobConfiguration(
                    null, null, AuthenticationType.CONNECTION_STRING, "conn", null, false, -1, 30))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxRetries");
  }

  @Test
  void constructor_nonPositiveTimeoutSeconds_throws() {
    assertThatThrownBy(
            () ->
                new AzureBlobConfiguration(
                    null, null, AuthenticationType.CONNECTION_STRING, "conn", null, false, 3, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timeoutSeconds");
  }

  // --- AzureBlobClientFactory.createClient() SAS_TOKEN branch ---

  @Test
  void createClient_sasTokenConfig_hitsSwitch() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.withSasToken(
            "https://myaccount.blob.core.windows.net/", "sv=2020-08-04&sig=abc");
    assertThatCode(() -> AzureBlobClientFactory.createClient(config)).doesNotThrowAnyException();
  }

  // --- AzureBlobClientFactory.extractAccountName() branches ---

  @Test
  void extractAccountName_found_returnsName() {
    String conn =
        "DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=abc;EndpointSuffix=core.windows.net";
    String name = AzureBlobClientFactory.extractAccountName(conn);
    assertThat(name).isEqualTo("myaccount");
  }

  @Test
  void extractAccountName_notFound_throws() {
    assertThatThrownBy(() -> AzureBlobClientFactory.extractAccountName("no-account-here"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("AccountName");
  }

  // --- Builder.useManagedIdentity(false) branch ---

  @Test
  void builder_useManagedIdentityFalse_doesNotChangeAuthType() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.builder()
            .connectionString(
                "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=abc==;EndpointSuffix=core.windows.net")
            .useManagedIdentity(false)
            .build();
    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.CONNECTION_STRING);
    assertThat(config.useManagedIdentity()).isFalse();
  }
}
