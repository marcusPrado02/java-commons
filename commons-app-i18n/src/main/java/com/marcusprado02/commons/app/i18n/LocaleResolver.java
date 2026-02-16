package com.marcusprado02.commons.app.i18n;

import java.util.List;
import java.util.Locale;

/**
 * Strategy interface for resolving a user's locale from various sources.
 *
 * <p>Implementations can extract locale from HTTP headers, cookies, session, query parameters, or
 * user preferences.
 *
 * <p>Example implementations:
 *
 * <pre>{@code
 * // Accept-Language header resolver
 * LocaleResolver headerResolver = request -> parseAcceptLanguage(request.getHeader("Accept-Language"));
 *
 * // Cookie-based resolver
 * LocaleResolver cookieResolver = request -> parseCookie(request.getCookie("locale"));
 * }</pre>
 *
 * @param <T> the type of request object (e.g., HttpServletRequest, ServerWebExchange)
 * @author Marcus Prado
 * @since 1.0.0
 */
@FunctionalInterface
public interface LocaleResolver<T> {

  /**
   * Resolves the locale from the given request.
   *
   * @param request the request object
   * @return the resolved locale, or null if cannot be determined
   */
  Locale resolve(T request);

  /**
   * Creates a composite locale resolver that tries multiple strategies in order.
   *
   * @param resolvers list of resolvers to try in order
   * @param <T> the request type
   * @return composite resolver
   */
  static <T> LocaleResolver<T> composite(List<LocaleResolver<T>> resolvers) {
    return request -> {
      for (LocaleResolver<T> resolver : resolvers) {
        Locale locale = resolver.resolve(request);
        if (locale != null) {
          return locale;
        }
      }
      return null;
    };
  }

  /**
   * Creates a locale resolver with a fallback.
   *
   * @param primary the primary resolver
   * @param fallback the fallback locale to use if primary returns null
   * @param <T> the request type
   * @return resolver with fallback
   */
  static <T> LocaleResolver<T> withFallback(LocaleResolver<T> primary, Locale fallback) {
    return request -> {
      Locale locale = primary.resolve(request);
      return locale != null ? locale : fallback;
    };
  }
}
