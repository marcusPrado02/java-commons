# Guia de Contribuição

Obrigado por considerar contribuir com o Java Commons Platform! Este documento fornece diretrizes para contribuições.

## Índice

1. [Código de Conduta](#código-de-conduta)
2. [Como Contribuir](#como-contribuir)
3. [Standards de Desenvolvimento](#standards-de-desenvolvimento)
4. [Workflow de Contribuição](#workflow-de-contribuição)
5. [Revisão de Código](#revisão-de-código)
6. [Documentação](#documentação)

---

## Código de Conduta

Este projeto segue os princípios de colaboração respeitosa:

- **Respeito**: Trate todos com respeito, independente de nível de experiência
- **Inclusão**: Seja acolhedor com contribuidores de todos os backgrounds
- **Profissionalismo**: Mantenha discussões focadas e construtivas
- **Colaboração**: Trabalhe junto para resolver problemas

---

## Como Contribuir

### Tipos de Contribuição

Aceitamos contribuições de diferentes formas:

#### 1. Reportar Bugs

Abra uma issue incluindo:
- Descrição clara do problema
- Passos para reproduzir
- Comportamento esperado vs. atual
- Versões (Java, Maven, módulos commons)
- Stack trace (se aplicável)

**Template**:
```markdown
### Descrição
[Descrição clara do bug]

### Passos para Reproduzir
1. ...
2. ...
3. ...

### Esperado
[O que deveria acontecer]

### Atual
[O que está acontecendo]

### Ambiente
- Java: 21
- Maven: 3.9.0
- commons-kernel-ddd: 1.0.0
```

#### 2. Propor Features

Antes de implementar uma feature grande:
1. Abra uma issue para discussão
2. Descreva o problema que resolve
3. Proponha solução de alto nível
4. Aguarde feedback da equipe

#### 3. Corrigir Bugs ou Implementar Features

1. Fork o repositório
2. Crie uma branch a partir de `main`
3. Implemente as mudanças
4. Teste extensivamente
5. Abra um Pull Request

#### 4. Melhorar Documentação

Documentação é crucial! Contribuições incluem:
- Corrigir typos
- Clarificar explicações
- Adicionar exemplos
- Traduzir conteúdo
- Criar tutoriais

---

## Standards de Desenvolvimento

### Princípios Arquiteturais

Todas as contribuições devem aderir aos princípios documentados:

1. **Hexagonal Architecture**: Ver [ADR-0001](docs/adr/0001-hexagonal-architecture.md)
2. **Domain-Driven Design**: Ver [ADR-0002](docs/adr/0002-domain-driven-design.md)  
3. **Result Type Pattern**: Ver [ADR-0003](docs/adr/0003-result-type-pattern.md)
4. **Framework-Agnostic Kernel**: Ver [ADR-0004](docs/adr/0004-framework-agnostic-kernel.md)
5. **Module Structure**: Ver [ADR-0005](docs/adr/0005-module-structure.md)

### Regras de Dependência

```
┌─────────────────────────────────────┐
│  Regra de Ouro:                     │
│  Dependências fluem de FORA → DENTRO│
└─────────────────────────────────────┘

Starters → Adapters → Ports → Application → Kernel
```

#### Módulos Kernel

✅ **PERMITIDO**:
- Java stdlib (`java.*`, `javax.*`)
- SLF4J API (`org.slf4j.api`)
- Outros `commons-kernel-*`

❌ **PROIBIDO**:
- Spring Framework
- Jakarta EE
- Hibernate/JPA annotations
- Jackson/Gson
- Qualquer framework externo

**Validação**: ArchUnit rules em `commons-archunit` garantem conformidade automaticamente.

### Code Style

#### Formatação

Usamos **Google Java Style** com Spotless:

```bash
# Verificar formatting
mvn spotless:check

# Aplicar formatting
mvn spotless:apply
```

**IMPORTANTE**: Execute `mvn spotless:apply` antes de commitar!

#### Naming Conventions

```java
// Classes: PascalCase
public class UserService {}
public record OrderId(String value) {}

// Métodos/Variáveis: camelCase
public void processOrder() {}
private String userName;

// Constantes: UPPER_SNAKE_CASE
private static final int MAX_RETRIES = 3;

// Packages: lowercase
package com.marcusprado02.commons.kernel.ddd;
```

#### Imutabilidade

Prefira imutabilidade:

```java
// ✅ BOM: Imutável
public final class Money {
  private final BigDecimal amount;
  
  public Money add(Money other) {
    return new Money(this.amount.add(other.amount));
  }
}

// ❌ RUIM: Mutável
public class Money {
  private BigDecimal amount;
  
  public void add(Money other) {
    this.amount = this.amount.add(other.amount);
  }
}
```

#### Records para DTOs

Use Records do Java 16+ para DTOs:

```java
// ✅ BOM
public record CreateUserCommand(
  Email email,
  UserName name,
  ActorId actorId
) {}

// ❌ RUIM (verboso)
public class CreateUserCommand {
  private final Email email;
  private final UserName name;
  private final ActorId actorId;
  
  // Constructor, getters, equals, hashCode, toString...
}
```

### Testing

#### Coverage Requirements

- **Linha**: 80% mínimo
- **Branch**: 75% mínimo

```bash
# Gerar relatório de cobertura
mvn clean verify

# Ver relatório
open commons-platform/target/site/jacoco-aggregate/index.html
```

#### Estrutura de Testes

```java
@DisplayName("User")
class UserTest {
  
  @Nested
  @DisplayName("Creation")
  class Creation {
    
    @Test
    @DisplayName("should create user with valid data")
    void shouldCreateWithValidData() {
      // Given
      var email = Email.of("test@example.com");
      var name = UserName.of("Test User");
      
      // When
      var user = new User(UserId.generate(), tenantId, email, name, auditStamp);
      
      // Then
      assertThat(user.email()).isEqualTo(email);
      assertThat(user.name()).isEqualTo(name);
    }
    
    @Test
    @DisplayName("should fail when email is null")
    void shouldFailWhenEmailIsNull() {
      // When / Then
      assertThatThrownBy(() -> 
        new User(UserId.generate(), tenantId, null, name, auditStamp)
      ).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
    }
  }
  
  @Nested
  @DisplayName("Deactivation")
  class Deactivation {
    // ...
  }
}
```

#### Test Naming

Use BDD style:
- `should[ExpectedBehavior]When[StateUnderTest]`
- `given[Precondition]When[Action]Then[ExpectedOutcome]`

#### Assertions

Use AssertJ para expressividade:

```java
// ✅ BOM: Fluent assertions
assertThat(user.status())
  .isEqualTo(UserStatus.ACTIVE);

assertThat(user.email().value())
  .isEqualTo("test@example.com");

assertThat(order.lines())
  .hasSize(3)
  .extracting(OrderLine::productId)
  .contains(productId1, productId2, productId3);

// ❌ RUIM: JUnit assertions
assertEquals(UserStatus.ACTIVE, user.status());
assertTrue(user.email().value().equals("test@example.com"));
```

### Static Analysis

Antes de submeter PR, execute análise estática:

```bash
# SpotBugs
mvn verify -Dspotbugs.skip=false

# Checkstyle
mvn verify -Dcheckstyle.skip=false

# PMD
mvn verify -Dpmd.skip=false

# Tudo junto
mvn verify -Dspotbugs.skip=false -Dcheckstyle.skip=false -Dpmd.skip=false
```

### Commit Messages

Seguimos [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: Nova feature
- `fix`: Bug fix
- `docs`: Documentação
- `style`: Formatação (não muda lógica)
- `refactor`: Refatoração
- `test`: Adicionar/corrigir testes
- `chore`: Manutenção (build, deps)

**Exemplos**:

```
feat(kernel-ddd): add soft delete support to Entity

Implements DeletionStamp to track soft deletes in entities.
Aggregates can now be marked as deleted while preserving data.

Closes #123
```

```
fix(adapters-jpa): correct null handling in PageableJpaRepository

findAll was throwing NPE when page request had null sort.
Now defaults to empty sort.

Fixes #456
```

```
docs(architecture): add C4 diagrams

Created Context, Container and Component diagrams using Mermaid.
Includes dependency flow diagram.
```

---

## Workflow de Contribuição

### 1. Fork e Clone

```bash
# Fork via GitHub UI
# Clone seu fork
git clone https://github.com/SEU-USER/java-commons.git
cd java-commons

# Adicione upstream
git remote add upstream https://github.com/marcusPrado02/java-commons.git
```

### 2. Criar Branch

```bash
# Atualize main
git checkout main
git pull upstream main

# Crie branch para sua feature/fix
git checkout -b feat/add-specification-builder
# ou
git checkout -b fix/null-pointer-in-repository
```

**Naming**:
- `feat/description`: Nova feature
- `fix/description`: Bug fix
- `docs/description`: Documentação
- `refactor/description`: Refatoração

### 3. Desenvolver

```bash
# Faça mudanças
# ...

# Aplique formatação
mvn spotless:apply

# Execute testes
mvn clean verify

# Execute static analysis
mvn verify -Dspotbugs.skip=false -Dcheckstyle.skip=false

# Commit
git add .
git commit -m "feat(kernel-ddd): add specification builder"
```

### 4. Push e Pull Request

```bash
# Push para seu fork
git push origin feat/add-specification-builder
```

Abra PR no GitHub:

**Template de PR**:

```markdown
## Descrição
[Descrição clara do que foi implementado/corrigido]

## Motivação
[Por que essa mudança é necessária]

## Mudanças
- [Mudança 1]
- [Mudança 2]

## Checklist
- [ ] Testes adicionados/atualizados
- [ ] Documentação atualizada
- [ ] `mvn spotless:apply` executado
- [ ] `mvn clean verify` passa
- [ ] Static analysis limpo
- [ ] ArchUnit rules passam
- [ ] Sem breaking changes (ou documentado)

## Issues Relacionadas
Closes #123
Relates to #456
```

### 5. Responder Feedback

- Endereçe comentários de revisores
- Faça commits adicionais conforme necessário
- Push força-os para atualizar PR

```bash
git add .
git commit -m "refactor: apply review feedback"
git push origin feat/add-specification-builder
```

---

## Revisão de Código

### O Que Revisores Verificam

1. **Conformidade Arquitetural**:
   - Segue padrões hexagonais?
   - Kernel está framework-free?
   - Dependências fluem corretamente?

2. **Qualidade de Código**:
   - Legibilidade
   - Imutabilidade quando apropriado
   - Tratamento de erros (Result types)
   - Testes adequados

3. **Documentação**:
   - Javadoc em classes/métodos públicos
   - ADRs para decisões significativas
   - README atualizado se necessário

4. **Performance**:
   - Sem alocações desnecessárias
   - Streams vs loops quando apropriado
   - Lazy evaluation quando possível

5. **Segurança**:
   - Input validation
   - SQL injection prevention
   - Secret handling

### Como Responder a Reviews

- **Seja receptivo**: Feedback é para melhorar o código
- **Faça perguntas**: Se não entender sugestão, pergunte
- **Explique decisões**: Justifique escolhas quando necessário
- **Reconheça quando errou**: Admitir erro é sinal de maturidade

---

## Documentação

### Javadoc

Documente APIs públicas:

```java
/**
 * Represents a user entity in the system.
 *
 * <p>Users are identified by unique {@link UserId} and belong to a {@link TenantId}.
 * They can be deactivated using {@link #deactivate(AuditStamp)}.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * var user = new User(
 *   UserId.generate(),
 *   tenantId,
 *   Email.of("user@example.com"),
 *   UserName.of("John Doe"),
 *   AuditStamp.now(actorId)
 * );
 * 
 * user.deactivate(AuditStamp.now(adminId));
 * }</pre>
 *
 * @see Entity
 * @see UserId
 * @since 1.0.0
 */
public class User extends Entity<UserId> {
  
  /**
   * Deactivates this user.
   *
   * @param updated Audit stamp with actor who deactivated
   * @throws IllegalStateException if user is already inactive
   */
  public void deactivate(AuditStamp updated) {
    // ...
  }
}
```

### ADRs (Architecture Decision Records)

Para decisões arquiteturais significativas, crie ADR:

```bash
# Criar novo ADR
touch docs/adr/0006-my-decision.md
```

Seguir estrutura:
- Status (Proposed/Accepted/Deprecated)
- Contexto
- Decisão
- Consequências
- Alternativas
- Referências

### README Updates

Atualize README quando adicionar:
- Novo módulo
- Nova feature importante
- Mudança em instalação/setup

---

## Build Local

### Requisitos

- Java 21+
- Maven 3.9.0+
- Git

### Build Completo

```bash
# Limpar e compilar tudo
mvn clean install

# Pular testes (mais rápido, uso temporário)
mvn clean install -DskipTests

# Com relatórios de cobertura
mvn clean verify

# Com análise estática
mvn verify -Dspotbugs.skip=false -Dcheckstyle.skip=false -Dpmd.skip=false
```

### Build Incremental

```bash
# Build apenas módulo específico (e dependências)
mvn clean install -pl commons-kernel-ddd -am

# Build módulo e dependentes
mvn clean install -pl commons-kernel-ddd -amd
```

### Verificar Dependências

```bash
# Tree de dependências
mvn dependency:tree

# Dependências não usadas
mvn dependency:analyze

# Versões desatualizadas
mvn versions:display-dependency-updates
```

---

## Dúvidas?

- **Issues**: Abra issue para perguntas
- **Discussions**: Use GitHub Discussions para discussões gerais
- **Email**: [Marco técnico responsável]

---

## Licença

Ao contribuir, você concorda que suas contribuições serão licenciadas sob a mesma licença do projeto (ver [LICENSE](LICENSE)).

---

**Obrigado por contribuir! 🎉**
