# Commons - Adapters Persistence MongoDB

MongoDB implementation of the `PageableRepository` interface using Spring Data MongoDB.

## Features

- Full CRUD operations
- Advanced filtering with `SearchCriteria`
- Multi-field sorting
- Pagination support
- Type-safe query building
- Testcontainers integration for testing

## Dependencies

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-persistence-mongodb</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Configure MongoDB Connection

```java
@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "mydatabase");
    }
}
```

### 2. Define Your Entity

```java
@Document(collection = "users")
public record User(
    @Id String id,
    String name,
    int age,
    String email,
    boolean active
) {}
```

### 3. Create a Repository Instance

```java
@Service
public class UserService {

    private final MongoPageableRepository<User, String> repository;

    public UserService(MongoTemplate mongoTemplate) {
        this.repository = new MongoPageableRepository<>(mongoTemplate, User.class);
    }

    public User saveUser(User user) {
        return repository.save(user);
    }

    public Optional<User> findUser(String id) {
        return repository.findById(id);
    }
}
```

## Usage Examples

### Basic CRUD Operations

```java
// Save
User user = new User("1", "Alice", 25, "alice@example.com", true);
repository.save(user);

// Find by ID
Optional<User> found = repository.findById("1");

// Delete
repository.deleteById("1");

// Check existence
boolean exists = repository.existsById("1");

// Count
long count = repository.count();
```

### Filtering with SearchCriteria

```java
// Single filter (equals)
SearchFilter filter = SearchFilter.of("name", FilterOperator.EQ, "Alice");
SearchCriteria criteria = SearchCriteria.of(filter);
PageRequest pageRequest = new PageRequest(0, 10);
PageResult<User> result = repository.findAll(pageRequest, criteria);

// Multiple filters (AND)
SearchFilter ageFilter = SearchFilter.of("age", FilterOperator.GT, "25");
SearchFilter activeFilter = SearchFilter.of("active", FilterOperator.EQ, "true");
SearchCriteria criteria = SearchCriteria.of(ageFilter, activeFilter);
PageResult<User> result = repository.findAll(pageRequest, criteria);

// LIKE operator (regex pattern)
SearchFilter emailFilter = SearchFilter.of("email", FilterOperator.LIKE, "%@example.com");
SearchCriteria criteria = SearchCriteria.of(emailFilter);
PageResult<User> result = repository.findAll(pageRequest, criteria);

// IN operator
SearchFilter nameFilter = SearchFilter.of("name", FilterOperator.IN, "Alice,Bob,Charlie");
SearchCriteria criteria = SearchCriteria.of(nameFilter);
PageResult<User> result = repository.findAll(pageRequest, criteria);
```

### Supported Filter Operators

| Operator | Description | Example Value |
|----------|-------------|---------------|
| `EQ` | Equals | `"Alice"` |
| `NEQ` | Not equals | `"Bob"` |
| `LIKE` | Pattern match (regex) | `"%@example.com"` |
| `GT` | Greater than | `"25"` |
| `LT` | Less than | `"30"` |
| `GTE` | Greater than or equal | `"18"` |
| `LTE` | Less than or equal | `"65"` |
| `IN` | In list (comma-separated) | `"Alice,Bob,Charlie"` |

### Sorting

```java
// Single field ascending
Sort sort = Sort.of(new Order("age", Order.Direction.ASC));
PageResult<User> result = repository.search(pageRequest, null, sort);

// Single field descending
Sort sort = Sort.of(new Order("age", Order.Direction.DESC));
PageResult<User> result = repository.search(pageRequest, null, sort);

// Multiple fields
Sort sort = Sort.of(
    new Order("age", Order.Direction.ASC),
    new Order("name", Order.Direction.ASC)
);
PageResult<User> result = repository.search(pageRequest, null, sort);
```

### Pagination

```java
// First page (10 items)
PageRequest pageRequest = new PageRequest(0, 10);
PageResult<User> firstPage = repository.findAll(pageRequest);

// Second page
PageRequest pageRequest = new PageRequest(1, 10);
PageResult<User> secondPage = repository.findAll(pageRequest);

// Access results
List<User> users = firstPage.content();
long totalElements = firstPage.totalElements();
int currentPage = firstPage.page();
int pageSize = firstPage.size();
```

## Query Translation

### Filter Operators

The `MongoQueryBuilder` translates filter operators to MongoDB queries:

- **EQ**: `Criteria.where(field).is(value)`
- **NEQ**: `Criteria.where(field).ne(value)`
- **LIKE**: `Criteria.where(field).regex(pattern, CASE_INSENSITIVE)` (% → .*)
- **GT/LT/GTE/LTE**: `Criteria.where(field).gt/lt/gte/lte(value)`
- **IN**: `Criteria.where(field).in(values)`

### Type Conversion

Values are automatically converted to appropriate types:
- Boolean: `"true"` → `true`
- Integer: `"42"` → `42`
- Long: `"1000000"` → `1000000L`
- Double: `"3.14"` → `3.14`
- String: Fallback for non-numeric values

### Wildcard Patterns

SQL-style wildcards in LIKE operator are converted to regex:
- `%`: Matches zero or more characters → `.*`
- Example: `"%@example.com"` → `".*@example\\.com"`

## Testing

The module includes comprehensive tests using Testcontainers with MongoDB 7.0:

```java
@Testcontainers
class MyTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    private static MongoTemplate mongoTemplate;

    @BeforeAll
    static void setup() {
        mongoDBContainer.start();
        mongoTemplate = new MongoTemplate(
            MongoClients.create(mongoDBContainer.getReplicaSetUrl()),
            "test"
        );
    }
}
```

### Test Coverage

- ✅ CRUD operations
- ✅ All filter operators (EQ, NEQ, LIKE, GT, LT, GTE, LTE, IN)
- ✅ Multi-field sorting (ASC/DESC)
- ✅ Pagination (first page, subsequent pages)
- ✅ Boolean field filtering
- ✅ Multiple filters (AND logic)

## When to Use MongoDB vs Other Persistence Options

### Use MongoDB When:
- You need flexible schema evolution
- You're working with document-oriented data
- You need horizontal scaling (sharding)
- You're implementing event sourcing
- You require embedded documents and arrays
- You have high-write, high-throughput scenarios

### Use JPA (Relational) When:
- You need ACID transactions across tables
- You have complex relationships with joins
- Your data model is stable and normalized
- You require referential integrity constraints

### Use InMemory When:
- You're writing unit tests
- You're prototyping
- You need a lightweight development environment
- You're building cache-like functionality

## Transaction Support

MongoDB transactions require:
- MongoDB 4.0+ with replica set
- MongoDB 4.2+ for sharded clusters

```java
@Transactional
public void performTransactionalOperations() {
    User user1 = new User("1", "Alice", 25, "alice@example.com", true);
    User user2 = new User("2", "Bob", 30, "bob@example.com", false);

    repository.save(user1);
    repository.save(user2);
    // Both operations committed or rolled back together
}
```

## Performance Considerations

1. **Indexing**: Create indexes on frequently queried fields
   ```javascript
   db.users.createIndex({ "email": 1 })
   db.users.createIndex({ "age": 1, "active": 1 })
   ```

2. **Batch Operations**: Use `save()` in loops efficiently (Spring Data handles this)

3. **Projection**: For large documents, consider querying only needed fields

4. **Connection Pooling**: Configure `ConnectionPoolSettings` in `MongoClientSettings`

## MongoDB Version Compatibility

- **Tested with**: MongoDB 7.0
- **Minimum required**: MongoDB 4.0
- **Recommended**: MongoDB 5.0+

## License

This module is part of the `java-commons` project and is licensed under the same terms.
