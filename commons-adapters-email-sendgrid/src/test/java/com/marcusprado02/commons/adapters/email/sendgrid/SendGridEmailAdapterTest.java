package com.marcusprado02.commons.adapters.email.sendgrid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.*;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendGridEmailAdapterTest {

  @Mock private SendGrid mockSendGrid;

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

    // Create adapter and inject mock
    emailAdapter = new SendGridEmailAdapter(config) {
      @Override
      public Result<EmailPort.EmailReceipt> send(Email email) {
        try {
          // Simulate the real implementation but use our mock
          Mail mail = super.convertToSendGridMail(email);
          Response response = mockSendGrid.api(mail);

          if (super.isSuccessResponse(response)) {
            String messageId = super.extractMessageId(response);
            return Result.ok(EmailPort.EmailReceipt.of(messageId));
          } else {
            return Result.fail(super.createSendGridProblem(response));
          }
        } catch (IOException e) {
          return Result.fail(super.mapIOException(e));
        } catch (Exception e) {
          return Result.fail(
              com.marcusprado02.commons.kernel.errors.Problem.of(
                  com.marcusprado02.commons.kernel.errors.ErrorCode.of("SENDGRID_EMAIL_SEND_ERROR"),
                  com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
                  com.marcusprado02.commons.kernel.errors.Severity.ERROR,
                  "Failed to send email via SendGrid: " + e.getMessage()));
        }
      }
    };
  }

  @Test
  @DisplayName("Should send simple email successfully")
  void shouldSendSimpleEmail() throws IOException {
    // Given
    Email email = Email.builder()
        .from("sender@example.com")
        .to("recipient@example.com")
        .subject("Test Subject")
        .textContent("Test message")
        .build();

    Map<String, String> headers = new HashMap<>();
    headers.put("X-Message-Id", "test-message-id-123");

    Response mockResponse = new Response(202, "Accepted", headers);
    when(mockSendGrid.api(any(Mail.class))).thenReturn(mockResponse);

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull().messageId()).isEqualTo("test-message-id-123");

    verify(mockSendGrid).api(any(Mail.class));
  }

  @Test
  @DisplayName("Should send HTML email with attachments")
  void shouldSendHtmlEmailWithAttachments() throws IOException {
    // Given
    byte[] pdfData = "PDF content".getBytes(StandardCharsets.UTF_8);
    EmailAttachment attachment = EmailAttachment.of("document.pdf", "application/pdf", pdfData);

    Email email = Email.builder()
        .from("sender@example.com")
        .to("recipient@example.com")
        .cc("cc@example.com")
        .subject("Email with Attachment")
        .htmlContent("<h1>Hello</h1><p>Please find attachment</p>")
        .attachment(attachment)
        .build();

    Map<String, String> headers = new HashMap<>();
    headers.put("X-Message-Id", "test-attachment-id");

    Response mockResponse = new Response(202, "Accepted", headers);
    when(mockSendGrid.api(any(Mail.class))).thenReturn(mockResponse);

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().messageId()).isEqualTo("test-attachment-id");

    verify(mockSendGrid).api(any(Mail.class));
  }

  @Test
  @DisplayName("Should handle SendGrid API error")
  void shouldHandleSendGridApiError() throws IOException {
    // Given
    Email email = Email.builder()
        .from("invalid@example.com")
        .to("recipient@example.com")
        .subject("Test")
        .textContent("Test")
        .build();

    Response mockResponse = new Response(401, "Unauthorized", Map.of());
    when(mockSendGrid.api(any(Mail.class))).thenReturn(mockResponse);

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).contains("SENDGRID_UNAUTHORIZED");

    verify(mockSendGrid).api(any(Mail.class));
  }

  @Test
  @DisplayName("Should handle IO exception")
  void shouldHandleIOException() throws IOException {
    // Given
    Email email = Email.builder()
        .from("sender@example.com")
        .to("recipient@example.com")
        .subject("Test")
        .textContent("Test")
        .build();

    when(mockSendGrid.api(any(Mail.class))).thenThrow(new IOException("Connection timeout"));

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).contains("SENDGRID_TIMEOUT");

    verify(mockSendGrid).api(any(Mail.class));
  }

  @Test
  @DisplayName("Should verify connection successfully")
  void shouldVerifyConnection() throws IOException {
    // Given
    Response mockResponse = new Response(202, "Accepted", Map.of());
    when(mockSendGrid.api(any(Mail.class))).thenReturn(mockResponse);

    // When
    Result<Void> result = emailAdapter.verify();

    // Then
    assertThat(result.isOk()).isTrue();

    verify(mockSendGrid).api(any(Mail.class));
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
