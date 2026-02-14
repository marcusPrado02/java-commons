package com.marcusprado02.commons.adapters.sms.twilio;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TwilioConfigurationTest {

  @Test
  @DisplayName("Should create configuration with builder")
  void shouldCreateConfigurationWithBuilder() {
    // When
    TwilioConfiguration config = TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("test-auth-token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ofSeconds(15))
        .deliveryReceiptsEnabled(true)
        .webhookUrl("https://example.com/webhook")
        .build();

    // Then
    assertThat(config.accountSid()).isEqualTo("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    assertThat(config.authToken()).isEqualTo("test-auth-token");
    assertThat(config.fromPhoneNumber()).isEqualTo("+1234567890");
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(15));
    assertThat(config.deliveryReceiptsEnabled()).isTrue();
    assertThat(config.webhookUrl()).isEqualTo("https://example.com/webhook");
  }

  @Test
  @DisplayName("Should create development configuration")
  void shouldCreateDevelopmentConfiguration() {
    // When
    TwilioConfiguration config = TwilioConfiguration.forDevelopment(
        "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
        "test-token",
        "+1234567890"
    );

    // Then
    assertThat(config.accountSid()).isEqualTo("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    assertThat(config.authToken()).isEqualTo("test-token");
    assertThat(config.fromPhoneNumber()).isEqualTo("+1234567890");
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.deliveryReceiptsEnabled()).isFalse();
    assertThat(config.webhookUrl()).isNull();
  }

  @Test
  @DisplayName("Should create production configuration")
  void shouldCreateProductionConfiguration() {
    // When
    TwilioConfiguration config = TwilioConfiguration.forProduction(
        "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
        "prod-token",
        "+1234567890",
        "https://prod.example.com/webhook"
    );

    // Then
    assertThat(config.accountSid()).isEqualTo("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    assertThat(config.authToken()).isEqualTo("prod-token");
    assertThat(config.fromPhoneNumber()).isEqualTo("+1234567890");
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(15));
    assertThat(config.deliveryReceiptsEnabled()).isTrue();
    assertThat(config.webhookUrl()).isEqualTo("https://prod.example.com/webhook");
  }

  @Test
  @DisplayName("Should validate required fields")
  void shouldValidateRequiredFields() {
    // When/Then - Account SID
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid(null)
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Account SID cannot be null");

    // When/Then - Auth token
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken(null)
        .fromPhoneNumber("+1234567890")
        .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Auth token cannot be null");

    // When/Then - From phone number
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber(null)
        .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("From phone number cannot be null");
  }

  @Test
  @DisplayName("Should validate blank fields")
  void shouldValidateBlankFields() {
    // When/Then - Blank Account SID
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Account SID cannot be blank");

    // When/Then - Blank auth token
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("   ")
        .fromPhoneNumber("+1234567890")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Auth token cannot be blank");
  }

  @Test
  @DisplayName("Should validate Account SID format")
  void shouldValidateAccountSidFormat() {
    // When/Then
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("invalid-sid")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid Twilio Account SID format: must start with 'AC'");
  }

  @Test
  @DisplayName("Should validate phone number format")
  void shouldValidatePhoneNumberFormat() {
    // When/Then - Invalid format
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("invalid-phone")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid from phone number format");

    // When/Then - Missing +
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("1234567890")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid from phone number format");
  }

  @Test
  @DisplayName("Should validate timeout")
  void shouldValidateTimeout() {
    // When/Then - Negative timeout
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ofSeconds(-1))
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Request timeout must be positive");

    // When/Then - Zero timeout
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ZERO)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Request timeout must be positive");
  }

  @Test
  @DisplayName("Should validate webhook URL format")
  void shouldValidateWebhookUrlFormat() {
    // When/Then - Invalid URL
    assertThatThrownBy(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .webhookUrl("invalid-url")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Webhook URL must be a valid HTTP/HTTPS URL");

    // Valid URLs should work
    assertThatCode(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .webhookUrl("https://example.com/webhook")
        .build())
        .doesNotThrowAnyException();

    assertThatCode(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .webhookUrl("http://localhost:8080/webhook")
        .build())
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should allow null webhook URL")
  void shouldAllowNullWebhookUrl() {
    // When/Then
    assertThatCode(() -> TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("token")
        .fromPhoneNumber("+1234567890")
        .webhookUrl(null)
        .build())
        .doesNotThrowAnyException();
  }
}
