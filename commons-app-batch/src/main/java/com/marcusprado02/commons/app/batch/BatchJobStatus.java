package com.marcusprado02.commons.app.batch;

/** Enumeration of batch job execution statuses. */
public enum BatchJobStatus {
  /** Job is starting up and initializing. */
  STARTING,

  /** Job is currently running and processing items. */
  RUNNING,

  /** Job completed successfully. */
  COMPLETED,

  /** Job failed with an error. */
  FAILED,

  /** Job was stopped by user request. */
  STOPPED,

  /** Job is currently stopping. */
  STOPPING,

  /** Job execution was abandoned (e.g., server shutdown). */
  ABANDONED,

  /** Job status is unknown or cannot be determined. */
  UNKNOWN
}
