package com.marcusprado02.commons.adapters.cache.memcached;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import org.junit.jupiter.api.Test;

class MemcachedBranchTest {

  // --- Constructor null keyPrefix branch ---

  @Test
  void constructor_nullKeyPrefix_treatedAsEmpty() {
    MemcachedClient client = mock(MemcachedClient.class);
    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, null);
    assertThat(adapter).isNotNull();
  }

  // --- put() null TTL branch ---

  @Test
  @SuppressWarnings("unchecked")
  void put_nullTtl_usesDefaultExpiration() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.set(anyString(), anyInt(), any())).thenReturn(future);
    when(future.get()).thenReturn(Boolean.TRUE);

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.put("key", "value", null);
  }

  // --- put() false success branch ---

  @Test
  @SuppressWarnings("unchecked")
  void put_falseSuccess_logsWarning() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.set(anyString(), anyInt(), any())).thenReturn(future);
    when(future.get()).thenReturn(Boolean.FALSE);

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.put("key", "value");
  }

  // --- put() InterruptedException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void put_interruptedException_noThrow() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.set(anyString(), anyInt(), any())).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.put("key", "value");
    Thread.interrupted();
  }

  // --- put() ExecutionException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void put_executionException_noThrow() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.set(anyString(), anyInt(), any())).thenReturn(future);
    when(future.get()).thenThrow(new ExecutionException("fail", new RuntimeException()));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.put("key", "value");
  }

  // --- remove() false success (key not found) branch ---

  @Test
  @SuppressWarnings("unchecked")
  void remove_keyNotFound_logsDebug() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.delete(anyString())).thenReturn(future);
    when(future.get()).thenReturn(Boolean.FALSE);

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.remove("nonexistent");
  }

  // --- remove() InterruptedException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void remove_interruptedException_noThrow() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.delete(anyString())).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.remove("key");
    Thread.interrupted();
  }

  // --- get() InterruptedException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void get_interruptedException_returnsEmpty() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    GetFuture<Object> future = mock(GetFuture.class);
    when(client.asyncGet(anyString())).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    assertThat(adapter.get("key")).isEmpty();
    Thread.interrupted();
  }

  // --- get() ExecutionException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void get_executionException_returnsEmpty() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    GetFuture<Object> future = mock(GetFuture.class);
    when(client.asyncGet(anyString())).thenReturn(future);
    when(future.get()).thenThrow(new ExecutionException("fail", new RuntimeException()));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    assertThat(adapter.get("key")).isEmpty();
  }

  // --- get() deserialize exception branch ---

  @Test
  @SuppressWarnings("unchecked")
  void get_deserializeException_throwsRuntimeException() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    GetFuture<Object> future = mock(GetFuture.class);
    when(client.asyncGet(anyString())).thenReturn(future);
    when(future.get()).thenReturn("not-valid-integer-json");

    MemcachedCacheAdapter<String, Integer> adapter =
        new MemcachedCacheAdapter<>(client, Integer.class, "");
    assertThatThrownBy(() -> adapter.get("key")).isInstanceOf(RuntimeException.class);
  }

  // --- contains() InterruptedException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void contains_interruptedException_returnsFalse() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    GetFuture<Object> future = mock(GetFuture.class);
    when(client.asyncGet(anyString())).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    assertThat(adapter.contains("key")).isFalse();
    Thread.interrupted();
  }

  // --- contains() ExecutionException branch ---

  @Test
  @SuppressWarnings("unchecked")
  void contains_executionException_returnsFalse() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    GetFuture<Object> future = mock(GetFuture.class);
    when(client.asyncGet(anyString())).thenReturn(future);
    when(future.get()).thenThrow(new ExecutionException("fail", new RuntimeException()));

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    assertThat(adapter.contains("key")).isFalse();
  }

  // --- clear() exception branch ---

  @Test
  void clear_exception_noThrow() {
    MemcachedClient client = mock(MemcachedClient.class);
    doThrow(new RuntimeException("fail")).when(client).flush();

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.clear();
  }

  // --- shutdown() happy path ---

  @Test
  void shutdown_callsClientShutdown() {
    MemcachedClient client = mock(MemcachedClient.class);
    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.shutdown();
    verify(client).shutdown();
  }

  // --- shutdown() exception branch ---

  @Test
  void shutdown_exception_noThrow() {
    MemcachedClient client = mock(MemcachedClient.class);
    doThrow(new RuntimeException("fail")).when(client).shutdown();

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.shutdown();
  }

  // --- keys() lambda: not-yet-expired branch ---

  @Test
  @SuppressWarnings("unchecked")
  void keys_notYetExpiredEntry_keptInResult() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.set(anyString(), anyInt(), any())).thenReturn(future);
    when(future.get()).thenReturn(Boolean.TRUE);

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.put("key", "value", Duration.ofSeconds(60));

    Set<String> keys = adapter.keys();
    assertThat(keys).contains("key");
  }

  // --- keys() lambda: already-expired branch ---

  @Test
  @SuppressWarnings("unchecked")
  void keys_alreadyExpiredEntry_removedFromResult() throws Exception {
    MemcachedClient client = mock(MemcachedClient.class);
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(client.set(anyString(), anyInt(), any())).thenReturn(future);
    when(future.get()).thenReturn(Boolean.TRUE);

    MemcachedCacheAdapter<String, String> adapter =
        new MemcachedCacheAdapter<>(client, String.class, "");
    adapter.put("key", "value", Duration.ofSeconds(1));

    Field keyTrackerField = MemcachedCacheAdapter.class.getDeclaredField("keyTracker");
    keyTrackerField.setAccessible(true);
    Map<String, Long> keyTracker = (Map<String, Long>) keyTrackerField.get(adapter);
    keyTracker.put("key", System.currentTimeMillis() - 5000);

    Set<String> keys = adapter.keys();
    assertThat(keys).doesNotContain("key");
  }
}
