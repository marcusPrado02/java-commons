package com.marcusprado02.commons.ports.sms;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a phone number.
 *
 * <p>Supports various formats:
 *
 * <ul>
 *   <li>E.164 format: +1234567890
 *   <li>National format with country code: +55 11 98765-4321
 *   <li>International dialing: +44 20 7946 0958
 * </ul>
 *
 * <p>The phone number is internally normalized to E.164 format for consistency.
 */
public record PhoneNumber(String value, String countryCode, String nationalNumber) {

  private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
  private static final Pattern PHONE_DIGITS_PATTERN = Pattern.compile("[^\\d+]");

  public PhoneNumber {
    Objects.requireNonNull(value, "phone number value cannot be null");

    if (value.isBlank()) {
      throw new IllegalArgumentException("phone number cannot be blank");
    }

    // Normalize the phone number
    String normalized = normalizePhoneNumber(value);
    if (!E164_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "Invalid phone number format: " + value + " (normalized: " + normalized + ")");
    }

    // Extract country code and national number from normalized format
    if (countryCode == null || nationalNumber == null) {
      String[] parts = parseE164(normalized);
      countryCode = parts[0];
      nationalNumber = parts[1];
    }

    // Update value to normalized format
    value = normalized;
  }

  /**
   * Creates a PhoneNumber from a string representation.
   *
   * @param phoneNumber the phone number string
   * @return PhoneNumber instance
   * @throws IllegalArgumentException if format is invalid
   */
  public static PhoneNumber of(String phoneNumber) {
    return new PhoneNumber(phoneNumber, null, null);
  }

  /**
   * Creates a PhoneNumber with explicit country code and national number.
   *
   * @param countryCode the country code (e.g., "1", "55", "44")
   * @param nationalNumber the national number without country code
   * @return PhoneNumber instance
   */
  public static PhoneNumber of(String countryCode, String nationalNumber) {
    Objects.requireNonNull(countryCode, "country code cannot be null");
    Objects.requireNonNull(nationalNumber, "national number cannot be null");

    String e164 = "+" + countryCode + nationalNumber.replaceAll("[^\\d]", "");
    return new PhoneNumber(e164, countryCode, nationalNumber.replaceAll("[^\\d]", ""));
  }

  /**
   * Normalizes phone number to E.164 format.
   *
   * @param input the input phone number
   * @return normalized E.164 format
   */
  private static String normalizePhoneNumber(String input) {
    // Remove all whitespace, parentheses, dashes, and dots
    String cleaned = PHONE_DIGITS_PATTERN.matcher(input).replaceAll("");

    // Ensure it starts with +
    if (!cleaned.startsWith("+")) {
      // If it doesn't start with +, assume it needs a + prefix
      // This is a simplification - real implementation should handle country-specific rules
      if (cleaned.length() >= 10) {
        cleaned = "+" + cleaned;
      } else {
        throw new IllegalArgumentException("Phone number too short: " + input);
      }
    }

    return cleaned;
  }

  /**
   * Parses an E.164 number into country code and national number.
   *
   * @param e164 the E.164 formatted number
   * @return array with [countryCode, nationalNumber]
   */
  private static String[] parseE164(String e164) {
    // Remove the + prefix
    String digits = e164.substring(1);

    // Simple country code extraction (this is simplified)
    // Real implementation should use a proper phone number library like libphonenumber
    String countryCode;
    String nationalNumber;

    if (digits.startsWith("1")) {
      // North America (US, Canada)
      countryCode = "1";
      nationalNumber = digits.substring(1);
    } else if (digits.startsWith("55")) {
      // Brazil
      countryCode = "55";
      nationalNumber = digits.substring(2);
    } else if (digits.startsWith("44")) {
      // UK
      countryCode = "44";
      nationalNumber = digits.substring(2);
    } else if (digits.startsWith("49")) {
      // Germany
      countryCode = "49";
      nationalNumber = digits.substring(2);
    } else if (digits.length() >= 3) {
      // Default: assume 2-digit country code for others
      countryCode = digits.substring(0, 2);
      nationalNumber = digits.substring(2);
    } else {
      throw new IllegalArgumentException("Cannot parse country code from: " + e164);
    }

    return new String[] {countryCode, nationalNumber};
  }

  /**
   * Returns the phone number in E.164 format.
   *
   * @return E.164 formatted phone number
   */
  public String toE164() {
    return value;
  }

  /**
   * Returns a formatted phone number for display.
   *
   * @return formatted phone number
   */
  public String toDisplayFormat() {
    // Simple formatting based on country code
    if ("1".equals(countryCode)) {
      // US/Canada format: +1 (555) 123-4567
      if (nationalNumber.length() == 10) {
        return String.format(
            "+%s (%s) %s-%s",
            countryCode,
            nationalNumber.substring(0, 3),
            nationalNumber.substring(3, 6),
            nationalNumber.substring(6));
      }
    } else if ("55".equals(countryCode)) {
      // Brazil format: +55 (11) 98765-4321
      if (nationalNumber.length() >= 10) {
        String areaCode = nationalNumber.substring(0, 2);
        String number = nationalNumber.substring(2);
        if (number.length() == 9) {
          return String.format(
              "+%s (%s) %s-%s", countryCode, areaCode, number.substring(0, 5), number.substring(5));
        }
      }
    }

    // Default format: +CC NNN-NNN-NNNN
    return "+" + countryCode + " " + nationalNumber;
  }

  @Override
  public String toString() {
    return toE164();
  }
}
