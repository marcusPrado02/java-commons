package com.marcusprado02.commons.app.validation;

/**
 * Severity level of a validation violation.
 *
 * <p>Allows distinguishing between errors that must be fixed and warnings that should be addressed.
 */
public enum ViolationSeverity {

  /** Error that must be fixed before proceeding. */
  ERROR,

  /** Warning that should be addressed but doesn't prevent proceeding. */
  WARNING,

  /** Informational message about validation. */
  INFO
}
