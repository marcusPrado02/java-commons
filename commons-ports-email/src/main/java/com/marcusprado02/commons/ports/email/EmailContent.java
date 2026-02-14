package com.marcusprado02.commons.ports.email;

import java.util.Objects;
import java.util.Optional;

/**
 * Email content value object.
 *
 * <p>Represents the body of an email, supporting both HTML and plain text content.
 *
 * <p>Best practice: Provide both HTML and plain text versions for better email client
 * compatibility.
 */
public record EmailContent(Optional<String> htmlContent, Optional<String> textContent) {

  public EmailContent {
    Objects.requireNonNull(htmlContent, "htmlContent must not be null");
    Objects.requireNonNull(textContent, "textContent must not be null");

    if (htmlContent.isEmpty() && textContent.isEmpty()) {
      throw new IllegalArgumentException(
          "at least one content type (HTML or text) must be provided");
    }

    // Validate HTML content if present
    htmlContent.ifPresent(
        html -> {
          if (html.trim().isEmpty()) {
            throw new IllegalArgumentException("HTML content must not be blank");
          }
        });

    // Validate text content if present
    textContent.ifPresent(
        text -> {
          if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("text content must not be blank");
          }
        });
  }

  /**
   * Creates email content with HTML only.
   *
   * @param htmlContent the HTML content
   * @return EmailContent with HTML only
   */
  public static EmailContent html(String htmlContent) {
    return new EmailContent(Optional.of(htmlContent), Optional.empty());
  }

  /**
   * Creates email content with plain text only.
   *
   * @param textContent the plain text content
   * @return EmailContent with plain text only
   */
  public static EmailContent text(String textContent) {
    return new EmailContent(Optional.empty(), Optional.of(textContent));
  }

  /**
   * Creates email content with both HTML and plain text.
   *
   * @param htmlContent the HTML content
   * @param textContent the plain text content
   * @return EmailContent with both formats
   */
  public static EmailContent both(String htmlContent, String textContent) {
    return new EmailContent(Optional.of(htmlContent), Optional.of(textContent));
  }

  /**
   * Returns true if HTML content is present.
   *
   * @return true if HTML content exists
   */
  public boolean hasHtml() {
    return htmlContent.isPresent();
  }

  /**
   * Returns true if plain text content is present.
   *
   * @return true if plain text content exists
   */
  public boolean hasText() {
    return textContent.isPresent();
  }
}
