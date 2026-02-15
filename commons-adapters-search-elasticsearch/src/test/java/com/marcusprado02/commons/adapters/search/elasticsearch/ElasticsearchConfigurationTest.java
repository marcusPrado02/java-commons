package com.marcusprado02.commons.adapters.search.elasticsearch;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElasticsearchConfigurationTest {

  @Test
  void shouldCreateMinimalConfiguration() {
    ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
        .serverUrl("http://localhost:9200")
        .username("elastic")
        .password("changeme")
        .build();

    assertEquals(1, config.serverUrls().size());
    assertEquals("http://localhost:9200", config.serverUrls().get(0));
    assertEquals("elastic", config.username());
    assertEquals("changeme", config.password());
    assertNull(config.apiKey());
    assertEquals(Duration.ofSeconds(15), config.connectionTimeout());
    assertEquals(Duration.ofSeconds(30), config.socketTimeout());
    assertEquals(20, config.maxConnections());
    assertTrue(config.enableSsl());
    assertTrue(config.verifySslCertificates());
  }

  @Test
  void shouldCreateClusterConfiguration() {
    List<String> urls = List.of(
        "https://node1:9200",
        "https://node2:9200",
        "https://node3:9200"
    );

    ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
        .serverUrls(urls)
        .username("elastic")
        .password("secret")
        .build();

    assertEquals(3, config.serverUrls().size());
    assertTrue(config.serverUrls().containsAll(urls));
  }

  @Test
  void shouldCreateDevelopmentConfiguration() {
    ElasticsearchConfiguration config = ElasticsearchConfiguration.forDevelopment(
        "http://localhost:9200",
        "elastic",
        "changeme"
    );

    assertEquals("http://localhost:9200", config.serverUrls().get(0));
    assertEquals("elastic", config.username());
    assertEquals("changeme", config.password());
    assertEquals(Duration.ofSeconds(30), config.connectionTimeout());
    assertEquals(Duration.ofSeconds(60), config.socketTimeout());
    assertEquals(10, config.maxConnections());
    assertFalse(config.enableSsl());
    assertFalse(config.verifySslCertificates());
  }

  @Test
  void shouldCreateProductionConfiguration() {
    List<String> urls = List.of("https://es1:9200", "https://es2:9200");

    ElasticsearchConfiguration config = ElasticsearchConfiguration.forProduction(
        urls,
        "elastic",
        "secretpassword"
    );

    assertEquals(2, config.serverUrls().size());
    assertEquals("elastic", config.username());
    assertEquals("secretpassword", config.password());
    assertEquals(Duration.ofSeconds(10), config.connectionTimeout());
    assertEquals(Duration.ofSeconds(30), config.socketTimeout());
    assertEquals(50, config.maxConnections());
    assertTrue(config.enableSsl());
    assertTrue(config.verifySslCertificates());
  }

  @Test
  void shouldCreateApiKeyConfiguration() {
    ElasticsearchConfiguration config = ElasticsearchConfiguration.withApiKey(
        "https://cloud.elastic.co:9200",
        "VnVhQ2ZHY0JDZGJrUW0tZTVhT3g6dWkybHAyYXhUTm1zeWFrdzl0dk5udw=="
    );

    assertEquals("https://cloud.elastic.co:9200", config.serverUrls().get(0));
    assertEquals("VnVhQ2ZHY0JDZGJrUW0tZTVhT3g6dWkybHAyYXhUTm1zeWFrdzl0dk5udw==", config.apiKey());
    assertNull(config.username());
    assertNull(config.password());
    assertTrue(config.enableSsl());
    assertTrue(config.verifySslCertificates());
  }

  @Test
  void shouldFailWhenNoServerUrls() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .username("elastic")
            .password("changeme")
            .build()
    );
    assertEquals("At least one server URL must be provided", exception.getMessage());
  }

  @Test
  void shouldFailWhenServerUrlIsBlank() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("   ")
            .username("elastic")
            .password("changeme")
            .build()
    );
    assertEquals("Server URL cannot be blank", exception.getMessage());
  }

  @Test
  void shouldFailWhenServerUrlInvalid() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("localhost:9200") // Missing http://
            .username("elastic")
            .password("changeme")
            .build()
    );
    assertEquals("Server URL must start with http:// or https://", exception.getMessage());
  }

  @Test
  void shouldFailWhenNoAuthentication() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .build()
    );
    assertEquals("Either username/password or apiKey must be provided for authentication",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUsernameWithoutPassword() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .build()
    );
    assertTrue(exception.getMessage().contains("authentication"));
  }

  @Test
  void shouldFailWhenConnectionTimeoutZero() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .password("changeme")
            .connectionTimeout(Duration.ZERO)
            .build()
    );
    assertEquals("Connection timeout must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenConnectionTimeoutNegative() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .password("changeme")
            .connectionTimeout(Duration.ofSeconds(-1))
            .build()
    );
    assertEquals("Connection timeout must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenSocketTimeoutZero() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .password("changeme")
            .socketTimeout(Duration.ZERO)
            .build()
    );
    assertEquals("Socket timeout must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenMaxConnectionsTooLow() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .password("changeme")
            .maxConnections(0)
            .build()
    );
    assertEquals("Max connections must be at least 1", exception.getMessage());
  }

  @Test
  void shouldFailWhenMaxConnectionsTooHigh() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .password("changeme")
            .maxConnections(1001)
            .build()
    );
    assertEquals("Max connections cannot exceed 1000", exception.getMessage());
  }

  @Test
  void shouldAllowMaxConnectionsAtLimit() {
    assertDoesNotThrow(() ->
        ElasticsearchConfiguration.builder()
            .serverUrl("http://localhost:9200")
            .username("elastic")
            .password("changeme")
            .maxConnections(1000)
            .build()
    );
  }

  @Test
  void shouldAllowApiKeyAuthWithoutUsernamePassword() {
    ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
        .serverUrl("https://localhost:9200")
        .apiKey("test-api-key")
        .build();

    assertEquals("test-api-key", config.apiKey());
    assertNull(config.username());
    assertNull(config.password());
  }

  @Test
  void shouldConfigureSslSettings() {
    ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
        .serverUrl("https://localhost:9200")
        .username("elastic")
        .password("changeme")
        .enableSsl(false)
        .verifySslCertificates(false)
        .build();

    assertFalse(config.enableSsl());
    assertFalse(config.verifySslCertificates());
  }
}
