# commons-app-idempotency

API framework-agnostic para executar ações de forma idempotente via uma chave (`IdempotencyKey`) e TTL.

## Conceitos

- `IdempotencyKey`: chave (string) normalizada/validada (trim, não vazia, <= 160 chars).
- `IdempotencyStorePort`: porta de persistência (DB/Redis/memória) para registrar o estado da execução.
- `IdempotencyService` / `DefaultIdempotencyService`: serviço que coordena `find → tryAcquire → action → markCompleted/markFailed`.

## Uso

```java
IdempotencyStorePort store = /* adapter (JPA/Redis/etc) */;
IdempotencyService service = new DefaultIdempotencyService(store, Duration.ofMinutes(5));

IdempotencyKey key = new IdempotencyKey("order:123");

IdempotencyResult<String> result = service.execute(
    key,
    Duration.ofMinutes(2),
    () -> createOrder(),
    createdOrderId -> createdOrderId
);

if (result.executed()) {
  // execução original
  String orderId = result.value();
} else {
  // já executado anteriormente (ou em progresso por outro worker)
  String existing = result.existingResultRef();
}
```

## Estratégias de storage

- **In-memory (referência)**: `InMemoryIdempotencyStore` (útil para testes, dev, single-instance).
- **DB/JPA**: `commons-adapters-persistence-jpa` implementa `IdempotencyStorePort` com `JpaIdempotencyStoreAdapter`.
- **Redis**: implemente `IdempotencyStorePort` usando operações atômicas (ex.: `SET key value NX PX ttl`) para `tryAcquire`.

## Semântica de TTL

- O TTL define até quando um registro é considerado válido. Após expirar, a chave pode ser adquirida novamente.
- Para correção, o TTL deve ser maior que o pior tempo esperado de execução do `action`.
