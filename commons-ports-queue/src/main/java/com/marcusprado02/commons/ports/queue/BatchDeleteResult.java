package com.marcusprado02.commons.ports.queue;

import java.util.ArrayList;
import java.util.List;

/** Result of deleting messages in a batch operation. */
public record BatchDeleteResult(List<String> successful, List<BatchFailure> failed) {

  public BatchDeleteResult {
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
