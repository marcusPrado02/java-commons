package com.marcusprado02.commons.adapters.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.adapters.cache.redis.config.RedisConfig;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

class RedisBranchTest {

  // --- RedisCacheAdapter constructor null-keyPrefix branch ---

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_nullKeyPrefix_treatedAsEmpty() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    ValueOperations<String, Object> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    when(ops.get(anyString())).thenReturn("val");

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, null);
    adapter.get("key");

    verify(ops).get("key");
  }

  // --- RedisCacheAdapter.clear() with empty cache (skip-delete branch) ---

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_clear_emptyCache_noDelete() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    when(template.keys(anyString())).thenReturn(new HashSet<>());

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    adapter.clear();
  }

  // --- RedisCacheAdapter.keys() null return from template ---

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_keys_nullFromTemplate_returnsEmpty() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    when(template.keys(anyString())).thenReturn(null);

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    Set<String> keys = adapter.keys();

    assertThat(keys).isEmpty();
  }

  // --- RedisCacheAdapter DataAccessException branches ---

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_get_dataAccessException_returnsEmpty() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    ValueOperations<String, Object> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    when(ops.get(anyString())).thenThrow(new InvalidDataAccessApiUsageException("fail"));

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    assertThat(adapter.get("key")).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_put_dataAccessException_noThrow() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    ValueOperations<String, Object> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    doThrow(new InvalidDataAccessApiUsageException("fail")).when(ops).set(anyString(), any());

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    adapter.put("key", "value");
  }

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_putWithTtl_dataAccessException_noThrow() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    ValueOperations<String, Object> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    doThrow(new InvalidDataAccessApiUsageException("fail"))
        .when(ops)
        .set(anyString(), any(), any(Duration.class));

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    adapter.put("key", "value", Duration.ofSeconds(1));
  }

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_remove_dataAccessException_noThrow() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    when(template.delete(anyString())).thenThrow(new InvalidDataAccessApiUsageException("fail"));

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    adapter.remove("key");
  }

  @Test
  @SuppressWarnings("unchecked")
  void redisCacheAdapter_contains_dataAccessException_returnsFalse() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    when(template.hasKey(anyString())).thenThrow(new InvalidDataAccessApiUsageException("fail"));

    RedisCacheAdapter<Object> adapter = new RedisCacheAdapter<>(template, "");
    assertThat(adapter.contains("key")).isFalse();
  }

  // --- RedisAtomicOperations constructor null-keyPrefix branch ---

  @Test
  @SuppressWarnings("unchecked")
  void redisAtomicOperations_nullKeyPrefix_treatedAsEmpty() {
    RedisTemplate<String, Long> template = mock(RedisTemplate.class);
    ValueOperations<String, Long> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString(), any(Long.class))).thenReturn(1L);

    RedisAtomicOperations<String> atomicOps = new RedisAtomicOperations<>(template, null);
    long result = atomicOps.increment("counter");

    assertThat(result).isEqualTo(1L);
  }

  // --- RedisAtomicOperations null result branches ---

  @Test
  @SuppressWarnings("unchecked")
  void redisAtomicOperations_incrementBy_nullResult_returnsZero() {
    RedisTemplate<String, Long> template = mock(RedisTemplate.class);
    ValueOperations<String, Long> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString(), any(Long.class))).thenReturn(null);

    RedisAtomicOperations<String> atomicOps = new RedisAtomicOperations<>(template, "");
    assertThat(atomicOps.incrementBy("key", 1L)).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  void redisAtomicOperations_decrementBy_nullResult_returnsZero() {
    RedisTemplate<String, Long> template = mock(RedisTemplate.class);
    ValueOperations<String, Long> ops = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(ops);
    when(ops.decrement(anyString(), any(Long.class))).thenReturn(null);

    RedisAtomicOperations<String> atomicOps = new RedisAtomicOperations<>(template, "");
    assertThat(atomicOps.decrementBy("key", 1L)).isZero();
  }

  // --- RedisConfig.redisTemplate bean method ---

  @Test
  void redisConfig_redisTemplate_createsConfiguredTemplate() {
    RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
    RedisConfig config = new RedisConfig();

    RedisTemplate<String, Object> template = config.redisTemplate(factory);

    assertThat(template).isNotNull();
    assertThat(template.getConnectionFactory()).isEqualTo(factory);
  }

  // --- RedisPubSub.publish() exception branch ---

  @Test
  @SuppressWarnings("unchecked")
  void redisPubSub_publish_exception_noThrow() {
    RedisTemplate<String, String> template = mock(RedisTemplate.class);
    RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
    doThrow(new RuntimeException("fail")).when(template).convertAndSend(anyString(), any());

    RedisPubSub<String, String> pubSub = new RedisPubSub<>(template, container, "chan");
    pubSub.publish("msg");
  }

  // --- RedisPubSub.unsubscribe() exception branch ---

  @Test
  @SuppressWarnings("unchecked")
  void redisPubSub_unsubscribe_exception_noThrow() {
    RedisTemplate<String, String> template = mock(RedisTemplate.class);
    RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
    doThrow(new RuntimeException("fail")).when(container).stop();

    RedisPubSub<String, String> pubSub = new RedisPubSub<>(template, container, "chan");
    pubSub.unsubscribe();
  }

  // --- RedisPubSub.onMessage() null-value branch ---

  @Test
  @SuppressWarnings("unchecked")
  void redisPubSub_onMessage_nullValue_doesNotCallHandler() {
    RedisTemplate<String, String> template = mock(RedisTemplate.class);
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    when(template.getValueSerializer())
        .thenReturn((org.springframework.data.redis.serializer.RedisSerializer) stringSerializer);

    RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
    RedisPubSub<String, String> pubSub = new RedisPubSub<>(template, container, "chan");

    ArgumentCaptor<MessageListener> listenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
    pubSub.subscribe(v -> {});
    verify(container)
        .addMessageListener(
            listenerCaptor.capture(), any(org.springframework.data.redis.listener.Topic.class));

    Message nullBodyMessage = mock(Message.class);
    when(nullBodyMessage.getBody()).thenReturn(null);
    listenerCaptor.getValue().onMessage(nullBodyMessage, null);
  }
}
