package com.marcusprado02.commons.app.auditlog;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Default implementation of {@link AuditService}.
 *
 * <p>Delegates storage to {@link AuditRepository}.
 */
public final class DefaultAuditService implements AuditService {

  private final AuditRepository repository;
  private final Executor executor;

  public DefaultAuditService(AuditRepository repository) {
    this(repository, ForkJoinPool.commonPool());
  }

  public DefaultAuditService(AuditRepository repository, Executor executor) {
    this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    this.executor = Objects.requireNonNull(executor, "executor cannot be null");
  }

  @Override
  public Result<Void> audit(AuditEvent event) {
    Objects.requireNonNull(event, "event cannot be null");
    return repository.save(event);
  }

  @Override
  public void auditAsync(AuditEvent event) {
    Objects.requireNonNull(event, "event cannot be null");
    CompletableFuture.runAsync(() -> repository.save(event), executor);
  }
}
