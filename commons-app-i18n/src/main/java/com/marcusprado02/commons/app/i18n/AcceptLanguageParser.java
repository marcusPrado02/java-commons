package com.marcusprado02.commons.app.i18n;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for HTTP Accept-Language header.
 *
 * <p>Parses the Accept-Language header and returns locales ordered by quality value (q parameter).
 *
 * <p>Example:
 *
 * <pre>{@code
 * String header = "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7";
 * List<Locale> locales = AcceptLanguageParser.parse(header);
 * // Returns: [fr_FR, fr, en_US, en]
 * }</pre>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public final class AcceptLanguageParser {

  private static final Pattern LOCALE_PATTERN =
      Pattern.compile("([a-z]{2,3})(?:-([a-z]{2,3}))?(?:;q=([0-9.]+))?");

  private AcceptLanguageParser() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Parses the Accept-Language header into a list of locales.
   *
   * <p>Locales are ordered by quality value (highest first).
   *
   * @param acceptLanguage the Accept-Language header value
   * @return list of locales, ordered by preference
   */
  public static List<Locale> parse(String acceptLanguage) {
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return Collections.emptyList();
    }

    List<LocaleWithQuality> locales = new ArrayList<>();

    String[] parts = acceptLanguage.split(",");
    for (String part : parts) {
      part = part.trim();
      if (part.isEmpty()) {
        continue;
      }

      Matcher matcher = LOCALE_PATTERN.matcher(part.toLowerCase(Locale.ROOT));
      if (matcher.find()) {
        String language = matcher.group(1);
        String country = matcher.group(2);
        String qValue = matcher.group(3);

        double quality = qValue != null ? parseQuality(qValue) : 1.0;

        Locale locale;
        if (country != null) {
          locale = new Locale(language, country.toUpperCase(Locale.ROOT));
        } else {
          locale = new Locale(language);
        }

        locales.add(new LocaleWithQuality(locale, quality));
      }
    }

    // Sort by quality (highest first)
    locales.sort(Comparator.comparingDouble(LocaleWithQuality::quality).reversed());

    return locales.stream().map(LocaleWithQuality::locale).toList();
  }

  /**
   * Parses the first (highest quality) locale from Accept-Language header.
   *
   * @param acceptLanguage the Accept-Language header value
   * @return the first locale, or null if none found
   */
  public static Locale parseFirst(String acceptLanguage) {
    List<Locale> locales = parse(acceptLanguage);
    return locales.isEmpty() ? null : locales.get(0);
  }

  private static double parseQuality(String qValue) {
    try {
      double q = Double.parseDouble(qValue);
      return Math.max(0.0, Math.min(1.0, q)); // Clamp between 0 and 1
    } catch (NumberFormatException e) {
      return 1.0;
    }
  }

  private record LocaleWithQuality(Locale locale, double quality) {}
}
