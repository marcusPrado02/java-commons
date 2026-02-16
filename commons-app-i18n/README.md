# Commons App - I18n

Comprehensive internationalization (i18n) support for Java applications with message bundles, locale detection, formatting, and pluralization.

## Features

- 🌍 **Message Bundles**: Load and resolve localized messages from resource bundles
- 🔍 **Locale Detection**: Parse and resolve locale from HTTP headers, cookies, user preferences
- 💱 **Currency Formatting**: Format currency values with proper symbols and decimals
- 📅 **Date/Time Formatting**: Format dates and times according to locale conventions
- 🔢 **Number Formatting**: Format numbers with locale-specific grouping and decimals
- 📝 **Pluralization**: Handle plural forms for different languages
- 🧵 **Thread-Local Context**: Thread-safe locale management for web applications
- ✅ **Type-Safe**: Result pattern for error handling

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-i18n</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Create Message Bundles

Create properties files in `src/main/resources`:

```properties
# messages.properties (default/English)
welcome.user=Welcome, {name}!
goodbye.user=Goodbye, {name}. See you soon!
items.one=You have {count} item in your cart
items.other=You have {count} items in your cart
price.label=Price: {amount}

# messages_fr.properties (French)
welcome.user=Bienvenue, {name}!
goodbye.user=Au revoir, {name}. À bientôt!
items.one=Vous avez {count} article dans votre panier
items.other=Vous avez {count} articles dans votre panier
price.label=Prix: {amount}

# messages_de.properties (German)
welcome.user=Willkommen, {name}!
goodbye.user=Auf Wiedersehen, {name}. Bis bald!
items.one=Sie haben {count} Artikel in Ihrem Warenkorb
items.other=Sie haben {count} Artikel in Ihrem Warenkorb
price.label=Preis: {amount}
```

### 2. Load and Use Messages

```java
// Create message source
MessageSource messages = new ResourceBundleMessageSource("messages");

// Get simple message
Result<String> welcome = messages.getMessage("welcome.user",
    Locale.FRENCH,
    Map.of("name", "Jean"));
// Result.ok("Bienvenue, Jean!")

// Get with default
String message = messages.getMessageOrDefault(
    "unknown.key",
    Locale.ENGLISH,
    "Default message");

// Pluralization
Result<String> items = messages.getPluralMessage(
    "items",
    5,
    Locale.FRENCH);
// Result.ok("Vous avez 5 articles dans votre panier")
```

### 3. Format Values

```java
// Currency formatting
LocaleFormatter formatter = new LocaleFormatter(Locale.FRANCE);
String price = formatter.formatCurrency(99.99, "EUR");
// "99,99 €"

// Date formatting
String date = formatter.formatDate(LocalDate.now());
// "16/02/2026"

// Number formatting
String number = formatter.formatNumber(1234567.89);
// "1 234 567,89"

// Percentage
String percent = formatter.formatPercent(0.15);
// "15 %"
```

### 4. Locale Detection

```java
// Parse Accept-Language header
String header = "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7";
List<Locale> locales = AcceptLanguageParser.parse(header);
// [fr_FR, fr, en_US, en]

Locale preferred = AcceptLanguageParser.parseFirst(header);
// fr_FR
```

### 5. Thread-Local Locale Management

```java
// Set locale for current thread
LocaleContext.setLocale(Locale.GERMAN);

// Get current locale
Locale locale = LocaleContext.getLocale();

// Always clear after use (important in web apps)
LocaleContext.clear();
```

## Advanced Usage

### Custom Locale Resolver

```java
// Create resolver for HTTP requests
LocaleResolver<HttpServletRequest> headerResolver = request -> {
    String acceptLanguage = request.getHeader("Accept-Language");
    return AcceptLanguageParser.parseFirst(acceptLanguage);
};

// Cookie-based resolver
LocaleResolver<HttpServletRequest> cookieResolver = request -> {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("locale".equals(cookie.getName())) {
                return Locale.forLanguageTag(cookie.getValue());
            }
        }
    }
    return null;
};

// Composite resolver with fallback
LocaleResolver<HttpServletRequest> resolver = LocaleResolver.composite(
    List.of(cookieResolver, headerResolver)
);

// With fallback
LocaleResolver<HttpServletRequest> safeResolver =
    LocaleResolver.withFallback(resolver, Locale.ENGLISH);
```

### Servlet Filter Integration

```java
@WebFilter("/*")
public class LocaleFilter implements Filter {

    private final MessageSource messages = new ResourceBundleMessageSource("messages");
    private final LocaleResolver<HttpServletRequest> resolver;

    public LocaleFilter() {
        this.resolver = request ->
            AcceptLanguageParser.parseFirst(request.getHeader("Accept-Language"));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        // Resolve locale
        Locale locale = resolver.resolve(request);
        if (locale != null) {
            LocaleContext.setLocale(locale);
        }

        try {
            chain.doFilter(req, res);
        } finally {
            // Always clear to prevent memory leaks
            LocaleContext.clear();
        }
    }
}
```

### Spring Boot Integration

```java
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        return new ResourceBundleMessageSource("messages");
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public LocaleFormatter localeFormatter() {
        return new LocaleFormatter(LocaleContext.getLocale());
    }

    @Bean
    public LocaleResolver<ServerHttpRequest> localeResolver() {
        return request -> {
            List<String> acceptLanguage = request.getHeaders().get(HttpHeaders.ACCEPT_LANGUAGE);
            if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                return AcceptLanguageParser.parseFirst(acceptLanguage.get(0));
            }
            return null;
        };
    }
}

@RestControllerAdvice
public class LocaleInterceptor implements HandlerInterceptor {

    private final LocaleResolver<ServerHttpRequest> resolver;

    public LocaleInterceptor(LocaleResolver<ServerHttpRequest> resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Locale locale = resolver.resolve(new ServletServerHttpRequest(request));
        if (locale != null) {
            LocaleContext.setLocale(locale);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LocaleContext.clear();
    }
}
```

### REST Controller Example

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final MessageSource messages;
    private final LocaleFormatter formatter;

    public ProductController(MessageSource messages, LocaleFormatter formatter) {
        this.messages = messages;
        this.formatter = formatter;
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        Product product = productService.findById(id);
        Locale locale = LocaleContext.getLocale();

        return new ProductResponse(
            product.getId(),
            product.getName(),
            formatter.formatCurrency(product.getPrice(), "USD"),
            messages.getMessageOrDefault("product.available", locale, "Available")
        );
    }
}
```

## Pluralization Rules

The library supports basic pluralization rules for multiple languages:

### English (en)
- `key.one`: count == 1
- `key.other`: count != 1

### French (fr)
- `key.zero`: count == 0 (optional)
- `key.one`: count == 1
- `key.other`: count > 1

### Complex Languages
For languages with complex plural rules (e.g., Russian, Polish, Arabic), consider:
- Creating separate keys for each form
- Using ICU MessageFormat library
- Implementing custom plural rule logic

## Best Practices

### 1. Always Clear LocaleContext

```java
try {
    LocaleContext.setLocale(userLocale);
    // Do work
} finally {
    LocaleContext.clear(); // Prevent memory leaks
}
```

### 2. Use Fallback Locales

```java
MessageSource messages = new ResourceBundleMessageSource("messages");

// Will fallback: fr_CA -> fr -> default
Result<String> msg = messages.getMessage("key", new Locale("fr", "CA"));
```

### 3. Cache Formatters

```java
// Instead of creating new formatters
LocaleFormatter formatter = new LocaleFormatter(locale);

// Cache by locale
Map<Locale, LocaleFormatter> formatters = new ConcurrentHashMap<>();
LocaleFormatter cached = formatters.computeIfAbsent(locale, LocaleFormatter::new);
```

### 4. Message Key Conventions

```properties
# Namespace your keys
user.welcome=Welcome!
user.goodbye=Goodbye!

# Use dots for hierarchy
form.validation.required=This field is required
form.validation.email=Invalid email address

# Plural forms
items.one=One item
items.other={count} items
```

### 5. Handle Missing Messages

```java
// Option 1: Use Result pattern
Result<String> result = messages.getMessage("key", locale);
if (result.isOk()) {
    String message = result.getOrNull();
}

// Option 2: Provide default
String message = messages.getMessageOrDefault("key", locale, "Default");

// Option 3: Check existence first
if (messages.hasMessage("key", locale)) {
    Result<String> result = messages.getMessage("key", locale);
}
```

## Performance Considerations

### Bundle Caching

```java
// Enable caching (default)
MessageSource cached = new ResourceBundleMessageSource("messages", true);

// Disable for development
MessageSource noCaching = new ResourceBundleMessageSource("messages", false);

// Reload bundles
cached.reload();
```

### Formatter Reuse

```java
// Don't create new formatters for each operation
for (Product product : products) {
    LocaleFormatter formatter = new LocaleFormatter(locale); // BAD
    String price = formatter.formatCurrency(product.getPrice(), "USD");
}

// Create once and reuse
LocaleFormatter formatter = new LocaleFormatter(locale); // GOOD
for (Product product : products) {
    String price = formatter.formatCurrency(product.getPrice(), "USD");
}
```

## Testing

```java
@Test
void testMessageResolution() {
    MessageSource messages = new ResourceBundleMessageSource("test-messages");

    Result<String> result = messages.getMessage("test.key", Locale.ENGLISH);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("Test message");
}

@Test
void testCurrencyFormatting() {
    LocaleFormatter formatter = new LocaleFormatter(Locale.US);

    String formatted = formatter.formatCurrency(99.99, "USD");

    assertThat(formatted).isEqualTo("$99.99");
}

@Test
void testThreadLocalContext() {
    LocaleContext.setLocale(Locale.GERMAN);

    Locale retrieved = LocaleContext.getLocale();

    assertThat(retrieved).isEqualTo(Locale.GERMAN);

    LocaleContext.clear();
}
```

## Comparison with Other Solutions

| Feature | Commons I18n | Spring MessageSource | Java ResourceBundle |
|---------|--------------|---------------------|---------------------|
| Message bundles | ✅ | ✅ | ✅ |
| Pluralization | ✅ | ❌ | ❌ |
| Type-safe errors | ✅ (Result) | ❌ | ❌ |
| Currency formatting | ✅ | ❌ | ❌ |
| Date formatting | ✅ | ❌ | ❌ |
| Locale detection | ✅ | ✅ | ❌ |
| Thread-local context | ✅ | ✅ | ❌ |
| Framework independent | ✅ | ❌ (Spring) | ✅ |
| Cache management | ✅ | ✅ | ✅ |

## License

This project is part of the Commons Platform and follows the same license.
