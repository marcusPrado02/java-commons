package com.marcusprado02.commons.app.i18n;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageSource implementation backed by Java ResourceBundle.
 *
 * <p>Loads message bundles from classpath using standard Java properties files (e.g.,
 * messages.properties, messages_fr.properties).
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Caching of loaded bundles for performance
 *   <li>Parameter substitution using MessageFormat
 *   <li>Fallback to parent locales
 *   <li>Pluralization support
 * </ul>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public class ResourceBundleMessageSource implements MessageSource {

  private static final Logger logger = LoggerFactory.getLogger(ResourceBundleMessageSource.class);

  private final String baseName;
  private final Map<Locale, ResourceBundle> bundleCache;
  private final boolean cacheEnabled;

  /**
   * Creates a message source with the given base name.
   *
   * @param baseName the base name of the resource bundle (e.g., "messages")
   */
  public ResourceBundleMessageSource(String baseName) {
    this(baseName, true);
  }

  /**
   * Creates a message source with the given base name and cache setting.
   *
   * @param baseName the base name of the resource bundle
   * @param cacheEnabled whether to cache loaded bundles
   */
  public ResourceBundleMessageSource(String baseName, boolean cacheEnabled) {
    this.baseName = Objects.requireNonNull(baseName, "Base name cannot be null");
    this.cacheEnabled = cacheEnabled;
    this.bundleCache = cacheEnabled ? new ConcurrentHashMap<>() : null;
    logger.debug(
        "ResourceBundleMessageSource created for base: {}, cache: {}", baseName, cacheEnabled);
  }

  @Override
  public Result<String> getMessage(String key, Locale locale) {
    return getMessage(key, locale, Collections.emptyMap());
  }

  @Override
  public Result<String> getMessage(String key, Locale locale, Map<String, Object> params) {
    if (key == null || key.isBlank()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("I18N_INVALID_KEY"),
              ErrorCategory.VALIDATION,
              Severity.ERROR,
              "Message key cannot be null or blank"));
    }

    if (locale == null) {
      locale = Locale.getDefault();
    }

    try {
      ResourceBundle bundle = getBundle(locale);
      if (!bundle.containsKey(key)) {
        logger.debug("Message key '{}' not found for locale '{}'", key, locale);
        return Result.fail(
            Problem.of(
                ErrorCode.of("I18N_MESSAGE_NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                String.format("Message not found: key=%s, locale=%s", key, locale)));
      }

      String message = bundle.getString(key);

      if (params != null && !params.isEmpty()) {
        message = formatMessage(message, params);
      }

      return Result.ok(message);

    } catch (MissingResourceException e) {
      logger.warn("Resource bundle not found for locale: {}", locale, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("I18N_BUNDLE_NOT_FOUND"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Resource bundle not found for locale: " + locale));
    }
  }

  @Override
  public String getMessageOrDefault(String key, Locale locale, String defaultMessage) {
    Result<String> result = getMessage(key, locale);
    return result.isOk() ? result.getOrNull() : defaultMessage;
  }

  @Override
  public Result<String> getPluralMessage(String key, long count, Locale locale) {
    return getPluralMessage(key, count, locale, Collections.emptyMap());
  }

  @Override
  public Result<String> getPluralMessage(
      String key, long count, Locale locale, Map<String, Object> params) {
    // Determine plural form key
    String pluralKey = getPluralKey(key, count, locale);

    // Add count to params
    Map<String, Object> allParams = new HashMap<>(params);
    allParams.put("count", count);

    return getMessage(pluralKey, locale, allParams);
  }

  @Override
  public boolean hasMessage(String key, Locale locale) {
    if (key == null || locale == null) {
      return false;
    }

    try {
      ResourceBundle bundle = getBundle(locale);
      return bundle.containsKey(key);
    } catch (MissingResourceException e) {
      return false;
    }
  }

  @Override
  public Result<Void> reload() {
    if (bundleCache != null) {
      bundleCache.clear();
      logger.info("Message bundle cache cleared");
    }
    ResourceBundle.clearCache();
    return Result.ok(null);
  }

  /** Gets or loads the resource bundle for the given locale. */
  private ResourceBundle getBundle(Locale locale) {
    if (cacheEnabled) {
      return bundleCache.computeIfAbsent(
          locale, l -> ResourceBundle.getBundle(baseName, l, getControl()));
    }
    return ResourceBundle.getBundle(baseName, locale, getControl());
  }

  /** Formats a message with parameters. */
  private String formatMessage(String message, Map<String, Object> params) {
    // Replace named parameters {name} with positional {0}, {1}, etc.
    List<String> paramNames = new ArrayList<>();
    String formatted = message;

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String placeholder = "{" + entry.getKey() + "}";
      if (formatted.contains(placeholder)) {
        int index = paramNames.size();
        formatted = formatted.replace(placeholder, "{" + index + "}");
        paramNames.add(entry.getKey());
      }
    }

    // Build args array in correct order
    Object[] args = paramNames.stream().map(params::get).toArray();

    // Use MessageFormat for advanced formatting
    MessageFormat messageFormat = new MessageFormat(formatted, Locale.getDefault());
    return messageFormat.format(args);
  }

  /** Determines the plural form key based on count and locale. */
  private String getPluralKey(String key, long count, Locale locale) {
    // Simple English pluralization rules
    // For more complex languages, use ICU MessageFormat or custom rules

    if (locale.getLanguage().equals("en")) {
      return count == 1 ? key + ".one" : key + ".other";
    }

    // For other languages, try standard plural forms
    // zero, one, two, few, many, other
    if (count == 0 && hasMessage(key + ".zero", locale)) {
      return key + ".zero";
    }
    if (count == 1) {
      return key + ".one";
    }
    if (count == 2 && hasMessage(key + ".two", locale)) {
      return key + ".two";
    }

    return key + ".other";
  }

  /** Gets the ResourceBundle.Control for customization. */
  private ResourceBundle.Control getControl() {
    return ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES);
  }

  /** Gets the base name of this message source. */
  public String getBaseName() {
    return baseName;
  }
}
