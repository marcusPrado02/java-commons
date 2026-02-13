# ADR-0005: Estrutura de Módulos por Camada Arquitetural

**Status**: Aceito  
**Data**: 2026-01-28  
**Decisores**: Equipe de Arquitetura  

## Contexto

Projetos monolíticos frequentemente sofrem com:
- Violações de dependência (camada inferior dependendo de superior)
- Acoplamento acidental entre concerns diferentes
- Dificuldade em identificar responsabilidades
- Build lento (tudo em um módulo)
- Testes lentos (precisa carregar tudo)

Precisávamos de uma organização que:
- Torne responsabilidades explícitas
- Permita evolução independente de módulos
- Facilite reuso granular
- Habilite builds e testes incrementais
- Force conformidade arquitetural

## Decisão

Organizamos o projeto em **múltiplos módulos Maven**, cada um representando uma camada ou conceito arquitetural específico.

### Taxonomia de Módulos

```
commons/
├── commons-kernel-*       → Domínio puro (framework-free)
├── commons-ports-*        → Contratos hexagonais (interfaces)
├── commons-app-*          → Serviços de aplicação e casos de uso
├── commons-adapters-*     → Implementações de infraestrutura
├── commons-spring-starter-* → Autoconfigurações Spring Boot
├── commons-testkit-*      → Utilitários de teste
├── commons-archunit       → Regras arquiteturais
├── commons-quality        → Regras de qualidade
├── commons-bom            → Bill of Materials
├── commons-parent         → Parent POM com configurações
└── commons-platform       → Aggregator e relatórios
```

### Regras de Dependência

```
┌────────────────────────────────────────────────────┐
│                Spring Starters                     │
│           (commons-spring-starter-*)               │
└────────────────────────────────────────────────────┘
                       ↓ depend on
┌────────────────────────────────────────────────────┐
│                   Adapters                         │
│            (commons-adapters-*)                    │
└────────────────────────────────────────────────────┘
                       ↓ implement
┌────────────────────────────────────────────────────┐
│         Ports (Interfaces)                         │
│            (commons-ports-*)                       │
└────────────────────────────────────────────────────┘
                       ↓ used by
┌────────────────────────────────────────────────────┐
│            Application Services                    │
│              (commons-app-*)                       │
└────────────────────────────────────────────────────┘
                       ↓ use
┌────────────────────────────────────────────────────┐
│         Kernel (Domain, pure Java)                 │
│            (commons-kernel-*)                      │
└────────────────────────────────────────────────────┘
```

**Regra de Ouro**: **Dependências fluem de fora para dentro. Kernel nunca depende de camadas superiores.**

## Módulos Detalhados

### Kernel (`commons-kernel-*`)

**Propósito**: Domínio puro, sem frameworks  
**Dependências permitidas**: Apenas JDK stdlib + outros kernels + SLF4J API

| Módulo | Descrição |
|--------|-----------|
| `commons-kernel-core` | Primitivos base, utilitários |
| `commons-kernel-ddd` | Entity, AggregateRoot, ValueObject, DomainEvent |
| `commons-kernel-errors` | Hierarquia de erros e exceções |
| `commons-kernel-result` | Result, Option, Either types |
| `commons-kernel-time` | ClockProvider, abstrações de tempo |

### Ports (`commons-ports-*`)

**Propósito**: Interfaces hexagonais (contratos)  
**Dependências permitidas**: Kernel + JDK

| Módulo | Descrição |
|--------|-----------|
| `commons-ports-persistence` | Repository, PageableRepository, Specification |
| `commons-ports-messaging` | MessagePublisher, MessageConsumer |
| `commons-ports-http` | HttpClient interface |
| `commons-ports-cache` | CacheProvider interface |
| `commons-ports-secrets` | SecretStore interface |
| `commons-ports-files` | FileStorage interface |

### Application (`commons-app-*`)

**Propósito**: Casos de uso e serviços de aplicação  
**Dependências permitidas**: Kernel + Ports

| Módulo | Descrição |
|--------|-----------|
| `commons-app-observability` | Correlation ID, request context, logging |
| `commons-app-resilience` | Circuit breaker, retry policies |
| `commons-app-outbox` | Transactional Outbox Pattern |
| `commons-app-idempotency` | Idempotency handling |

### Adapters (`commons-adapters-*`)

**Propósito**: Implementações concretas de Ports  
**Dependências permitidas**: Kernel + Ports + frameworks

| Módulo | Descrição |
|--------|-----------|
| `commons-adapters-web` | REST controllers genéricos, query parser |
| `commons-adapters-web-spring` | Spring MVC adapters |
| `commons-adapters-web-spring-webflux` | Spring WebFlux adapters |
| `commons-adapters-persistence-jpa` | JPA repositories |
| `commons-adapters-persistence-inmemory` | In-memory repositories (testes) |
| `commons-adapters-otel` | OpenTelemetry integration |
| `commons-adapters-resilience4j` | Resilience4j implementation |

### Spring Starters (`commons-spring-starter-*`)

**Propósito**: Autoconfigurações Spring Boot  
**Dependências permitidas**: Todos os anteriores + Spring Boot

| Módulo | Descrição |
|--------|-----------|
| `commons-spring-starter-observability` | Auto-config de observability |
| `commons-spring-starter-otel` | Auto-config OpenTelemetry |
| `commons-spring-starter-resilience` | Auto-config resilience |
| `commons-spring-starter-outbox` | Auto-config Transactional Outbox |
| `commons-spring-starter-idempotency` | Auto-config idempotency |

### Governança

| Módulo | Descrição |
|--------|-----------|
| `commons-archunit` | Regras ArchUnit para conformidade |
| `commons-quality` | Regras de qualidade (SpotBugs, Checkstyle) |
| `commons-testkit-core` | Test fixtures, builders, assertions |

### Build

| Módulo | Descrição |
|--------|-----------|
| `commons-bom` | Bill of Materials (dependency management) |
| `commons-parent` | Parent POM com plugins e configurações |
| `commons-platform` | Aggregator module para builds e relatórios |

## Consequências

### Positivas

✅ **Separação Clara**: Cada módulo tem propósito bem definido  
✅ **Reuso Granular**: Consumidores escolhem apenas módulos necessários  
✅ **Build Incremental**: Maven compila apenas o que mudou  
✅ **Testes Focados**: Testar camada isoladamente  
✅ **Conformidade Forçada**: Impossível violar dependências (cyclic dependency detection)  
✅ **Documentação Arquitetural**: Estrutura documenta intenção  
✅ **Evolução Independente**: Módulos versiona separadamente no futuro  

### Negativas

⚠️ **Overhead de Setup**: Mais pom.xml para gerenciar  
⚠️ **Complexidade Inicial**: Novos desenvolvedores precisam entender estrutura  
⚠️ **Refactorings**: Mover código entre módulos requer updates de dependências  

### Mitigações

- BOM (`commons-bom`) simplifica gestão de versões
- Parent POM (`commons-parent`) centraliza configurações
- Documentação clara (este ADR + architecture.md)
- ArchUnit rules detectam violações automaticamente

## Uso Prático

### Consumindo Apenas o Necessário

```xml
<!-- Projeto precisa apenas de DDD base -->
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-kernel-ddd</artifactId>
</dependency>

<!-- Projeto precisa de persistence completa -->
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-ports-persistence</artifactId>
</dependency>
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-persistence-jpa</artifactId>
</dependency>

<!-- Spring Boot project usa starter -->
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-spring-starter-observability</artifactId>
</dependency>
```

### Usar BOM para Gestão de Versões

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.marcusprado02.commons</groupId>
      <artifactId>commons-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- Sem precisar especificar version, BOM gerencia -->
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-ddd</artifactId>
  </dependency>
</dependencies>
```

## Alternativas Consideradas

### 1. Módulo Monolítico
❌ **Rejeitada**: Acoplamento inevitável, builds lentos, testes lentos

### 2. Módulos por Feature
❌ **Rejeitada**: Não alinha com arquitetura hexagonal em camadas

### 3. Microrepos (um repo por módulo)
❌ **Rejeitada**: Overhead de gestão, dificuldade de mudanças cross-cutting

## Referências

- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Modular Monoliths](https://www.kamilgrzybek.com/design/modular-monolith-primer/)
- Clean Architecture - Robert C. Martin

## Notas de Implementação

- Ver `pom.xml` (root) para lista completa de módulos
- Ver `commons-bom/pom.xml` para dependencyManagement
- Ver `commons-archunit` para regras de conformidade
- Ver `docs/dependency-rules.md` para regras detalhadas
