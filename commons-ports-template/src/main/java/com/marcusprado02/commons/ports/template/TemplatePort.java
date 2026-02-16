package com.marcusprado02.commons.ports.template;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Port for template rendering operations.
 *
 * <p>Provides platform-agnostic interface for template engines like Thymeleaf, FreeMarker,
 * Velocity, etc.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TemplateContext context = TemplateContext.builder()
 *     .variable("userName", "John Doe")
 *     .variable("totalAmount", 1250.00)
 *     .locale(Locale.US)
 *     .build();
 *
 * Result<TemplateResult> result = templatePort.render("email/invoice", context);
 * if (result.isOk()) {
 *     String html = result.getValue().getContent();
 *     System.out.println(html);
 * }
 * }</pre>
 *
 * @see TemplateContext
 * @see TemplateResult
 */
public interface TemplatePort {

  /**
   * Renders a template with the given context.
   *
   * @param templateName template name or path (e.g., "email/invoice", "reports/monthly")
   * @param context template context with variables and locale
   * @return Result containing the rendered content or an error
   */
  Result<TemplateResult> render(String templateName, TemplateContext context);

  /**
   * Renders a template from a string literal.
   *
   * <p>Useful for dynamic templates not stored in files.
   *
   * @param templateContent template content as string
   * @param context template context with variables and locale
   * @return Result containing the rendered content or an error
   */
  Result<TemplateResult> renderString(String templateContent, TemplateContext context);

  /**
   * Checks if a template exists.
   *
   * @param templateName template name or path
   * @return true if template exists, false otherwise
   */
  boolean exists(String templateName);

  /**
   * Clears all cached templates.
   *
   * <p>Forces reload of all templates on next render.
   */
  void clearCache();

  /**
   * Clears a specific template from cache.
   *
   * @param templateName template name or path
   */
  void clearCache(String templateName);
}
