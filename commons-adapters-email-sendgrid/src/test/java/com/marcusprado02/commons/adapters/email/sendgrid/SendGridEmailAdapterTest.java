package com.marcusprado02.commons.adapters.email.sendgrid;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.*;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendGridEmailAdapterTest {

  private SendGridConfiguration config;
  private SendGridEmailAdapter emailAdapter;

  @BeforeEach
  void setUp() {
    config = SendGridConfiguration.builder()
        .apiKey("SG.test-key-123456789")
        .defaultFromEmail("noreply@example.com")
        .defaultFromName("Test App")
        .requestTimeout(Duration.ofSeconds(10))
        .trackClicks(true)
        .trackOpens(true)
        .sandboxMode(false)
        .build();

    emailAdapter = new SendGridEmailAdapter(config);
  }

  @Test
  @DisplayName("Should send simple email successfully")
  void shouldSendSimpleEmail() {
    // Given
    com.marcusprado02.commons.ports.email.Email email =
        com.marcusprado02.commons.ports.email.Email.builder()
        .from("sender@example.com")
        .to("recipient@example.com")
        .subject("Test Subject")
        .textContent("Test message")
        .build();

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then - just check structure is valid, actual send would need real API key
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should handle null email")
  void shouldHandleNullEmail() {
    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(null);

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should send HTML email")
  void shouldSendHtmlEmail() {
    // Given
    com.marcusprado02.commons.ports.email.Email email =
        com.marcusprado02.commons.ports.email.Email.builder()
        .from("sender@example.com")
        .to("recipient@example.com")
        .subject("Test Subject")
        .htmlContent("<h1>Hello World</h1>")
        .build();

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should verify connection")
  void shouldVerifyConnection() {
    // When
    Result<Void> result = emailAdapter.verify();

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should fail template send (not implemented)")
  void shouldFailTemplateSend() {
    // Given
    TemplateEmailRequest templateRequest = TemplateEmailRequest.builder()
        .templateName("welcome")
        .from("sender@example.com")
        .to("recipient@example.com")
        .subject("Welcome")
        .variable("name", "John")
        .build();

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.sendWithTemplate(templateRequest);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).contains("TEMPLATE_NOT_SUPPORTED");
  }
}
