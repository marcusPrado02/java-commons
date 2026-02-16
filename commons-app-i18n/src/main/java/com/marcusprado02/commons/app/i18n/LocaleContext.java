package com.marcusprado02.commons.app.i18n;

import java.util.Locale;
import java.util.Optional;

/**
 * Thread-local context for managing the current user's locale.
 *
 * <p>Provides a mechanism to set and retrieve locale information in a thread-safe manner, typically
 * used in web applications where each request has its own locale.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Set locale for current thread
 * LocaleContext.setLocale(Locale.FRENCH);
 *
 * // Get current locale (or default)
 * Locale locale = LocaleContext.getLocale();
 *
 * // Clear after request processing
 * LocaleContext.clear();
 * }</pre>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public final class LocaleContext {

  private static final ThreadLocal<Locale> LOCALE_HOLDER = new ThreadLocal<>();
  private static volatile Locale defaultLocale = Locale.getDefault();

  private LocaleContext() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Sets the locale for the current thread.
   *
   * @param locale the locale to set
   * @throws IllegalArgumentException if locale is null
   */
  public static void setLocale(Locale locale) {
    if (locale == null) {
      throw new IllegalArgumentException("Locale cannot be null");
    }
    LOCALE_HOLDER.set(locale);
  }

  /**
   * Gets the locale for the current thread.
   *
   * <p>If no locale has been set for the current thread, returns the default locale.
   *
   * @return the current thread's locale or default locale
   */
  public static Locale getLocale() {
    Locale locale = LOCALE_HOLDER.get();
    return locale != null ? locale : defaultLocale;
  }

  /**
   * Gets the locale for the current thread as an Optional.
   *
   * @return optional containing the locale if set, empty otherwise
   */
  public static Optional<Locale> getLocaleIfPresent() {
    return Optional.ofNullable(LOCALE_HOLDER.get());
  }

  /**
   * Clears the locale for the current thread.
   *
   * <p>Should be called after request processing to prevent memory leaks.
   */
  public static void clear() {
    LOCALE_HOLDER.remove();
  }

  /**
   * Sets the global default locale used when no thread-specific locale is set.
   *
   * @param locale the default locale
   * @throws IllegalArgumentException if locale is null
   */
  public static void setDefaultLocale(Locale locale) {
    if (locale == null) {
      throw new IllegalArgumentException("Default locale cannot be null");
    }
    defaultLocale = locale;
  }

  /**
   * Gets the global default locale.
   *
   * @return the default locale
   */
  public static Locale getDefaultLocale() {
    return defaultLocale;
  }
}
