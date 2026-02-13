# Simple User Service - Exemplo

Exemplo simples demonstrando uso do java-commons para criar um microserviço de gerenciamento de usuários.

## 📋 O que este exemplo demonstra

- ✅ **Domain-Driven Design**: Entity, Value Objects, Aggregate
- ✅ **Hexagonal Architecture**: Separação clara de camadas
- ✅ **Result Type Pattern**: Tratamento elegante de erros
- ✅ **Persistence**: Repository pattern com JPA
- ✅ **REST API**: Controllers Spring Boot
- ✅ **Testing**: Testes unitários e de integração

## 🏗️ Arquitetura

```
┌─────────────────────────────────────┐
│        REST API (Controller)        │  ← Adapter (Input)
├─────────────────────────────────────┤
│      Application (Use Cases)        │  ← Application Layer
├─────────────────────────────────────┤
│        Ports (Interfaces)           │  ← Hexagonal Boundary
├─────────────────────────────────────┤
│      Domain (User, Email, etc.)     │  ← Kernel (Pure)
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│     JPA Repository Adapter          │  ← Adapter (Output)
└─────────────────────────────────────┘
```

## 📦 Estrutura do Código

```
src/main/java/com/example/userservice/
├── domain/                    # Kernel (framework-free)
│   ├── User.java             # Entity
│   ├── UserId.java           # Value Object (ID)
│   ├── Email.java            # Value Object
│   ├── UserName.java         # Value Object
│   └── UserStatus.java       # Enum
│
├── application/               # Use Cases
│   ├── CreateUserUseCase.java
│   ├── FindUserUseCase.java
│   └── ListUsersUseCase.java
│
├── ports/                     # Interfaces
│   └── UserRepository.java   # Port Interface
│
├── adapters/                  # Implementações
│   ├── persistence/
│   │   ├── JpaUserRepository.java
│   │   ├── UserEntity.java
│   │   └── UserMapper.java
│   └── web/
│       ├── UserController.java
│       ├── UserRequest.java
│       └── UserResponse.java
│
└── UserServiceApplication.java
```

## 🚀 Como Executar

### 1. Compilar

```bash
cd examples/simple-user-service
mvn clean install
```

### 2. Executar

```bash
mvn spring-boot:run
```

Aplicação inicia em `http://localhost:8080`

### 3. Testar API

```bash
# Criar usuário
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "name": "John Doe"
  }'

# Listar usuários
curl http://localhost:8080/api/users

# Buscar por ID
curl http://localhost:8080/api/users/{id}

# Buscar por email
curl http://localhost:8080/api/users/search?email=john@example.com
```

## 📚 Conceitos Demonstrados

### 1. Domain-Driven Design

#### Entity com Identidade

```java
public class User extends Entity<UserId> {
  private Email email;
  private UserName name;
  private UserStatus status;
  
  // Comportamento de domínio
  public void activate() { ... }
  public void deactivate() { ... }
}
```

#### Value Objects Imutáveis

```java
public final class Email extends ValueObject {
  private final String value;
  
  public static Email of(String value) {
    // Validações
    Invariant.notBlank(value, "email");
    Invariant.isTrue(value.contains("@"), "Invalid email");
    return new Email(value);
  }
}
```

### 2. Hexagonal Architecture

#### Port (Interface)

```java
public interface UserRepository {
  void save(User user);
  Option<User> findById(UserId id);
  Option<User> findByEmail(Email email);
  List<User> findAll();
}
```

#### Adapter (Implementação JPA)

```java
@Repository
public class JpaUserRepository implements UserRepository {
  // Implementa port usando JPA
}
```

### 3. Result Type Pattern

```java
public Result<User, UserError> execute(CreateUserCommand cmd) {
  // Validação: email já existe?
  if (userRepository.findByEmail(cmd.email()).isPresent()) {
    return Result.failure(UserError.EMAIL_ALREADY_EXISTS);
  }
  
  // Criar e salvar
  User user = new User(...);
  userRepository.save(user);
  
  return Result.success(user);
}
```

### 4. Application Use Cases

```java
public class CreateUserUseCase {
  private final UserRepository userRepository;
  
  public Result<User, UserError> execute(CreateUserCommand cmd) {
    // Orquestra: validação → criação → persistência
  }
}
```

### 5. REST Adapter

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
  
  @PostMapping
  public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
    Result<User, UserError> result = createUserUseCase.execute(request.toCommand());
    
    return result
      .map(user -> ResponseEntity.ok(UserResponse.from(user)))
      .getOrElse(this::handleError);
  }
}
```

## 🧪 Testes

### Testes de Domínio (Rápidos)

```java
@Test
void shouldCreateUserWithValidData() {
  var user = new User(
    UserId.generate(),
    TenantId.DEFAULT,
    Email.of("test@example.com"),
    UserName.of("Test User"),
    AuditStamp.now(ActorId.SYSTEM)
  );
  
  assertThat(user.email().value()).isEqualTo("test@example.com");
  assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
}
```

### Testes de Integração (com Testcontainers)

```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {
  
  @Container
  static PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>("postgres:15-alpine");
  
  @Test
  void shouldCreateAndFindUser() {
    // Given
    var request = new CreateUserRequest("test@example.com", "Test");
    
    // When
    var response = restTemplate.postForEntity("/api/users", request, UserResponse.class);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
```

## 📖 Lições Aprendidas

### ✅ Boas Práticas Aplicadas

1. **Domínio Puro**: `User`, `Email`, `UserName` não dependem de frameworks
2. **Value Objects**: Encapsulam validação e lógica
3. **Result Type**: Erros de negócio são valores, não exceptions
4. **Ports & Adapters**: Fácil trocar JPA por outro ORM
5. **Testabilidade**: Domínio testável sem banco de dados

### ⚠️ Simplificações (não use em produção)

1. **Sem autenticação/autorização**: Foco em arquitetura
2. **Banco H2 em memória**: Facilita quick start
3. **Sem paginação**: Exemplo mantido simples
4. **Sem cache**: Não é foco deste exemplo

## 🔄 Evoluindo o Exemplo

Próximos passos para tornar production-ready:

1. **Segurança**: Adicionar Spring Security + OAuth2
2. **Observability**: Correlation ID, tracing, metrics
3. **Resilience**: Circuit breaker, retry policies
4. **Paginação**: Usar `PageableRepository`
5. **Validation**: Bean Validation nos DTOs
6. **API Docs**: OpenAPI/Swagger
7. **Docker**: Containerização
8. **Kubernetes**: Manifests

## 📚 Referências

- [Architecture Guide](../../docs/architecture.md)
- [Usage Patterns](../../docs/usage-patterns.md)
- [DDD ADR](../../docs/adr/0002-domain-driven-design.md)
- [Hexagonal ADR](../../docs/adr/0001-hexagonal-architecture.md)

## 💡 Dúvidas?

- Leia a [documentação principal](../../README.md)
- Veja [outros exemplos](../README.md)
- Abra uma issue no repositório
