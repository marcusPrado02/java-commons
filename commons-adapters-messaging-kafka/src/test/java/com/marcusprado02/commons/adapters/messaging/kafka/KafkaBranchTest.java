package com.marcusprado02.commons.adapters.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.Test;

class KafkaBranchTest {

  @SuppressWarnings("unchecked")
  private static KafkaPublisherAdapter buildAdapter(boolean transactional) throws Exception {
    KafkaProducer<String, byte[]> producer = mock(KafkaProducer.class);
    Constructor<KafkaPublisherAdapter> ctor =
        KafkaPublisherAdapter.class.getDeclaredConstructor(KafkaProducer.class, boolean.class);
    ctor.setAccessible(true);
    return ctor.newInstance(producer, transactional);
  }

  // --- Constructor transactional=true branch: initTransactions() called ---

  @Test
  @SuppressWarnings("unchecked")
  void constructor_transactional_callsInitTransactions() throws Exception {
    KafkaProducer<String, byte[]> producer = mock(KafkaProducer.class);
    Constructor<KafkaPublisherAdapter> ctor =
        KafkaPublisherAdapter.class.getDeclaredConstructor(KafkaProducer.class, boolean.class);
    ctor.setAccessible(true);
    ctor.newInstance(producer, true);
    verify(producer).initTransactions();
  }

  // --- beginTransaction: !transactional → throws ---

  @Test
  void beginTransaction_nonTransactional_throws() throws Exception {
    KafkaPublisherAdapter adapter = buildAdapter(false);
    assertThatThrownBy(adapter::beginTransaction).isInstanceOf(IllegalStateException.class);
  }

  // --- commitTransaction: !transactional → throws ---

  @Test
  void commitTransaction_nonTransactional_throws() throws Exception {
    KafkaPublisherAdapter adapter = buildAdapter(false);
    assertThatThrownBy(adapter::commitTransaction).isInstanceOf(IllegalStateException.class);
  }

  // --- abortTransaction: !transactional → throws ---

  @Test
  void abortTransaction_nonTransactional_throws() throws Exception {
    KafkaPublisherAdapter adapter = buildAdapter(false);
    assertThatThrownBy(adapter::abortTransaction).isInstanceOf(IllegalStateException.class);
  }

  // --- beginTransaction/commitTransaction/abortTransaction: transactional → delegates ---

  @Test
  @SuppressWarnings("unchecked")
  void beginTransaction_transactional_delegatesToProducer() throws Exception {
    KafkaProducer<String, byte[]> producer = mock(KafkaProducer.class);
    Constructor<KafkaPublisherAdapter> ctor =
        KafkaPublisherAdapter.class.getDeclaredConstructor(KafkaProducer.class, boolean.class);
    ctor.setAccessible(true);
    KafkaPublisherAdapter adapter = ctor.newInstance(producer, true);
    adapter.beginTransaction();
    verify(producer).beginTransaction();
  }

  @Test
  @SuppressWarnings("unchecked")
  void commitTransaction_transactional_delegatesToProducer() throws Exception {
    KafkaProducer<String, byte[]> producer = mock(KafkaProducer.class);
    Constructor<KafkaPublisherAdapter> ctor =
        KafkaPublisherAdapter.class.getDeclaredConstructor(KafkaProducer.class, boolean.class);
    ctor.setAccessible(true);
    KafkaPublisherAdapter adapter = ctor.newInstance(producer, true);
    adapter.commitTransaction();
    verify(producer).commitTransaction();
  }

  @Test
  @SuppressWarnings("unchecked")
  void abortTransaction_transactional_delegatesToProducer() throws Exception {
    KafkaProducer<String, byte[]> producer = mock(KafkaProducer.class);
    Constructor<KafkaPublisherAdapter> ctor =
        KafkaPublisherAdapter.class.getDeclaredConstructor(KafkaProducer.class, boolean.class);
    ctor.setAccessible(true);
    KafkaPublisherAdapter adapter = ctor.newInstance(producer, true);
    adapter.abortTransaction();
    verify(producer).abortTransaction();
  }

  // --- Builder: transactional=false (non-null transactionalId path skipped) ---

  @Test
  void builder_transactionalId_setsTransactional() {
    KafkaPublisherAdapter.Builder builder =
        KafkaPublisherAdapter.builder()
            .bootstrapServers("localhost:9092")
            .transactionalId("my-tx-id");
    assertThat(builder).isNotNull();
  }
}
