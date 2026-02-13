# ADR-0003: Result Type ao invés de Exceptions para Casos Esperados

**Status**: Aceito  
**Data**: 2026-01-28  
**Decisores**: Equipe de Arquitetura  

## Contexto

Exceptions em Java têm custos e problemas:
- Performance overhead (stack trace)
- Fluxo de controle oculto
- Difícil rastreamento de cenários de erro esperados
- Checked exceptions forçam try-catch verboso
- Unchecked exceptions podem passar despercebidas

Precisávamos de uma abordagem para:
- Representar erros de negócio de forma explícita
- Diferenciar falhas técnicas de resultados esperados
- Tornar assinatura de métodos mais expressiva
- Facilitar composição e pipeline de operações

## Decisão

Adotamos **Result Type Pattern** para representar resultados de operações que podem falhar de forma esperada.

### Implementação: `commons-kernel-result`

```java
// Result<T, E> representa sucesso (T) ou falha (E)
public sealed interface Result<T, E> {
  record Success<T, E>(T value) implements Result<T, E> {}
  record Failure<T, E>(E error) implements Result<T, E> {}
  
  // Métodos utilitários
  boolean isSuccess();
  boolean isFailure();
  T get();
  E getError();
  
  // Transformações funcionais
  <U> Result<U, E> map(Function<T, U> mapper);
  <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
  Result<T, E> mapError(Function<E, E> mapper);
}
```

### Uso Recomendado

```java
// ✅ BOM: Validação de negócio retorna Result
public Result<Order, OrderError> placeOrder(OrderRequest request) {
  if (request.items().isEmpty()) {
    return Result.failure(OrderError.EMPTY_ORDER);
  }
  
  if (request.total().isNegative()) {
    return Result.failure(OrderError.NEGATIVE_AMOUNT);
  }
  
  Order order = new Order(request);
  return Result.success(order);
}

// ❌ RUIM: Exception para caso de negócio esperado
public Order placeOrder(OrderRequest request) throws EmptyOrderException {
  if (request.items().isEmpty()) {
    throw new EmptyOrderException(); // Não use exceptions assim!
  }
  return new Order(request);
}
```

### Option<T> para Valores Opcionais

```java
// Option<T> representa presença ou ausência de valor
public sealed interface Option<T> {
  record Some<T>(T value) implements Option<T> {}
  record None<T>() implements Option<T> {}
  
  // Conversão de/para Optional
  static <T> Option<T> of(T value);
  static <T> Option<T> ofNullable(T value);
  Optional<T> toOptional();
}
```

### Either<L, R> para Escolhas Binárias

```java
// Either<L, R> representa left ou right (por convenção: error é left)
public sealed interface Either<L, R> {
  record Left<L, R>(L value) implements Either<L, R> {}
  record Right<L, R>(R value) implements Either<L, R> {}
}
```

## Consequências

### Positivas

✅ **Explícito**: Assinatura do método documenta possibilidade de falha  
✅ **Type-safe**: Compilador força tratamento de erros  
✅ **Performance**: Sem overhead de stack trace  
✅ **Composição**: Facilita pipeline funcional (map/flatMap)  
✅ **Rastreamento**: Erros de negócio são valores, não efeitos colaterais  
✅ **Pattern Matching**: Java 21+ sealed types permitem switch expressions  

### Negativas

⚠️ **Verbosidade**: Mais código para criar e desempacotar Results  
⚠️ **Curva de Aprendizado**: Desenvolvedores acostumados com exceptions precisam adaptar  
⚠️ **Interoperabilidade**: Bibliotecas externas usam exceptions  

### Quando Usar Exceptions

❌ **NÃO use exceptions para**:
- Validação de negócio
- Dados não encontrados (retorne `Option.none()`)
- Regras de domínio violadas

✅ **USE exceptions para**:
- Falhas técnicas irrecuperáveis (OutOfMemoryError, IOException)
- Bugs de programação (NullPointerException, IllegalStateException)
- Violações de pre-condições (assertions)

## Exemplos Práticos

### 1. Validação em Cadeia

```java
public Result<User, ValidationError> createUser(UserRequest req) {
  return validateEmail(req.email())
    .flatMap(email -> validateAge(req.age())
      .map(age -> new User(email, age)));
}
```

### 2. Conversão para HTTP Response

```java
Result<Order, OrderError> result = orderService.placeOrder(request);

return result
  .map(order -> ResponseEntity.ok(order))
  .getOrElse(error -> ResponseEntity
    .status(error.httpStatus())
    .body(error.message()));
```

### 3. Pattern Matching (Java 21+)

```java
return switch (userService.findById(id)) {
  case Success(User user) -> ResponseEntity.ok(user);
  case Failure(UserError.NOT_FOUND) -> ResponseEntity.notFound().build();
  case Failure(UserError error) -> ResponseEntity.badRequest().body(error);
};
```

## Alternativas Consideradas

### 1. Vavr/Functional Java
✅ **Parcialmente adotada**: Implementação própria mais leve, sem dependências externas

### 2. Optional com Exceptions
❌ **Rejeitada**: Optional não carrega informação de erro

### 3. Checked Exceptions
❌ **Rejeitada**: Verbosas e mal utilizadas na prática

### 4. Unchecked Exceptions Everywhere
❌ **Rejeitada**: Fluxo de erro não documentado na assinatura

## Referências

- [Railway Oriented Programming](https://fsharpforfunandprofit.com/rop/) - Scott Wlaschin
- [Effective Java](https://www.pearson.com/us/higher-education/program/Bloch-Effective-Java-3rd-Edition/PGM1763855.html) - Item 72: Favor exceptions for exceptional conditions
- Vavr Library (inspiração de design)

## Notas de Implementação

- Ver `commons-kernel-result` para tipos base
- Ver `commons-kernel-errors` para hierarquia de erro padronizada
- Spring adapters em `commons-adapters-web-spring` facilitam conversão Result → ResponseEntity
- Exemplos de uso em todos os módulos de aplicação
