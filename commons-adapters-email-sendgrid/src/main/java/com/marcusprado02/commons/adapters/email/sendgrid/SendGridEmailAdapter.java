package com.marcusprado02.commons.adapters.email.sendgrid;

import com.marcusprado02.commons.kernel.errors.*;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.*;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import java.io.IOException;
import java.util.Objects;

/**
 * SendGrid adapter for EmailPort using SendGrid Java SDK.
 *
 * <p>Basic implementation supporting:
 * <ul>
 *   <li>Simple email sending
 *   <li>HTML and plain text content
 *   <li>Single recipient
 *   <li>Sandbox mode for testing
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SendGridConfiguration config = SendGridConfiguration.builder()
 *     .apiKey("SG.your-api-key")
 *     .build();
 *
 * SendGridEmailAdapter adapter = new SendGridEmailAdapter(config);
 *
 * Email email = Email.builder()
 *     .from("sender@example.com")
 *     .to("recipient@example.com")
 *     .subject("Hello")
 *     .textContent("Hello World")
 *     .build();
 *
 * Result<EmailReceipt> result = adapter.send(email);
 * }</pre>
 */
public class SendGridEmailAdapter implements EmailPort, AutoCloseable {

  private final SendGridConfiguration configuration;
  private final SendGrid sendGrid;

  /**
   * Creates a new SendGridEmailAdapter with the given configuration.
   *
   * @param configuration SendGrid configuration with API key
   * @throws IllegalArgumentException if configuration is null
   */
  public SendGridEmailAdapter(SendGridConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.sendGrid = new SendGrid(configuration.apiKey());
  }

  @Override
  public Result<EmailReceipt> send(Email email) {
    try {
      // Create a simple mail object
      com.sendgrid.helpers.mail.objects.Email fromEmail =
          new com.sendgrid.helpers.mail.objects.Email(email.from().value());

      com.sendgrid.helpers.mail.objects.Email toEmail =
          new com.sendgrid.helpers.mail.objects.Email(email.to().get(0).value());

      Content content = createContent(email.content());

      Mail mail = new Mail(fromEmail, email.subject().value(), toEmail, content);

      if (configuration.sandboxMode()) {
        mail.getMailSettings().setSandboxMode(new Setting(true));
      }

      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);

      if (isSuccessResponse(response)) {
        String messageId = "sendgrid-" + System.currentTimeMillis();
        return Result.ok(EmailReceipt.of(messageId));
      } else {
        return Result.fail(createSendGridProblem(response));
      }

    } catch (IOException e) {
      return Result.fail(mapIOException(e));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SENDGRID_EMAIL_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send email via SendGrid: " + e.getMessage()));
    }
  }

  @Override
  public Result<EmailReceipt> sendWithTemplate(TemplateEmailRequest templateRequest) {
    return Result.fail(
        Problem.of(
            ErrorCode.of("SENDGRID_TEMPLATE_NOT_SUPPORTED"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "SendGrid template emails not yet implemented"));
  }

  @Override
  public Result<Void> verify() {
    try {
      // Simple connection test
      com.sendgrid.helpers.mail.objects.Email fromEmail =
          new com.sendgrid.helpers.mail.objects.Email("test@example.com");

      com.sendgrid.helpers.mail.objects.Email toEmail =
          new com.sendgrid.helpers.mail.objects.Email("test@example.com");

      Content content = new Content("text/plain", "Connection test");

      Mail testMail = new Mail(fromEmail, "SendGrid Connection Test", toEmail, content);
      testMail.getMailSettings().setSandboxMode(new Setting(true));

      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(testMail.build());

      Response response = sendGrid.api(request);

      if (isSuccessResponse(response)) {
        return Result.ok(null);
      } else {
        return Result.fail(createSendGridProblem(response));
      }

    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SENDGRID_CONNECTION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to verify SendGrid connection: " + e.getMessage()));
    }
  }

  @Override
  public void close() throws Exception {
    // SendGrid client doesn't require explicit cleanup
  }

  private Content createContent(EmailContent emailContent) {
    if (emailContent.hasHtml()) {
      return new Content("text/html", emailContent.htmlContent().get());
    } else if (emailContent.hasText()) {
      return new Content("text/plain", emailContent.textContent().get());
    } else {
      return new Content("text/plain", "");
    }
  }

  private boolean isSuccessResponse(Response response) {
    int statusCode = response.getStatusCode();
    return statusCode >= 200 && statusCode < 300;
  }

  private Problem createSendGridProblem(Response response) {
    String errorMessage = String.format(
        "SendGrid API request failed with status %d", response.getStatusCode());

    String errorCode = switch (response.getStatusCode()) {
      case 401 -> "SENDGRID_UNAUTHORIZED";
      case 403 -> "SENDGRID_FORBIDDEN";
      case 429 -> "SENDGRID_RATE_LIMITED";
      default -> "SENDGRID_REQUEST_FAILED";
    };

    ErrorCategory category = switch (response.getStatusCode()) {
      case 401, 403 -> ErrorCategory.UNAUTHORIZED;
      case 429 -> ErrorCategory.BUSINESS;
      default -> ErrorCategory.TECHNICAL;
    };

    return Problem.of(
        ErrorCode.of(errorCode),
        category,
        Severity.ERROR,
        errorMessage);
  }

  private Problem mapIOException(IOException e) {
    return Problem.of(
        ErrorCode.of("SENDGRID_IO_ERROR"),
        ErrorCategory.TECHNICAL,
        Severity.ERROR,
        "SendGrid API I/O error: " + e.getMessage());
  }
}
