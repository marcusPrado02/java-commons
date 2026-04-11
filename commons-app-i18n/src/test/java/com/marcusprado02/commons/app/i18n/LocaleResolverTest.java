package com.marcusprado02.commons.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocaleResolverTest {

  @Test
  void composite_returnsFirstNonNullLocale() {
    LocaleResolver<String> first = req -> null;
    LocaleResolver<String> second = req -> Locale.FRENCH;

    LocaleResolver<String> composite = LocaleResolver.composite(List.of(first, second));

    assertThat(composite.resolve("req")).isEqualTo(Locale.FRENCH);
  }

  @Test
  void composite_returnsNullWhenAllResolversReturnNull() {
    LocaleResolver<String> resolver = LocaleResolver.composite(List.of(req -> null));

    assertThat(resolver.resolve("req")).isNull();
  }

  @Test
  void withFallback_returnsPrimaryLocaleWhenNonNull() {
    LocaleResolver<String> primary = req -> Locale.GERMAN;
    LocaleResolver<String> resolver = LocaleResolver.withFallback(primary, Locale.ENGLISH);

    assertThat(resolver.resolve("req")).isEqualTo(Locale.GERMAN);
  }

  @Test
  void withFallback_returnsFallbackWhenPrimaryReturnsNull() {
    LocaleResolver<String> primary = req -> null;
    LocaleResolver<String> resolver = LocaleResolver.withFallback(primary, Locale.ENGLISH);

    assertThat(resolver.resolve("req")).isEqualTo(Locale.ENGLISH);
  }
}
