package com.marcusprado02.commons.adapters.search.opensearch;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.ports.search.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OpenSearchAdapterTest {

  // -------------------------------------------------------------------------
  // Configuration tests (no real connection required)
  // -------------------------------------------------------------------------

  @Test
  void shouldBuildDevelopmentConfiguration() {
    var config = OpenSearchConfiguration.forDevelopment();

    assertThat(config.urls()).containsExactly("http://localhost:9200");
    assertThat(config.hasBasicAuth()).isFalse();
    assertThat(config.hasApiKey()).isFalse();
    assertThat(config.enableSsl()).isFalse();
  }

  @Test
  void shouldBuildProductionConfigurationWithAuth() {
    var config =
        OpenSearchConfiguration.forProduction()
            .addUrl("https://opensearch.example.com:9200")
            .username("admin")
            .password("secret")
            .build();

    assertThat(config.urls()).containsExactly("https://opensearch.example.com:9200");
    assertThat(config.hasBasicAuth()).isTrue();
    assertThat(config.enableSsl()).isTrue();
    assertThat(config.connectionTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.socketTimeout()).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void shouldBuildConfigurationWithApiKey() {
    var config =
        OpenSearchConfiguration.withApiKey(
            "https://opensearch.example.com:9200", "key-id", "key-secret");

    assertThat(config.hasApiKey()).isTrue();
    assertThat(config.hasBasicAuth()).isFalse();
    assertThat(config.apiKeyId()).isEqualTo("key-id");
    assertThat(config.apiKeySecret()).isEqualTo("key-secret");
  }

  @Test
  void shouldRejectBothBasicAuthAndApiKey() {
    assertThatThrownBy(
            () ->
                OpenSearchConfiguration.builder()
                    .addUrl("http://localhost:9200")
                    .username("user")
                    .password("pass")
                    .apiKeyId("id")
                    .apiKeySecret("secret")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot use both basic auth and API key");
  }

  @Test
  void shouldRejectEmptyUrlList() {
    assertThatThrownBy(() -> OpenSearchConfiguration.builder().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("At least one URL must be provided");
  }

  @Test
  void shouldRejectInvalidMaxConnections() {
    assertThatThrownBy(
            () ->
                OpenSearchConfiguration.builder()
                    .addUrl("http://localhost:9200")
                    .maxConnections(0)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Max connections must be between 1 and 1000");
  }

  @Test
  void shouldRejectNonPositiveConnectionTimeout() {
    assertThatThrownBy(
            () ->
                OpenSearchConfiguration.builder()
                    .addUrl("http://localhost:9200")
                    .connectionTimeout(Duration.ZERO)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Connection timeout must be positive");
  }

  @Test
  void shouldSupportMultipleUrls() {
    var config =
        OpenSearchConfiguration.builder()
            .addUrl("https://node1.example.com:9200")
            .addUrl("https://node2.example.com:9200")
            .addUrl("https://node3.example.com:9200")
            .build();

    assertThat(config.urls()).hasSize(3);
  }

  @Test
  void shouldInstantiateAdapter() {
    var config = OpenSearchConfiguration.forDevelopment();

    // Just verifying constructor doesn't throw — real connection is established lazily
    var adapter = new OpenSearchAdapter(config);
    assertThat(adapter).isNotNull().isInstanceOf(SearchPort.class);
    adapter.close();
  }

  @Test
  void shouldRejectNullConfiguration() {
    assertThatThrownBy(() -> new OpenSearchAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Configuration cannot be null");
  }

  @Test
  void shouldImplementSearchPort() {
    var config = OpenSearchConfiguration.forDevelopment();
    SearchPort port = new OpenSearchAdapter(config);
    assertThat(port).isNotNull();
    port.close();
  }
}
