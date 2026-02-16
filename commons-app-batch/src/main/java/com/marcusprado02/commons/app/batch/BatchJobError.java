package com.marcusprado02.commons.app.batch;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ProblemBuilder;
import com.marcusprado02.commons.kernel.errors.Severity;

/** Factory methods for creating batch job errors. */
public final class BatchJobError {

  private BatchJobError() {}

  public static Problem of(String code, String message) {
    return ProblemBuilder.of(code)
        .category(ErrorCategory.TECHNICAL)
        .severity(Severity.ERROR)
        .message(message)
        .build();
  }

  public static Problem of(String code, String message, Throwable cause) {
    return ProblemBuilder.of(code)
        .category(ErrorCategory.TECHNICAL)
        .severity(Severity.ERROR)
        .message(message)
        .detail("cause", cause.getMessage())
        .build();
  }

  // Common batch job error codes
  public static final String JOB_INITIALIZATION_FAILED = "BATCH_JOB_INIT_FAILED";
  public static final String ITEM_READ_FAILED = "BATCH_ITEM_READ_FAILED";
  public static final String ITEM_PROCESS_FAILED = "BATCH_ITEM_PROCESS_FAILED";
  public static final String ITEM_WRITE_FAILED = "BATCH_ITEM_WRITE_FAILED";
  public static final String MAX_SKIPS_EXCEEDED = "BATCH_MAX_SKIPS_EXCEEDED";
  public static final String MAX_RETRIES_EXCEEDED = "BATCH_MAX_RETRIES_EXCEEDED";
  public static final String JOB_INTERRUPTED = "BATCH_JOB_INTERRUPTED";
  public static final String RESTART_FAILED = "BATCH_RESTART_FAILED";
}
