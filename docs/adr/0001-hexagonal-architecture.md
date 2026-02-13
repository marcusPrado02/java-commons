# ADR-0001: Adoção de Arquitetura Hexagonal

**Status**: Aceito  
**Data**: 2026-01-28  
**Decisores**: Equipe de Arquitetura  

## Contexto

O projeto java-commons visa fornecer uma plataforma compartilhada para desenvolvimento de microserviços Java de alta qualidade. Precisávamos de uma estrutura arquitetural que:

- Promova separação clara de responsabilidades
- Facilite testabilidade independente de frameworks
- Permita substituição de tecnologias sem impacto no domínio
- Suporte múltiplas implementações (Spring, Quarkus, etc.)

## Decisão

Adotamos **Arquitetura Hexagonal (Ports & Adapters)** como padrão arquitetural fundamental do projeto.

### Estrutura de Camadas

```
┌─────────────────────────────────────────┐
│         Adapters (Outside)              │
│  Web, Persistence, Messaging, etc.      │
├─────────────────────────────────────────┤
│         Ports (Interfaces)              │
│  Repository, Publisher, HttpClient      │
├─────────────────────────────────────────┤
│       Application Services              │
│  Use Cases, Orchestration               │
├─────────────────────────────────────────┤
│        Domain (Kernel)                  │
│  Entities, Value Objects, Events        │
└─────────────────────────────────────────┘
```

### Módulos Correspondentes

- **Kernel** (`commons-kernel-*`): Domínio puro, sem dependências externas
- **Ports** (`commons-ports-*`): Interfaces framework-agnostic
- **Application** (`commons-app-*`): Serviços de aplicação e casos de uso
- **Adapters** (`commons-adapters-*`): Implementações concretas (JPA, Spring, etc.)

## Consequências

### Positivas

✅ **Testabilidade**: Domínio pode ser testado isoladamente sem frameworks  
✅ **Independência de Framework**: Kernel não depende de Spring, Jakarta, etc.  
✅ **Flexibilidade**: Fácil trocar JPA por outro ORM, ou Spring por Quarkus  
✅ **Clareza**: Fronteiras explícitas entre camadas  
✅ **Reusabilidade**: Mesmo kernel pode ter múltiplos adapters  

### Negativas

⚠️ **Verbosidade**: Mais interfaces e camadas de indireção  
⚠️ **Curva de Aprendizado**: Desenvolvedores precisam entender conceitos hexagonais  
⚠️ **Complexidade Inicial**: Setup inicial mais complexo que abordagem tradicional  

### Mitigações

- Fornecer exemplos claros e documentação completa
- Criar Spring Boot Starters que facilitem integração
- ArchUnit rules para garantir conformidade arquitetural

## Alternativas Consideradas

### 1. Arquitetura em Camadas Tradicional
❌ **Rejeitada**: Acoplamento entre camadas, difícil isolar domínio de frameworks

### 2. Clean Architecture (Uncle Bob)
✅ **Semelhante**: Hexagonal é variação de Clean Architecture, escolhemos terminologia "Ports & Adapters" por ser mais explícita

### 3. Sem Padrão Arquitetural
❌ **Rejeitada**: Não atende requisito de governança e qualidade

## Referências

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) - Alistair Cockburn
- [Ports and Adapters Pattern](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)
- Clean Architecture - Robert C. Martin

## Notas de Implementação

- Ver `commons-archunit` para regras de conformidade
- Ver `docs/architecture.md` para diagrama completo
- Exemplos em `examples/` demonstram uso prático
