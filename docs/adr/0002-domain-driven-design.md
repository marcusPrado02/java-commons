# ADR-0002: Domain-Driven Design como Fundação

**Status**: Aceito  
**Data**: 2026-01-28  
**Decisores**: Equipe de Arquitetura  

## Contexto

Microserviços corporativos lidam com lógica de negócio complexa e regras de domínio sofisticadas. Precisávamos de uma abordagem que:

- Mantenha regras de negócio isoladas de infraestrutura
- Promova linguagem ubíqua entre desenvolvedores e stakeholders
- Facilite modelagem de conceitos de negócio complexos
- Suporte evolução controlada do modelo de domínio

## Decisão

Adotamos **Domain-Driven Design (DDD)** como metodologia fundamental, com foco em **Tactical Patterns**.

### Building Blocks Implementados

#### `commons-kernel-ddd`

```java
// Entities com identidade
public abstract class Entity<ID> {
  private final ID id;
  private final TenantId tenantId;
  private EntityVersion version;
  private final AuditTrail audit;
}

// Aggregate Roots com eventos de domínio
public abstract class AggregateRoot<ID> extends Entity<ID> 
    implements DomainEventRecorder {
  private final List<DomainEvent> domainEvents = new ArrayList<>();
}

// Value Objects imutáveis
public abstract class ValueObject {
  // Igualdade por valor, não por identidade
}

// Domain Events
public interface DomainEvent {
  EventId eventId();
  Instant occurredAt();
}
```

### Conceitos Principais

1. **Entities**: Objetos com identidade única (`User`, `Order`)
2. **Value Objects**: Objetos definidos por atributos (`Money`, `Address`)
3. **Aggregates**: Cluster de entidades tratadas como unidade (`Order + OrderLines`)
4. **Domain Events**: Fatos que ocorreram no domínio (`OrderPlaced`, `PaymentReceived`)
5. **Repositories**: Abstração de persistência (em `commons-ports-persistence`)

## Consequências

### Positivas

✅ **Clareza de Negócio**: Código reflete modelo mental do domínio  
✅ **Invariantes Protegidas**: Aggregates garantem consistência  
✅ **Auditabilidade**: AuditTrail e eventos built-in  
✅ **Multi-Tenancy**: TenantId nativo em entities  
✅ **Eventual Consistency**: Eventos facilitam comunicação entre bounded contexts  
✅ **Testabilidade**: Domínio testável sem banco de dados  

### Negativas

⚠️ **Overhead Conceitual**: Necessidade de treinar desenvolvedores em DDD  
⚠️ **Verbosidade**: Mais classes (entities, VOs, eventos)  
⚠️ **Complexidade**: Gestão de aggregate boundaries requer cuidado  

### Mitigações

- Documentação clara com exemplos práticos
- Guias de modelagem de aggregates
- Code reviews focados em design de domínio
- Ferramentas de geração de código para boilerplate

## Patterns Aplicados

### 1. Entity Lifecycle

```java
// Criação com audit stamp
var user = new User(
  UserId.generate(),
  tenantId,
  AuditStamp.now(actorId)
);

// Soft delete
user.markAsDeleted(AuditStamp.now(actorId));
```

### 2. Domain Events

```java
// Aggregate registra eventos
public class Order extends AggregateRoot<OrderId> {
  public void place() {
    // Validações...
    recordEvent(new OrderPlaced(id(), Instant.now()));
  }
}

// Pull events para publicação (Transactional Outbox)
List<DomainEvent> events = order.pullDomainEvents();
```

### 3. Invariants

```java
public class Money extends ValueObject {
  public Money(BigDecimal amount, Currency currency) {
    Invariant.notNull(amount, "amount");
    Invariant.notNull(currency, "currency");
    Invariant.isTrue(amount.compareTo(BigDecimal.ZERO) >= 0, 
        "amount must be non-negative");
  }
}
```

## Alternativas Consideradas

### 1. Anemic Domain Model
❌ **Rejeitada**: Lógica espalhada em services, dificulta manutenção

### 2. Transaction Script
❌ **Rejeitada**: Não escala para domínios complexos

### 3. Active Record
❌ **Rejeitada**: Acopla domínio com persistência (viola hexagonal)

## Referências

- Domain-Driven Design - Eric Evans (Blue Book)
- Implementing Domain-Driven Design - Vaughn Vernon (Red Book)
- [DDD Reference](https://www.domainlanguage.com/ddd/reference/) - Eric Evans

## Notas de Implementação

- Ver `commons-kernel-ddd` para base classes
- Ver `commons-app-outbox` para Transactional Outbox Pattern
- ArchUnit rules em `DomainPurityRules` enforçam que kernel não depende de frameworks
- Exemplos em `examples/examples-full-hexagonal`
