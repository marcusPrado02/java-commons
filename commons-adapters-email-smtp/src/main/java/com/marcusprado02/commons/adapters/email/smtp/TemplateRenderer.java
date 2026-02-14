package com.marcusprado02.commons.adapters.email.smtp;

import java.util.Map;

/**
 * Template renderer interface for email templates.
 *
 * <p>Implementations can use various template engines (Thymeleaf, Freemarker, Mustache, etc.).
 */
public interface TemplateRenderer {

  /**
   * Renders a template with the given variables.
   *
   * @param templateName the template name/path
   * @param variables the template variables
   * @return rendered HTML content
   * @throws TemplateRenderingException if rendering fails
   */
  String render(String templateName, Map<String, Object> variables);

  /** Exception thrown when template rendering fails. */
  class TemplateRenderingException extends RuntimeException {
    public TemplateRenderingException(String message, Throwable cause) {
      super(message, cause);
    }

    public TemplateRenderingException(String message) {
      super(message);
    }
  }
}
