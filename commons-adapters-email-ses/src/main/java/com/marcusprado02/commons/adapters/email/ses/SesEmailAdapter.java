package com.marcusprado02.commons.adapters.email.ses;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.Email;
import com.marcusprado02.commons.ports.email.EmailAddress;
import com.marcusprado02.commons.ports.email.EmailPort;
import com.marcusprado02.commons.ports.email.TemplateEmailRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

/**
 * AWS SES v2 adapter for {@link EmailPort}.
 *
 * <p>Supports HTML and plain text emails, multiple recipients, CC/BCC, reply-to, and SES
 * Template rendering. Credentials are resolved via the AWS Default Credential Provider Chain
 * (environment variables, IAM roles, etc.).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * SesEmailAdapter adapter = SesEmailAdapter.create(
 *     SesV2Client.builder().region(Region.US_EAST_1).build());
 *
 * Email email = Email.builder()
 *     .from("noreply@example.com")
 *     .to("user@example.com")
 *     .subject("Welcome")
 *     .htmlContent("<h1>Hello!</h1>")
 *     .build();
 *
 * Result<EmailReceipt> result = adapter.send(email);
 * }</pre>
 */
public final class SesEmailAdapter implements EmailPort, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(SesEmailAdapter.class);

  private final SesV2Client sesClient;

  private SesEmailAdapter(SesV2Client sesClient) {
    this.sesClient = Objects.requireNonNull(sesClient, "sesClient must not be null");
  }

  /**
   * Creates an adapter wrapping an existing {@link SesV2Client}.
   *
   * @param sesClient pre-configured SES v2 client
   * @return adapter instance
   */
  public static SesEmailAdapter create(SesV2Client sesClient) {
    return new SesEmailAdapter(sesClient);
  }

  @Override
  public Result<EmailReceipt> send(Email email) {
    if (email == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SES.NULL_EMAIL"),
              ErrorCategory.VALIDATION,
              Severity.WARNING,
              "Email must not be null"));
    }

    try {
      var request = buildSendRequest(email);
      var response = sesClient.sendEmail(request);

      logger.info("SES email sent: {}", response.messageId());
      return Result.ok(EmailReceipt.of(response.messageId()));

    } catch (SesV2Exception e) {
      logger.error("SES send failed: {}", e.getMessage(), e);
      return Result.fail(mapSesException(e));
    } catch (Exception e) {
      logger.error("Unexpected error sending SES email: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SES.SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send email via SES: " + e.getMessage()));
    }
  }

  @Override
  public Result<EmailReceipt> sendWithTemplate(TemplateEmailRequest templateRequest) {
    if (templateRequest == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SES.NULL_TEMPLATE_REQUEST"),
              ErrorCategory.VALIDATION,
              Severity.WARNING,
              "TemplateEmailRequest must not be null"));
    }

    try {
      // Build template data as JSON key-value pairs
      var templateDataBuilder = new StringBuilder("{");
      boolean first = true;
      for (Map.Entry<String, Object> entry : templateRequest.variables().entrySet()) {
        if (!first) templateDataBuilder.append(",");
        templateDataBuilder
            .append("\"")
            .append(entry.getKey())
            .append("\":\"")
            .append(entry.getValue())
            .append("\"");
        first = false;
      }
      templateDataBuilder.append("}");

      var destination =
          Destination.builder()
              .toAddresses(toStringList(templateRequest.to()))
              .ccAddresses(toStringList(templateRequest.cc()))
              .bccAddresses(toStringList(templateRequest.bcc()))
              .build();

      var templateContent =
          EmailTemplateContent.builder()
              .templateName(templateRequest.templateName())
              .templateData(templateDataBuilder.toString())
              .build();

      var requestBuilder =
          SendEmailRequest.builder()
              .fromEmailAddress(templateRequest.from().value())
              .destination(destination)
              .content(
                  EmailContent.builder()
                      .template(Template.builder()
                          .templateName(templateRequest.templateName())
                          .templateData(templateDataBuilder.toString())
                          .build())
                      .build());

      if (templateRequest.replyTo() != null) {
        requestBuilder.replyToAddresses(templateRequest.replyTo().value());
      }

      var response = sesClient.sendEmail(requestBuilder.build());
      logger.info("SES template email sent: {}", response.messageId());
      return Result.ok(EmailReceipt.of(response.messageId()));

    } catch (SesV2Exception e) {
      logger.error("SES template send failed: {}", e.getMessage(), e);
      return Result.fail(mapSesException(e));
    } catch (Exception e) {
      logger.error("Unexpected error sending SES template email: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SES.TEMPLATE_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send template email via SES: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> verify() {
    try {
      // List sending identities to test connectivity and credentials
      sesClient.listEmailIdentities(ListEmailIdentitiesRequest.builder().pageSize(1).build());
      return Result.ok(null);
    } catch (SesV2Exception e) {
      return Result.fail(mapSesException(e));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SES.CONNECTION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "SES connection check failed: " + e.getMessage()));
    }
  }

  @Override
  public void close() {
    sesClient.close();
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private SendEmailRequest buildSendRequest(Email email) {
    var destination =
        Destination.builder()
            .toAddresses(toStringList(email.to()))
            .ccAddresses(toStringList(email.cc()))
            .bccAddresses(toStringList(email.bcc()))
            .build();

    var bodyBuilder = Body.builder();
    var content = email.content();
    if (content.hasHtml()) {
      bodyBuilder.html(Content.builder().data(content.htmlContent().get()).charset("UTF-8").build());
    }
    if (content.hasText()) {
      bodyBuilder.text(Content.builder().data(content.textContent().get()).charset("UTF-8").build());
    }

    var message =
        Message.builder()
            .subject(Content.builder().data(email.subject().value()).charset("UTF-8").build())
            .body(bodyBuilder.build())
            .build();

    var requestBuilder =
        SendEmailRequest.builder()
            .fromEmailAddress(email.from().value())
            .destination(destination)
            .content(EmailContent.builder().simple(message).build());

    if (email.replyTo() != null) {
      requestBuilder.replyToAddresses(email.replyTo().value());
    }

    return requestBuilder.build();
  }

  private List<String> toStringList(List<EmailAddress> addresses) {
    if (addresses == null) return List.of();
    return addresses.stream().map(EmailAddress::value).toList();
  }

  private Problem mapSesException(SesV2Exception e) {
    String code =
        switch (e.statusCode()) {
          case 400 -> "SES.BAD_REQUEST";
          case 401, 403 -> "SES.UNAUTHORIZED";
          case 404 -> "SES.NOT_FOUND";
          case 429 -> "SES.RATE_LIMITED";
          default -> "SES.API_ERROR";
        };

    ErrorCategory category =
        switch (e.statusCode()) {
          case 401, 403 -> ErrorCategory.UNAUTHORIZED;
          case 400, 404 -> ErrorCategory.BUSINESS;
          case 429 -> ErrorCategory.BUSINESS;
          default -> ErrorCategory.TECHNICAL;
        };

    return Problem.of(
        ErrorCode.of(code), category, Severity.ERROR, "SES error: " + e.awsErrorDetails().errorMessage());
  }
}
