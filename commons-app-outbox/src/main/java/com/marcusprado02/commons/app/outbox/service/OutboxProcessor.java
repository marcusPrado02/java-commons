package com.marcusprado02.commons.app.outbox.service;

public interface OutboxProcessor {
  void processOnce(int batchSize);
}
