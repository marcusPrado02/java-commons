package com.marcusprado02.commons.adapters.email.sendgrid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendGridConfigurationTest {

  @Test
  @DisplayName("Should create configuration with all fields")
  void shouldCreateConfigurationWithAllFields() {
    // When
    SendGridConfiguration config = SendGridConfiguration.builder()
        .apiKey("SG.test-key-123")
        .requestTimeout(Duration.ofSeconds(30))
        .defaultFromEmail("noreply@example.com")
        .defaultFromName("My App")
        .trackClicks(true)
        .trackOpens(false)
        .sandboxMode(true)
        .build();

    // Then
    assertThat(config.apiKey()).isEqualTo("SG.test-key-123");
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.defaultFromEmail()).isEqualTo("noreply@example.com");
    assertThat(config.defaultFromName()).isEqualTo("My App");
    assertThat(config.trackClicks()).isTrue();
    assertThat(config.trackOpens()).isFalse();
    assertThat(config.sandboxMode()).isTrue();
  }

  @Test
  @DisplayName("Should create configuration with defaults")
  void shouldCreateConfigurationWithDefaults() {
    // When
    SendGridConfiguration config = SendGridConfiguration.builder()
        .apiKey("SG.test-key-123")
        .build();

    // Then
    assertThat(config.apiKey()).isEqualTo("SG.test-key-123");
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.defaultFromEmail()).isNull();
    assertThat(config.defaultFromName()).isNull();
    assertThat(config.trackClicks()).isTrue();
    assertThat(config.trackOpens()).isTrue();
    assertThat(config.sandboxMode()).isFalse();
  }

  @Test
  @DisplayName("Should create testing configuration")
  void shouldCreateTestingConfiguration() {
    // When
    SendGridConfiguration config = SendGridConfiguration.forTesting("SG.test-key");

    // Then
    assertThat(config.apiKey()).isEqualTo("SG.test-key");
    assertThat(config.sandboxMode()).isTrue();
    assertThat(config.trackClicks()).isTrue();
    assertThat(config.trackOpens()).isTrue();
  }

  @Test
  @DisplayName("Should create production configuration")
  void shouldCreateProductionConfiguration() {
    // When
    SendGridConfiguration config = SendGridConfiguration.forProduction(
        "SG.prod-key",
        "noreply@company.com",
        "Company Name");

    // Then
    assertThat(config.apiKey()).isEqualTo("SG.prod-key");
    assertThat(config.defaultFromEmail()).isEqualTo("noreply@company.com");
    assertThat(config.defaultFromName()).isEqualTo("Company Name");
    assertThat(config.sandboxMode()).isFalse();
    assertThat(config.trackClicks()).isTrue();
    assertThat(config.trackOpens()).isTrue();
  }

  @Test
  @DisplayName("Should reject null API key")
  void shouldRejectNullApiKey() {
    // When/Then
    assertThatThrownBy(() -> SendGridConfiguration.builder()
        .apiKey(null)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("API key cannot be null");
  }

  @Test
  @DisplayName("Should reject blank API key")
  void shouldRejectBlankApiKey() {
    // When/Then
    assertThatThrownBy(() -> SendGridConfiguration.builder()
        .apiKey("   ")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("API key cannot be null or blank");
  }

  @Test
  @DisplayName("Should reject null timeout")
  void shouldRejectNullTimeout() {
    // When/Then
    assertThatThrownBy(() -> SendGridConfiguration.builder()
        .apiKey("SG.test")
        .requestTimeout(null)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Request timeout cannot be null");
  }

  @Test
  @DisplayName("Should reject negative timeout")
  void shouldRejectNegativeTimeout() {
    // When/Then
    assertThatThrownBy(() -> SendGridConfiguration.builder()
        .apiKey("SG.test")
        .requestTimeout(Duration.ofSeconds(-1))
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Request timeout must be positive");
  }

  @Test
  @DisplayName("Should reject zero timeout")
  void shouldRejectZeroTimeout() {
    // When/Then
    assertThatThrownBy(() -> SendGridConfiguration.builder()
        .apiKey("SG.test")
        .requestTimeout(Duration.ZERO)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Request timeout must be positive");
  }
}
