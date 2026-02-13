# Java Commons Platform - Exemplos

Este diretório contém exemplos práticos de uso da plataforma java-commons.

## Estrutura

```
examples/
├── README.md (este arquivo)
├── simple-user-service/     → Exemplo simples de serviço REST
└── (futuros exemplos)
```

## Exemplos Disponíveis

### 1. Simple User Service

Demonstra conceitos fundamentais:
- Domain-Driven Design (Entity, ValueObject, Aggregate)
- Hexagonal Architecture (Ports & Adapters)
- Result Type Pattern
- Persistence com JPA
- REST API com Spring Boot

**Ver**: [simple-user-service/README.md](simple-user-service/README.md)

## Como Usar os Exemplos

### Pré-requisitos

- Java 21+
- Maven 3.9.0+
- Docker (para testes com containers)

### Executar um Exemplo

```bash
# Navegar para o exemplo
cd examples/simple-user-service

# Compilar
mvn clean install

# Executar
mvn spring-boot:run

# Testar
curl http://localhost:8080/api/users
```

### Estudar o Código

Cada exemplo contém:
- 📂 **Domain**: Lógica de negócio pura (kernel)
- 📂 **Application**: Casos de uso
- 📂 **Ports**: Interfaces hexagonais
- 📂 **Adapters**: Implementações (JPA, REST)
- 📄 **README.md**: Guia detalhado

## Conceitos Demonstrados

| Conceito | Simple User Service | (Futuro) Order Service | (Futuro) Event-Driven |
|----------|---------------------|------------------------|------------------------|
| Entity | ✅ | ✅ | ✅ |
| Value Object | ✅ | ✅ | ✅ |
| Aggregate Root | ✅ | ✅ | ✅ |
| Domain Events | ⚠️ Básico | ✅ Completo | ✅ Completo |
| Result Type | ✅ | ✅ | ✅ |
| Repository | ✅ | ✅ | ✅ |
| Specification | - | ✅ | - |
| REST API | ✅ | ✅ | ✅ |
| Transactional Outbox | - | ✅ | ✅ |
| Messaging | - | - | ✅ |
| Observability | ✅ Básico | ✅ | ✅ |

## Aprendizado Progressivo

Recomendamos estudar os exemplos nesta ordem:

1. **Simple User Service** (⭐ Comece aqui)
   - Conceitos básicos
   - DDD essencial
   - Hexagonal simples
   - Quick start

2. **Order Service** (em breve)
   - Aggregates complexos
   - Domain events completos
   - Transactional outbox
   - Specifications

3. **Event-Driven Service** (em breve)
   - Messaging assíncrono
   - Event sourcing
   - CQRS
   - Observability completa

## Feedback e Melhorias

Encontrou um problema ou tem sugestão de exemplo?
- Abra uma issue no repositório
- Contribua com Pull Request

## Referências

- [Architecture Documentation](../docs/architecture.md)
- [Usage Patterns](../docs/usage-patterns.md)
- [ADRs](../docs/adr/README.md)
- [Contributing Guide](../CONTRIBUTING.md)
