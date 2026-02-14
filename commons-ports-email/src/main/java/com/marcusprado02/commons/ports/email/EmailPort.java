package com.marcusprado02.commons.ports.email;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Port for email operations.
 *
 * <p>Abstraction for sending emails through various providers (SMTP, SendGrid, AWS SES, etc.).
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Send emails with HTML/plain text content
 *   <li>Support attachments
 *   <li>Support CC/BCC recipients
 *   <li>Support reply-to addresses
 *   <li>Template rendering (adapter-specific)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Email email = Email.builder()
 *     .from("noreply@example.com")
 *     .to("user@example.com")
 *     .subject("Welcome!")
 *     .htmlContent("<h1>Welcome to our service</h1>")
 *     .build();
 *
 * Result<EmailReceipt> result = emailPort.send(email);
 * }</pre>
 */
public interface EmailPort {

  /**
   * Send an email.
   *
   * @param email the email to send
   * @return Result with receipt or problem
   */
  Result<EmailReceipt> send(Email email);

  /**
   * Send an email with a template.
   *
   * <p>Template rendering is adapter-specific. Consult your adapter's documentation for supported
   * template engines and syntax.
   *
   * @param template the template request
   * @return Result with receipt or problem
   */
  default Result<EmailReceipt> sendWithTemplate(TemplateEmailRequest template) {
    throw new UnsupportedOperationException("Template support not implemented by this adapter");
  }

  /**
   * Verify connection to email service.
   *
   * <p>Optional operation. Implementations may choose to return success immediately.
   *
   * @return Result with void or problem
   */
  default Result<Void> verify() {
    return Result.ok(null);
  }

  /** Receipt returned after successful email send. */
  record EmailReceipt(String messageId) {
    public EmailReceipt {
      if (messageId == null || messageId.isBlank()) {
        throw new IllegalArgumentException("messageId must not be null or blank");
      }
    }

    public static EmailReceipt of(String messageId) {
      return new EmailReceipt(messageId);
    }
  }
}
