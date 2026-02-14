package com.marcusprado02.commons.kernel.errors;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for creating common Problem instances.
 *
 * <p>Provides factory methods for quickly creating Problems for common scenarios like validation,
 * business rules, not found, etc.
 */
public final class Problems {

  private static I18nMessageResolver messageResolver = I18nMessageResolver.noOp();

  private Problems() {}

  /**
   * Sets the global I18nMessageResolver for all Problems created through this class.
   *
   * @param resolver the message resolver
   */
  public static void setMessageResolver(I18nMessageResolver resolver) {
    messageResolver = Objects.requireNonNull(resolver, "resolver");
  }

  /**
   * Gets the current message resolver.
   *
   * @return the message resolver
   */
  public static I18nMessageResolver getMessageResolver() {
    return messageResolver;
  }

  // ===== Factory Methods =====

  /**
   * Creates a validation Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return a validation Problem
   */
  public static Problem validation(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.VALIDATION, Severity.ERROR, message);
  }

  /**
   * Creates a validation Problem with I18n support.
   *
   * @param code the error code
   * @param messageKey the i18n message key
   * @param locale the locale
   * @param defaultMessage the default message
   * @param args message arguments
   * @return a validation Problem
   */
  public static Problem validation(
      String code, String messageKey, Locale locale, String defaultMessage, Object... args) {
    String message = messageResolver.resolve(messageKey, locale, defaultMessage, args);
    return validation(code, message);
  }

  /**
   * Creates a business Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return a business Problem
   */
  public static Problem business(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.BUSINESS, Severity.ERROR, message);
  }

  /**
   * Creates a business Problem with I18n support.
   *
   * @param code the error code
   * @param messageKey the i18n message key
   * @param locale the locale
   * @param defaultMessage the default message
   * @param args message arguments
   * @return a business Problem
   */
  public static Problem business(
      String code, String messageKey, Locale locale, String defaultMessage, Object... args) {
    String message = messageResolver.resolve(messageKey, locale, defaultMessage, args);
    return business(code, message);
  }

  /**
   * Creates a not found Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return a not found Problem
   */
  public static Problem notFound(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.NOT_FOUND, Severity.ERROR, message);
  }

  /**
   * Creates a not found Problem with I18n support.
   *
   * @param code the error code
   * @param messageKey the i18n message key
   * @param locale the locale
   * @param defaultMessage the default message
   * @param args message arguments
   * @return a not found Problem
   */
  public static Problem notFound(
      String code, String messageKey, Locale locale, String defaultMessage, Object... args) {
    String message = messageResolver.resolve(messageKey, locale, defaultMessage, args);
    return notFound(code, message);
  }

  /**
   * Creates a conflict Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return a conflict Problem
   */
  public static Problem conflict(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.CONFLICT, Severity.ERROR, message);
  }

  /**
   * Creates a conflict Problem with I18n support.
   *
   * @param code the error code
   * @param messageKey the i18n message key
   * @param locale the locale
   * @param defaultMessage the default message
   * @param args message arguments
   * @return a conflict Problem
   */
  public static Problem conflict(
      String code, String messageKey, Locale locale, String defaultMessage, Object... args) {
    String message = messageResolver.resolve(messageKey, locale, defaultMessage, args);
    return conflict(code, message);
  }

  /**
   * Creates an unauthorized Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return an unauthorized Problem
   */
  public static Problem unauthorized(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.UNAUTHORIZED, Severity.ERROR, message);
  }

  /**
   * Creates a forbidden Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return a forbidden Problem
   */
  public static Problem forbidden(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.FORBIDDEN, Severity.ERROR, message);
  }

  /**
   * Creates a technical Problem.
   *
   * @param code the error code
   * @param message the error message
   * @return a technical Problem
   */
  public static Problem technical(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.TECHNICAL, Severity.CRITICAL, message);
  }

  /**
   * Creates a technical Problem with I18n support.
   *
   * @param code the error code
   * @param messageKey the i18n message key
   * @param locale the locale
   * @param defaultMessage the default message
   * @param args message arguments
   * @return a technical Problem
   */
  public static Problem technical(
      String code, String messageKey, Locale locale, String defaultMessage, Object... args) {
    String message = messageResolver.resolve(messageKey, locale, defaultMessage, args);
    return technical(code, message);
  }

  // ===== Enrichment Methods =====

  /**
   * Adds details and metadata to an existing Problem.
   *
   * @param base the base Problem
   * @param details the problem details
   * @param meta the metadata
   * @return a new Problem with added details and metadata
   */
  public static Problem withDetails(
      Problem base, List<ProblemDetail> details, Map<String, Object> meta) {
    Objects.requireNonNull(base, "base");
    return new Problem(
        base.code(),
        base.category(),
        base.severity(),
        base.message(),
        details,
        meta,
        base.timestamp());
  }

  // ===== Conversion Methods =====

  /**
   * Converts a Problem to RFC 7807 format.
   *
   * @param problem the problem
   * @return RFC 7807 problem detail
   */
  public static RFC7807ProblemDetail toRFC7807(Problem problem) {
    return RFC7807ProblemDetail.from(problem).build();
  }

  /**
   * Converts a Problem to an ErrorEnvelope.
   *
   * @param problem the problem
   * @return error envelope
   */
  public static ErrorEnvelope toEnvelope(Problem problem) {
    return ErrorEnvelope.of(problem);
  }

  /**
   * Converts a Problem to an ErrorEnvelope with context.
   *
   * @param problem the problem
   * @param correlationId the correlation ID
   * @param context the error context
   * @return error envelope
   */
  public static ErrorEnvelope toEnvelope(
      Problem problem, String correlationId, Map<String, Object> context) {
    return new ErrorEnvelope(problem, correlationId, context);
  }
}
