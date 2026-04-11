package com.marcusprado02.commons.benchmarks;

import com.marcusprado02.commons.app.domainevents.DomainEventBus;
import com.marcusprado02.commons.app.domainevents.DomainEventHandler;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;
import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for the domain events system.
 *
 * <p>Measures:
 *
 * <ul>
 *   <li>Single event publish/dispatch throughput
 *   <li>Overhead of multiple handlers per event type
 *   <li>Batch publish via {@code publishAll}
 * </ul>
 *
 * <p>To run:
 *
 * <pre>{@code
 * mvn package -pl commons-benchmarks -am -DskipTests
 * java -jar commons-benchmarks/target/benchmarks.jar DomainEventsBenchmark
 * }</pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class DomainEventsBenchmark {

  private DomainEventBus busWith1Handler;
  private DomainEventBus busWith5Handlers;
  private DomainEventBus busNoHandlers;
  private DomainEventBus busForBatch;

  private OrderCreated singleEvent;
  private List<DomainEvent> batchOf10;

  @Setup
  public void setup() {
    singleEvent = new OrderCreated("order-1");
    batchOf10 =
        List.<DomainEvent>of(
            new OrderCreated("o1"),
            new OrderCreated("o2"),
            new OrderCreated("o3"),
            new OrderCreated("o4"),
            new OrderCreated("o5"),
            new OrderCreated("o6"),
            new OrderCreated("o7"),
            new OrderCreated("o8"),
            new OrderCreated("o9"),
            new OrderCreated("o10"));

    AtomicLong counter = new AtomicLong();

    // Concrete handler implementation (DomainEventHandler requires handle + eventType)
    DomainEventHandler<OrderCreated> handler =
        new DomainEventHandler<OrderCreated>() {
          @Override
          public void handle(OrderCreated event) {
            counter.incrementAndGet();
          }

          @Override
          public Class<OrderCreated> eventType() {
            return OrderCreated.class;
          }
        };

    busNoHandlers = new DomainEventBus();

    busWith1Handler = new DomainEventBus();
    busWith1Handler.register(handler);

    busWith5Handlers = new DomainEventBus();
    for (int i = 0; i < 5; i++) {
      // Each iteration registers a separate handler instance
      final long idx = i;
      busWith5Handlers.register(
          new DomainEventHandler<OrderCreated>() {
            @Override
            public void handle(OrderCreated event) {
              counter.addAndGet(idx);
            }

            @Override
            public Class<OrderCreated> eventType() {
              return OrderCreated.class;
            }
          });
    }

    busForBatch = new DomainEventBus();
    busForBatch.register(handler);
  }

  /**
   * Benchmark: publish an event with no registered handlers. Measures the baseline overhead of
   * event routing.
   */
  @Benchmark
  public void publish_noHandlers(Blackhole bh) {
    busNoHandlers.publish(singleEvent);
  }

  /**
   * Benchmark: publish an event with 1 handler. Models the common case in a well-separated service.
   */
  @Benchmark
  public void publish_oneHandler(Blackhole bh) {
    busWith1Handler.publish(singleEvent);
  }

  /**
   * Benchmark: publish an event with 5 handlers. Models a service with multiple consumers
   * (projections, notifications, etc.).
   */
  @Benchmark
  public void publish_fiveHandlers(Blackhole bh) {
    busWith5Handlers.publish(singleEvent);
  }

  /**
   * Benchmark: publishAll with a batch of 10 events. Measures overhead of sequential batch
   * dispatch.
   */
  @Benchmark
  public void publishAll_batchOf10(Blackhole bh) {
    busForBatch.publishAll(batchOf10);
  }

  // -------------------------------------------------------------------------
  // Test event
  // -------------------------------------------------------------------------

  record OrderCreated(String orderId) implements DomainEvent {
    @Override
    public EventId eventId() {
      return EventId.newId();
    }

    @Override
    public Instant occurredAt() {
      return Instant.now();
    }

    @Override
    public String aggregateType() {
      return "Order";
    }

    @Override
    public String aggregateId() {
      return orderId;
    }

    @Override
    public long aggregateVersion() {
      return 1L;
    }

    @Override
    public EventMetadata metadata() {
      return EventMetadata.empty();
    }
  }
}
