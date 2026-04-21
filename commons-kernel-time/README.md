# commons-kernel-time

Port de relógio (`ClockProvider`) e implementações para produção e testes. Elimina dependência direta de `Instant.now()` e `LocalDateTime.now()` no domínio, tornando o tempo controlável em testes.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-kernel-time</artifactId>
</dependency>
```

## ClockProvider

```java
// Domínio usa o port — sem chamar Instant.now() diretamente
public class Order extends AggregateRoot<OrderId> {

    private final Instant createdAt;

    public Order(OrderId id, ClockProvider clock) {
        super(id);
        this.createdAt = clock.now();   // controlável em testes
    }
}
```

## Implementações

```java
// Produção — relógio real
ClockProvider realClock = ClockProvider.system();

// Produção com fuso horário específico
ClockProvider saoPauloClock = ClockProvider.system(ZoneId.of("America/Sao_Paulo"));

// Testes — relógio fixo
ClockProvider fixedClock = ClockProvider.fixed(
    Instant.parse("2026-01-15T10:00:00Z"),
    ZoneId.of("UTC")
);

// Testes — relógio avançável manualmente
MutableClock mutableClock = ClockProvider.mutable(Instant.parse("2026-01-15T10:00:00Z"));
mutableClock.advance(Duration.ofMinutes(30)); // avança no tempo
```

## Uso em testes

```java
@Test
void shouldExpireSessionAfter30Minutes() {
    MutableClock clock = ClockProvider.mutable(Instant.parse("2026-01-15T10:00:00Z"));
    Session session = new Session(SessionId.generate(), clock);

    assertThat(session.isExpired(clock)).isFalse();

    clock.advance(Duration.ofMinutes(31));

    assertThat(session.isExpired(clock)).isTrue();
}
```

## Spring Boot

```java
@Bean
public ClockProvider clockProvider() {
    return ClockProvider.system(ZoneId.of("America/Sao_Paulo"));
}

// Testes
@TestConfiguration
public class TestTimeConfig {
    @Bean
    @Primary
    public ClockProvider clockProvider() {
        return ClockProvider.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC"));
    }
}
```
