package com.marcusprado02.commons.kernel.errors;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Implementation of I18nMessageResolver using Java ResourceBundles.
 *
 * <p>Loads messages from .properties files following the standard ResourceBundle naming convention.
 *
 * <p>Example:
 *
 * <pre>
 * # messages/errors_en.properties
 * user.not.found=User with ID {0} not found
 * validation.email.invalid=Invalid email format: {0}
 *
 * # messages/errors_pt.properties
 * user.not.found=Usuário com ID {0} não encontrado
 * validation.email.invalid=Formato de email inválido: {0}
 * </pre>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * I18nMessageResolver resolver = new ResourceBundleMessageResolver("messages/errors");
 * String message = resolver.resolve(
 *     "user.not.found",
 *     new Locale("pt", "BR"),
 *     "User not found",
 *     "user-123"
 * );
 * // Returns: "Usuário com ID user-123 não encontrado"
 * }</pre>
 */
public final class ResourceBundleMessageResolver implements I18nMessageResolver {

  private final String baseName;

  /**
   * Creates a new ResourceBundleMessageResolver.
   *
   * @param baseName the base name of the resource bundle (e.g., "messages/errors")
   */
  public ResourceBundleMessageResolver(String baseName) {
    this.baseName = baseName;
  }

  @Override
  public String resolve(String key, Locale locale, String defaultMessage, Object... args) {
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
      String pattern = bundle.getString(key);

      if (args != null && args.length > 0) {
        MessageFormat formatter = new MessageFormat(pattern, locale);
        return formatter.format(args);
      }

      return pattern;
    } catch (MissingResourceException e) {
      // Fall back to default message
      if (defaultMessage != null) {
        if (args != null && args.length > 0) {
          MessageFormat formatter = new MessageFormat(defaultMessage, locale);
          return formatter.format(args);
        }
        return defaultMessage;
      }
      return key;
    }
  }

  @Override
  public boolean hasMessage(String key, Locale locale) {
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
      bundle.getString(key);
      return true;
    } catch (MissingResourceException e) {
      return false;
    }
  }
}
