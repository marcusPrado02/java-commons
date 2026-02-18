# API Reference: Internationalization (i18n)

## Visão Geral

`commons-app-i18n` fornece internacionalização e localização completas com suporte a múltiplos idiomas, formatação de dados, mensagens dinâmicas e fallback inteligente.

**Quando usar:**
- Aplicações multi-idioma
- Formatação de datas, números e moedas
- Mensagens de erro localizadas
- Validação localizada
- Emails e notificações em múltiplos idiomas

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-i18n</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### LocaleContext

Gerencia locale atual do usuário.

```java
public class LocaleContext {
    
    private static final ThreadLocal<Locale> current = new ThreadLocal<>();
    
    public static Locale getCurrentLocale() {
        Locale locale = current.get();
        return locale != null ? locale : Locale.getDefault();
    }
    
    public static void setLocale(Locale locale) {
        current.set(locale);
    }
    
    public static void clear() {
        current.remove();
    }
}
```

### MessageSource

Busca mensagens localizadas.

```java
public interface MessageSource {
    
    /**
     * Busca mensagem localizada.
     */
    String getMessage(String code, Locale locale);
    
    /**
     * Busca mensagem com parâmetros.
     */
    String getMessage(String code, Object[] args, Locale locale);
    
    /**
     * Busca mensagem com fallback.
     */
    String getMessage(
        String code,
        Object[] args,
        String defaultMessage,
        Locale locale
    );
}
```

### I18nService

Serviço principal de internacionalização.

```java
public interface I18nService {
    
    /**
     * Traduz código de mensagem.
     */
    String translate(String code, Object... args);
    
    /**
     * Formata número.
     */
    String formatNumber(Number number);
    
    /**
     * Formata moeda.
     */
    String formatCurrency(BigDecimal amount, Currency currency);
    
    /**
     * Formata data.
     */
    String formatDate(Instant instant, String pattern);
    
    /**
     * Formata data relativa ("2 hours ago").
     */
    String formatRelativeTime(Instant instant);
}
```

---

## 💡 Uso Básico

### Message Files

```properties
# messages_en.properties
welcome.message=Welcome, {0}!
order.created=Order #{0} created successfully
order.total=Total: {0}

validation.required={0} is required
validation.min={0} must be at least {1}
validation.email=Invalid email format

error.order.not_found=Order not found
error.payment.failed=Payment failed: {0}

# messages_pt.properties
welcome.message=Bem-vindo, {0}!
order.created=Pedido #{0} criado com sucesso
order.total=Total: {0}

validation.required={0} é obrigatório
validation.min={0} deve ser no mínimo {1}
validation.email=Formato de email inválido

error.order.not_found=Pedido não encontrado
error.payment.failed=Falha no pagamento: {0}

# messages_es.properties
welcome.message=¡Bienvenido, {0}!
order.created=Pedido #{0} creado exitosamente
order.total=Total: {0}
```

### Translating Messages

```java
@Service
public class OrderService {
    
    private final I18nService i18n;
    private final OrderRepository orderRepository;
    
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        Order order = Order.create(command);
        Result<Void> saveResult = orderRepository.save(order);
        
        if (saveResult.isFail()) {
            return Result.fail(saveResult.problemOrNull());
        }
        
        // Mensagem localizada
        String message = i18n.translate(
            "order.created",
            order.id().value()
        );
        
        log.info(message).log();
        
        return Result.ok(order.id());
    }
    
    public Result<Order> findOrder(OrderId id) {
        Optional<Order> order = orderRepository.findById(id);
        
        if (order.isEmpty()) {
            // Erro localizado
            String errorMessage = i18n.translate("error.order.not_found");
            
            return Result.fail(Problem.of(
                "ORDER.NOT_FOUND",
                errorMessage
            ));
        }
        
        return Result.ok(order.get());
    }
}
```

---

## 🌍 Locale Resolution

### Accept-Language Header

```java
@Component
public class LocaleResolverFilter implements Filter {
    
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Extrair locale do Accept-Language header
        Locale locale = parseAcceptLanguage(
            httpRequest.getHeader("Accept-Language")
        );
        
        // Definir no contexto
        LocaleContext.setLocale(locale);
        
        try {
            chain.doFilter(request, response);
        } finally {
            LocaleContext.clear();
        }
    }
    
    private Locale parseAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.ENGLISH;  // Default
        }
        
        // Parse: "pt-BR,pt;q=0.9,en;q=0.8"
        List<Locale.LanguageRange> ranges = 
            Locale.LanguageRange.parse(acceptLanguage);
        
        List<Locale> supportedLocales = List.of(
            Locale.ENGLISH,
            new Locale("pt", "BR"),
            new Locale("es", "ES")
        );
        
        Locale matched = Locale.lookup(ranges, supportedLocales);
        return matched != null ? matched : Locale.ENGLISH;
    }
}
```

### Custom Header

```java
@Component
public class CustomLocaleResolver {
    
    public Locale resolve(HttpServletRequest request) {
        // 1. Tentar custom header
        String langHeader = request.getHeader("X-Language");
        if (langHeader != null) {
            return Locale.forLanguageTag(langHeader);
        }
        
        // 2. Tentar query parameter
        String langParam = request.getParameter("lang");
        if (langParam != null) {
            return Locale.forLanguageTag(langParam);
        }
        
        // 3. Tentar Accept-Language
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null) {
            return parseAcceptLanguage(acceptLanguage);
        }
        
        // 4. Fallback para default
        return Locale.ENGLISH;
    }
}
```

### User Preference

```java
@Service
public class UserLocaleService {
    
    private final UserRepository userRepository;
    
    public Locale getUserLocale(UserId userId) {
        return userRepository.findById(userId)
            .map(User::preferredLocale)
            .orElse(Locale.ENGLISH);
    }
    
    public void setUserLocale(UserId userId, Locale locale) {
        userRepository.findById(userId)
            .ifPresent(user -> {
                user.setPreferredLocale(locale);
                userRepository.save(user);
            });
    }
}
```

---

## 🔢 Formatting

### Numbers

```java
@Service
public class NumberFormattingService {
    
    public String formatNumber(Number number, Locale locale) {
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        return formatter.format(number);
    }
    
    public String formatPercentage(double percentage, Locale locale) {
        NumberFormat formatter = NumberFormat.getPercentInstance(locale);
        return formatter.format(percentage);
    }
    
    public String formatCompact(long number, Locale locale) {
        NumberFormat formatter = NumberFormat.getCompactNumberInstance(
            locale,
            NumberFormat.Style.SHORT
        );
        return formatter.format(number);
    }
}
```

**Examples:**
```java
// English (US)
formatNumber(1234567.89, Locale.US);  // "1,234,567.89"
formatPercentage(0.75, Locale.US);    // "75%"
formatCompact(1_500_000, Locale.US);  // "1.5M"

// Portuguese (Brazil)
formatNumber(1234567.89, new Locale("pt", "BR"));  // "1.234.567,89"
formatPercentage(0.75, new Locale("pt", "BR"));    // "75%"
formatCompact(1_500_000, new Locale("pt", "BR"));  // "1,5 mi"
```

### Currency

```java
@Service
public class CurrencyFormattingService {
    
    public String formatCurrency(
        BigDecimal amount,
        Currency currency,
        Locale locale
    ) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(currency);
        return formatter.format(amount);
    }
    
    public String formatCurrencySymbol(Currency currency, Locale locale) {
        return currency.getSymbol(locale);
    }
}
```

**Examples:**
```java
BigDecimal amount = BigDecimal.valueOf(1234.56);
Currency usd = Currency.getInstance("USD");
Currency brl = Currency.getInstance("BRL");

// English (US)
formatCurrency(amount, usd, Locale.US);  // "$1,234.56"

// Portuguese (Brazil)
formatCurrency(amount, brl, new Locale("pt", "BR"));  // "R$ 1.234,56"

// Spanish (Spain)
formatCurrency(amount, Currency.getInstance("EUR"), new Locale("es", "ES"));
// "1.234,56 €"
```

### Dates

```java
@Service
public class DateFormattingService {
    
    public String formatDate(Instant instant, String pattern, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern(pattern)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault());
        
        return formatter.format(instant);
    }
    
    public String formatRelativeTime(Instant instant, Locale locale) {
        Duration duration = Duration.between(instant, Instant.now());
        
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return getMessage("time.seconds.ago", seconds, locale);
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return getMessage("time.minutes.ago", minutes, locale);
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return getMessage("time.hours.ago", hours, locale);
        } else {
            long days = seconds / 86400;
            return getMessage("time.days.ago", days, locale);
        }
    }
}
```

**Message Files:**
```properties
# messages_en.properties
time.seconds.ago={0} seconds ago
time.minutes.ago={0} minutes ago
time.hours.ago={0} hours ago
time.days.ago={0} days ago

# messages_pt.properties
time.seconds.ago=há {0} segundos
time.minutes.ago=há {0} minutos
time.hours.ago=há {0} horas
time.days.ago=há {0} dias
```

**Examples:**
```java
Instant now = Instant.now();
Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);

formatRelativeTime(twoHoursAgo, Locale.ENGLISH);           // "2 hours ago"
formatRelativeTime(twoHoursAgo, new Locale("pt", "BR"));   // "há 2 horas"
formatRelativeTime(twoHoursAgo, new Locale("es", "ES"));   // "hace 2 horas"
```

---

## ✉️ Localized Emails

### Email Template

```html
<!-- templates/order-confirmation_en.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Order Confirmation</title>
</head>
<body>
    <h1>Thank you for your order!</h1>
    <p>Hello {{customerName}},</p>
    <p>Your order #{{orderId}} has been confirmed.</p>
    <p><strong>Total:</strong> {{total}}</p>
    <p>Estimated delivery: {{deliveryDate}}</p>
</body>
</html>

<!-- templates/order-confirmation_pt.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Confirmação de Pedido</title>
</head>
<body>
    <h1>Obrigado pelo seu pedido!</h1>
    <p>Olá {{customerName}},</p>
    <p>Seu pedido #{{orderId}} foi confirmado.</p>
    <p><strong>Total:</strong> {{total}}</p>
    <p>Previsão de entrega: {{deliveryDate}}</p>
</body>
</html>
```

### Email Service

```java
@Service
public class OrderEmailService {
    
    private final TemplateEngine templateEngine;
    private final EmailSender emailSender;
    private final I18nService i18n;
    
    public Result<Void> sendOrderConfirmation(Order order, Locale locale) {
        // Trocar contexto de locale
        LocaleContext.setLocale(locale);
        
        try {
            // Preparar dados
            Map<String, Object> templateData = Map.of(
                "customerName", order.customerName(),
                "orderId", order.id().value(),
                "total", i18n.formatCurrency(
                    order.total().amount(),
                    order.total().currency()
                ),
                "deliveryDate", i18n.formatDate(
                    order.estimatedDelivery(),
                    "MMMM d, yyyy"
                )
            );
            
            // Renderizar template localizado
            String templateName = "order-confirmation_" + locale.getLanguage();
            String htmlContent = templateEngine.render(templateName, templateData);
            
            // Subject localizado
            String subject = i18n.translate(
                "email.order.confirmation.subject",
                order.id().value()
            );
            
            // Enviar email
            return emailSender.send(Email.builder()
                .to(order.customerEmail())
                .subject(subject)
                .htmlBody(htmlContent)
                .build());
                
        } finally {
            LocaleContext.clear();
        }
    }
}
```

---

## ⚠️ Validation Messages

### Localized Validation

```java
public class CreateOrderCommand {
    
    @NotBlank(message = "{validation.required}")
    private String customerId;
    
    @NotEmpty(message = "{validation.order.items.required}")
    private List<OrderItemCommand> items;
    
    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.01", message = "{validation.order.total.min}")
    private BigDecimal total;
    
    @Email(message = "{validation.email}")
    private String customerEmail;
}
```

**Validation Messages:**
```properties
# ValidationMessages_en.properties
validation.required={0} is required
validation.email=Invalid email format
validation.order.items.required=Order must have at least one item
validation.order.total.min=Order total must be positive

# ValidationMessages_pt.properties
validation.required={0} é obrigatório
validation.email=Formato de email inválido
validation.order.items.required=Pedido deve ter pelo menos um item
validation.order.total.min=Total do pedido deve ser positivo
```

### Custom Validator with i18n

```java
@Component
public class LocalizedValidator {
    
    private final Validator validator;
    private final I18nService i18n;
    
    public <T> Result<T> validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (violations.isEmpty()) {
            return Result.ok(object);
        }
        
        // Traduzir mensagens de erro
        List<String> errors = violations.stream()
            .map(violation -> {
                String template = violation.getMessage();
                String propertyName = violation.getPropertyPath().toString();
                
                // Traduzir nome da propriedade
                String localizedProperty = i18n.translate(
                    "field." + propertyName
                );
                
                // Traduzir mensagem
                return i18n.translate(
                    template.replaceAll("[{}]", ""),
                    localizedProperty
                );
            })
            .toList();
        
        return Result.fail(Problem.of(
            "VALIDATION.FAILED",
            String.join(", ", errors)
        ));
    }
}
```

---

## 🔍 Pluralization

### Plural Rules

```properties
# messages_en.properties
item.count={0, choice, 0#no items|1#1 item|1<{0} items}
order.status={0, choice, 0#No orders|1#1 order|1<{0} orders}

# messages_pt.properties
item.count={0, choice, 0#nenhum item|1#1 item|1<{0} itens}
order.status={0, choice, 0#Nenhum pedido|1#1 pedido|1<{0} pedidos}
```

### Usage

```java
@Service
public class CartService {
    
    private final I18nService i18n;
    
    public String getCartSummary(Cart cart) {
        int itemCount = cart.items().size();
        
        String itemCountMessage = i18n.translate(
            "item.count",
            itemCount
        );
        
        String totalMessage = i18n.translate(
            "order.total",
            i18n.formatCurrency(
                cart.total().amount(),
                cart.total().currency()
            )
        );
        
        return itemCountMessage + " - " + totalMessage;
    }
}
```

**Output:**
```
English:
- "no items - Total: $0.00"
- "1 item - Total: $19.99"
- "3 items - Total: $59.97"

Portuguese:
- "nenhum item - Total: R$ 0,00"
- "1 item - Total: R$ 19,99"
- "3 itens - Total: R$ 59,97"
```

---

## 🧪 Testing

### Unit Tests

```java
class I18nServiceTest {
    
    private I18nService i18nService;
    
    @BeforeEach
    void setUp() {
        i18nService = new DefaultI18nService();
    }
    
    @Test
    void shouldTranslateToEnglish() {
        LocaleContext.setLocale(Locale.ENGLISH);
        
        String message = i18nService.translate(
            "welcome.message",
            "John"
        );
        
        assertThat(message).isEqualTo("Welcome, John!");
    }
    
    @Test
    void shouldTranslateToPortuguese() {
        LocaleContext.setLocale(new Locale("pt", "BR"));
        
        String message = i18nService.translate(
            "welcome.message",
            "João"
        );
        
        assertThat(message).isEqualTo("Bem-vindo, João!");
    }
    
    @Test
    void shouldFormatCurrencyInPortuguese() {
        LocaleContext.setLocale(new Locale("pt", "BR"));
        
        String formatted = i18nService.formatCurrency(
            BigDecimal.valueOf(1234.56),
            Currency.getInstance("BRL")
        );
        
        assertThat(formatted).isEqualTo("R$ 1.234,56");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use message codes, não mensagens hardcoded
String message = i18n.translate("order.created");  // ✅

// ✅ Formate datas e números com locale
String formatted = i18n.formatCurrency(amount, currency);  // ✅

// ✅ Use ResourceBundle para mensagens
ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);

// ✅ Suporte fallback para idioma padrão
Locale matched = Locale.lookup(ranges, supported);
return matched != null ? matched : Locale.ENGLISH;

// ✅ Extraia locale de múltiplas fontes
locale = fromUserPreference()
    .or(() -> fromCustomHeader())
    .or(() -> fromAcceptLanguage())
    .orElse(Locale.ENGLISH);
```

### ❌ DON'T

```java
// ❌ NÃO faça concatenação de strings
String message = "Welcome, " + name + "!";  // ❌

// ❌ NÃO hardcode formatos
String formatted = amount + " USD";  // ❌

// ❌ NÃO assuma formato de data
String date = "01/02/2024";  // ❌ MM/DD ou DD/MM?

// ❌ NÃO traduza em runtime sem cache
for (String key : keys) {
    translate(key);  // ❌ Traduz toda vez!
}

// ❌ NÃO ignore pluralização
String msg = count + " items";  // ❌ E se count == 1?
```

---

## Ver Também

- [Data Validation](app-data-validation.md) - Validação localizada
- [Configuration](../guides/configuration.md) - Configuração de locales
- [Error Handling](../guides/error-handling.md) - Erros localizados
