# java-commons

Biblioteca modular de commons para serviços Java 21+ com arquitetura hexagonal.

## Estrutura

| Grupo | Descrição |
|-------|-----------|
| `commons-kernel-*` | Domínio puro — sem dependências de framework |
| `commons-app-*` | Casos de uso e orquestração de aplicação |
| `commons-ports-*` | Interfaces hexagonais (ports) |
| `commons-adapters-*` | Implementações de infraestrutura |
| `commons-spring-starter-*` | Auto-configuração Spring Boot |

## Começando

### 1. Importe o BOM

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.marcusprado02.commons</groupId>
      <artifactId>commons-bom</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2. Adicione os módulos necessários

```xml
<dependencies>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-result</artifactId>
  </dependency>
  <dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-errors</artifactId>
  </dependency>
</dependencies>
```

### 3. Use `Result<T>` para tratamento de erros

```java
public Result<User> findById(String id) {
    return userRepository.findById(id)
        .map(Result::ok)
        .orElseGet(() -> Result.fail(
            Problem.of(
                ErrorCode.of("USER.NOT_FOUND"),
                ErrorCategory.NOT_FOUND,
                Severity.WARNING,
                "User not found: " + id)));
}
```

## Build

Requer Java 21+ e Maven 3.9+.

```bash
./mvnw verify
```

## Requisitos

- Java 21+
- Maven 3.9+
- Docker (para testes de integração com Testcontainers)
