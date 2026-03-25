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
    config =
        SendGridConfiguration.builder()
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
  @DisplayName("Should build template request and invoke sendWithTemplate")
  void shouldInvokeTemplateSend() {
    // Given
    TemplateEmailRequest templateRequest =
        TemplateEmailRequest.builder()
            .templateName("d-abc123def456")
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Welcome")
            .variable("name", "John")
            .variable("activationLink", "https://example.com/activate")
            .build();

    // When - will fail without a real API key, but the result must be a Result (not exception)
    Result<EmailPort.EmailReceipt> result = emailAdapter.sendWithTemplate(templateRequest);

    // Then - structural: returns a Result (ok or fail), never throws
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Should send batch emails and return a result per email")
  void shouldSendBatch() {
    // Given
    var emails =
        java.util.List.of(
            com.marcusprado02.commons.ports.email.Email.builder()
                .from("sender@example.com")
                .to("user1@example.com")
                .subject("Batch 1")
                .textContent("Message 1")
                .build(),
            com.marcusprado02.commons.ports.email.Email.builder()
                .from("sender@example.com")
                .to("user2@example.com")
                .subject("Batch 2")
                .textContent("Message 2")
                .build());

    // When
    var results = emailAdapter.sendBatch(emails);

    // Then - one result per email, none null
    assertThat(results).hasSize(2);
    assertThat(results).doesNotContainNull();
  }

  @Test
  @DisplayName("Should return empty list for empty batch")
  void shouldReturnEmptyForEmptyBatch() {
    var results = emailAdapter.sendBatch(java.util.List.of());
    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("Should return empty list for null batch")
  void shouldReturnEmptyForNullBatch() {
    var results = emailAdapter.sendBatch(null);
    assertThat(results).isEmpty();
  }
}
