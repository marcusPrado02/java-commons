# Commons App Audit Log

Comprehensive audit logging system with AOP-based automatic capturing and flexible storage.

## Features

- **Automatic audit logging** - AOP-based capturing with `@Audited` annotation
- **Manual audit logging** - Direct API for custom audit events
- **Flexible storage** - Pluggable repository interface
- **Rich event model** - Actor, action, resource, timestamp, metadata
- **Query API** - Powerful query builder for searching audit events
- **Actor providers** - Extract current user from security context
- **Async support** - Non-blocking audit event recording
- **Integration ready** - Spring, Jakarta EE, custom frameworks

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-app-audit-log</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For AOP support, add:

```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.20.1</version>
</dependency>
```

## Quick Start

### Manual Audit Logging

```java
import com.marcusprado02.commons.app.auditlog.*;

@Service
public class UserService {
    private final AuditService auditService;

    public void createUser(User user) {
        // Business logic...

        auditService.audit(
            AuditEvent.builder()
                .eventType("USER_CREATED")
                .actor(getCurrentUser())
                .action("create")
                .resourceType("User")
                .resourceId(user.getId())
                .metadata(Map.of(
                    "email", user.getEmail(),
                    "role", user.getRole()
                ))
                .build()
        );
    }

    public void deleteUser(String userId) {
        // Business logic...

        // Async audit (non-blocking)
        auditService.auditAsync(
            AuditEvent.builder()
                .eventType("USER_DELETED")
                .actor(getCurrentUser())
                .action("delete")
                .resourceType("User")
                .resourceId(userId)
                .build()
        );
    }
}
```

### Automatic Audit Logging with AOP

```java
@Service
public class UserService {

    @Audited(
        eventType = "USER_CREATED",
        action = "create",
        resourceType = "User",
        resourceIdExpression = "#result.id",
        includeResult = true
    )
    public User createUser(User user) {
        // Method implementation
        return repository.save(user);
    }

    @Audited(
        eventType = "USER_UPDATED",
        action = "update",
        resourceType = "User",
        resourceIdParam = "userId",
        includeParameters = true
    )
    public User updateUser(String userId, UserUpdateDto dto) {
        // Method implementation
        return repository.update(userId, dto);
    }

    @Audited(
        eventType = "USER_DELETED",
        action = "delete",
        resourceType = "User",
        resourceIdParam = "userId"
    )
    public void deleteUser(String userId) {
        repository.deleteById(userId);
    }
}
```

## Configuration

### Spring Configuration

```java
@Configuration
@EnableAspectJAutoProxy
public class AuditConfig {

    @Bean
    public AuditRepository auditRepository() {
        // For development/testing
        return new InMemoryAuditRepository();

        // For production - implement database repository
        // return new JpaAuditRepository(entityManager);
    }

    @Bean
    public AuditService auditService(AuditRepository repository) {
        return new DefaultAuditService(repository);
    }

    @Bean
    public ActorProvider actorProvider() {
        return new SecurityContextActorProvider();
    }

    @Bean
    public AuditAspect auditAspect(
            AuditService auditService,
            ActorProvider actorProvider) {
        return new AuditAspect(auditService, actorProvider);
    }
}
```

### Actor Provider Implementation

```java
@Component
public class SecurityContextActorProvider implements ActorProvider {

    @Override
    public String getCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    @Override
    public ActorContext getActorContext() {
        // Extract from HTTP request if available
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return new ActorContext(
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
            );
        }
        return null;
    }
}
```

## Audit Event Model

```java
AuditEvent event = AuditEvent.builder()
    .id("evt-123")                          // Optional - auto-generated if not provided
    .eventType("USER_LOGIN")                // Required - type of event
    .actor("user@example.com")              // Required - who performed the action
    .action("login")                        // Required - what was done
    .resourceType("Session")                // Optional - type of resource
    .resourceId("session-456")              // Optional - resource identifier
    .timestamp(Instant.now())               // Optional - defaults to now
    .ipAddress("192.168.1.1")               // Optional - actor's IP
    .userAgent("Mozilla/5.0...")            // Optional - actor's user agent
    .metadata(Map.of("device", "mobile"))   // Optional - additional data
    .result("SUCCESS")                      // Optional - defaults to SUCCESS
    .errorMessage(null)                     // Optional - error details if failed
    .build();
```

## Querying Audit Events

### Simple Queries

```java
// Find by actor
Result<List<AuditEvent>> userEvents =
    repository.findByActor("user@example.com", 100);

// Find by resource
Result<List<AuditEvent>> resourceEvents =
    repository.findByResource("User", "123", 50);

// Find by event type
Result<List<AuditEvent>> loginEvents =
    repository.findByEventType("USER_LOGIN", 100);
```

### Advanced Queries

```java
AuditQuery query = AuditQuery.builder()
    .actor("user@example.com")
    .eventType("USER_UPDATED")
    .action("update")
    .resourceType("User")
    .from(Instant.now().minus(Duration.ofDays(30)))
    .to(Instant.now())
    .result("SUCCESS")
    .limit(100)
    .offset(0)
    .build();

Result<List<AuditEvent>> events = repository.query(query);

events.ifOk(list -> {
    list.forEach(event -> {
        System.out.println(event.getActor() + " " +
                          event.getAction() + " " +
                          event.getResourceType() + "/" +
                          event.getResourceId());
    });
});
```

## @Audited Annotation Options

```java
@Audited(
    eventType = "ORDER_PLACED",              // Required - event type
    action = "create",                       // Required - action name
    resourceType = "Order",                  // Optional - resource type
    resourceIdParam = "orderId",             // Optional - param name for resource ID
    resourceIdExpression = "#result.id",     // Optional - SpEL to extract ID from result
    includeParameters = true,                // Optional - include method params in metadata
    includeResult = true,                    // Optional - include result in metadata
    auditOnFailure = true                    // Optional - audit even if method throws exception
)
public Order placeOrder(String orderId, OrderDto dto) {
    // Method implementation
}
```

### Resource ID Extraction

**From Parameter:**

```java
@Audited(
    eventType = "USER_VIEWED",
    action = "read",
    resourceType = "User",
    resourceIdParam = "userId"  // Extracts from method parameter named "userId"
)
public User getUser(String userId) {
    return repository.findById(userId);
}
```

**From Result:**

```java
@Audited(
    eventType = "USER_CREATED",
    action = "create",
    resourceType = "User",
    resourceIdExpression = "#result.id"  // Extracts from returned User object
)
public User createUser(CreateUserDto dto) {
    return repository.save(new User(dto));
}
```

## Storage Implementations

### In-Memory Repository (Testing)

```java
AuditRepository repository = new InMemoryAuditRepository();

// Store events
repository.save(event);

// Query
Result<List<AuditEvent>> events = repository.findByActor("user123", 10);

// Clear for testing
((InMemoryAuditRepository) repository).clear();
```

### Custom Database Repository

```java
@Repository
public class JpaAuditRepository implements AuditRepository {

    private final EntityManager entityManager;

    @Override
    public Result<Void> save(AuditEvent event) {
        try {
            AuditEventEntity entity = toEntity(event);
            entityManager.persist(entity);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.fail(
                ProblemBuilder.technical()
                    .message("Failed to save audit event")
                    .cause(e)
                    .build()
            );
        }
    }

    @Override
    public Result<List<AuditEvent>> query(AuditQuery query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEventEntity> cq = cb.createQuery(AuditEventEntity.class);
        Root<AuditEventEntity> root = cq.from(AuditEventEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        if (query.getActor() != null) {
            predicates.add(cb.equal(root.get("actor"), query.getActor()));
        }

        if (query.getEventType() != null) {
            predicates.add(cb.equal(root.get("eventType"), query.getEventType()));
        }

        // Add more predicates...

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("timestamp")));

        List<AuditEventEntity> entities = entityManager.createQuery(cq)
            .setFirstResult(query.getOffset())
            .setMaxResults(query.getLimit())
            .getResultList();

        List<AuditEvent> events = entities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());

        return Result.ok(events);
    }
}
```

## Integration Patterns

### REST API Audit Logging

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Audited(
        eventType = "USER_API_CREATE",
        action = "create",
        resourceType = "User",
        resourceIdExpression = "#result.body.id",
        includeParameters = true
    )
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserDto dto) {
        User user = userService.create(dto);
        return ResponseEntity.ok(user);
    }
}
```

### Async Operations

```java
@Service
public class EmailService {

    private final AuditService auditService;

    @Async
    public CompletableFuture<Void> sendEmail(String to, String subject) {
        // Send email...

        // Audit asynchronously (non-blocking)
        auditService.auditAsync(
            AuditEvent.builder()
                .eventType("EMAIL_SENT")
                .actor("system")
                .action("send")
                .resourceType("Email")
                .resourceId(to)
                .metadata(Map.of("subject", subject))
                .build()
        );

        return CompletableFuture.completedFuture(null);
    }
}
```

### Scheduled Tasks

```java
@Component
public class CleanupJob {

    private final AuditService auditService;

    @Scheduled(cron = "0 0 2 * * *")
    @Audited(
        eventType = "CLEANUP_EXECUTED",
        action = "execute",
        resourceType = "CleanupJob"
    )
    public void cleanupOldData() {
        // Cleanup logic...
    }
}
```

## Security Best Practices

1. **Protect Audit Logs** - Restrict access to audit data
2. **Encrypt Sensitive Data** - Don't store passwords or tokens
3. **Retention Policies** - Archive or delete old audit events
4. **Integrity** - Prevent tampering with audit logs
5. **Performance** - Use async auditing for high-throughput systems

### Filtering Sensitive Data

```java
public class SanitizingAuditService implements AuditService {

    private final AuditService delegate;
    private final Set<String> sensitiveFields = Set.of("password", "token", "secret");

    @Override
    public Result<Void> audit(AuditEvent event) {
        AuditEvent sanitized = sanitize(event);
        return delegate.audit(sanitized);
    }

    private AuditEvent sanitize(AuditEvent event) {
        Map<String, Object> metadata = new HashMap<>(event.getMetadata());

        sensitiveFields.forEach(field -> {
            if (metadata.containsKey(field)) {
                metadata.put(field, "***REDACTED***");
            }
        });

        return AuditEvent.builder()
            .id(event.getId())
            .eventType(event.getEventType())
            .actor(event.getActor())
            .action(event.getAction())
            .resourceType(event.getResourceType())
            .resourceId(event.getResourceId())
            .timestamp(event.getTimestamp())
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .metadata(metadata)
            .result(event.getResult())
            .errorMessage(event.getErrorMessage())
            .build();
    }
}
```

## Performance Considerations

### Async Auditing

For high-throughput systems, always use async auditing:

```java
// Sync - blocks until saved
auditService.audit(event);

// Async - returns immediately
auditService.auditAsync(event);
```

### Batch Processing

```java
public class BatchAuditService {

    private final AuditRepository repository;
    private final List<AuditEvent> buffer = new CopyOnWriteArrayList<>();
    private final int batchSize = 100;

    public void audit(AuditEvent event) {
        buffer.add(event);

        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    public void flush() {
        List<AuditEvent> toSave = new ArrayList<>(buffer);
        buffer.clear();
        repository.saveAll(toSave);
    }
}
```

## Testing

```java
@Test
void shouldAuditUserCreation() {
    // Arrange
    InMemoryAuditRepository repository = new InMemoryAuditRepository();
    AuditService auditService = new DefaultAuditService(repository);

    AuditEvent event = AuditEvent.builder()
        .eventType("USER_CREATED")
        .actor("admin")
        .action("create")
        .resourceType("User")
        .resourceId("123")
        .build();

    // Act
    Result<Void> result = auditService.audit(event);

    // Assert
    assertThat(result.isOk()).isTrue();
    assertThat(repository.size()).isEqualTo(1);

    Result<List<AuditEvent>> events = repository.findByActor("admin", 10);
    assertThat(events.value()).hasSize(1);
    assertThat(events.value().get(0).getEventType()).isEqualTo("USER_CREATED");
}
```

## Thread Safety

All classes in this module are thread-safe and can be safely shared across threads. The `InMemoryAuditRepository` uses `ConcurrentHashMap` for thread-safe storage.

## License

This module is part of the commons library and follows the same license.
