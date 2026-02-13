# commons-spring-starter-idempotency

Starter Spring Boot para wiring do `commons-app-idempotency`.

## Uso

Adicione o starter no seu projeto e forneça um `IdempotencyStorePort` (por exemplo via JPA, Redis etc). O starter também consegue auto-configurar a store JPA se existir `EntityManager` + o módulo `commons-adapters-persistence-jpa` no classpath.

## Properties

Prefixo: `commons.idempotency`

- `commons.idempotency.default-ttl` (default `PT5M`)
- `commons.idempotency.web.enabled` (default `false`)
- `commons.idempotency.web.header-name` (default `Idempotency-Key`)
- `commons.idempotency.web.on-duplicate` (default `CONFLICT`)
- `commons.idempotency.web.result-ref-strategy` (default `LOCATION_HEADER`)
- `commons.idempotency.aop.enabled` (default `false`)

## Web MVC (Interceptor)

Quando `commons.idempotency.web.enabled=true` e `spring-webmvc` está presente, o starter registra um `HandlerInterceptor` que:

- Lê o header `Idempotency-Key` (configurável)
- Faz `tryAcquire` antes de processar requests mutáveis (`POST/PUT/PATCH/DELETE`)
- Em caso de duplicata, responde `409 Conflict` e, se disponível, retorna `Idempotency-Result-Ref`
- Ao final do request, marca como `COMPLETED` (e opcionalmente salva `Location` como resultRef)

## AOP (`@Idempotent`)

Quando `commons.idempotency.aop.enabled=true` e AOP está presente, o starter habilita a annotation `@Idempotent`:

```java
@Idempotent(key = "#p0", resultRef = "#result")
public String create(String id) {
  return "ok:" + id;
}
```

Em duplicatas, o starter lança exceções próprias que podem ser convertidas em HTTP via `@RestControllerAdvice` incluído quando o modo Web está habilitado.
