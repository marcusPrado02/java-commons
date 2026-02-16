package com.marcusprado02.commons.app.i18n;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/**
 * Interface for resolving messages from internationalized message bundles.
 *
 * <p>Provides type-safe message resolution with support for parameterization, pluralization, and
 * fallback to default locales.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MessageSource messages = new ResourceBundleMessageSource("messages");
 * Result<String> result = messages.getMessage("welcome.user", locale, Map.of("name", "John"));
 * }</pre>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public interface MessageSource {

  /**
   * Retrieves a message for the given key and locale.
   *
   * @param key the message key
   * @param locale the target locale
   * @return result containing the message or error if not found
   */
  Result<String> getMessage(String key, Locale locale);

  /**
   * Retrieves a message with parameter substitution.
   *
   * @param key the message key
   * @param locale the target locale
   * @param params parameters to substitute in the message (e.g., {name}, {count})
   * @return result containing the formatted message or error if not found
   */
  Result<String> getMessage(String key, Locale locale, Map<String, Object> params);

  /**
   * Retrieves a message with default value if not found.
   *
   * @param key the message key
   * @param locale the target locale
   * @param defaultMessage the default message to return if key not found
   * @return the resolved message or default
   */
  String getMessageOrDefault(String key, Locale locale, String defaultMessage);

  /**
   * Retrieves a pluralized message based on count.
   *
   * @param key the base message key (e.g., "items")
   * @param count the count determining plural form
   * @param locale the target locale
   * @return result containing the pluralized message
   */
  Result<String> getPluralMessage(String key, long count, Locale locale);

  /**
   * Retrieves a pluralized message with parameters.
   *
   * @param key the base message key
   * @param count the count determining plural form
   * @param locale the target locale
   * @param params additional parameters for substitution
   * @return result containing the formatted pluralized message
   */
  Result<String> getPluralMessage(
      String key, long count, Locale locale, Map<String, Object> params);

  /**
   * Checks if a message exists for the given key and locale.
   *
   * @param key the message key
   * @param locale the target locale
   * @return true if message exists, false otherwise
   */
  boolean hasMessage(String key, Locale locale);

  /**
   * Reloads the message bundles from source.
   *
   * <p>Useful for development environments where bundles may change.
   *
   * @return result indicating success or failure
   */
  Result<Void> reload();
}
