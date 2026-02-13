# Commons Testkit Contracts

Contract testing base classes for port implementations.

## Overview

This module provides abstract test classes that verify implementations correctly follow port contracts. By extending these base classes, you ensure that your adapters behave consistently with the expected interface contracts.

## Contract Tests Included

- **PageableRepositoryContract** - Tests for `PageableRepository<E, ID>` implementations
- **HttpClientPortContract** - Tests for `HttpClientPort` implementations
- **MessagePublisherPortContract** - Tests for `MessagePublisherPort` implementations
- **CachePortContract** - Tests for `CachePort<K, V>` implementations

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-testkit-contracts</artifactId>
  <scope>test</scope>
</dependency>
```

## Usage

### PageableRepositoryContract

Verify your repository implementation follows the PageableRepository contract:

```java
class JpaUserRepositoryContractTest extends PageableRepositoryContract<User, String> {

  @Autowired
  private UserRepository userRepository;

  @Override
  protected PageableRepository<User, String> createRepository() {
    return userRepository;
  }

  @Override
  protected User createEntity() {
    return new User(UUID.randomUUID().toString(), "John Doe", "john@example.com");
  }

  @Override
  protected User createAnotherEntity() {
    return new User(UUID.randomUUID().toString(), "Jane Smith", "jane@example.com");
  }

  @Override
  protected String getEntityId(User entity) {
    return entity.getId();
  }

  @Override
  protected void cleanupRepository() {
    userRepository.deleteAll();
  }
}
```

**Tests included:**
- ✅ Save and find by ID
- ✅ Return empty when not found
- ✅ Delete by ID and delete entity
- ✅ Find all with pagination
- ✅ Respect page size
- ✅ Navigate to second page
- ✅ Return empty page when no entities

### HttpClientPortContract

Verify your HTTP client implementation:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkHttpClientContractTest extends HttpClientPortContract {

  private WireMockServer wireMock;

  @BeforeAll
  void startWireMock() {
    wireMock = new WireMockServer(8089);
    wireMock.start();
    WireMock.configureFor("localhost", 8089);
  }

  @AfterAll
  void stopWireMock() {
    wireMock.stop();
  }

  @BeforeEach
  void setupStubs() {
    // Setup WireMock stubs for /get, /post, /headers, /status/404, etc.
    stubFor(get(urlEqualTo("/get"))
        .willReturn(aResponse().withStatus(200).withBody("{}")));
    stubFor(post(urlEqualTo("/post"))
        .willReturn(aResponse().withStatus(200)));
    // ... more stubs
  }

  @Override
  protected HttpClientPort createHttpClient() {
    return new OkHttpClientAdapter();
  }

  @Override
  protected String getTestServerUrl() {
    return "http://localhost:8089";
  }
}
```

**Tests included:**
- ✅ Execute GET request
- ✅ Execute POST request with body
- ✅ Include custom headers
- ✅ Handle 4xx error responses
- ✅ Execute with response mapper
- ✅ Preserve response headers

### CachePortContract

Verify your cache implementation:

```java
class RedisCacheContractTest extends CachePortContract<String, User> {

  @Autowired
  private RedisTemplate<String, User> redisTemplate;

  @Override
  protected CachePort<String, User> createCache() {
    return new RedisCacheAdapter<>(redisTemplate);
  }

  @Override
  protected String createTestKey() {
    return "user:" + UUID.randomUUID();
  }

  @Override
  protected String createAnotherTestKey() {
    return "user:" + UUID.randomUUID();
  }

  @Override
  protected User createTestValue() {
    return new User("1", "John", "john@example.com");
  }

  @Override
  protected User createAnotherTestValue() {
    return new User("2", "Jane", "jane@example.com");
  }
}
```

**Tests included:**
- ✅ Put and get value
- ✅ Return empty when key not found
- ✅ Remove value
- ✅ Clear all entries
- ✅ Check if key exists
- ✅ Return all keys
- ✅ Return correct size
- ✅ Update existing value
- ✅ Put value with TTL

### MessagePublisherPortContract

Verify your messaging implementation:

```java
@SpringBootTest
@EmbeddedKafka(topics = "test-topic")
class KafkaPublisherContractTest extends MessagePublisherPortContract<String> {

  @Autowired
  private MessagePublisherPort publisher;

  @Override
  protected MessagePublisherPort createPublisher() {
    return publisher;
  }

  @Override
  protected MessageSerializer<String> createSerializer() {
    return new StringMessageSerializer();
  }

  @Override
  protected TopicName getTestTopic() {
    return TopicName.of("test-topic");
  }

  @Override
  protected String createTestPayload() {
    return "test message " + System.currentTimeMillis();
  }
}
```

**Tests included:**
- ✅ Publish message with MessageEnvelope
- ✅ Publish with simple API (topic + payload)
- ✅ Publish batch of messages
- ✅ Publish with custom headers
- ✅ Publish with message ID

## Benefits

1. **Consistency** - All implementations of the same port behave identically
2. **Confidence** - Comprehensive test coverage for contract compliance
3. **Maintainability** - Update contract tests once, all implementations benefit
4. **Documentation** - Tests serve as living documentation of port contracts
5. **Regression Prevention** - Catch breaking changes early

## Best Practices

1. **Run contract tests in CI/CD** - Ensure all implementations remain compliant
2. **Use test containers** - For repositories, caches, and messaging (see commons-testkit-core)
3. **Clean up between tests** - Override `cleanupRepository()` or use `@BeforeEach`
4. **Test with realistic data** - Use domain-specific entities and values
5. **Combine with integration tests** - Contract tests + specific adapter tests = full coverage

## Dependencies

- JUnit 5
- AssertJ
- Awaitility (for async messaging tests)
- Commons ports modules (persistence, http, messaging, cache)
- Commons testkit-core

## License

See [LICENSE](../LICENSE)
