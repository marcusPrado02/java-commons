# Architecture Decision Records (ADRs)

Este diretório contém as decisões arquiteturais fundamentais do projeto java-commons.

## O que são ADRs?

Architecture Decision Records documentam decisões arquiteturais significativas tomadas no projeto, incluindo:
- **Contexto**: Por que a decisão foi necessária
- **Decisão**: O que foi decidido
- **Consequências**: Impactos positivos e negativos
- **Alternativas**: Opções consideradas e rejeitadas

## Índice de ADRs

| # | Título | Status | Data |
|---|--------|--------|------|
| [0001](0001-hexagonal-architecture.md) | Adoção de Arquitetura Hexagonal | Aceito | 2026-01-28 |
| [0002](0002-domain-driven-design.md) | Domain-Driven Design como Fundação | Aceito | 2026-01-28 |
| [0003](0003-result-type-pattern.md) | Result Type ao invés de Exceptions para Casos Esperados | Aceito | 2026-01-28 |
| [0004](0004-framework-agnostic-kernel.md) | Kernel Framework-Agnostic | Aceito | 2026-01-28 |
| [0005](0005-module-structure.md) | Estrutura de Módulos por Camada Arquitetural | Aceito | 2026-01-28 |

## Status Possíveis

- **Proposto**: Em discussão
- **Aceito**: Implementado e em uso
- **Depreciado**: Não mais recomendado
- **Substituído**: Substituído por outro ADR

## Como Contribuir com ADRs

Ao tomar uma decisão arquitetural significativa:

1. Copie o template (se existir) ou use estrutura de ADR existente
2. Numere sequencialmente (próximo número disponível)
3. Inclua:
   - Contexto claro do problema
   - Decisão tomada com justificativa
   - Consequências (positivas e negativas)
   - Alternativas consideradas
   - Referências relevantes
4. Submeta para revisão via Pull Request
5. Atualize este índice

## Referências

- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions) - Michael Nygard
- [ADR GitHub Organization](https://adr.github.io/)
