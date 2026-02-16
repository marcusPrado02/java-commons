package com.marcusprado02.commons.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AcceptLanguageParserTest {

  @Test
  void parse_withSingleLocale_shouldReturnOne() {
    List<Locale> locales = AcceptLanguageParser.parse("en-US");

    assertThat(locales).hasSize(1);
    assertThat(locales.get(0)).isEqualTo(new Locale("en", "US"));
  }

  @Test
  void parse_withMultipleLocales_shouldReturnOrdered() {
    List<Locale> locales = AcceptLanguageParser.parse("fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");

    assertThat(locales).hasSize(4);
    assertThat(locales.get(0)).isEqualTo(new Locale("fr", "FR"));
    assertThat(locales.get(1)).isEqualTo(new Locale("fr"));
    assertThat(locales.get(2)).isEqualTo(new Locale("en", "US"));
    assertThat(locales.get(3)).isEqualTo(new Locale("en"));
  }

  @Test
  void parseFirst_shouldReturnHighestQuality() {
    Locale locale = AcceptLanguageParser.parseFirst("en;q=0.8,fr;q=0.9");

    assertThat(locale).isEqualTo(new Locale("fr"));
  }

  @Test
  void parse_withEmptyString_shouldReturnEmpty() {
    List<Locale> locales = AcceptLanguageParser.parse("");

    assertThat(locales).isEmpty();
  }

  @Test
  void parse_withNull_shouldReturnEmpty() {
    List<Locale> locales = AcceptLanguageParser.parse(null);

    assertThat(locales).isEmpty();
  }
}
