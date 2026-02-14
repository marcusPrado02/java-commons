# commons-kernel-errors

Sistema robusto de tratamento de erros com suporte a RFC 7807, internacionalização e contexto rico de observabilidade.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-errors</artifactId>
</dependency>
```

## 🎯 Visão Geral

Este módulo fornece uma forma estruturada e type-safe de lidar com erros em aplicações corporativas:

- **Problem**: Modelo de erro estruturado baseado em RFC 7807
- **StandardErrorCodes**: Catálogo de códigos de erro predefinidos
- **ProblemBuilder**: API fluente para construção de erros
- **ErrorContext**: Contexto rico para observabilidade e rastreamento distribuído
- **I18n Support**: Mensagens de erro internacionalizadas
- **RFC 7807 Compliance**: Formato padrão para erros em APIs HTTP

## 🏗️ Componentes Principais

### Problem

Representa um erro estruturado com código, mensagem, detalhes e metadados.

```java
Problem problem = Problem.of(
    ErrorCode.of("VALIDATION.REQUIRED_FIELD"),
    "Campos obrigatórios ausentes",
    List.of(
        ProblemDetail.of("email", "O campo email é obrigatório"),
        ProblemDetail.of("name", "O campo name é obrigatório")
    ),
    Map.of("correlationId", "abc-123")
);
```

### StandardErrorCodes

Catálogo de códigos de erro predefinidos seguindo convenção hierárquica.

```java
// Erros de validação
ErrorCode code = StandardErrorCodes.VALIDATION_REQUIRED_FIELD;
ErrorCode code = StandardErrorCodes.VALIDATION_INVALID_FORMAT;
ErrorCode code = StandardErrorCodes.VALIDATION_OUT_OF_RANGE;

// Erros de negócio
ErrorCode code = StandardErrorCodes.BUSINESS_RULE_VIOLATED;
ErrorCode code = StandardErrorCodes.BUSINESS_OPERATION_NOT_ALLOWED;

// Erros de recurso não encontrado
ErrorCode code = StandardErrorCodes.NOT_FOUND_RESOURCE;
ErrorCode code = StandardErrorCodes.NOT_FOUND_ENTITY;

// Erros de conflito
ErrorCode code = StandardErrorCodes.CONFLICT_ALREADY_EXISTS;
ErrorCode code = StandardErrorCodes.CONFLICT_OPTIMISTIC_LOCK;

// Erros de autorização
ErrorCode code = StandardErrorCodes.UNAUTHORIZED_MISSING_CREDENTIALS;
ErrorCode code = StandardErrorCodes.FORBIDDEN_INSUFFICIENT_PERMISSIONS;

// Erros técnicos
ErrorCode code = StandardErrorCodes.TECHNICAL_DATABASE_ERROR;
ErrorCode code = StandardErrorCodes.TECHNICAL_TIMEOUT;

// Erros de integração
ErrorCode code = StandardErrorCodes.INTEGRATION_SERVICE_UNAVAILABLE;
ErrorCode code = StandardErrorCodes.INTEGRATION_INVALID_RESPONSE;
```

**Convenção de Nomenclatura:**
```
DOMAIN.CATEGORY.SPECIFIC_ERROR

Exemplos:
- VALIDATION.REQUIRED_FIELD
- BUSINESS.RULE_VIOLATED
- NOT_FOUND.ENTITY
- TECHNICAL.DATABASE_ERROR
```

### ProblemBuilder

API fluente para construção ergonômica de Problems.

#### Factory Methods

```java
// Erro de validação
Problem problem = ProblemBuilder
    .validation(StandardErrorCodes.VALIDATION_REQUIRED_FIELD, "Email é obrigatório")
    .detail("email", "Campo não pode ser vazio")
    .build();

// Erro de negócio
Problem problem = ProblemBuilder
    .business(StandardErrorCodes.BUSINESS_RULE_VIOLATED, "Operação não permitida")
    .detail("reason", "Usuário não possui saldo suficiente")
    .meta("accountBalance", "50.00")
    .build();

// Erro de recurso não encontrado
Problem problem = ProblemBuilder
    .notFound(StandardErrorCodes.NOT_FOUND_ENTITY, "Usuário não encontrado")
    .detail("userId", "123")
    .build();

// Erro técnico
Problem problem = ProblemBuilder
    .technical(StandardErrorCodes.TECHNICAL_DATABASE_ERROR, "Erro ao acessar banco")
    .detail("cause", e.getMessage())
    .build();
```

#### Metadata Helpers

```java
Problem problem = ProblemBuilder
    .of(StandardErrorCodes.BUSINESS_RULE_VIOLATED)
    .message("Operação falhou")
    .correlationId("abc-123")       // Correlation ID para rastreamento
    .traceId("xyz-789")              // Trace ID para distributed tracing
    .userId("user-456")              // ID do usuário que causou o erro
    .tenantId("tenant-001")          // ID do tenant (multi-tenancy)
    .helpUrl("https://docs.example.com/errors/BUSINESS.RULE_VIOLATED")
    .build();
```

#### Lançando Exceções

```java
// Lança DomainException
ProblemBuilder
    .validation(StandardErrorCodes.VALIDATION_REQUIRED_FIELD, "Email obrigatório")
    .detail("email", "Campo vazio")
    .throwIt();  // throws DomainException

// Lança exceção customizada
ProblemBuilder
    .notFound(StandardErrorCodes.NOT_FOUND_ENTITY, "Usuário não encontrado")
    .throwAs(NotFoundException::new);  // throws NotFoundException
```

### ErrorContext

Builder para contexto rico de erro com informações de observabilidade e rastreamento distribuído.

#### Contexto de Rastreamento

```java
ErrorContext context = ErrorContext.builder()
    .correlationId("abc-123")           // Correlation ID
    .traceId("trace-xyz-789")           // Distributed trace ID
    .spanId("span-456")                 // Span ID
    .sessionId("session-001")           // Session ID
    .build();
```

#### Contexto de Usuário

```java
ErrorContext context = ErrorContext.builder()
    .userId("user-123")
    .tenantId("tenant-001")
    .build();
```

#### Contexto HTTP

```java
ErrorContext context = ErrorContext.builder()
    .httpMethod("POST")
    .httpPath("/api/users")
    .ipAddress("192.168.1.1")
    .userAgent("Mozilla/5.0...")
    .build();
```

#### Contexto de Aplicação

```java
ErrorContext context = ErrorContext.builder()
    .environment("production")
    .appVersion("1.2.3")
    .operation("CreateUser")
    .resource("User", "123")            // resource(type, id)
    .build();
```

#### Contexto de Exceção

```java
try {
    // ...
} catch (Exception e) {
    ErrorContext context = ErrorContext.builder()
        .exceptionClass(e.getClass().getName())
        .exceptionMessage(e.getMessage())
        .timestamp(Instant.now())
        .build();
}
```

#### Envolvendo Problems

```java
Problem problem = ProblemBuilder
    .validation(StandardErrorCodes.VALIDATION_REQUIRED_FIELD, "Email obrigatório")
    .build();

ErrorEnvelope envelope = ErrorContext.builder()
    .correlationId("abc-123")
    .traceId("xyz-789")
    .userId("user-456")
    .environment("production")
    .timestamp(Instant.now())
    .wrapProblem(problem);  // Retorna ErrorEnvelope com contexto

// ErrorEnvelope contém:
// - error: Problem original
// - context: Map com todos os metadados de contexto
```

### I18n Support

Suporte a mensagens de erro internacionalizadas usando ResourceBundle.

#### Configurando Resolver

```java
// 1. Crie um ResourceBundle (ex: errors_pt_BR.properties, errors_en_US.properties)
// errors_pt_BR.properties:
// VALIDATION.REQUIRED_FIELD=O campo {0} é obrigatório
// BUSINESS.INSUFFICIENT_BALANCE=Saldo insuficiente. Saldo atual: {0}, necessário: {1}

// 2. Configure o resolver
I18nMessageResolver resolver = new ResourceBundleMessageResolver("errors");
Problems.setMessageResolver(resolver);
```

#### Usando I18n em Problems

```java
// Mensagem internacionalizada
Problem problem = Problems.validation(
    StandardErrorCodes.VALIDATION_REQUIRED_FIELD,
    "VALIDATION.REQUIRED_FIELD",  // message key
    Locale.of("pt", "BR"),
    "Campo obrigatório",           // fallback message
    "email"                        // placeholder {0}
);

// Com múltiplos placeholders
Problem problem = Problems.business(
    StandardErrorCodes.BUSINESS_INSUFFICIENT_BALANCE,
    "BUSINESS.INSUFFICIENT_BALANCE",
    Locale.of("pt", "BR"),
    "Saldo insuficiente",
    "50.00",    // {0}
    "100.00"    // {1}
);
```

#### No-Op Resolver

```java
// Para desabilitar I18n (apenas usa fallback)
Problems.setMessageResolver(I18nMessageResolver.noOp());
```

### RFC 7807 Compliance

Representação de erros seguindo RFC 7807 (Problem Details for HTTP APIs).

#### Convertendo Problem para RFC 7807

```java
Problem problem = Problems.validation(
    StandardErrorCodes.VALIDATION_REQUIRED_FIELD,
    "Email é obrigatório"
);

RFC7807ProblemDetail rfc7807 = RFC7807ProblemDetail.from(problem);

// Serializa como JSON (com Jackson, etc)
// {
//   "type": "https://example.com/errors/VALIDATION.REQUIRED_FIELD",
//   "title": "Validation Error",
//   "status": 400,
//   "detail": "Email é obrigatório - email: Campo vazio",
//   "instance": null,
//   "extensions": {
//     "correlationId": "abc-123"
//   }
// }
```

#### Builder Customizado

```java
RFC7807ProblemDetail detail = RFC7807ProblemDetail.builder()
    .type(URI.create("https://example.com/errors/business-rule"))
    .title("Business Rule Violated")
    .status(422)
    .detail("Operação não permitida")
    .instance(URI.create("/api/transactions/123"))
    .extension("transactionId", "tx-123")
    .extension("reason", "insufficient-balance")
    .build();
```

#### Integração com Spring Boot

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<RFC7807ProblemDetail> handleDomainException(DomainException ex) {
        RFC7807ProblemDetail detail = RFC7807ProblemDetail.from(ex.problem());
        return ResponseEntity
            .status(detail.status())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(detail);
    }
}
```

## 📋 Exemplos Práticos

### Exemplo 1: Validação de Campos

```java
public class CreateUserValidator {

    public void validate(CreateUserCommand command) {
        ValidationResult<Void> result = ValidationResult.builder();

        if (command.email() == null || command.email().isBlank()) {
            result.addError(Problems.validation(
                StandardErrorCodes.VALIDATION_REQUIRED_FIELD,
                "Email é obrigatório",
                ProblemDetail.of("email", "Campo não pode ser vazio")
            ));
        }

        if (command.age() != null && command.age() < 18) {
            result.addError(Problems.validation(
                StandardErrorCodes.VALIDATION_OUT_OF_RANGE,
                "Idade inválida",
                ProblemDetail.of("age", "Deve ser maior ou igual a 18")
            ));
        }

        result.build().orElseThrow(problems ->
            new ValidationException(Problems.combine(
                StandardErrorCodes.VALIDATION_FAILED,
                "Validação falhou",
                problems
            ))
        );
    }
}
```

### Exemplo 2: Regras de Negócio com Contexto

```java
public class TransferService {

    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        Account from = accountRepository.findById(fromAccountId)
            .orElseThrow(() ->
                ProblemBuilder
                    .notFound(StandardErrorCodes.NOT_FOUND_ENTITY, "Conta origem não encontrada")
                    .detail("accountId", fromAccountId)
                    .correlationId(RequestContext.getCorrelationId())
                    .throwAs(NotFoundException::new)
            );

        if (from.balance().compareTo(amount) < 0) {
            ProblemBuilder
                .business(StandardErrorCodes.BUSINESS_INSUFFICIENT_BALANCE, "Saldo insuficiente")
                .detail("balance", from.balance().toString())
                .detail("requested", amount.toString())
                .meta("accountId", fromAccountId)
                .correlationId(RequestContext.getCorrelationId())
                .traceId(RequestContext.getTraceId())
                .throwIt();
        }

        // ... lógica de transferência
    }
}
```

### Exemplo 3: Tratamento de Erros de Integração

```java
public class PaymentGatewayClient {

    public PaymentResult processPayment(PaymentRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 500) {
                Problem problem = ProblemBuilder
                    .technical(StandardErrorCodes.INTEGRATION_SERVICE_UNAVAILABLE, "Gateway indisponível")
                    .detail("statusCode", String.valueOf(response.statusCode()))
                    .detail("response", response.body())
                    .correlationId(request.correlationId())
                    .build();

                return PaymentResult.failure(problem);
            }

            // ... parse response

        } catch (IOException e) {
            ErrorEnvelope envelope = ErrorContext.builder()
                .correlationId(request.correlationId())
                .traceId(request.traceId())
                .operation("ProcessPayment")
                .exceptionClass(e.getClass().getName())
                .exceptionMessage(e.getMessage())
                .timestamp(Instant.now())
                .wrapProblem(Problems.technical(
                    StandardErrorCodes.TECHNICAL_NETWORK_ERROR,
                    "Erro de rede ao comunicar com gateway"
                ));

            log.error("Payment processing failed: {}", envelope, e);
            return PaymentResult.failure(envelope.error());
        }
    }
}
```

### Exemplo 4: API REST com RFC 7807

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            User user = userService.create(request);
            return ResponseEntity.ok(user);

        } catch (DomainException e) {
            RFC7807ProblemDetail detail = Problems.toRFC7807(e.problem());

            return ResponseEntity
                .status(detail.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
        }
    }
}
```

### Exemplo 5: Observabilidade e Logging Estruturado

```java
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                ErrorEnvelope envelope = ErrorContext.builder()
                    .correlationId(MDC.get("correlationId"))
                    .traceId(MDC.get("traceId"))
                    .userId(SecurityContext.getCurrentUserId())
                    .operation("CancelOrder")
                    .resource("Order", orderId)
                    .timestamp(Instant.now())
                    .wrapProblem(Problems.notFound(
                        StandardErrorCodes.NOT_FOUND_ENTITY,
                        "Pedido não encontrado"
                    ));

                log.warn("Order not found: {}", envelope.context());
                return new NotFoundException(envelope.error());
            });

        if (!order.canBeCancelled()) {
            ErrorEnvelope envelope = ErrorContext.builder()
                .correlationId(MDC.get("correlationId"))
                .traceId(MDC.get("traceId"))
                .userId(SecurityContext.getCurrentUserId())
                .operation("CancelOrder")
                .resource("Order", orderId)
                .put("orderStatus", order.status().toString())
                .timestamp(Instant.now())
                .wrapProblem(Problems.business(
                    StandardErrorCodes.BUSINESS_OPERATION_NOT_ALLOWED,
                    "Pedido não pode ser cancelado no status atual"
                ));

            log.warn("Order cancellation not allowed: {}", envelope.context());
            throw new BusinessException(envelope.error());
        }

        // ... lógica de cancelamento
    }
}
```

## 🎯 Best Practices

### ✅ DO

```java
// Use StandardErrorCodes para consistência
ProblemBuilder.validation(StandardErrorCodes.VALIDATION_REQUIRED_FIELD, "Email obrigatório")

// Adicione correlation e trace IDs para rastreamento
ProblemBuilder.business(code, message)
    .correlationId(requestContext.correlationId())
    .traceId(requestContext.traceId())

// Use ErrorContext para logging estruturado
ErrorContext.builder()
    .correlationId(correlationId)
    .userId(userId)
    .operation("CreateUser")
    .wrapProblem(problem)

// Lance exceções tipadas quando apropriado
ProblemBuilder.notFound(code, message).throwAs(NotFoundException::new)

// Use I18n para mensagens voltadas ao usuário
Problems.validation(code, messageKey, locale, fallback, args)

// Converta para RFC 7807 em APIs REST
return ResponseEntity.status(400).body(Problems.toRFC7807(problem))
```

### ❌ DON'T

```java
// Não crie ErrorCodes adhoc sem padronização
ErrorCode.of("ERRO_1")  // ❌ Sem contexto

// Não ignore contexto de rastreamento
ProblemBuilder.of(code).build()  // ❌ Sem correlationId/traceId

// Não misture exceptions não tratadas
throw new RuntimeException("Erro")  // ❌ Perde estrutura de Problem

// Não exponha detalhes técnicos ao usuário
Problem.of(code, e.getStackTrace().toString())  // ❌ Informação sensível

// Não use metadata para dados sensíveis
Problem.of(code, message, Map.of("password", pwd))  // ❌ Security risk
```

## 🔗 Integração com Outros Módulos

### com commons-kernel-result

```java
public Result<User> findUser(String userId) {
    return userRepository.findById(userId)
        .map(Result::ok)
        .orElseGet(() -> Result.fail(
            Problems.notFound(
                StandardErrorCodes.NOT_FOUND_ENTITY,
                "Usuário não encontrado",
                ProblemDetail.of("userId", userId)
            )
        ));
}
```

### com commons-kernel-ddd

```java
@Entity
public class Order extends AggregateRoot<OrderId> {

    public void cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw ProblemBuilder
                .business(StandardErrorCodes.BUSINESS_OPERATION_NOT_ALLOWED, "Pedido já entregue")
                .detail("status", status.toString())
                .throwAs(BusinessException::new);
        }

        // ... lógica de cancelamento
        addDomainEvent(new OrderCancelledEvent(id()));
    }
}
```

## 📚 Referências

- [RFC 7807 - Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807)
- [Spring Boot - Problem Details](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Zalando Problem](https://github.com/zalando/problem)

## 📄 Licença

Este projeto está sob a licença definida no arquivo raiz do repositório.
