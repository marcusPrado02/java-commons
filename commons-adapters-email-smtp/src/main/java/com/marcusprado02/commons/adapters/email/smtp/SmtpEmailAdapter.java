package com.marcusprado02.commons.adapters.email.smtp;

import com.marcusprado02.commons.kernel.errors.*;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Objects;
import java.util.Optional;

/**
 * SMTP adapter for EmailPort using Jakarta Mail.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>HTML and plain text emails
 *   <li>Attachments
 *   <li>CC/BCC recipients
 *   <li>Reply-To addresses
 *   <li>Template rendering (with TemplateRenderer)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SmtpConfiguration config = SmtpConfiguration.builder()
 *     .host("smtp.gmail.com")
 *     .port(587)
 *     .username("user@gmail.com")
 *     .password("password")
 *     .useStartTls(true)
 *     .build();
 *
 * EmailPort emailPort = new SmtpEmailAdapter(config);
 *
 * Email email = Email.builder()
 *     .from("noreply@example.com")
 *     .to("user@example.com")
 *     .subject("Welcome!")
 *     .htmlContent("<h1>Hello</h1>")
 *     .build();
 *
 * Result<EmailReceipt> result = emailPort.send(email);
 * }</pre>
 */
public final class SmtpEmailAdapter implements EmailPort, AutoCloseable {

  private final SmtpConfiguration configuration;
  private final Session session;
  private final Optional<TemplateRenderer> templateRenderer;

  /**
   * Creates SMTP adapter with configuration.
   *
   * @param configuration the SMTP configuration
   */
  public SmtpEmailAdapter(SmtpConfiguration configuration) {
    this(configuration, null);
  }

  /**
   * Creates SMTP adapter with configuration and template renderer.
   *
   * @param configuration the SMTP configuration
   * @param templateRenderer optional template renderer for template support
   */
  public SmtpEmailAdapter(SmtpConfiguration configuration, TemplateRenderer templateRenderer) {
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
    this.templateRenderer = Optional.ofNullable(templateRenderer);

    // Create Jakarta Mail session
    this.session = createSession(configuration);
  }

  @Override
  public Result<EmailReceipt> send(Email email) {
    try {
      MimeMessage message = createMimeMessage(email);
      Transport.send(message);

      String messageId = message.getMessageID();
      return Result.ok(EmailReceipt.of(messageId != null ? messageId : generateMessageId()));

    } catch (MessagingException e) {
      return Result.fail(mapException(e));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EMAIL_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send email: " + e.getMessage()));
    }
  }

  @Override
  public Result<EmailReceipt> sendWithTemplate(TemplateEmailRequest templateRequest) {
    if (templateRenderer.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_RENDERER_NOT_CONFIGURED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Template renderer not configured"));
    }

    try {
      // Render template
      String renderedContent =
          templateRenderer
              .get()
              .render(templateRequest.templateName(), templateRequest.variables());

      // Build Email from template
      Email email =
          Email.builder()
              .from(templateRequest.from())
              .to(templateRequest.to())
              .cc(templateRequest.cc())
              .bcc(templateRequest.bcc())
              .subject(templateRequest.subject())
              .htmlContent(renderedContent)
              .attachments(templateRequest.attachments())
              .replyTo(templateRequest.replyTo())
              .build();

      return send(email);

    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_RENDERING_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to render email template: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> verify() {
    try (Transport transport = session.getTransport("smtp")) {
      transport.connect(
          configuration.host(),
          configuration.port(),
          configuration.username(),
          configuration.password());
      return Result.ok(null);
    } catch (MessagingException e) {
      return Result.fail(mapException(e));
    }
  }

  @Override
  public void close() {
    // Jakarta Mail doesn't hold persistent connections, nothing to close
  }

  private Session createSession(SmtpConfiguration config) {
    Authenticator authenticator = null;
    if (config.requireAuth()) {
      authenticator =
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(config.username(), config.password());
            }
          };
    }

    return Session.getInstance(config.toProperties(), authenticator);
  }

  private MimeMessage createMimeMessage(Email email) throws MessagingException {
    MimeMessage message = new MimeMessage(session);

    // Set from
    message.setFrom(toInternetAddress(email.from()));

    // Set recipients
    for (EmailAddress to : email.to()) {
      message.addRecipient(Message.RecipientType.TO, toInternetAddress(to));
    }

    for (EmailAddress cc : email.cc()) {
      message.addRecipient(Message.RecipientType.CC, toInternetAddress(cc));
    }

    for (EmailAddress bcc : email.bcc()) {
      message.addRecipient(Message.RecipientType.BCC, toInternetAddress(bcc));
    }

    // Set reply-to
    if (email.replyTo() != null) {
      message.setReplyTo(new Address[] {toInternetAddress(email.replyTo())});
    }

    // Set subject
    message.setSubject(email.subject().value(), "UTF-8");

    // Set content
    if (email.hasAttachments()) {
      message.setContent(createMultipartWithAttachments(email));
    } else {
      Object content = createContent(email.content());
      if (content instanceof Multipart) {
        message.setContent((Multipart) content);
      } else {
        message.setContent(content, "text/html; charset=UTF-8");
      }
    }

    return message;
  }

  private Multipart createMultipartWithAttachments(Email email) throws MessagingException {
    MimeMultipart multipart = new MimeMultipart();

    // Add body part
    MimeBodyPart bodyPart = new MimeBodyPart();
    Object content = createContent(email.content());
    if (content instanceof Multipart) {
      bodyPart.setContent((Multipart) content, "multipart/alternative");
    } else {
      bodyPart.setContent(content, "text/html; charset=UTF-8");
    }
    multipart.addBodyPart(bodyPart);

    // Add attachments
    for (EmailAttachment attachment : email.attachments()) {
      MimeBodyPart attachmentPart = new MimeBodyPart();
      attachmentPart.setDataHandler(
          new jakarta.activation.DataHandler(
              new jakarta.mail.util.ByteArrayDataSource(
                  attachment.data(), attachment.contentType())));
      attachmentPart.setFileName(attachment.filename());
      multipart.addBodyPart(attachmentPart);
    }

    return multipart;
  }

  private Object createContent(EmailContent content) throws MessagingException {
    if (content.hasHtml() && content.hasText()) {
      // Send multipart/alternative with both HTML and text
      MimeMultipart multipart = new MimeMultipart("alternative");

      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(content.textContent().get(), "UTF-8");
      multipart.addBodyPart(textPart);

      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(content.htmlContent().get(), "text/html; charset=UTF-8");
      multipart.addBodyPart(htmlPart);

      return multipart;
    } else if (content.hasHtml()) {
      return content.htmlContent().get();
    } else {
      return content.textContent().get();
    }
  }

  private InternetAddress toInternetAddress(EmailAddress address) {
    try {
      return new InternetAddress(address.value());
    } catch (AddressException e) {
      throw new IllegalArgumentException("Invalid email address: " + address.value(), e);
    }
  }

  private Problem mapException(MessagingException e) {
    if (e instanceof AuthenticationFailedException) {
      return Problem.of(
          ErrorCode.of("SMTP_AUTHENTICATION_FAILED"),
          ErrorCategory.UNAUTHORIZED,
          Severity.ERROR,
          "SMTP authentication failed: " + e.getMessage());
    }
    if (e instanceof SendFailedException) {
      return Problem.of(
          ErrorCode.of("SMTP_SEND_FAILED"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "SMTP send failed: " + e.getMessage());
    }
    if (e.getMessage() != null && e.getMessage().contains("connect")) {
      return Problem.of(
          ErrorCode.of("SMTP_CONNECTION_FAILED"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "SMTP connection failed: " + e.getMessage());
    }
    return Problem.of(
        ErrorCode.of("SMTP_ERROR"),
        ErrorCategory.TECHNICAL,
        Severity.ERROR,
        "SMTP error: " + e.getMessage());
  }

  private String generateMessageId() {
    return java.util.UUID.randomUUID().toString() + "@" + configuration.host();
  }
}
