# Commons Testkit Core

Reusable test utilities for building robust test suites.

## Features

- ✅ **Test Data Builders** - Fluent builders for creating test objects
- ✅ **Fixtures** - Pre-configured test data and default values
- ✅ **Custom AssertJ Matchers** - Domain-specific assertions
- ✅ **Test Containers** - Pre-configured PostgreSQL and Kafka containers
- ✅ **Test Utilities** - ID generation, clocks, random data

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-testkit-core</artifactId>
  <scope>test</scope>
</dependency>
```

## Test Data Builders

Create fluent builders for your domain objects:

```java
public class UserBuilder extends TestDataBuilder<User> {
  private String id = TestIds.nextId("user");
  private String name = "Test User";
  private String email = RandomData.randomEmail();

  public UserBuilder withId(String id) {
    this.id = id;
    return this;
  }

  public UserBuilder withName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public User build() {
    return new User(id, name, email);
  }
}

// Usage
User user = new UserBuilder()
    .withName("John Doe")
    .build();
```

## Test Utilities

### TestIds - ID Generation

```java
// Sequential IDs
String id1 = TestIds.nextId();        // "1"
String id2 = TestIds.nextId("user");  // "user-2"

// Random IDs
String uuid = TestIds.randomId();     // UUID
String timestamp = TestIds.timestampId();

// Reset sequence (useful in @AfterEach)
TestIds.reset();
```

### TestClock - Controllable Time

```java
// Fixed clock
ClockProvider clock = TestClock.fixed(Instant.parse("2024-01-01T00:00:00Z"));
assertThat(clock.now()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));

// Advancing clock
ClockProvider advancing = TestClock.advancing(start, 1000); // +1s per call
Instant t1 = advancing.now(); // start
Instant t2 = advancing.now(); // start + 1s
Instant t3 = advancing.now(); // start + 2s

// Fixed at specific date
ClockProvider clock = TestClock.fixedAt(2024, 6, 15, 14, 30);
```

### RandomData - Random Test Data

```java
int randomInt = RandomData.randomInt(1, 100);
String randomString = RandomData.randomString(10);
String randomEmail = RandomData.randomEmail(); // "abc123@test.com"
boolean randomBool = RandomData.randomBoolean();

// Pick random from array
String color = RandomData.randomFrom("red", "green", "blue");

// Pick random enum
Status status = RandomData.randomEnum(Status.class);
```

## Fixtures

Pre-configured test data:

```java
// Time fixtures
Instant instant = Fixtures.DEFAULT_INSTANT; // 2024-01-01T00:00:00Z
ClockProvider clock = Fixtures.DEFAULT_CLOCK;

// Identifier constants
String tenantId = Fixtures.DEFAULT_TENANT_ID;
String correlationId = Fixtures.DEFAULT_CORRELATION_ID;
String actorId = Fixtures.DEFAULT_ACTOR_ID;

// Factory methods
ClockProvider clockAt = Fixtures.clockAt(2024, 12, 25, 0, 0);
```

## Custom AssertJ Matchers

### ResultAssert

```java
import static com.marcusprado02.commons.testkit.matchers.ResultAssert.assertThat;

Result<User> result = userService.findById(id);

assertThat(result)
    .isSuccess()
    .hasValue(user -> assertThat(user.name()).isEqualTo("John"));

assertThat(result)
    .isFailure()
    .hasError(problem -> assertThat(problem.title()).isEqualTo("User not found"));
```

### AggregateRootAssert

```java
import static com.marcusprado02.commons.testkit.matchers.AggregateRootAssert.assertThat;

Order order = new Order(...);
order.confirmOrder();

assertThat(order)
    .hasDomainEvents(1)
    .hasDomainEventOfType(OrderConfirmedEvent.class);
```

## Test Containers

Pre-configured containers with sensible defaults:

### PostgreSQL

```java
import com.marcusprado02.commons.testkit.containers.TestPostgres;

@Container
static PostgreSQLContainer<?> postgres = TestPostgres.container();

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
  registry.add("spring.datasource.url", postgres::getJdbcUrl);
  registry.add("spring.datasource.username", postgres::getUsername);
  registry.add("spring.datasource.password", postgres::getPassword);
}
```

### Kafka

```java
import com.marcusprado02.commons.testkit.containers.TestKafka;

@Container
static KafkaContainer kafka = TestKafka.container();

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
  registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
}
```

## Best Practices

1. **Use TestIds.reset() in @AfterEach** to ensure test isolation
2. **Prefer fixed clocks** over system time for deterministic tests
3. **Use builders** for complex object creation to improve readability
4. **Leverage fixtures** for common test data to reduce duplication

## Dependencies

### Required
- JUnit 5
- AssertJ
- Mockito

### Optional
- Testcontainers (for container utilities)

## License

See [LICENSE](../LICENSE)
