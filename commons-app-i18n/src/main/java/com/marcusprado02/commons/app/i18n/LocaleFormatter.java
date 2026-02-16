package com.marcusprado02.commons.app.i18n;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;

/**
 * Utility for formatting values according to locale-specific conventions.
 *
 * <p>Provides formatters for:
 *
 * <ul>
 *   <li>Currency values with proper symbols and decimals
 *   <li>Dates and times in locale-appropriate formats
 *   <li>Numbers with locale-specific grouping and decimals
 *   <li>Percentages
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * LocaleFormatter formatter = new LocaleFormatter(Locale.FRANCE);
 * String price = formatter.formatCurrency(99.99, "EUR"); // "99,99 €"
 * String date = formatter.formatDate(LocalDate.now()); // "16/02/2026"
 * String number = formatter.formatNumber(1234567.89); // "1 234 567,89"
 * }</pre>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public class LocaleFormatter {

  private final Locale locale;

  /**
   * Creates a formatter for the given locale.
   *
   * @param locale the target locale
   */
  public LocaleFormatter(Locale locale) {
    this.locale = locale != null ? locale : Locale.getDefault();
  }

  /**
   * Formats a currency amount with the given currency code.
   *
   * @param amount the amount to format
   * @param currencyCode the ISO 4217 currency code (e.g., "USD", "EUR")
   * @return formatted currency string
   */
  public String formatCurrency(BigDecimal amount, String currencyCode) {
    if (amount == null) {
      return "";
    }

    NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
    if (currencyCode != null) {
      formatter.setCurrency(Currency.getInstance(currencyCode));
    }

    return formatter.format(amount);
  }

  /**
   * Formats a currency amount.
   *
   * @param amount the amount to format
   * @param currencyCode the currency code
   * @return formatted currency string
   */
  public String formatCurrency(double amount, String currencyCode) {
    return formatCurrency(BigDecimal.valueOf(amount), currencyCode);
  }

  /**
   * Formats a number with locale-specific grouping and decimals.
   *
   * @param number the number to format
   * @return formatted number string
   */
  public String formatNumber(Number number) {
    if (number == null) {
      return "";
    }

    NumberFormat formatter = NumberFormat.getNumberInstance(locale);
    return formatter.format(number);
  }

  /**
   * Formats a number with specific decimal places.
   *
   * @param number the number to format
   * @param minDecimals minimum decimal places
   * @param maxDecimals maximum decimal places
   * @return formatted number string
   */
  public String formatNumber(Number number, int minDecimals, int maxDecimals) {
    if (number == null) {
      return "";
    }

    NumberFormat formatter = NumberFormat.getNumberInstance(locale);
    formatter.setMinimumFractionDigits(minDecimals);
    formatter.setMaximumFractionDigits(maxDecimals);

    return formatter.format(number);
  }

  /**
   * Formats a percentage value.
   *
   * @param value the value to format (0.15 for 15%)
   * @return formatted percentage string
   */
  public String formatPercent(double value) {
    NumberFormat formatter = NumberFormat.getPercentInstance(locale);
    return formatter.format(value);
  }

  /**
   * Formats a date in SHORT style.
   *
   * @param date the date to format
   * @return formatted date string
   */
  public String formatDate(LocalDate date) {
    return formatDate(date, FormatStyle.SHORT);
  }

  /**
   * Formats a date with the specified style.
   *
   * @param date the date to format
   * @param style the format style (SHORT, MEDIUM, LONG, FULL)
   * @return formatted date string
   */
  public String formatDate(LocalDate date, FormatStyle style) {
    if (date == null) {
      return "";
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(style).withLocale(locale);
    return date.format(formatter);
  }

  /**
   * Formats a date-time in SHORT style.
   *
   * @param dateTime the date-time to format
   * @return formatted date-time string
   */
  public String formatDateTime(LocalDateTime dateTime) {
    return formatDateTime(dateTime, FormatStyle.SHORT, FormatStyle.SHORT);
  }

  /**
   * Formats a date-time with the specified styles.
   *
   * @param dateTime the date-time to format
   * @param dateStyle the date format style
   * @param timeStyle the time format style
   * @return formatted date-time string
   */
  public String formatDateTime(
      LocalDateTime dateTime, FormatStyle dateStyle, FormatStyle timeStyle) {
    if (dateTime == null) {
      return "";
    }

    DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle).withLocale(locale);
    return dateTime.format(formatter);
  }

  /**
   * Formats a zoned date-time.
   *
   * @param dateTime the zoned date-time to format
   * @return formatted date-time string with timezone
   */
  public String formatDateTime(ZonedDateTime dateTime) {
    return formatDateTime(dateTime, FormatStyle.SHORT, FormatStyle.SHORT);
  }

  /**
   * Formats a zoned date-time with the specified styles.
   *
   * @param dateTime the zoned date-time to format
   * @param dateStyle the date format style
   * @param timeStyle the time format style
   * @return formatted date-time string with timezone
   */
  public String formatDateTime(
      ZonedDateTime dateTime, FormatStyle dateStyle, FormatStyle timeStyle) {
    if (dateTime == null) {
      return "";
    }

    DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle).withLocale(locale);
    return dateTime.format(formatter);
  }

  /**
   * Formats using a custom pattern.
   *
   * @param dateTime the date-time to format
   * @param pattern the custom pattern (e.g., "yyyy-MM-dd HH:mm")
   * @return formatted date-time string
   */
  public String formatDateTime(LocalDateTime dateTime, String pattern) {
    if (dateTime == null) {
      return "";
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
    return dateTime.format(formatter);
  }

  /**
   * Gets the locale being used by this formatter.
   *
   * @return the locale
   */
  public Locale getLocale() {
    return locale;
  }
}
