package com.marcusprado02.commons.ports.queue;

import java.util.ArrayList;
import java.util.List;

/** Result of sending messages in a batch operation. */
public record BatchSendResult(List<SendMessageResult> successful, List<BatchFailure> failed) {

  public BatchSendResult {
    successful = new ArrayList<>(successful);
    failed = new ArrayList<>(failed);
  }

  public int successCount() {
    return successful.size();
  }

  public int failureCount() {
    return failed.size();
  }

  public boolean hasFailures() {
    return !failed.isEmpty();
  }

  public record BatchFailure(String id, String code, String message) {}
}
