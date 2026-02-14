package com.marcusprado02.commons.kernel.errors;

import java.util.Locale;

/**
 * Interface for resolving internationalized error messages.
 *
 * <p>Implementations can integrate with Spring MessageSource, ResourceBundles, or custom i18n
 * systems.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * I18nMessageResolver resolver = new ResourceBundleMessageResolver("messages/errors");
 * String message = resolver.resolve("user.not.found", Locale.US, "User not found");
 * }</pre>
 */
@FunctionalInterface
public interface I18nMessageResolver {

  /**
   * Resolves a message for the given key and locale.
   *
   * @param key the message key
   * @param locale the locale
   * @param defaultMessage the default message if key is not found
   * @param args arguments for message placeholders
   * @return the resolved message
   */
  String resolve(String key, Locale locale, String defaultMessage, Object... args);

  /**
   * Resolves a message using the default locale.
   *
   * @param key the message key
   * @param defaultMessage the default message
   * @param args arguments for message placeholders
   * @return the resolved message
   */
  default String resolve(String key, String defaultMessage, Object... args) {
    return resolve(key, Locale.getDefault(), defaultMessage, args);
  }

  /**
   * Checks if a message exists for the given key and locale.
   *
   * @param key the message key
   * @param locale the locale
   * @return true if message exists
   */
  default boolean hasMessage(String key, Locale locale) {
    return !resolve(key, locale, null).equals(key);
  }

  /**
   * Creates a no-op resolver that always returns the default message.
   *
   * @return a no-op resolver
   */
  static I18nMessageResolver noOp() {
    return (key, locale, defaultMessage, args) -> defaultMessage != null ? defaultMessage : key;
  }
}
