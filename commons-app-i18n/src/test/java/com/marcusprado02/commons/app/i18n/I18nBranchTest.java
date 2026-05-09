package com.marcusprado02.commons.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Branch/line coverage tests for the commons-app-i18n module. */
class I18nBranchTest {

  // -----------------------------------------------------------------------
  // AcceptLanguageParser
  // -----------------------------------------------------------------------

  @Test
  void parse_withBlankString_shouldReturnEmpty() {
    assertThat(AcceptLanguageParser.parse("   ")).isEmpty();
  }

  @Test
  void parse_withEmptyPartsAfterSplit_shouldSkipThem() {
    // A header that produces empty tokens after split+trim (comma with nothing between)
    List<Locale> locales = AcceptLanguageParser.parse("en,,fr");
    assertThat(locales).hasSize(2);
  }

  @Test
  void parse_withInvalidQualityValue_shouldDefaultToOne() {
    // "abc" is not a valid double — exercises the NumberFormatException catch branch
    List<Locale> locales = AcceptLanguageParser.parse("en;q=abc,fr;q=0.5");
    // en gets quality 1.0 (fallback), fr gets 0.5 → en first
    assertThat(locales.get(0).getLanguage()).isEqualTo("en");
  }

  @Test
  void parse_withQualityAboveOne_shouldClampToOne() {
    // quality > 1.0 should be clamped to 1.0
    List<Locale> locales = AcceptLanguageParser.parse("de;q=1.5,fr;q=0.9");
    assertThat(locales.get(0).getLanguage()).isEqualTo("de");
  }

  @Test
  void parse_withQualityClampedToMax_orderStillCorrect() {
    // en gets q=1.5 clamped to 1.0, fr gets q=0.9 → en still first
    List<Locale> locales = AcceptLanguageParser.parse("en;q=1.5,fr;q=0.9");
    assertThat(locales.get(0).getLanguage()).isEqualTo("en");
    assertThat(locales.get(1).getLanguage()).isEqualTo("fr");
  }

  @Test
  void parseFirst_withEmptyHeader_shouldReturnNull() {
    Locale result = AcceptLanguageParser.parseFirst("");
    assertThat(result).isNull();
  }

  // -----------------------------------------------------------------------
  // LocaleContext — null-argument guards
  // -----------------------------------------------------------------------

  @AfterEach
  void clearLocaleContext() {
    LocaleContext.clear();
  }

  @Test
  void setLocale_withNull_shouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> LocaleContext.setLocale(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
  }

  @Test
  void setDefaultLocale_withNull_shouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> LocaleContext.setDefaultLocale(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
  }

  // -----------------------------------------------------------------------
  // LocaleFormatter — null inputs and null-locale constructor
  // -----------------------------------------------------------------------

  @Test
  void constructor_withNullLocale_shouldFallBackToSystemDefault() {
    LocaleFormatter formatter = new LocaleFormatter(null);
    assertThat(formatter.getLocale()).isEqualTo(Locale.getDefault());
  }

  @Test
  void formatCurrency_withNullAmount_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatCurrency((BigDecimal) null, "USD")).isEmpty();
  }

  @Test
  void formatCurrency_withNullCurrencyCode_shouldUseLocaleDefault() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    // currencyCode == null → skip setCurrency branch
    String result = formatter.formatCurrency(BigDecimal.TEN, null);
    assertThat(result).isNotEmpty();
  }

  @Test
  void formatNumber_withNullNumber_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatNumber((Number) null)).isEmpty();
  }

  @Test
  void formatNumber_withDecimals_andNullNumber_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatNumber(null, 2, 4)).isEmpty();
  }

  @Test
  void formatDate_withNullDate_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDate((LocalDate) null)).isEmpty();
  }

  @Test
  void formatDate_withNullDateAndStyle_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDate(null, FormatStyle.MEDIUM)).isEmpty();
  }

  @Test
  void formatDateTime_withNullLocalDateTime_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDateTime((LocalDateTime) null)).isEmpty();
  }

  @Test
  void formatDateTime_withNullLocalDateTimeAndStyles_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDateTime((LocalDateTime) null, FormatStyle.SHORT, FormatStyle.SHORT))
        .isEmpty();
  }

  @Test
  void formatDateTime_withNullLocalDateTimeAndPattern_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDateTime((LocalDateTime) null, "yyyy-MM-dd")).isEmpty();
  }

  @Test
  void formatDateTime_withZonedDateTime_shouldFormatNonEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    ZonedDateTime zdt = ZonedDateTime.of(2026, 2, 16, 10, 30, 0, 0, ZoneId.of("UTC"));
    assertThat(formatter.formatDateTime(zdt)).isNotEmpty();
  }

  @Test
  void formatDateTime_withNullZonedDateTime_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDateTime((ZonedDateTime) null)).isEmpty();
  }

  @Test
  void formatDateTime_withNullZonedDateTimeAndStyles_shouldReturnEmpty() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    assertThat(formatter.formatDateTime((ZonedDateTime) null, FormatStyle.SHORT, FormatStyle.SHORT))
        .isEmpty();
  }

  @Test
  void formatDateTime_withCustomPattern_shouldFormatCorrectly() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 10, 30, 0);
    assertThat(formatter.formatDateTime(dt, "yyyy-MM-dd")).isEqualTo("2026-02-16");
  }

  // -----------------------------------------------------------------------
  // ResourceBundleMessageSource — uncovered branches
  // -----------------------------------------------------------------------

  @Test
  void getMessage_withNullKey_shouldReturnFailResult() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    Result<String> result = src.getMessage(null, Locale.ENGLISH);
    assertThat(result.isOk()).isFalse();
  }

  @Test
  void getMessage_withBlankKey_shouldReturnFailResult() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    Result<String> result = src.getMessage("   ", Locale.ENGLISH);
    assertThat(result.isOk()).isFalse();
  }

  @Test
  void getMessage_withNullLocale_shouldFallBackToSystemDefault() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    // "welcome" exists in the default English bundle; null locale should fall back
    Result<String> result = src.getMessage("welcome", null);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("Welcome!");
  }

  @Test
  void getMessage_withParamsThatDoNotMatchAnyPlaceholder_shouldReturnMessageUnchanged() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    // "welcome" is "Welcome!" which has no placeholders → params map non-empty but ignored
    Result<String> result = src.getMessage("welcome", Locale.ENGLISH, Map.of("unused", "value"));
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("Welcome!");
  }

  @Test
  void hasMessage_withNullKey_shouldReturnFalse() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    assertThat(src.hasMessage(null, Locale.ENGLISH)).isFalse();
  }

  @Test
  void hasMessage_withNullLocale_shouldReturnFalse() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    assertThat(src.hasMessage("welcome", null)).isFalse();
  }

  @Test
  void constructor_withCacheDisabled_shouldLoadBundleWithoutCache() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages", false);
    Result<String> result = src.getMessage("welcome", Locale.ENGLISH);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("Welcome!");
  }

  @Test
  void reload_withCacheDisabled_shouldSucceedWithoutNpe() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages", false);
    Result<Void> result = src.reload();
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void getPluralMessage_forFrenchWithCountOne_shouldReturnSingularForm() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    // French locale (non-English) with count=1 → key + ".one"
    Result<String> result = src.getPluralMessage("items", 1, Locale.FRENCH);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("1");
  }

  @Test
  void getPluralMessage_forFrenchWithCountOther_shouldReturnPluralForm() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    // French locale (non-English) with count=5 → key + ".other"
    Result<String> result = src.getPluralMessage("items", 5, Locale.FRENCH);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("5");
  }

  @Test
  void getPluralMessage_forFrenchWithCountZero_whenZeroKeyExists_shouldReturnZeroForm() {
    // "items.zero" exists in test-messages_fr.properties
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    Result<String> result = src.getPluralMessage("items", 0, Locale.FRENCH);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("0");
  }

  @Test
  void getPluralMessage_forFrenchWithCountTwo_whenTwoKeyExists_shouldReturnTwoForm() {
    // "items.two" exists in test-messages_fr.properties
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    Result<String> result = src.getPluralMessage("items", 2, Locale.FRENCH);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("2");
  }

  @Test
  void getBaseName_shouldReturnConfiguredName() {
    ResourceBundleMessageSource src = new ResourceBundleMessageSource("test-messages");
    assertThat(src.getBaseName()).isEqualTo("test-messages");
  }

  // -----------------------------------------------------------------------
  // LocaleResolver — composite with empty list
  // -----------------------------------------------------------------------

  @Test
  void composite_withEmptyResolverList_shouldReturnNull() {
    LocaleResolver<String> composite = LocaleResolver.composite(List.of());
    assertThat(composite.resolve("req")).isNull();
  }
}
