package com.marcusprado02.commons.ports.template;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Result of template rendering.
 *
 * <p>Contains the rendered content and metadata.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Result<TemplateResult> result = templatePort.render("email/welcome", context);
 * if (result.isOk()) {
 *     TemplateResult templateResult = result.getValue();
 *     String html = templateResult.getContent();
 *     byte[] bytes = templateResult.getBytes();
 * }
 * }</pre>
 *
 * @param templateName name of the rendered template
 * @param content rendered content as string
 * @param contentType content type (e.g., "text/html", "text/plain", "application/xml")
 * @param charset character encoding
 */
public record TemplateResult(
    String templateName, String content, String contentType, Charset charset) {

  public TemplateResult {
    Objects.requireNonNull(templateName, "Template name cannot be null");
    Objects.requireNonNull(content, "Content cannot be null");
    Objects.requireNonNull(contentType, "Content type cannot be null");
    Objects.requireNonNull(charset, "Charset cannot be null");
  }

  /**
   * Creates a result with HTML content.
   *
   * @param templateName template name
   * @param content rendered HTML
   * @return template result
   */
  public static TemplateResult html(String templateName, String content) {
    return new TemplateResult(templateName, content, "text/html", StandardCharsets.UTF_8);
  }

  /**
   * Creates a result with plain text content.
   *
   * @param templateName template name
   * @param content rendered text
   * @return template result
   */
  public static TemplateResult text(String templateName, String content) {
    return new TemplateResult(templateName, content, "text/plain", StandardCharsets.UTF_8);
  }

  /**
   * Creates a result with XML content.
   *
   * @param templateName template name
   * @param content rendered XML
   * @return template result
   */
  public static TemplateResult xml(String templateName, String content) {
    return new TemplateResult(templateName, content, "application/xml", StandardCharsets.UTF_8);
  }

  /**
   * Gets content as bytes using the configured charset.
   *
   * @return content bytes
   */
  public byte[] getBytes() {
    return content.getBytes(charset);
  }

  /**
   * Gets content length in bytes.
   *
   * @return content length
   */
  public int getContentLength() {
    return getBytes().length;
  }

  /**
   * Checks if content is empty.
   *
   * @return true if content is empty or blank
   */
  public boolean isEmpty() {
    return content == null || content.isBlank();
  }

  /**
   * Gets the content.
   *
   * @return rendered content
   */
  public String getContent() {
    return content;
  }
}
