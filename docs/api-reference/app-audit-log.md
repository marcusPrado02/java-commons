# API Reference: Audit Log

## Visão Geral

`commons-app-audit-log` fornece sistema completo de auditoria para rastrear todas as operações críticas, mudanças de dados, acessos e ações de usuários com compliance GDPR/LGPD.

**Quando usar:**
- Rastrear quem fez o quê e quando
- Compliance (SOX, GDPR, LGPD, HIPAA)
- Investigação de incidentes de segurança
- Análise de comportamento de usuários
- Recuperação e rollback de operações

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-audit-log</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### AuditLog

Representa um log de auditoria.

```java
public class AuditLog {
    
    private final AuditLogId id;
    private final Instant timestamp;
    private final String actor;           // user-123 ou system
    private final String action;          // CREATE, UPDATE, DELETE, LOGIN
    private final String resource;        // orders, users, payments
    private final String resourceId;      // order-123
    private final AuditLogLevel level;    // INFO, WARNING, CRITICAL
    private final Map<String, Object> before;  // Estado anterior
    private final Map<String, Object> after;   // Estado posterior
    private final String ipAddress;
    private final String userAgent;
    private final Map<String, String> metadata;
    
    public boolean isDataChange() {
        return before != null || after != null;
    }
    
    public boolean isCritical() {
        return level == AuditLogLevel.CRITICAL;
    }
}
```

### AuditLogger

Interface para registrar logs de auditoria.

```java
public interface AuditLogger {
    
    /**
     * Registra ação de auditoria.
     */
    void log(
        String action,
        String resource,
        String resourceId
    );
    
    /**
     * Registra mudança de dados.
     */
    void logDataChange(
        String action,
        String resource,
        String resourceId,
        Object before,
        Object after
    );
    
    /**
     * Registra acesso a recurso.
     */
    void logAccess(
        String resource,
        String resourceId,
        AccessType accessType
    );
    
    /**
     * Registra evento crítico.
     */
    void logCritical(
        String action,
        String resource,
        String resourceId,
        String reason
    );
}
```

### AuditLogRepository

Armazena e consulta logs.

```java
public interface AuditLogRepository {
    
    /**
     * Salva log de auditoria.
     */
    Result<Void> save(AuditLog log);
    
    /**
     * Busca logs por recurso.
     */
    List<AuditLog> findByResource(
        String resource,
        String resourceId,
        Instant from,
        Instant to
    );
    
    /**
     * Busca logs por ator.
     */
    List<AuditLog> findByActor(
        String actor,
        Instant from,
        Instant to
    );
    
    /**
     * Busca eventos críticos.
     */
    List<AuditLog> findCriticalEvents(Instant from, Instant to);
}
```

---

## 💡 Uso Básico

### Manual Logging

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final AuditLogger auditLogger;
    
    @Transactional
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        Order order = Order.create(command);
        Result<Void> saveResult = orderRepository.save(order);
        
        if (saveResult.isFail()) {
            return Result.fail(saveResult.problemOrNull());
        }
        
        // Audit log
        auditLogger.log(
            "CREATE",
            "orders",
            order.id().value()
        );
        
        return Result.ok(order.id());
    }
    
    @Transactional
    public Result<Void> updateOrder(OrderId orderId, UpdateOrderCommand command) {
        return orderRepository.findById(orderId)
            .flatMap(order -> {
                // Capturar estado anterior
                Map<String, Object> before = toMap(order);
                
                // Aplicar mudanças
                order.update(command);
                
                Result<Void> saveResult = orderRepository.save(order);
                if (saveResult.isFail()) {
                    return saveResult;
                }
                
                // Audit log com before/after
                Map<String, Object> after = toMap(order);
                auditLogger.logDataChange(
                    "UPDATE",
                    "orders",
                    orderId.value(),
                    before,
                    after
                );
                
                return Result.ok();
            });
    }
}
```

---

## 🎯 Annotation-Based Auditing

### @Audited Annotation

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    
    /**
     * Ação sendo auditada.
     */
    String action();
    
    /**
     * Tipo de recurso.
     */
    String resource();
    
    /**
     * SpEL expression para resource ID.
     */
    String resourceId() default "";
    
    /**
     * Capturar estado antes/depois?
     */
    boolean captureData() default false;
    
    /**
     * Nível de criticidade.
     */
    AuditLogLevel level() default AuditLogLevel.INFO;
}
```

### Audit Aspect

```java
@Aspect
@Component
public class AuditAspect {
    
    private final AuditLogger auditLogger;
    private final SpelExpressionParser parser;
    
    @Around("@annotation(audited)")
    public Object auditMethod(
        ProceedingJoinPoint joinPoint,
        Audited audited
    ) throws Throwable {
        
        Instant start = Instant.now();
        Object result = null;
        Throwable error = null;
        Object before = null;
        
        try {
            // Capturar estado anterior se necessário
            if (audited.captureData()) {
                before = captureState(joinPoint);
            }
            
            // Executar método
            result = joinPoint.proceed();
            
            return result;
            
        } catch (Throwable t) {
            error = t;
            throw t;
            
        } finally {
            // Registrar audit log
            logAudit(joinPoint, audited, before, result, error, start);
        }
    }
    
    private void logAudit(
        ProceedingJoinPoint joinPoint,
        Audited audited,
        Object before,
        Object after,
        Throwable error,
        Instant start
    ) {
        try {
            // Resolver resource ID via SpEL
            String resourceId = resolveResourceId(
                joinPoint,
                audited.resourceId()
            );
            
            if (audited.captureData() && before != null) {
                auditLogger.logDataChange(
                    audited.action(),
                    audited.resource(),
                    resourceId,
                    before,
                    after
                );
            } else {
                auditLogger.log(
                    audited.action(),
                    audited.resource(),
                    resourceId
                );
            }
            
            if (error != null) {
                auditLogger.logCritical(
                    audited.action(),
                    audited.resource(),
                    resourceId,
                    "Operation failed: " + error.getMessage()
                );
            }
            
        } catch (Exception e) {
            // Audit logging não deve quebrar aplicação
            log.error("Failed to log audit", e).log();
        }
    }
    
    private String resolveResourceId(
        ProceedingJoinPoint joinPoint,
        String expression
    ) {
        if (expression.isEmpty()) {
            return "";
        }
        
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("args", joinPoint.getArgs());
        context.setVariable("method", joinPoint.getSignature().getName());
        
        Expression expr = parser.parseExpression(expression);
        Object value = expr.getValue(context);
        
        return value != null ? value.toString() : "";
    }
}
```

### Usage

```java
@Service
public class OrderService {
    
    @Audited(
        action = "CREATE",
        resource = "orders",
        resourceId = "#result.value()",
        level = AuditLogLevel.INFO
    )
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // Implementação...
    }
    
    @Audited(
        action = "UPDATE",
        resource = "orders",
        resourceId = "#orderId.value()",
        captureData = true,
        level = AuditLogLevel.INFO
    )
    public Result<Void> updateOrder(OrderId orderId, UpdateOrderCommand command) {
        // Implementação...
    }
    
    @Audited(
        action = "DELETE",
        resource = "orders",
        resourceId = "#orderId.value()",
        level = AuditLogLevel.CRITICAL
    )
    public Result<Void> deleteOrder(OrderId orderId) {
        // Implementação...
    }
}
```

---

## 🔍 Querying Audit Logs

### Audit Trail

```java
@Service
public class AuditTrailService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Histórico completo de um recurso.
     */
    public List<AuditLog> getResourceHistory(
        String resource,
        String resourceId
    ) {
        return auditLogRepository.findByResource(
            resource,
            resourceId,
            Instant.EPOCH,
            Instant.now()
        );
    }
    
    /**
     * Ações de um usuário.
     */
    public List<AuditLog> getUserActivity(
        String userId,
        Instant from,
        Instant to
    ) {
        return auditLogRepository.findByActor(
            userId,
            from,
            to
        );
    }
    
    /**
     * Mudanças em período.
     */
    public List<AuditLog> getDataChanges(
        String resource,
        Instant from,
        Instant to
    ) {
        return auditLogRepository
            .findByResource(resource, null, from, to)
            .stream()
            .filter(AuditLog::isDataChange)
            .toList();
    }
    
    /**
     * Eventos críticos.
     */
    public List<AuditLog> getCriticalEvents(Duration lookback) {
        Instant from = Instant.now().minus(lookback);
        return auditLogRepository.findCriticalEvents(from, Instant.now());
    }
}
```

### REST API

```java
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {
    
    private final AuditTrailService auditTrailService;
    
    @GetMapping("/resources/{resource}/{resourceId}")
    public ResponseEntity<List<AuditLogResponse>> getResourceHistory(
        @PathVariable String resource,
        @PathVariable String resourceId
    ) {
        List<AuditLog> logs = auditTrailService.getResourceHistory(
            resource,
            resourceId
        );
        
        return ResponseEntity.ok(
            logs.stream()
                .map(AuditLogResponse::from)
                .toList()
        );
    }
    
    @GetMapping("/users/{userId}/activity")
    public ResponseEntity<List<AuditLogResponse>> getUserActivity(
        @PathVariable String userId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to
    ) {
        Instant fromInstant = from != null 
            ? Instant.parse(from) 
            : Instant.now().minus(7, ChronoUnit.DAYS);
        
        Instant toInstant = to != null 
            ? Instant.parse(to) 
            : Instant.now();
        
        List<AuditLog> logs = auditTrailService.getUserActivity(
            userId,
            fromInstant,
            toInstant
        );
        
        return ResponseEntity.ok(
            logs.stream()
                .map(AuditLogResponse::from)
                .toList()
        );
    }
    
    @GetMapping("/critical")
    public ResponseEntity<List<AuditLogResponse>> getCriticalEvents(
        @RequestParam(defaultValue = "24h") String lookback
    ) {
        Duration duration = parseDuration(lookback);
        
        List<AuditLog> logs = auditTrailService.getCriticalEvents(duration);
        
        return ResponseEntity.ok(
            logs.stream()
                .map(AuditLogResponse::from)
                .toList()
        );
    }
}
```

---

## 🔐 Security & Compliance

### Actor Context

```java
@Component
public class ActorContextFilter implements Filter {
    
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Extrair informações do usuário
        String userId = extractUserId(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Definir contexto de ator
        ActorContext.setActor(userId != null ? userId : "anonymous");
        ActorContext.setIpAddress(ipAddress);
        ActorContext.setUserAgent(userAgent);
        
        try {
            chain.doFilter(request, response);
        } finally {
            ActorContext.clear();
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### GDPR Compliance

```java
@Service
public class GdprAuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Exporta todos os dados de auditoria de um usuário (direito GDPR).
     */
    public byte[] exportUserAuditData(String userId) {
        List<AuditLog> logs = auditLogRepository.findByActor(
            userId,
            Instant.EPOCH,
            Instant.now()
        );
        
        // Converter para JSON
        String json = toJson(logs);
        return json.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Anonimiza dados de um usuário (direito ao esquecimento GDPR).
     */
    @Transactional
    public Result<Void> anonymizeUserAuditData(String userId) {
        List<AuditLog> logs = auditLogRepository.findByActor(
            userId,
            Instant.EPOCH,
            Instant.now()
        );
        
        for (AuditLog log : logs) {
            AuditLog anonymized = log.anonymize();
            auditLogRepository.update(anonymized);
        }
        
        // Audit log da própria anonimização
        auditLogRepository.save(AuditLog.builder()
            .actor("system")
            .action("ANONYMIZE")
            .resource("audit-logs")
            .resourceId(userId)
            .level(AuditLogLevel.CRITICAL)
            .metadata(Map.of(
                "reason", "GDPR right to be forgotten",
                "logsAnonymized", String.valueOf(logs.size())
            ))
            .build());
        
        return Result.ok();
    }
}
```

---

## 📊 Audit Reports

### Activity Report

```java
@Service
public class AuditReportService {
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditReport generateActivityReport(
        String resource,
        Instant from,
        Instant to
    ) {
        List<AuditLog> logs = auditLogRepository.findByResource(
            resource,
            null,
            from,
            to
        );
        
        // Agregar estatísticas
        long totalActions = logs.size();
        
        Map<String, Long> actionCounts = logs.stream()
            .collect(Collectors.groupingBy(
                AuditLog::action,
                Collectors.counting()
            ));
        
        Map<String, Long> actorCounts = logs.stream()
            .collect(Collectors.groupingBy(
                AuditLog::actor,
                Collectors.counting()
            ));
        
        long criticalEvents = logs.stream()
            .filter(AuditLog::isCritical)
            .count();
        
        return AuditReport.builder()
            .resource(resource)
            .period(from, to)
            .totalActions(totalActions)
            .actionBreakdown(actionCounts)
            .actorBreakdown(actorCounts)
            .criticalEvents(criticalEvents)
            .build();
    }
}
```

---

## 🧪 Testing

### Unit Tests

```java
class AuditLoggerTest {
    
    private AuditLogger auditLogger;
    private AuditLogRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        auditLogger = new DefaultAuditLogger(repository);
    }
    
    @Test
    void shouldLogAction() {
        // When
        auditLogger.log("CREATE", "orders", "order-123");
        
        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertThat(log.action()).isEqualTo("CREATE");
        assertThat(log.resource()).isEqualTo("orders");
        assertThat(log.resourceId()).isEqualTo("order-123");
    }
    
    @Test
    void shouldLogDataChange() {
        // Given
        Map<String, Object> before = Map.of("status", "PENDING");
        Map<String, Object> after = Map.of("status", "COMPLETED");
        
        // When
        auditLogger.logDataChange(
            "UPDATE",
            "orders",
            "order-123",
            before,
            after
        );
        
        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertThat(log.isDataChange()).isTrue();
        assertThat(log.before()).isEqualTo(before);
        assertThat(log.after()).isEqualTo(after);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Capture contexto de ator
ActorContext.setActor(userId);
ActorContext.setIpAddress(request.getRemoteAddr());

// ✅ Use níveis apropriados
auditLogger.logCritical("DELETE", "users", userId, "Admin deletion");

// ✅ Capture before/after para mudanças
Map<String, Object> before = toMap(order);
// ... update ...
Map<String, Object> after = toMap(order);
auditLogger.logDataChange("UPDATE", "orders", id, before, after);

// ✅ Audit operações sensíveis
@Audited(action = "ACCESS", resource = "sensitive-data", level = CRITICAL)

// ✅ Implemente GDPR compliance
public byte[] exportUserData(String userId) { ... }
public void anonymizeUserData(String userId) { ... }
```

### ❌ DON'T

```java
// ❌ NÃO logue dados sensíveis
auditLogger.log("LOGIN", "users", userId, Map.of(
    "password", password  // ❌ Nunca!
));

// ❌ NÃO ignore falhas de audit
try {
    auditLogger.log(...);
} catch (Exception e) {
    // ❌ Ignorar silenciosamente
}

// ❌ NÃO faça audit síncrono em operações críticas
auditLogger.log(...).block();  // ❌ Vai atrasar operação

// ❌ NÃO audit tudo
@Audited  // ❌ Getter não precisa audit
public String getName() { ... }

// ❌ NÃO mantenha logs para sempre sem política de retenção
// Configure retenção: 90 dias para INFO, 2 anos para CRITICAL
```

---

## Ver Também

- [Observability Guide](../guides/observability.md) - Structured logging
- [Multi-Tenancy](app-multi-tenancy.md) - Tenant context
- [Security Best Practices](../guides/security.md)
