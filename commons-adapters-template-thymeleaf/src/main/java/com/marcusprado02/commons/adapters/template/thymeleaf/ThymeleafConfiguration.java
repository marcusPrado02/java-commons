package com.marcusprado02.commons.adapters.template.thymeleaf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Configuration for Thymeleaf template engine.
 *
 * <p>Provides settings for template resolution, caching, and processing.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ThymeleafConfiguration config = ThymeleafConfiguration.builder()
 *     .templatePrefix("classpath:/templates/")
 *     .templateSuffix(".html")
 *     .templateMode(TemplateMode.HTML)
 *     .cacheable(true)
 *     .cacheableTTLMs(3600000L) // 1 hour
 *     .charset(StandardCharsets.UTF_8)
 *     .build();
 *
 * ThymeleafTemplateAdapter adapter = new ThymeleafTemplateAdapter(config);
 * }</pre>
 */
public class ThymeleafConfiguration {

  private final String templatePrefix;
  private final String templateSuffix;
  private final TemplateMode templateMode;
  private final boolean cacheable;
  private final Long cacheableTTLMs;
  private final Charset charset;

  private ThymeleafConfiguration(Builder builder) {
    this.templatePrefix = builder.templatePrefix;
    this.templateSuffix = builder.templateSuffix;
    this.templateMode = builder.templateMode;
    this.cacheable = builder.cacheable;
    this.cacheableTTLMs = builder.cacheableTTLMs;
    this.charset = builder.charset;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getTemplatePrefix() {
    return templatePrefix;
  }

  public String getTemplateSuffix() {
    return templateSuffix;
  }

  public TemplateMode getTemplateMode() {
    return templateMode;
  }

  public boolean isCacheable() {
    return cacheable;
  }

  public Long getCacheableTTLMs() {
    return cacheableTTLMs;
  }

  public Charset getCharset() {
    return charset;
  }

  /** Builder for ThymeleafConfiguration. */
  public static class Builder {
    private String templatePrefix = "classpath:/templates/";
    private String templateSuffix = ".html";
    private TemplateMode templateMode = TemplateMode.HTML;
    private boolean cacheable = true;
    private Long cacheableTTLMs = 3600000L; // 1 hour
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * Sets the template prefix (directory path).
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"classpath:/templates/" - Load from classpath
     *   <li>"file:/var/app/templates/" - Load from filesystem
     *   <li>"" - No prefix
     * </ul>
     *
     * @param templatePrefix template prefix
     * @return this builder
     */
    public Builder templatePrefix(String templatePrefix) {
      this.templatePrefix = Objects.requireNonNull(templatePrefix);
      return this;
    }

    /**
     * Sets the template suffix (file extension).
     *
     * <p>Examples: ".html", ".xml", ".txt"
     *
     * @param templateSuffix template suffix
     * @return this builder
     */
    public Builder templateSuffix(String templateSuffix) {
      this.templateSuffix = Objects.requireNonNull(templateSuffix);
      return this;
    }

    /**
     * Sets the template mode.
     *
     * <p>Modes:
     *
     * <ul>
     *   <li>HTML - HTML5 templates (default)
     *   <li>XML - XML templates
     *   <li>TEXT - Plain text templates
     *   <li>JAVASCRIPT - JavaScript templates
     *   <li>CSS - CSS templates
     *   <li>RAW - No processing
     * </ul>
     *
     * @param templateMode template mode
     * @return this builder
     */
    public Builder templateMode(TemplateMode templateMode) {
      this.templateMode = Objects.requireNonNull(templateMode);
      return this;
    }

    /**
     * Sets whether templates should be cached.
     *
     * <p>Default: true (recommended for production)
     *
     * @param cacheable true to enable caching
     * @return this builder
     */
    public Builder cacheable(boolean cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    /**
     * Sets the cache TTL in milliseconds.
     *
     * <p>After TTL expires, template will be reloaded on next access.
     *
     * <p>Default: 3600000 (1 hour)
     *
     * @param cacheableTTLMs cache TTL in milliseconds, null for infinite
     * @return this builder
     */
    public Builder cacheableTTLMs(Long cacheableTTLMs) {
      this.cacheableTTLMs = cacheableTTLMs;
      return this;
    }

    /**
     * Sets the character encoding.
     *
     * <p>Default: UTF-8
     *
     * @param charset character encoding
     * @return this builder
     */
    public Builder charset(Charset charset) {
      this.charset = Objects.requireNonNull(charset);
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return thymeleaf configuration
     */
    public ThymeleafConfiguration build() {
      return new ThymeleafConfiguration(this);
    }
  }

  /** Creates default configuration for HTML templates in classpath. */
  public static ThymeleafConfiguration defaultHtml() {
    return builder().build();
  }

  /** Creates configuration for text templates. */
  public static ThymeleafConfiguration text() {
    return builder().templateMode(TemplateMode.TEXT).templateSuffix(".txt").build();
  }

  /** Creates configuration for XML templates. */
  public static ThymeleafConfiguration xml() {
    return builder().templateMode(TemplateMode.XML).templateSuffix(".xml").build();
  }

  /** Creates configuration for development (no cache). */
  public static ThymeleafConfiguration dev() {
    return builder().cacheable(false).build();
  }
}
