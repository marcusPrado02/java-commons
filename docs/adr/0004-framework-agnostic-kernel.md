# ADR-0004: Kernel Framework-Agnostic

**Status**: Aceito  
**Data**: 2026-01-28  
**Decisores**: Equipe de Arquitetura  

## Contexto

Frameworks mudam, evoluem e às vezes tornam-se obsoletos. Aplicações empresariais têm vida longa, frequentemente sobrevivendo a múltiplas gerações de frameworks.

Problemas com acoplamento a frameworks:
- Migração custosa quando framework muda (Spring 5 → 6, Jakarta EE)
- Vendor lock-in
- Testes requerem infrastructure pesada
- Lógica de negócio misturada com concerns técnicos
- Dificuldade de reuso em contextos diferentes

## Decisão

Módulos **`commons-kernel-*`** são **100% framework-agnostic**: sem dependências de Spring, Jakarta EE, Quarkus, ou qualquer framework.

### Regras de Dependências

```
┌─────────────────────────────────────┐
│  commons-kernel-*                   │
│  ├─ PERMITIDO:                      │
│  │  ✅ JDK stdlib (java.*)          │
│  │  ✅ SLF4J API (logging)          │
│  │  ✅ Outros commons-kernel-*      │
│  │                                   │
│  ├─ PROIBIDO:                       │
│  │  ❌ Spring Framework              │
│  │  ❌ Jakarta EE                    │
│  │  ❌ Hibernate/JPA                 │
│  │  ❌ Jackson/Gson                  │
│  │  ❌ Qualquer framework            │
└─────────────────────────────────────┘
```

### Módulos Kernel

1. **`commons-kernel-core`**: Primitivos e utilitários base
2. **`commons-kernel-ddd`**: Building blocks DDD (Entity, ValueObject, AggregateRoot)
3. **`commons-kernel-errors`**: Modelo de erro e hierarquia de exceções
4. **`commons-kernel-result`**: Result/Option/Either types
5. **`commons-kernel-time`**: Abstrações de tempo (ClockProvider)

### Aplicação das Regras

**ArchUnit** garante conformidade automaticamente:

```java
// commons-archunit/src/main/java/*/KernelIsolationRules.java
@ArchTest
static final ArchRule kernel_should_be_framework_free =
  classes()
    .that().resideInAPackage("..kernel..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("java..", "..kernel..", "org.slf4j..");
```

## Consequências

### Positivas

✅ **Longevidade**: Domínio sobrevive à mudança de frameworks  
✅ **Portabilidade**: Mesmo kernel funciona com Spring, Quarkus, Micronaut, etc.  
✅ **Testabilidade**: Testes unitários ultra-rápidos sem contexto de framework  
✅ **Clareza**: Separação cristalina entre domínio e infraestrutura  
✅ **Performance**: Menor footprint de memória e startup  
✅ **Reusabilidade**: Kernel pode ser usado em CLIs, batch jobs, cloud functions  

### Negativas

⚠️ **Produtividade inicial menor**: Não pode usar anotações/features de frameworks  
⚠️ **Duplicação**: Necessidade de criar próprios primitivos (ClockProvider)  
⚠️ **Adaptadores necessários**: Bridges entre kernel e frameworks  

### Mitigações

- **Spring Boot Starters** (`commons-spring-starter-*`) fornecem integrações prontas
- **Adapters** (`commons-adapters-*`) implementam bridges comuns
- **Documentação** clara de como integrar com frameworks populares

## Alternativas Consideradas

### 1. Kernel com Spring
❌ **Rejeitada**: Lock-in, testes lentos, violaria princípios hexagonais

### 2. Kernel com Jakarta EE
❌ **Rejeitada**: Mesmos problemas de acoplamento

### 3. Kernel com Annotations "leves"
❌ **Rejeitada**: Qualquer annotation framework é acoplamento

## Exemplos Práticos

### ❌ ERRADO: Kernel com Spring

```java
// NUNCA faça isso em commons-kernel-*!
@Entity // ❌ JPA
public class Order extends AggregateRoot<OrderId> {
  
  @Autowired // ❌ Spring
  private OrderRepository repository;
  
  @Transactional // ❌ Spring
  public void place() { ... }
}
```

### ✅ CORRETO: Kernel Puro

```java
// commons-kernel-ddd
public abstract class Order extends AggregateRoot<OrderId> {
  // Apenas Java puro + DDD patterns
  
  public void place() {
    // Validações e lógica de domínio
    recordEvent(new OrderPlaced(id(), Instant.now()));
  }
}

// commons-ports-persistence (interface)
public interface OrderRepository {
  void save(Order order);
  Option<Order> findById(OrderId id);
}

// commons-adapters-persistence-jpa (implementação)
@Repository // ✅ Spring pode estar em adapters!
public class JpaOrderRepository implements OrderRepository {
  // Implementação com JPA
}
```

### Integração com Spring Boot

```java
// commons-spring-starter-*/autoconfigure
@Configuration
@ConditionalOnClass(ClockProvider.class)
public class TimeAutoConfiguration {
  
  @Bean
  @ConditionalOnMissingBean
  public ClockProvider clockProvider() {
    return Clock::systemUTC; // Adapter do kernel para Java stdlib
  }
}
```

## Verificação de Conformidade

### Build Time

```bash
# ArchUnit rules executam automaticamente em testes
mvn test

# Exemplo de falha:
# Rule 'kernel should be framework free' was violated:
#   Class <Order> depends on <org.springframework.stereotype.Service>
```

### IDE Time

- Checkstyle/PMD configurados para alertar sobre imports proibidos em kernel
- SonarLint rules customizadas

## Referências

- Clean Architecture - Robert C. Martin, Chapter 22
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Ports and Adapters](https://herbertograca.com/2017/09/14/ports-adapters-architecture/)

## Notas de Implementação

- Ver `commons-archunit/KernelIsolationRules` para regras de validação
- Ver `commons-parent/pom.xml` para enforcer plugin configuration
- Exceção: `org.slf4j.api` é permitido (logging é ubíquo)
- Para necessidades específicas de framework, crie adapter em `commons-adapters-*`
