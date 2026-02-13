package com.marcusprado02.commons.app.idempotency.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DefaultIdempotencyServiceTest {

  @Test
  void shouldExecuteOnlyOnceAndReuseResultRef() {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    DefaultIdempotencyService service = new DefaultIdempotencyService(store, Duration.ofMinutes(10));

    AtomicInteger calls = new AtomicInteger();
    IdempotencyKey key = new IdempotencyKey("order:123");

    IdempotencyResult<String> first =
        service.execute(key, Duration.ofMinutes(1), () -> {
          calls.incrementAndGet();
          return "OK";
        }, value -> "ref:" + value);

    IdempotencyResult<String> second =
        service.execute(key, Duration.ofMinutes(1), () -> {
          calls.incrementAndGet();
          return "SHOULD_NOT_RUN";
        }, value -> "ref:" + value);

    assertTrue(first.executed());
    assertEquals("OK", first.value());
    assertFalse(second.executed());
    assertEquals("ref:OK", second.existingResultRef());
    assertEquals(1, calls.get());
  }

  @Test
  void shouldAllowOnlyOneConcurrentExecution() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    DefaultIdempotencyService service = new DefaultIdempotencyService(store, Duration.ofMinutes(10));

    AtomicInteger calls = new AtomicInteger();
    IdempotencyKey key = new IdempotencyKey("payment:abc");

    CyclicBarrier barrier = new CyclicBarrier(2);

    Callable<IdempotencyResult<String>> task = () -> {
      barrier.await(2, TimeUnit.SECONDS);
      return service.execute(
          key,
          Duration.ofSeconds(5),
          () -> {
            calls.incrementAndGet();
            try {
              Thread.sleep(150);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return "DONE";
          },
          value -> "ref:" + value);
    };

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<IdempotencyResult<String>> f1 = pool.submit(task);
      Future<IdempotencyResult<String>> f2 = pool.submit(task);

      IdempotencyResult<String> r1 = f1.get(3, TimeUnit.SECONDS);
      IdempotencyResult<String> r2 = f2.get(3, TimeUnit.SECONDS);

      assertEquals(1, calls.get());
      assertTrue(r1.executed() ^ r2.executed());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void shouldReacquireAfterTtlExpires() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-13T00:00:00Z"));
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock);
    DefaultIdempotencyService service = new DefaultIdempotencyService(store, Duration.ofMinutes(10));

    AtomicInteger calls = new AtomicInteger();
    IdempotencyKey key = new IdempotencyKey("shipment:1");

    IdempotencyResult<String> first =
        service.execute(key, Duration.ofSeconds(10), () -> {
          calls.incrementAndGet();
          return "A";
        }, v -> "ref:" + v);

    clock.advance(Duration.ofSeconds(11));

    IdempotencyResult<String> second =
        service.execute(key, Duration.ofSeconds(10), () -> {
          calls.incrementAndGet();
          return "B";
        }, v -> "ref:" + v);

    assertTrue(first.executed());
    assertTrue(second.executed());
    assertEquals(2, calls.get());
  }

  @Test
  void shouldMarkFailedAndRethrow() {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    DefaultIdempotencyService service = new DefaultIdempotencyService(store, Duration.ofMinutes(10));

    IdempotencyKey key = new IdempotencyKey("fail:1");

    assertThrows(
        IllegalStateException.class,
        () ->
            service.execute(
                key,
                Duration.ofMinutes(1),
                () -> {
                  throw new IllegalStateException("boom");
                },
                ignored -> null));

    assertTrue(store.find(key).isPresent());
    assertNotNull(store.find(key).get().lastError());
    assertEquals("IllegalStateException", store.find(key).get().lastError());
  }

  private static final class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;

    private MutableClock(Instant start) {
      this.instant = new AtomicReference<>(start);
    }

    void advance(Duration delta) {
      instant.updateAndGet(i -> i.plus(delta));
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant.get();
    }
  }
}
