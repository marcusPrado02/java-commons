# GraphQL Server Adapter

Production-ready GraphQL server adapter with Spring GraphQL, extended scalars, validation, and subscriptions.

## Features

- **Spring GraphQL Integration**: Full integration with Spring Boot
- **Extended Scalars**: DateTime, UUID, URL, JSON, and more
- **Validation**: Built-in validation directives (@Size, @Pattern, @Range)
- **Error Handling**: Domain error mapping to GraphQL errors
- **Subscriptions**: Real-time data with GraphQL subscriptions
- **Custom Directives**: Authorization, rate limiting, and more
- **Type Safety**: Strong typing with schema-first approach
- **Testing Support**: Utilities for testing GraphQL queries and mutations

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-adapters-graphql-server</artifactId>
    <version>${commons.version}</version>
</dependency>
```

## Quick Start

### 1. Create GraphQL Schema

Create `schema.graphqls` in `src/main/resources/graphql/`:

```graphql
scalar DateTime
scalar UUID
scalar JSON

type Query {
    user(id: UUID!): User
    users(page: Int, size: Int): UserPage
}

type Mutation {
    createUser(input: CreateUserInput!): User!
    updateUser(id: UUID!, input: UpdateUserInput!): User!
    deleteUser(id: UUID!): Boolean!
}

type Subscription {
    onUserCreated: User!
    onUserUpdated: User!
}

type User {
    id: UUID!
    name: String!
    email: String!
    createdAt: DateTime!
}

type UserPage {
    content: [User!]!
    totalElements: Int!
    totalPages: Int!
}

input CreateUserInput {
    name: String! @Size(min: 3, max: 100)
    email: String! @Pattern(regexp: "^[A-Za-z0-9+_.-]+@(.+)$")
}

input UpdateUserInput {
    name: String @Size(min: 3, max: 100)
    email: String @Pattern(regexp: "^[A-Za-z0-9+_.-]+@(.+)$")
}
```

### 2. Create Controller

```java
package com.example.graphql;

import com.marcusprado02.commons.adapters.graphql.resolver.BaseGraphQLResolver;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
public class UserController extends BaseGraphQLResolver {

    private final UserService userService;
    private final GraphQLSubscriptionManager<User> subscriptionManager;

    public UserController(UserService userService) {
        this.userService = userService;
        this.subscriptionManager = new GraphQLSubscriptionManager<>();
    }

    @QueryMapping
    public CompletableFuture<User> user(@Argument UUID id) {
        return asyncResult(() -> userService.findById(id));
    }

    @QueryMapping
    public CompletableFuture<UserPage> users(
            @Argument Integer page,
            @Argument Integer size) {
        return async(() -> userService.findAll(page, size));
    }

    @MutationMapping
    public CompletableFuture<User> createUser(@Argument CreateUserInput input) {
        return asyncResult(() -> userService.create(input))
            .thenApply(user -> {
                subscriptionManager.publish(user);
                return user;
            });
    }

    @MutationMapping
    public CompletableFuture<User> updateUser(
            @Argument UUID id,
            @Argument UpdateUserInput input) {
        return asyncResult(() -> userService.update(id, input));
    }

    @MutationMapping
    public CompletableFuture<Boolean> deleteUser(@Argument UUID id) {
        return asyncResult(() -> userService.delete(id))
            .thenApply(result -> true);
    }

    @SubscriptionMapping
    public Publisher<User> onUserCreated() {
        return subscriptionManager.subscribe();
    }
}
```

### 3. Configure Application

```java
@Configuration
@Import(GraphQLConfiguration.class)
public class AppConfig {
    // Your beans here
}
```

### 4. Application Properties

```properties
spring.graphql.graphiql.enabled=true
spring.graphql.graphiql.path=/graphiql
spring.graphql.path=/graphql
spring.graphql.schema.printer.enabled=true
```

### 5. Run and Test

Start your application and navigate to `http://localhost:8080/graphiql` to test queries:

```graphql
query {
  user(id: "550e8400-e29b-41d4-a716-446655440000") {
    id
    name
    email
    createdAt
  }
}

mutation {
  createUser(input: {
    name: "John Doe"
    email: "john@example.com"
  }) {
    id
    name
    email
  }
}

subscription {
  onUserCreated {
    id
    name
    email
  }
}
```

## Extended Scalars

The adapter provides additional scalar types beyond GraphQL's default scalars:

### Date and Time Scalars

```graphql
scalar DateTime  # ISO-8601 DateTime (2023-12-25T10:30:00Z)
scalar Date      # ISO-8601 Date (2023-12-25)
scalar Time      # ISO-8601 Time (10:30:00)
scalar LocalTime # Time without timezone (10:30:00)
```

Example usage:

```graphql
type Event {
    id: UUID!
    title: String!
    startDate: DateTime!
    endDate: DateTime!
    createdAt: DateTime!
}
```

### Other Scalars

```graphql
scalar UUID      # UUID (550e8400-e29b-41d4-a716-446655440000)
scalar Url       # URL (https://example.com)
scalar JSON      # JSON object
scalar Object    # Any object
scalar Long      # 64-bit integer
scalar BigDecimal # Arbitrary precision decimal
scalar BigInteger # Arbitrary precision integer
scalar Locale    # Locale (en_US, pt_BR)
```

### Numeric Constraints

```graphql
scalar PositiveInt     # > 0
scalar NegativeInt     # < 0
scalar NonPositiveInt  # <= 0
scalar NonNegativeInt  # >= 0
scalar PositiveFloat   # > 0.0
scalar NegativeFloat   # < 0.0
scalar NonPositiveFloat # <= 0.0
scalar NonNegativeFloat # >= 0.0
```

## Validation Directives

The adapter includes validation directives that work on input types:

```graphql
input CreateUserInput {
    name: String! @Size(min: 3, max: 100)
    email: String! @Pattern(regexp: "^[A-Za-z0-9+_.-]+@(.+)$")
    age: Int! @Range(min: 18, max: 120)
    website: String @Pattern(regexp: "https?://.+")
}

input UpdateProfileInput {
    bio: String @Size(max: 500)
    tags: [String!] @Size(max: 10)
}
```

Available directives:
- `@Size(min, max)`: String or collection size
- `@Pattern(regexp)`: String regex pattern
- `@Range(min, max)`: Numeric range
- `@DecimalMin(value)`: Minimum decimal value
- `@DecimalMax(value)`: Maximum decimal value

## Error Handling

The adapter automatically maps domain errors to GraphQL errors:

```java
// Service throws DomainError
public Result<User> findById(UUID id) {
    return userRepository.findById(id)
        .map(Result::success)
        .orElseGet(() -> Result.failure(
            new DomainError(
                "User not found",
                new ErrorCode("USER_NOT_FOUND", ErrorCategory.NOT_FOUND),
                Map.of("userId", id.toString())
            )
        ));
}
```

GraphQL response:

```json
{
  "errors": [
    {
      "message": "User not found",
      "locations": [{"line": 2, "column": 3}],
      "path": ["user"],
      "extensions": {
        "errorCode": "USER_NOT_FOUND",
        "details": {
          "userId": "550e8400-e29b-41d4-a716-446655440000"
        }
      }
    }
  ],
  "data": {
    "user": null
  }
}
```

Error type mapping:
- `VALIDATION` → `ValidationError`
- `NOT_FOUND` → `DataFetchingException`
- `BUSINESS` → `ExecutionAborted`
- `UNAUTHORIZED` → `ValidationError`
- `FORBIDDEN` → `ValidationError`

## Subscriptions

Real-time data with GraphQL subscriptions:

### Simple Subscription

```java
@Controller
public class NotificationController {

    private final GraphQLSubscriptionManager<Notification> subscriptionManager =
        new GraphQLSubscriptionManager<>();

    @SubscriptionMapping
    public Publisher<Notification> onNotification() {
        return subscriptionManager.subscribe();
    }

    public void publishNotification(Notification notification) {
        subscriptionManager.publish(notification);
    }
}
```

### Filtered Subscription

```graphql
type Subscription {
    onUserEvent(userId: UUID!): UserEvent!
}
```

```java
@SubscriptionMapping
public Publisher<UserEvent> onUserEvent(@Argument UUID userId) {
    return subscriptionManager.subscribe(
        event -> event.getUserId().equals(userId)
    );
}
```

### Subscription with Heartbeat

```java
@SubscriptionMapping
public Publisher<SystemStatus> onSystemStatus() {
    return subscriptionManager.subscribeWithHeartbeat(
        Duration.ofSeconds(30),
        SystemStatus.heartbeat()
    );
}
```

## Custom Directives

### Authorization Directive

```graphql
directive @auth(
  requires: [String!]!
) on FIELD_DEFINITION

type Query {
    adminData: AdminData @auth(requires: ["ROLE_ADMIN"])
    userData: UserData @auth(requires: ["ROLE_USER", "ROLE_ADMIN"])
}
```

Implementation:

```java
@Configuration
public class GraphQLSecurityConfig {

    @Bean
    public RuntimeWiringConfigurer authDirectiveWiring(
            AuthorizationService authService) {
        return builder -> builder.directive(
            "auth",
            new AuthDirective(authService)
        );
    }

    @Bean
    public AuthorizationService authorizationService() {
        return roles -> {
            // Check if current user has any of the required roles
            Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();
            
            return roles.stream()
                .anyMatch(role -> auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals(role)));
        };
    }
}
```

## Base Resolver

Extend `BaseGraphQLResolver` for common utilities:

```java
@Controller
public class ProductController extends BaseGraphQLResolver {

    @QueryMapping
    public CompletableFuture<Product> product(@Argument UUID id) {
        // Async execution with Result unwrapping
        return asyncResult(() -> productService.findById(id));
    }

    @MutationMapping
    public CompletableFuture<Product> createProduct(@Argument ProductInput input) {
        // Maps Result<ProductEntity> to Result<Product>
        return asyncResult(() ->
            map(productService.create(input), this::toGraphQL)
        );
    }

    private Product toGraphQL(ProductEntity entity) {
        return new Product(
            entity.getId(),
            entity.getName(),
entity.getPrice()
        );
    }
}
```

## Testing

### Query Testing

```java
@SpringBootTest
@AutoConfigureGraphQlTester
class UserControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void shouldGetUser() {
        graphQlTester
            .document("""
                query {
                    user(id: "550e8400-e29b-41d4-a716-446655440000") {
                        id
                        name
                        email
                    }
                }
                """)
            .execute()
            .path("user.name")
            .entity(String.class)
            .isEqualTo("John Doe");
    }

    @Test
    void shouldCreateUser() {
        graphQlTester
            .document("""
                mutation CreateUser($input: CreateUserInput!) {
                    createUser(input: $input) {
                        id
                        name
                        email
                    }
                }
                """)
            .variable("input", Map.of(
                "name", "Jane Doe",
                "email", "jane@example.com"
            ))
            .execute()
            .path("createUser.name")
            .entity(String.class)
            .isEqualTo("Jane Doe");
    }
}
```

### Subscription Testing

```java
@Test
void shouldReceiveSubscriptionEvents() {
    Flux<User> subscription = graphQlTester
        .document("subscription { onUserCreated { id name } }")
        .executeSubscription()
        .toFlux("onUserCreated", User.class);

    StepVerifier.create(subscription)
        .then(() -> publishUserCreated(new User("John Doe")))
        .assertNext(user -> assertEquals("John Doe", user.getName()))
        .thenCancel()
        .verify();
}
```

## Best Practices

### 1. Use Schema-First Approach

Define your schema in `.graphqls` files:

```graphql
# schema.graphqls
type Query {
    # All queries here
}

type Mutation {
    # All mutations here
}

type Subscription {
    # All subscriptions here
}
```

### 2. Organize by Domain

```
src/main/resources/graphql/
├── schema.graphqls         # Root types
├── user.graphqls          # User domain
├── product.graphqls       # Product domain
└── order.graphqls         # Order domain
```

### 3. Use Input Types

Always use input types for mutations:

```graphql
# Good
input CreateUserInput {
    name: String!
    email: String!
}

mutation {
    createUser(input: CreateUserInput!): User!
}

# Bad
mutation {
    createUser(name: String!, email: String!): User!
}
```

### 4. Implement Pagination

```graphql
type Query {
    users(page: Int, size: Int): UserPage!
}

type UserPage {
    content: [User!]!
    totalElements: Int!
    totalPages: Int!
    number: Int!
    size: Int!
}
```

### 5. Handle Nullability Carefully

```graphql
type User {
    id: UUID!           # Required
    name: String!       # Required
    email: String!      # Required
    phoneNumber: String # Optional
}
```

### 6. Use Fragments

```graphql
fragment UserFields on User {
    id
    name
    email
    createdAt
}

query {
    user(id: "...") {
        ...UserFields
    }
}
```

### 7. Implement DataLoaders

Prevent N+1 queries:

```java
@Bean
public DataLoader<UUID, User> userDataLoader(UserService userService) {
    return DataLoader.newDataLoader(ids ->
        CompletableFuture.supplyAsync(() ->
            userService.findByIds(ids)
        )
    );
}

@SchemaMapping
public CompletableFuture<User> author(Post post, DataLoader<UUID, User> userDataLoader) {
    return userDataLoader.load(post.getAuthorId());
}
```

## Performance Tips

### 1. Enable Query Complexity Analysis

```java
@Bean
public RuntimeWiringConfigurer complexityAnalysis() {
    return builder -> builder
        .codeRegistry(codeRegistry -> {
            // Configure max query depth
            // Configure max query complexity
        });
}
```

### 2. Use Persisted Queries

```properties
spring.graphql.persisted-queries.enabled=true
```

### 3. Implement Caching

```java
@Cacheable("users")
@QueryMapping
public User user(@Argument UUID id) {
    return userService.findById(id);
}
```

### 4. Optimize Subscriptions

```java
// Use filters to reduce network traffic
@SubscriptionMapping
public Publisher<Event> onEvent(@Argument String category) {
    return subscriptionManager.subscribe(
        event -> event.getCategory().equals(category)
    );
}
```

## Troubleshooting

### Schema Not Found

```
Error: Could not find schema file
```

Solution: Ensure schema files are in `src/main/resources/graphql/`

### Scalar Not Registered

```
Error: Unknown type DateTime
```

Solution: Import `GraphQLConfiguration`:

```java
@Import(GraphQLConfiguration.class)
public class AppConfig {}
```

### Subscription Not Working

```
Error: Subscription support is not enabled
```

Solution: Add WebSocket support:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Validation Not Applied

```
Validation directive not working
```

Solution: Ensure `graphql-java-extended-validation` is on classpath and configured in `GraphQLConfiguration`

## Example: Complete Application

```java
// Domain Entity
public record User(UUID id, String name, String email, LocalDateTime createdAt) {}

// Service
@Service
public class UserService {
    public Result<User> findById(UUID id) {
        // Implementation
    }

    public Result<User> create(CreateUserInput input) {
        // Implementation
    }
}

// Controller
@Controller
public class UserController extends BaseGraphQLResolver {

    private final UserService userService;
    private final GraphQLSubscriptionManager<User> subscriptionManager;

    public UserController(UserService userService) {
        this.userService = userService;
        this.subscriptionManager = new GraphQLSubscriptionManager<>();
    }

    @QueryMapping
    public CompletableFuture<User> user(@Argument UUID id) {
        return asyncResult(() -> userService.findById(id));
    }

    @MutationMapping
    public CompletableFuture<User> createUser(@Argument CreateUserInput input) {
        return asyncResult(() -> userService.create(input))
            .thenApply(user -> {
                subscriptionManager.publish(user);
                return user;
            });
    }

    @SubscriptionMapping
    public Publisher<User> onUserCreated() {
        return subscriptionManager.subscribe();
    }
}

// Configuration
@Configuration
@Import(GraphQLConfiguration.class)
public class AppConfig {}
```

## Dependencies

Core dependencies:
- Spring Boot GraphQL Starter
- GraphQL Java Extended Scalars 21.0
- GraphQL Java Extended Validation 21.0

## License

MIT License
