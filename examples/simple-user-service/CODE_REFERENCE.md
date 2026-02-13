# Examples - Code Structure Reference

Este documento serve como referência rápida da estrutura de código do exemplo Simple User Service.

## Code Snippets - Principais Conceitos

### 1. Domain Layer (Kernel - Framework Free)

#### User.java - Entity
```java
package com.example.userservice.domain;

import com.marcusprado02.commons.kernel.ddd.entity.Entity;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;

public class User extends Entity<UserId> {
  private Email email;
  private UserName name;
  private UserStatus status;
  
  public User(UserId id, TenantId tenantId, Email email, 
              UserName name, AuditStamp created) {
    super(id, tenantId, created);
    this.email = email;
    this.name = name;
    this.status = UserStatus.ACTIVE;
  }
  
  public void activate() {
    this.status = UserStatus.ACTIVE;
  }
  
  public void deactivate() {
    this.status = UserStatus.INACTIVE;
  }
  
  // Getters
  public Email email() { return email; }
  public UserName name() { return name; }
  public UserStatus status() { return status; }
}
```

#### Value Objects
```java
// Email.java
public final class Email extends ValueObject {
  private final String value;
  
  private Email(String value) {
    this.value = value;
  }
  
  public static Email of(String value) {
    Invariant.notBlank(value, "email");
    Invariant.isTrue(value.contains("@"), "Invalid email format");
    return new Email(value.toLowerCase().trim());
  }
  
  public String value() {
    return value;
  }
}

// UserName.java  
public final class UserName extends ValueObject {
  private final String value;
  
  public static UserName of(String value) {
    Invariant.notBlank(value, "name");
    Invariant.isTrue(value.length() <= 100, "Name too long");
    return new UserName(value.trim());
  }
}

// UserId.java
public final class UserId extends ValueObject {
  private final String value;
  
  public static UserId of(String value) {
    return new UserId(value);
  }
  
  public static UserId generate() {
    return new UserId(UUID.randomUUID().toString());
  }
}
```

### 2. Application Layer (Use Cases)

```java
// CreateUserUseCase.java
public class CreateUserUseCase {
  private final UserRepository userRepository;
  private final ClockProvider clockProvider;
  
  public Result<User, UserError> execute(CreateUserCommand cmd) {
    // Validation
    if (userRepository.findByEmail(cmd.email()).isPresent()) {
      return Result.failure(UserError.EMAIL_ALREADY_EXISTS);
    }
    
    // Create domain object
    var user = new User(
      UserId.generate(),
      TenantId.DEFAULT,
      cmd.email(),
      cmd.name(),
      AuditStamp.now(cmd.actorId(), clockProvider)
    );
    
    // Persist
    userRepository.save(user);
    
    return Result.success(user);
  }
}

// Commands (immutable DTOs)
public record CreateUserCommand(
  Email email,
  UserName name,
  ActorId actorId
) {}
```

### 3. Ports Layer (Interfaces)

```java
// UserRepository.java
package com.example.userservice.ports;

import com.marcusprado02.commons.kernel.result.Option;

public interface UserRepository {
  void save(User user);
  Option<User> findById(UserId id);
  Option<User> findByEmail(Email email);
  List<User> findAll();
  void delete(UserId id);
}
```

### 4. Adapters Layer (Infrastructure)

#### Persistence Adapter
```java
// JpaUserRepository.java
@Repository
public class JpaUserRepository implements UserRepository {
  
  @PersistenceContext
  private EntityManager entityManager;
  
  @Override
  @Transactional
  public void save(User user) {
    UserEntity entity = UserMapper.toEntity(user);
    entityManager.merge(entity);
  }
  
  @Override
  public Option<User> findById(UserId id) {
    UserEntity entity = entityManager.find(UserEntity.class, id.value());
    return entity == null 
      ? Option.none() 
      : Option.of(UserMapper.toDomain(entity));
  }
  
  @Override
  public Option<User> findByEmail(Email email) {
    var query = entityManager
      .createQuery("SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
      .setParameter("email", email.value());
    
    return query.getResultList().stream()
      .findFirst()
      .map(UserMapper::toDomain)
      .map(Option::of)
      .orElse(Option.none());
  }
}

// UserEntity.java (JPA)
@Entity
@Table(name = "users")
class UserEntity {
  @Id
  private String id;
  private String email;
  private String name;
  private String status;
  private String tenantId;
  private Instant createdAt;
  
  // Getters/Setters
}

// UserMapper.java
class UserMapper {
  static User toDomain(UserEntity entity) {
    // Convert JPA entity → Domain object
  }
  
  static UserEntity toEntity(User user) {
    // Convert Domain object → JPA entity
  }
}
```

#### Web Adapter
```java
// UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
  
  private final CreateUserUseCase createUserUseCase;
  private final FindUserUseCase findUserUseCase;
  private final ListUsersUseCase listUsersUseCase;
  
  @PostMapping
  public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
    Result<User, UserError> result = createUserUseCase.execute(
      new CreateUserCommand(
        Email.of(request.email()),
        UserName.of(request.name()),
        ActorId.SYSTEM
      )
    );
    
    return result
      .map(user -> ResponseEntity.ok(UserResponse.from(user)))
      .getOrElse(error -> ResponseEntity
        .badRequest()
        .body(Map.of("error", error.message())));
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<?> getUser(@PathVariable String id) {
    return findUserUseCase.execute(UserId.of(id))
      .map(user -> ResponseEntity.ok(UserResponse.from(user)))
      .getOrElse(() -> ResponseEntity.notFound().build());
  }
  
  @GetMapping
  public List<UserResponse> listUsers() {
    return listUsersUseCase.execute().stream()
      .map(UserResponse::from)
      .toList();
  }
}

// DTOs
record CreateUserRequest(String email, String name) {}

record UserResponse(
  String id,
  String email,
  String name,
  String status
) {
  static UserResponse from(User user) {
    return new UserResponse(
      user.id().value(),
      user.email().value(),
      user.name().value(),
      user.status().name()
    );
  }
}
```

### 5. Main Application

```java
// UserServiceApplication.java
@SpringBootApplication
public class UserServiceApplication {
  
  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
  
  // Bean configurations
  @Bean
  public ClockProvider clockProvider() {
    return Clock::systemUTC;
  }
  
  @Bean
  public TenantProvider tenantProvider() {
    return () -> TenantId.DEFAULT;
  }
  
  @Bean
  public CreateUserUseCase createUserUseCase(
      UserRepository userRepository, 
      ClockProvider clockProvider) {
    return new CreateUserUseCase(userRepository, clockProvider);
  }
}
```

## Testes

### Domain Tests (Fast, No Infrastructure)

```java
class UserTest {
  
  @Test
  void shouldCreateUserWithValidData() {
    // Given
    var email = Email.of("test@example.com");
    var name = UserName.of("Test User");
    
    // When
    var user = new User(
      UserId.generate(),
      TenantId.DEFAULT,
      email,
      name,
      AuditStamp.now(ActorId.SYSTEM)
    );
    
    // Then
    assertThat(user.email()).isEqualTo(email);
    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
  }
  
  @Test
  void shouldDeactivateUser() {
    var user = createTestUser();
    
    user.deactivate();
    
    assertThat(user.status()).isEqualTo(UserStatus.INACTIVE);
  }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserServiceIntegrationTest {
  
  @Autowired
  private TestRestTemplate restTemplate;
  
  @Test
  void shouldCreateAndRetrieveUser() {
    // Given
    var request = new CreateUserRequest("test@example.com", "Test User");
    
    // When - Create
    var createResponse = restTemplate.postForEntity(
      "/api/users", 
      request, 
      UserResponse.class
    );
    
    // Then
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    UserResponse created = createResponse.getBody();
    assertThat(created.email()).isEqualTo("test@example.com");
    
    // When - Retrieve
    var getResponse = restTemplate.getForEntity(
      "/api/users/" + created.id(), 
      UserResponse.class
    );
    
    // Then
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isEqualTo(created);
  }
}
```

## Dependency Injection Flow

```
Main (Spring Boot)
  └─> Configura Beans
       ├─> UserRepository (implementação JPA)
       ├─> ClockProvider
       ├─> TenantProvider
       └─> Use Cases
            ├─> CreateUserUseCase (recebe repository, clock)
            ├─> FindUserUseCase
            └─> ListUsersUseCase

Controller
  └─> Injeta Use Cases
       └─> Chama execute()
            └─> Usa Ports (repository)
                 └─> Adapter implementa
```

## Notas Importantes

1. **Domain nunca depende de** Spring, JPA, HTTP
2. **Use Cases orquestram** domínio + ports
3. **Adapters implementam** ports usando frameworks
4. **Result type** evita exceptions para casos de negócio
5. **Value Objects** encapsulam validação

## Para Implementar

Os snippets acima fornecem a estrutura completa. Para implementação real:

1. Criar pacotes conforme estrutura
2. Copiar e adaptar código dos snippets
3. Adicionar tratamento de erros completo
4. Implementar testes
5. Executar e testar via REST

---

**Ver também**: [README do exemplo](README.md) para mais detalhes.
