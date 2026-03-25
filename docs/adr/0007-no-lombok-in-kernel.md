# ADR-0007: Proibição de Lombok no Kernel

**Status**: Aceito
**Data**: 2026-03-25
**Decisores**: Equipe de Arquitetura

## Contexto

O [Lombok](https://projectlombok.org/) é uma biblioteca popular para reduzir boilerplate
em Java (getters, setters, builders, equals/hashCode). Projetos que adotam Lombok
frequentemente o utilizam em toda a base de código, incluindo nas camadas de domínio e kernel.

Durante o design do `commons-kernel-*`, avaliamos se o Lombok deveria ser permitido
nos módulos que formam o núcleo da plataforma.

## Decisão

**Lombok é proibido em todos os módulos `commons-kernel-*` e `commons-ports-*`.**

Lombok é **permitido** nos módulos `commons-adapters-*` e `commons-app-*`, onde
o trade-off é favorável (código de infraestrutura, sem impacto na API pública do kernel).

## Motivação

### 1. Java 16+ Records eliminam o principal caso de uso

O caso de uso mais frequente do Lombok — criar classes de valor imutáveis com
getters, equals, hashCode e toString — é agora atendido nativamente por Java Records.

```java
// Com Lombok (pre-Records)
@Value
public class Money {
    BigDecimal amount;
    String currency;
}

// Com Java Records (Java 21, já requisito deste projeto)
public record Money(BigDecimal amount, String currency) {}
```

Records eliminam ~80% da motivação de usar Lombok no kernel.

### 2. Anotações de processamento em tempo de compilação afetam a API pública

Lombok gera código via `javac` annotation processor. O código gerado:
- Não aparece no código-fonte — o que um consumidor da biblioteca vê no IDE
  difere do que existe no bytecode
- Pode variar entre versões do Lombok, gerando breaking changes não intencionais
- Interfere com ferramentas de análise estática (SpotBugs, PMD, JApiCmp) que
  analisam o bytecode, não o código anotado

### 3. Dependências transitivas contaminam consumidores

Se `commons-kernel-core` dependesse de Lombok, qualquer projeto que usasse o kernel
adicionaria Lombok ao classpath de compilação. Mesmo marcado como `provided`, isso
cria acoplamento implícito e potencial para conflitos de versão.

A regra do kernel é: **zero dependências externas desnecessárias**.

### 4. Transparência e debuggability

Em código de kernel (Result<T>, Problem, ErrorCode, AggregateRoot), é essencial que
o código seja completamente auditável sem ferramentas auxiliares. Desenvolvedores que
fazem `F3` (Go to Definition) no IDE devem ver o código real, não uma anotação que
esconde a implementação.

### 5. Manutenibilidade a longo prazo

O kernel é projetado para estabilidade de décadas. Lombok é uma ferramenta de terceiros
que pode mudar sua API, ser descontinuada, ou conflitar com futuras versões do Java.
Remover uma dependência de geração de código de módulos estáveis é impossível sem
breaking changes.

## Alternativas consideradas

| Alternativa | Decisão |
|---|---|
| Usar Lombok em todo o projeto | Rejeitado — contamina API pública do kernel |
| Usar Lombok apenas em `@Builder` | Rejeitado — Java Records + custom builders são suficientes |
| Usar Lombok apenas em testes | Aceitável, mas desnecessário (Mockito/builders de teste são suficientes) |
| Usar Immutables em vez de Lombok | Rejeitado — mesma categoria de problemas (APT externo) |

## Consequências

### Positivas
- Kernel sem dependências de annotation processing
- API pública 100% transparente e navegável no IDE
- Sem interferência com SpotBugs, PMD, JApiCmp e cobertura JaCoCo
- Compatibilidade com qualquer versão futura do Java

### Negativas / Trade-offs
- Builders para classes complexas precisam ser escritos manualmente
  (mitigado: padrão `Builder` estático interno é exigido pelo Checkstyle)
- Mais código-fonte nas classes de domínio
  (mitigado: Java Records cobrem ~80% dos casos de valor imutável)

## Enforcement

A regra é garantida por ArchUnit no módulo `commons-archunit`:

```java
// KernelFrameworkFreeArchTest
noClasses()
    .that().resideInAnyPackage("com.marcusprado02.commons.kernel..")
    .should().dependOnClassesThat()
    .resideInAPackage("lombok..")
    .as("Kernel must not depend on Lombok")
    .check(classes);
```

E por `KernelIsolationArchTest` que bloqueia qualquer dependência de framework externo
no pacote `commons.kernel`.
