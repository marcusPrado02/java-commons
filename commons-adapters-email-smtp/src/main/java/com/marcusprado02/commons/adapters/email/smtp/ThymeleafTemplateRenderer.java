package com.marcusprado02.commons.adapters.email.smtp;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * Thymeleaf-based template renderer for email templates.
 *
 * <p>Renders templates using Thymeleaf template engine with HTML template mode.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create renderer with templates in classpath:/email-templates/
 * TemplateRenderer renderer = ThymeleafTemplateRenderer.builder()
 *     .templatePrefix("/email-templates/")
 *     .templateSuffix(".html")
 *     .build();
 *
 * // Render template
 * Map<String, Object> vars = Map.of(
 *     "userName", "John",
 *     "activationLink", "https://example.com/activate"
 * );
 * String html = renderer.render("welcome-email", vars);
 * }</pre>
 */
public final class ThymeleafTemplateRenderer implements TemplateRenderer {

  private final TemplateEngine templateEngine;

  /**
   * Creates renderer with custom template engine.
   *
   * @param templateEngine the Thymeleaf template engine
   */
  public ThymeleafTemplateRenderer(TemplateEngine templateEngine) {
    this.templateEngine = Objects.requireNonNull(templateEngine, "templateEngine must not be null");
  }

  /**
   * Creates renderer with default configuration.
   *
   * <p>Default configuration:
   *
   * <ul>
   *   <li>Template prefix: "/email-templates/"
   *   <li>Template suffix: ".html"
   *   <li>Template mode: HTML
   *   <li>Character encoding: UTF-8
   *   <li>Cacheable: true
   * </ul>
   *
   * @return renderer with defaults
   */
  public static ThymeleafTemplateRenderer withDefaults() {
    return builder().build();
  }

  /**
   * Creates a builder for customizing the renderer.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String render(String templateName, Map<String, Object> variables) {
    try {
      Context context = new Context(Locale.getDefault(), variables);
      return templateEngine.process(templateName, context);
    } catch (Exception e) {
      throw new TemplateRenderingException("Failed to render template: " + templateName, e);
    }
  }

  /** Builder for ThymeleafTemplateRenderer. */
  public static final class Builder {
    private String templatePrefix = "/email-templates/";
    private String templateSuffix = ".html";
    private TemplateMode templateMode = TemplateMode.HTML;
    private String characterEncoding = "UTF-8";
    private boolean cacheable = true;
    private Long cacheableTTLMs = null;

    private Builder() {}

    /**
     * Sets template prefix (directory path).
     *
     * @param templatePrefix the prefix
     * @return this builder
     */
    public Builder templatePrefix(String templatePrefix) {
      this.templatePrefix = templatePrefix;
      return this;
    }

    /**
     * Sets template suffix (file extension).
     *
     * @param templateSuffix the suffix
     * @return this builder
     */
    public Builder templateSuffix(String templateSuffix) {
      this.templateSuffix = templateSuffix;
      return this;
    }

    /**
     * Sets template mode.
     *
     * @param templateMode the template mode
     * @return this builder
     */
    public Builder templateMode(TemplateMode templateMode) {
      this.templateMode = templateMode;
      return this;
    }

    /**
     * Sets character encoding.
     *
     * @param characterEncoding the encoding
     * @return this builder
     */
    public Builder characterEncoding(String characterEncoding) {
      this.characterEncoding = characterEncoding;
      return this;
    }

    /**
     * Sets whether templates are cacheable.
     *
     * @param cacheable true to enable caching
     * @return this builder
     */
    public Builder cacheable(boolean cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    /**
     * Sets cache TTL in milliseconds.
     *
     * @param cacheableTTLMs the cache TTL
     * @return this builder
     */
    public Builder cacheableTTLMs(Long cacheableTTLMs) {
      this.cacheableTTLMs = cacheableTTLMs;
      return this;
    }

    /**
     * Builds the renderer.
     *
     * @return ThymeleafTemplateRenderer
     */
    public ThymeleafTemplateRenderer build() {
      ITemplateResolver resolver = createTemplateResolver();
      TemplateEngine engine = new TemplateEngine();
      engine.setTemplateResolver(resolver);
      return new ThymeleafTemplateRenderer(engine);
    }

    private ITemplateResolver createTemplateResolver() {
      ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
      resolver.setPrefix(templatePrefix);
      resolver.setSuffix(templateSuffix);
      resolver.setTemplateMode(templateMode);
      resolver.setCharacterEncoding(characterEncoding);
      resolver.setCacheable(cacheable);
      if (cacheableTTLMs != null) {
        resolver.setCacheTTLMs(cacheableTTLMs);
      }
      return resolver;
    }
  }
}
