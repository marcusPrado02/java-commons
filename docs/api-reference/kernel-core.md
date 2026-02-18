# API Reference: commons-kernel-core

## Visão Geral

O módulo `commons-kernel-core` fornece utilitários fundamentais e primitivos base para toda a plataforma. Não tem dependências externas além do JDK.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-kernel-core</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## Classes Principais

### Preconditions

Validações de pré-condições com mensagens claras.

**Package:** `com.marcusprado02.commons.kernel.core`

#### Métodos

```java
public final class Preconditions {
    
    // Validação de não-nulo
    public static <T> T checkNotNull(T reference, String paramName);
    
    // Validação de string não vazia
    public static String checkNotBlank(String value, String paramName);
    
    // Validação de condição
    public static void checkArgument(boolean condition, String message);
    
    // Validação de estado
    public static void checkState(boolean condition, String message);
    
    // Validação de índice
    public static int checkIndex(int index, int size);
}
```

#### Exemplos de Uso

```java
import com.marcusprado02.commons.kernel.core.Preconditions;

public class UserService {
    
    public void createUser(String name, String email) {
        // Valida parâmetros não-nulos
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(email, "email");
        
        // Valida que não estão vazios
        Preconditions.checkNotBlank(name, "name");
        Preconditions.checkNotBlank(email, "email");
        
        // Valida formato
        Preconditions.checkArgument(
            email.contains("@"), 
            "email must contain @"
        );
        
        // ... resto da lógica
    }
    
    public void processOrder(Order order) {
        Preconditions.checkNotNull(order, "order");
        
        // Valida estado
        Preconditions.checkState(
            order.getStatus() == OrderStatus.PENDING,
            "Order must be in PENDING status"
        );
    }
}
```

#### Quando Usar

- ✅ Validação de parâmetros de métodos públicos
- ✅ Validação de estado antes de operações críticas
- ✅ Fail-fast: detectar erros o mais cedo possível
- ❌ NÃO usar para regras de negócio (use Result<T> ou DomainException)

---

### StringUtils

Utilitários para manipulação de strings.

**Package:** `com.marcusprado02.commons.kernel.core`

#### Métodos

```java
public final class StringUtils {
    
    // Verificações
    public static boolean isBlank(String str);
    public static boolean isNotBlank(String str);
    public static boolean isEmpty(String str);
    public static boolean isNotEmpty(String str);
    
    // Transformações
    public static String trim(String str);
    public static String trimToEmpty(String str);
    public static String trimToNull(String str);
    
    // Operações
    public static String defaultIfBlank(String str, String defaultStr);
    public static String capitalize(String str);
    public static String uncapitalize(String str);
    
    // Comparação
    public static boolean equals(String str1, String str2);
    public static boolean equalsIgnoreCase(String str1, String str2);
}
```

#### Exemplos de Uso

```java
import com.marcusprado02.commons.kernel.core.StringUtils;

public class EmailService {
    
    public void sendEmail(String recipient, String subject, String body) {
        // Validação segura de strings
        if (StringUtils.isBlank(recipient)) {
            throw new IllegalArgumentException("Recipient cannot be blank");
        }
        
        // Usa valor padrão se vazio
        String finalSubject = StringUtils.defaultIfBlank(subject, "No Subject");
        
        // Trim seguro (null-safe)
        String cleanBody = StringUtils.trimToEmpty(body);
        
        // ... envia email
    }
    
    public String formatName(String firstName, String lastName) {
        // Capitaliza nomes
        String first = StringUtils.capitalize(StringUtils.trim(firstName));
        String last = StringUtils.capitalize(StringUtils.trim(lastName));
        
        return first + " " + last;
    }
}
```

---

### Dates

Utilitários para trabalhar com datas.

**Package:** `com.marcusprado02.commons.kernel.core`

#### Métodos

```java
public final class Dates {
    
    // Data/hora atual
    public static Instant now();
    public static LocalDate today();
    public static LocalDateTime nowLocal();
    
    // Formatação
    public static String formatIso(Instant instant);
    public static String formatIsoDate(LocalDate date);
    
    // Parsing
    public static Instant parseIso(String isoString);
    public static LocalDate parseIsoDate(String isoDate);
    
    // Comparações
    public static boolean isBefore(Instant instant1, Instant instant2);
    public static boolean isAfter(Instant instant1, Instant instant2);
    
    // Operações
    public static Instant plusDays(Instant instant, long days);
    public static Instant minusDays(Instant instant, long days);
}
```

#### Exemplos de Uso

```java
import com.marcusprado02.commons.kernel.core.Dates;
import java.time.Instant;

public class OrderService {
    
    public boolean isOrderExpired(Order order) {
        Instant expirationDate = Dates.plusDays(order.getCreatedAt(), 30);
        return Dates.isAfter(Dates.now(), expirationDate);
    }
    
    public String getOrderCreatedAtFormatted(Order order) {
        return Dates.formatIso(order.getCreatedAt());
    }
    
    public Order createOrder() {
        return Order.builder()
            .id(OrderId.generate())
            .createdAt(Dates.now())
            .expiresAt(Dates.plusDays(Dates.now(), 30))
            .build();
    }
}
```

---

### Collections

Utilitários para trabalhar com coleções.

**Package:** `com.marcusprado02.commons.kernel.core`

#### Métodos

```java
public final class Collections {
    
    // Verificações
    public static boolean isEmpty(Collection<?> collection);
    public static boolean isNotEmpty(Collection<?> collection);
    
    // Criação imutável
    public static <T> List<T> immutableListOf(T... elements);
    public static <T> Set<T> immutableSetOf(T... elements);
    public static <K, V> Map<K, V> immutableMapOf(K k1, V v1, K k2, V v2);
    
    // Operações
    public static <T> List<T> filterNotNull(List<T> list);
    public static <T> List<T> distinct(List<T> list);
    
    // Conversões
    public static <T> List<T> toList(Set<T> set);
    public static <T> Set<T> toSet(List<T> list);
}
```

#### Exemplos de Uso

```java
import com.marcusprado02.commons.kernel.core.Collections;
import java.util.List;

public class ProductService {
    
    public List<Product> getAvailableProducts(List<Product> products) {
        if (Collections.isEmpty(products)) {
            return List.of();
        }
        
        return products.stream()
            .filter(Product::isAvailable)
            .toList();
    }
    
    public Set<String> getUniqueCategories(List<Product> products) {
        return Collections.toSet(
            products.stream()
                .map(Product::getCategory)
                .toList()
        );
    }
}
```

---

## Best Practices

### 1. Use Preconditions para Fail-Fast

```java
// ✅ BOM - Falha imediatamente com mensagem clara
public void transfer(Account from, Account to, Money amount) {
    Preconditions.checkNotNull(from, "from");
    Preconditions.checkNotNull(to, "to");
    Preconditions.checkNotNull(amount, "amount");
    Preconditions.checkArgument(amount.isPositive(), "amount must be positive");
    // ... lógica
}

// ❌ RUIM - NullPointerException obscura mais tarde
public void transfer(Account from, Account to, Money amount) {
    // Se from for null, vai quebrar em algum lugar não óbvio
    from.debit(amount);
}
```

### 2. Prefira StringUtils para Operações Null-Safe

```java
// ✅ BOM - Null-safe
String name = StringUtils.trimToEmpty(user.getName());
String displayName = StringUtils.defaultIfBlank(name, "Anonymous");

// ❌ RUIM - Pode lançar NullPointerException
String name = user.getName().trim();
```

### 3. Use Dates para Operações Temporais

```java
// ✅ BOM - API clara e consistente
Instant expiresAt = Dates.plusDays(Dates.now(), 30);

// ❌ RUIM - Verboso e propenso a erros
Instant expiresAt = Instant.now().plus(Duration.ofDays(30));
```

---

## Dependências

Este módulo tem **ZERO dependências externas** além do JDK 21+.

---

## Testing

### Exemplo de Teste com JUnit 5

```java
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PreconditionsTest {
    
    @Test
    void shouldThrowWhenNullParameter() {
        assertThatThrownBy(() -> {
            Preconditions.checkNotNull(null, "value");
        })
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
    }
    
    @Test
    void shouldThrowWhenBlankString() {
        assertThatThrownBy(() -> {
            Preconditions.checkNotBlank("  ", "name");
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
    }
}
```

---

## Ver Também

- [commons-kernel-ddd](kernel-ddd.md) - Building blocks DDD
- [commons-kernel-result](kernel-result.md) - Result pattern
- [commons-kernel-errors](kernel-errors.md) - Error handling
