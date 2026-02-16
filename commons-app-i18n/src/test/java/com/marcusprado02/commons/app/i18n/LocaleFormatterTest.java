package com.marcusprado02.commons.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocaleFormatterTest {

  @Test
  void formatCurrency_withUS_shouldFormatCorrectly() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);

    String formatted = formatter.formatCurrency(99.99, "USD");

    assertThat(formatted).isEqualTo("$99.99");
  }

  @Test
  void formatCurrency_withFrance_shouldFormatCorrectly() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.FRANCE);

    String formatted = formatter.formatCurrency(99.99, "EUR");

    assertThat(formatted).contains("99,99").contains("€");
  }

  @Test
  void formatNumber_withUS_shouldUseComma() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);

    String formatted = formatter.formatNumber(1234567.89);

    assertThat(formatted).isEqualTo("1,234,567.89");
  }

  @Test
  void formatNumber_withFrance_shouldUseSpace() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.FRANCE);

    String formatted = formatter.formatNumber(1234567.89);

    // French uses non-breaking space and comma
    assertThat(formatted).contains("234").contains("567");
  }

  @Test
  void formatPercent_shouldFormatCorrectly() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);

    String formatted = formatter.formatPercent(0.15);

    assertThat(formatted).isEqualTo("15%");
  }

  @Test
  void formatDate_shouldFormatInShortStyle() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    LocalDate date = LocalDate.of(2026, 2, 16);

    String formatted = formatter.formatDate(date);

    assertThat(formatted).contains("2/16/26");
  }

  @Test
  void formatDate_withMediumStyle_shouldFormatVerbose() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    LocalDate date = LocalDate.of(2026, 2, 16);

    String formatted = formatter.formatDate(date, FormatStyle.MEDIUM);

    assertThat(formatted).contains("Feb").contains("16").contains("2026");
  }

  @Test
  void formatDateTime_shouldFormatBothDateAndTime() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);
    LocalDateTime dateTime = LocalDateTime.of(2026, 2, 16, 15, 30);

    String formatted = formatter.formatDateTime(dateTime);

    assertThat(formatted).isNotEmpty();
  }

  @Test
  void formatNumber_withDecimals_shouldRespectPrecision() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);

    String formatted = formatter.formatNumber(123.456789, 2, 2);

    assertThat(formatted).isEqualTo("123.46");
  }
}
