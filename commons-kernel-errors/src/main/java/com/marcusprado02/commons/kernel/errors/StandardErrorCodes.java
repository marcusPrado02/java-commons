package com.marcusprado02.commons.kernel.errors;

/**
 * Standard error codes following a hierarchical naming convention.
 *
 * <p>Format: DOMAIN.CATEGORY.SPECIFIC_ERROR Example: USER.VALIDATION.EMAIL_INVALID
 *
 * <p>This class provides commonly used error codes across the commons library. Applications should
 * extend this pattern for domain-specific codes.
 */
public final class StandardErrorCodes {

  private StandardErrorCodes() {}

  // Validation Errors
  public static final ErrorCode VALIDATION_FAILED = ErrorCode.of("VALIDATION.FAILED");
  public static final ErrorCode VALIDATION_REQUIRED_FIELD =
      ErrorCode.of("VALIDATION.REQUIRED_FIELD");
  public static final ErrorCode VALIDATION_INVALID_FORMAT =
      ErrorCode.of("VALIDATION.INVALID_FORMAT");
  public static final ErrorCode VALIDATION_OUT_OF_RANGE = ErrorCode.of("VALIDATION.OUT_OF_RANGE");
  public static final ErrorCode VALIDATION_CONSTRAINT_VIOLATION =
      ErrorCode.of("VALIDATION.CONSTRAINT_VIOLATION");

  // Business Errors
  public static final ErrorCode BUSINESS_RULE_VIOLATION = ErrorCode.of("BUSINESS.RULE_VIOLATION");
  public static final ErrorCode BUSINESS_INVALID_STATE = ErrorCode.of("BUSINESS.INVALID_STATE");
  public static final ErrorCode BUSINESS_OPERATION_NOT_ALLOWED =
      ErrorCode.of("BUSINESS.OPERATION_NOT_ALLOWED");
  public static final ErrorCode BUSINESS_DUPLICATE = ErrorCode.of("BUSINESS.DUPLICATE");

  // Not Found Errors
  public static final ErrorCode NOT_FOUND_ENTITY = ErrorCode.of("NOT_FOUND.ENTITY");
  public static final ErrorCode NOT_FOUND_RESOURCE = ErrorCode.of("NOT_FOUND.RESOURCE");

  // Conflict Errors
  public static final ErrorCode CONFLICT_VERSION = ErrorCode.of("CONFLICT.VERSION");
  public static final ErrorCode CONFLICT_CONCURRENT_MODIFICATION =
      ErrorCode.of("CONFLICT.CONCURRENT_MODIFICATION");
  public static final ErrorCode CONFLICT_DUPLICATE_ENTRY = ErrorCode.of("CONFLICT.DUPLICATE_ENTRY");

  // Authorization/Authentication  Errors
  public static final ErrorCode UNAUTHORIZED_MISSING_CREDENTIALS =
      ErrorCode.of("UNAUTHORIZED.MISSING_CREDENTIALS");
  public static final ErrorCode UNAUTHORIZED_INVALID_CREDENTIALS =
      ErrorCode.of("UNAUTHORIZED.INVALID_CREDENTIALS");
  public static final ErrorCode UNAUTHORIZED_TOKEN_EXPIRED =
      ErrorCode.of("UNAUTHORIZED.TOKEN_EXPIRED");
  public static final ErrorCode FORBIDDEN_INSUFFICIENT_PERMISSIONS =
      ErrorCode.of("FORBIDDEN.INSUFFICIENT_PERMISSIONS");
  public static final ErrorCode FORBIDDEN_ACCESS_DENIED = ErrorCode.of("FORBIDDEN.ACCESS_DENIED");

  // Technical Errors
  public static final ErrorCode TECHNICAL_DATABASE_ERROR = ErrorCode.of("TECHNICAL.DATABASE_ERROR");
  public static final ErrorCode TECHNICAL_EXTERNAL_SERVICE_ERROR =
      ErrorCode.of("TECHNICAL.EXTERNAL_SERVICE_ERROR");
  public static final ErrorCode TECHNICAL_TIMEOUT = ErrorCode.of("TECHNICAL.TIMEOUT");
  public static final ErrorCode TECHNICAL_INTERNAL_ERROR = ErrorCode.of("TECHNICAL.INTERNAL_ERROR");
  public static final ErrorCode TECHNICAL_CONFIGURATION_ERROR =
      ErrorCode.of("TECHNICAL.CONFIGURATION_ERROR");

  // Integration Errors
  public static final ErrorCode INTEGRATION_COMMUNICATION_FAILURE =
      ErrorCode.of("INTEGRATION.COMMUNICATION_FAILURE");
  public static final ErrorCode INTEGRATION_SERIALIZATION_ERROR =
      ErrorCode.of("INTEGRATION.SERIALIZATION_ERROR");
  public static final ErrorCode INTEGRATION_PROTOCOL_ERROR =
      ErrorCode.of("INTEGRATION.PROTOCOL_ERROR");
}
