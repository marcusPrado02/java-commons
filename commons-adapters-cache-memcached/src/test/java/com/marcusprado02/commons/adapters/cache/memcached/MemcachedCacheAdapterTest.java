package com.marcusprado02.commons.adapters.cache.memcached;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Set;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class MemcachedCacheAdapterTest {

  private static final int MEMCACHED_PORT = 11211;

  @Container
  static GenericContainer<?> memcached =
      new GenericContainer<>(DockerImageName.parse("memcached:1.6-alpine"))
          .withExposedPorts(MEMCACHED_PORT);

  private static MemcachedClient client;
  private MemcachedCacheAdapter<String, String> cache;

  @BeforeAll
  static void setupMemcached() throws Exception {
    memcached.start();

    InetSocketAddress address =
        new InetSocketAddress(memcached.getHost(), memcached.getMappedPort(MEMCACHED_PORT));
    client = new MemcachedClient(address);
  }

  @AfterAll
  static void cleanup() {
    if (client != null) {
      client.shutdown();
    }
    memcached.stop();
  }

  @BeforeEach
  void setUp() {
    client.flush();
    cache = new MemcachedCacheAdapter<>(client, String.class);
  }

  @AfterEach
  void tearDown() {
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void shouldPutAndGetValue() {
    cache.put("key1", "value1");

    var result = cache.get("key1");

    assertThat(result).isPresent().contains("value1");
  }

  @Test
  void shouldReturnEmptyForNonExistentKey() {
    var result = cache.get("nonexistent");

    assertThat(result).isEmpty();
  }

  @Test
  void shouldPutValueWithTTL() throws InterruptedException {
    cache.put("key2", "value2", Duration.ofSeconds(2));

    assertThat(cache.get("key2")).isPresent().contains("value2");

    Thread.sleep(3000);

    assertThat(cache.get("key2")).isEmpty();
  }

  @Test
  void shouldRemoveValue() {
    cache.put("key3", "value3");
    assertThat(cache.get("key3")).isPresent();

    cache.remove("key3");

    assertThat(cache.get("key3")).isEmpty();
  }

  @Test
  void shouldCheckIfKeyExists() {
    cache.put("key4", "value4");

    assertThat(cache.contains("key4")).isTrue();
    assertThat(cache.contains("nonexistent")).isFalse();
  }

  @Test
  void shouldGetAllKeys() {
    cache.put("key5", "value5");
    cache.put("key6", "value6");
    cache.put("key7", "value7");

    Set<String> keys = cache.keys();

    assertThat(keys).containsExactlyInAnyOrder("key5", "key6", "key7");
  }

  @Test
  void shouldGetCacheSize() {
    cache.put("key8", "value8");
    cache.put("key9", "value9");

    assertThat(cache.size()).isEqualTo(2);
  }

  @Test
  void shouldClearCache() {
    cache.put("key10", "value10");
    cache.put("key11", "value11");
    assertThat(cache.size()).isGreaterThanOrEqualTo(2);

    cache.clear();

    assertThat(cache.size()).isZero();
    assertThat(cache.get("key10")).isEmpty();
    assertThat(cache.get("key11")).isEmpty();
  }

  @Test
  void shouldHandleComplexObjects() {
    TestObject obj = new TestObject("test", 42);

    MemcachedCacheAdapter<String, TestObject> objectCache =
        new MemcachedCacheAdapter<>(client, TestObject.class);

    objectCache.put("obj1", obj);

    var result = objectCache.get("obj1");

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("test");
    assertThat(result.get().value()).isEqualTo(42);
  }

  @Test
  void shouldHandleKeyPrefix() {
    MemcachedCacheAdapter<String, String> prefixedCache =
        new MemcachedCacheAdapter<>(client, String.class, "prefix:");

    prefixedCache.put("key12", "value12");

    assertThat(prefixedCache.get("key12")).isPresent().contains("value12");
    assertThat(prefixedCache.contains("key12")).isTrue();
    assertThat(prefixedCache.keys()).contains("key12");
  }

  @Test
  void shouldUpdateExistingValue() {
    cache.put("key13", "oldValue");
    assertThat(cache.get("key13")).contains("oldValue");

    cache.put("key13", "newValue");

    assertThat(cache.get("key13")).contains("newValue");
  }

  @Test
  void shouldHandleNullKey() {
    assertThat(cache.get(null)).isEmpty();
  }

  @Test
  void shouldHandleZeroTTL() {
    cache.put("key14", "value14", Duration.ZERO);

    assertThat(cache.get("key14")).isPresent().contains("value14");
  }

  @Test
  void shouldHandleMultipleOperations() {
    // Put multiple values
    for (int i = 0; i < 10; i++) {
      cache.put("key" + i, "value" + i);
    }

    // Verify all values
    for (int i = 0; i < 10; i++) {
      assertThat(cache.get("key" + i)).isPresent().contains("value" + i);
    }

    // Remove half
    for (int i = 0; i < 5; i++) {
      cache.remove("key" + i);
    }

    // Verify remaining
    assertThat(cache.size()).isEqualTo(5);
  }

  @Test
  void shouldHandleConcurrentWrites() throws InterruptedException {
    Thread t1 =
        new Thread(
            () -> {
              for (int i = 0; i < 50; i++) {
                cache.put("thread1-" + i, "value" + i);
              }
            });

    Thread t2 =
        new Thread(
            () -> {
              for (int i = 0; i < 50; i++) {
                cache.put("thread2-" + i, "value" + i);
              }
            });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertThat(cache.size()).isEqualTo(100);
  }

  record TestObject(String name, int value) {}
}
