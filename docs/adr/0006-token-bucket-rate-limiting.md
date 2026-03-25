# ADR-0006: Token Bucket para Rate Limiting

**Status**: Aceito
**Data**: 2026-03-25
**Decisores**: Equipe de Arquitetura

## Contexto

O módulo `commons-app-rate-limiting` precisava de uma estratégia de controle de taxa
para proteger serviços de sobrecarga. As principais alternativas consideradas foram:

1. **Fixed Window Counter** — conta requisições em janelas de tempo fixas (ex: 100 req/min)
2. **Sliding Window Log** — registra o timestamp de cada requisição e conta a janela deslizante
3. **Sliding Window Counter** — aproximação da janela deslizante com dois contadores
4. **Leaky Bucket** — processa requisições a uma taxa constante, excesso é descartado ou enfileirado
5. **Token Bucket** — repõe tokens a uma taxa constante; cada requisição consome um token

## Decisão

Adotamos o **Token Bucket** como algoritmo padrão da implementação `RateLimiter` do kernel.

## Motivação

### Por que Token Bucket vence sobre as alternativas

| Critério | Fixed Window | Sliding Log | Sliding Counter | Leaky Bucket | **Token Bucket** |
|---|---|---|---|---|---|
| Permite bursts legítimos | Parcialmente | Não | Parcialmente | Não | **Sim** |
| Memória O(1) por limite | Sim | Não (O(n)) | Sim | Sim | **Sim** |
| Implementação simples | Sim | Não | Moderada | Moderada | **Sim** |
| Justo para tráfego uniforme | Não* | Sim | Sim | Sim | **Sim** |
| Suporte assíncrono natural | Sim | Difícil | Moderado | Difícil | **Sim** |

*Fixed Window sofre de "double spend" — até 2× o limite pode passar na fronteira de janelas.

### Vantagens específicas para microserviços

1. **Tolerância a bursts legítimos**: um serviço ocioso acumula tokens e pode processar
   rajadas de tráfego sem ser bloqueado — comportamento correto para eventos como login
   após manutenção programada.

2. **Overhead O(1)**: o estado do token bucket é apenas `(tokens, lastRefillTime)` —
   sem necessidade de armazenar histórico de requisições (ao contrário do Sliding Log).

3. **Composabilidade**: múltiplos limitadores com taxas diferentes (por usuário, por IP,
   por endpoint) podem ser combinados sem interferência — cada um é um estado independente.

4. **Suporte nativo a `tryAcquire` não-bloqueante**: a semântica de "tente consumir N tokens,
   retorne falso se não houver" mapeia diretamente para `RateLimiter.tryConsume()` sem necessidade
   de filas ou threads dedicadas.

5. **Integração com backpressure**: o número de tokens disponíveis pode ser exposto como métrica
   para dashboards de observabilidade e alertas de capacidade.

## Consequências

### Positivas
- `RateLimiter.tryConsume()` e `tryConsumeAsync()` são O(1) em tempo e memória
- Suporte a limites por burst configurável (`burstCapacity`) separado da taxa base
- Implementação distribuída simples via Redis (armazenar apenas dois longs por chave)

### Negativas / Trade-offs
- Burst excessivo pode ser visto como injusto por consumidores que chegam logo após
  (o bucket cheio permite rajadas que parecem violar o limite médio)
- Para casos onde tráfego absolutamente uniforme é necessário (streaming de vídeo, IoT),
  o Leaky Bucket seria mais adequado — documentado como caso de uso alternativo

## Implementação

```java
// commons-app-rate-limiting
RateLimiterConfig config = RateLimiterConfig.tokenBucket()
    .refillRate(100)           // tokens por segundo
    .burstCapacity(200)        // máximo acumulado (burst de 2s)
    .build();

RateLimiter limiter = RateLimiterFactory.create(config);

// Uso síncrono
if (!limiter.tryConsume()) {
    throw new RateLimitExceededException();
}

// Uso assíncrono (non-blocking)
limiter.tryConsumeAsync()
    .thenAccept(allowed -> { ... });
```

## Referências

- Cloudflare Blog: "How we built rate limiting" — análise comparativa dos algoritmos
- IETF RFC 4115: Token Bucket descrição formal
- `commons-app-rate-limiting` — implementação de referência
- `commons-benchmarks` — JMH benchmarks para validação de throughput
