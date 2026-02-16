package com.marcusprado02.commons.adapters.template.thymeleaf;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.template.TemplateContext;
import com.marcusprado02.commons.ports.template.TemplatePort;
import com.marcusprado02.commons.ports.template.TemplateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Thymeleaf implementation of {@link TemplatePort}.
 *
 * <p>Provides HTML, XML, and text template rendering using Thymeleaf 3.x.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create configuration
 * ThymeleafConfiguration config = ThymeleafConfiguration.builder()
 *     .templatePrefix("classpath:/templates/")
 *     .templateSuffix(".html")
 *     .cacheable(true)
 *     .build();
 *
 * // Create adapter
 * TemplatePort templatePort = new ThymeleafTemplateAdapter(config);
 *
 * // Render template
 * TemplateContext context = TemplateContext.builder()
 *     .variable("userName", "John")
 *     .variable("items", itemsList)
 *     .build();
 *
 * Result<TemplateResult> result = templatePort.render("email/welcome", context);
 * }</pre>
 *
 * <p><b>Template Location:</b> Templates should be placed in the configured prefix directory. For
 * example, with prefix "classpath:/templates/", template "email/welcome" will be resolved to
 * "classpath:/templates/email/welcome.html".
 *
 * <p><b>Caching:</b> Templates are cached by default. Use {@link #clearCache()} or {@link
 * #clearCache(String)} to invalidate cache. In development, disable caching with {@link
 * ThymeleafConfiguration#dev()}.
 *
 * @see TemplatePort
 * @see ThymeleafConfiguration
 */
public class ThymeleafTemplateAdapter implements TemplatePort {

  private static final Logger logger = LoggerFactory.getLogger(ThymeleafTemplateAdapter.class);

  private final TemplateEngine templateEngine;
  private final TemplateEngine stringTemplateEngine;
  private final ThymeleafConfiguration configuration;

  /**
   * Creates adapter with the given configuration.
   *
   * @param configuration Thymeleaf configuration
   */
  public ThymeleafTemplateAdapter(ThymeleafConfiguration configuration) {
    this.configuration = configuration;
    this.templateEngine = createTemplateEngine(createFileResolver());
    this.stringTemplateEngine = createTemplateEngine(createStringResolver());
    logger.info(
        "Thymeleaf adapter initialized: prefix={}, suffix={}, mode={}, cacheable={}",
        configuration.getTemplatePrefix(),
        configuration.getTemplateSuffix(),
        configuration.getTemplateMode(),
        configuration.isCacheable());
  }

  @Override
  public Result<TemplateResult> render(String templateName, TemplateContext templateContext) {
    try {
      Context context = createThymeleafContext(templateContext);
      String content = templateEngine.process(templateName, context);

      TemplateResult result =
          new TemplateResult(templateName, content, getContentType(), configuration.getCharset());

      logger.debug("Template rendered successfully: {}", templateName);
      return Result.ok(result);

    } catch (TemplateInputException e) {
      logger.error("Template not found: {}", templateName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_NOT_FOUND"),
              ErrorCategory.VALIDATION,
              Severity.ERROR,
              "Template not found: " + templateName));

    } catch (TemplateEngineException e) {
      logger.error("Template processing error: {}", templateName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_PROCESSING_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to process template: " + e.getMessage()));

    } catch (Exception e) {
      logger.error("Unexpected error rendering template: {}", templateName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_RENDER_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error rendering template: " + e.getMessage()));
    }
  }

  @Override
  public Result<TemplateResult> renderString(
      String templateContent, TemplateContext templateContext) {
    try {
      Context context = createThymeleafContext(templateContext);
      String content = stringTemplateEngine.process(templateContent, context);

      TemplateResult result =
          new TemplateResult(
              "inline-template", content, getContentType(), configuration.getCharset());

      logger.debug("String template rendered successfully");
      return Result.ok(result);

    } catch (TemplateEngineException e) {
      logger.error("String template processing error", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_PROCESSING_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to process string template: " + e.getMessage()));

    } catch (Exception e) {
      logger.error("Unexpected error rendering string template", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("TEMPLATE_RENDER_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error rendering string template: " + e.getMessage()));
    }
  }

  @Override
  public boolean exists(String templateName) {
    try {
      // Try to resolve template without actually processing it
      ITemplateResolver resolver = createFileResolver();
      return templateEngine.getConfiguration().getTemplateResolvers().stream()
          .anyMatch(
              r ->
                  r.resolveTemplate(templateEngine.getConfiguration(), null, templateName, null)
                      != null);
    } catch (Exception e) {
      logger.debug("Template existence check failed: {}", templateName);
      return false;
    }
  }

  @Override
  public void clearCache() {
    templateEngine.clearTemplateCache();
    logger.info("Template cache cleared");
  }

  @Override
  public void clearCache(String templateName) {
    templateEngine.clearTemplateCacheFor(templateName);
    logger.debug("Template cache cleared for: {}", templateName);
  }

  private TemplateEngine createTemplateEngine(ITemplateResolver resolver) {
    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

  private ITemplateResolver createFileResolver() {
    String prefix = configuration.getTemplatePrefix();

    if (prefix.startsWith("classpath:")) {
      ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
      resolver.setPrefix(prefix.substring("classpath:".length()));
      resolver.setSuffix(configuration.getTemplateSuffix());
      resolver.setTemplateMode(configuration.getTemplateMode());
      resolver.setCharacterEncoding(configuration.getCharset().name());
      resolver.setCacheable(configuration.isCacheable());
      if (configuration.getCacheableTTLMs() != null) {
        resolver.setCacheTTLMs(configuration.getCacheableTTLMs());
      }
      return resolver;

    } else if (prefix.startsWith("file:")) {
      FileTemplateResolver resolver = new FileTemplateResolver();
      resolver.setPrefix(prefix.substring("file:".length()));
      resolver.setSuffix(configuration.getTemplateSuffix());
      resolver.setTemplateMode(configuration.getTemplateMode());
      resolver.setCharacterEncoding(configuration.getCharset().name());
      resolver.setCacheable(configuration.isCacheable());
      if (configuration.getCacheableTTLMs() != null) {
        resolver.setCacheTTLMs(configuration.getCacheableTTLMs());
      }
      return resolver;

    } else {
      // Default to classpath
      ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
      resolver.setPrefix(prefix);
      resolver.setSuffix(configuration.getTemplateSuffix());
      resolver.setTemplateMode(configuration.getTemplateMode());
      resolver.setCharacterEncoding(configuration.getCharset().name());
      resolver.setCacheable(configuration.isCacheable());
      if (configuration.getCacheableTTLMs() != null) {
        resolver.setCacheTTLMs(configuration.getCacheableTTLMs());
      }
      return resolver;
    }
  }

  private ITemplateResolver createStringResolver() {
    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode(configuration.getTemplateMode());
    resolver.setCacheable(false); // String templates are not cached
    return resolver;
  }

  private Context createThymeleafContext(TemplateContext templateContext) {
    Context context = new Context(templateContext.locale());
    context.setVariables(templateContext.variables());
    return context;
  }

  private String getContentType() {
    return switch (configuration.getTemplateMode()) {
      case HTML -> "text/html";
      case XML -> "application/xml";
      case TEXT -> "text/plain";
      case JAVASCRIPT -> "application/javascript";
      case CSS -> "text/css";
      case RAW -> "application/octet-stream";
    };
  }
}
