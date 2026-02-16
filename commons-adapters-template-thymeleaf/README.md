# Commons Adapters - Thymeleaf Template Engine

Thymeleaf 3.x adapter for `TemplatePort`.

## Overview

This adapter provides Thymeleaf template engine implementation for the `commons-ports-template` interface. Thymeleaf is a modern server-side Java template engine for web and standalone environments, with powerful text processing capabilities.

## Features

- ✅ HTML5, XML, and text template rendering
- ✅ Template caching with configurable TTL
- ✅ Classpath and filesystem template loading
- ✅ String template rendering for dynamic templates
- ✅ Locale-aware formatting and i18n
- ✅ Template existence checking
- ✅ Cache management (clear all or specific templates)

## Dependencies

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-template-thymeleaf</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Create Configuration

```java
ThymeleafConfiguration config = ThymeleafConfiguration.builder()
    .templatePrefix("classpath:/templates/")
    .templateSuffix(".html")
    .templateMode(TemplateMode.HTML)
    .cacheable(true)
    .cacheableTTLMs(3600000L) // 1 hour
    .build();
```

### 2. Create Adapter

```java
TemplatePort templatePort = new ThymeleafTemplateAdapter(config);
```

### 3. Render Template

```java
TemplateContext context = TemplateContext.builder()
    .variable("userName", "John Doe")
    .variable("orderNumber", "12345")
    .build();

Result<TemplateResult> result = templatePort.render("email/welcome", context);

if (result.isOk()) {
    String html = result.getValue().getContent();
    System.out.println(html);
}
```

## Configuration Options

### Template Prefix

Specifies where to load templates from:

```java
// Load from classpath
.templatePrefix("classpath:/templates/")

// Load from filesystem
.templatePrefix("file:/var/app/templates/")

// No prefix
.templatePrefix("")
```

### Template Mode

Choose appropriate mode for your templates:

```java
// HTML5 templates (default)
.templateMode(TemplateMode.HTML)

// XML templates
.templateMode(TemplateMode.XML)

// Plain text templates
.templateMode(TemplateMode.TEXT)

// JavaScript templates
.templateMode(TemplateMode.JAVASCRIPT)

// CSS templates
.templateMode(TemplateMode.CSS)

// Raw (no processing)
.templateMode(TemplateMode.RAW)
```

### Cache Settings

```java
// Enable caching (recommended for production)
.cacheable(true)
.cacheableTTLMs(3600000L) // 1 hour

// Disable caching (development)
.cacheable(false)

// Infinite cache (no expiration)
.cacheableTTLMs(null)
```

### Pre-configured Profiles

```java
// Default HTML configuration
ThymeleafConfiguration.defaultHtml()

// Development mode (no cache)
ThymeleafConfiguration.dev()

// Text templates
ThymeleafConfiguration.text()

// XML templates
ThymeleafConfiguration.xml()
```

## Usage Examples

### Email Welcome Template

**Template** (`templates/email/welcome.html`):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Welcome!</title>
</head>
<body>
    <h1>Welcome, <span th:text="${userName}">User</span>!</h1>
    <p>Thank you for joining us.</p>
    <p>Click <a th:href="${activationLink}">here</a> to activate your account.</p>
</body>
</html>
```

**Java Code**:
```java
TemplateContext context = TemplateContext.builder()
    .variable("userName", user.getName())
    .variable("activationLink", generateActivationLink(user))
    .locale(user.getPreferredLocale())
    .build();

Result<TemplateResult> result = templatePort.render("email/welcome", context);
```

### Invoice Generation

**Template** (`templates/invoices/standard.html`):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Invoice</title>
</head>
<body>
    <h1>Invoice #<span th:text="${invoice.number}">12345</span></h1>

    <h2>Customer</h2>
    <p th:text="${customer.name}">Customer Name</p>
    <p th:text="${customer.email}">email@example.com</p>

    <h2>Items</h2>
    <table>
        <thead>
            <tr>
                <th>Item</th>
                <th>Quantity</th>
                <th>Price</th>
                <th>Total</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="item : ${items}">
                <td th:text="${item.name}">Item Name</td>
                <td th:text="${item.quantity}">1</td>
                <td th:text="${#numbers.formatCurrency(item.price)}">$10.00</td>
                <td th:text="${#numbers.formatCurrency(item.total)}">$10.00</td>
            </tr>
        </tbody>
    </table>

    <h3>Total: <span th:text="${#numbers.formatCurrency (totalAmount)}">$100.00</span></h3>
    <p>Due Date: <span th:text="${#temporals.format(dueDate, 'dd/MM/yyyy')}">01/01/2026</span></p>
</body>
</html>
```

**Java Code**:
```java
TemplateContext context = TemplateContext.builder()
    .variable("invoice", invoice)
    .variable("customer", invoice.getCustomer())
    .variable("items", invoice.getItems())
    .variable("totalAmount", invoice.getTotalAmount())
    .variable("dueDate", invoice.getDueDate())
    .locale(customer.getLocale())
    .build();

Result<TemplateResult> result = templatePort.render("invoices/standard", context);
```

### Dynamic String Templates

```java
String template = """
    <html>
      <body>
        <h1 th:text="${title}">Title</h1>
        <ul>
          <li th:each="item : ${items}" th:text="${item}">Item</li>
        </ul>
      </body>
    </html>
    """;

TemplateContext context = TemplateContext.builder()
    .variable("title", "Dynamic Report")
    .variable("items", List.of("Item 1", "Item 2", "Item 3"))
    .build();

Result<TemplateResult> result = templatePort.renderString(template, context);
```

### Text Templates

**Configuration**:
```java
ThymeleafConfiguration config = ThymeleafConfiguration.text();
TemplatePort templatePort = new ThymeleafTemplateAdapter(config);
```

**Template** (`templates/email/welcome.txt`):
```
Welcome, [(${userName})]!

Thank you for registering.

Your activation link: [(${activationLink})]

Best regards,
The Team
```

**Java Code**:
```java
TemplateContext context = TemplateContext.builder()
    .variable("userName", "John")
    .variable("activationLink", "https://example.com/activate/xyz")
    .build();

Result<TemplateResult> result = templatePort.render("email/welcome", context);
```

### Conditional Rendering

```html
<div th:if="${user.isPremium}">
    <h2>Premium Features</h2>
    <p>Thank you for being a premium member!</p>
</div>

<div th:unless="${user.isPremium}">
    <h2>Upgrade to Premium</h2>
    <p>Get access to exclusive features!</p>
</div>
```

### Loops and Collections

```html
<h2>Products</h2>
<div th:each="product, iterStat : ${products}">
    <h3 th:text="${product.name}">Product Name</h3>
    <p>Price: <span th:text="${#numbers.formatCurrency(product.price)}">$0.00</span></p>
    <p th:if="${iterStat.first}">⭐ Featured</p>
</div>

<p th:if="${#lists.isEmpty(products)}">No products available.</p>
```

### Fragments and Layouts

**Base Layout** (`templates/layouts/base.html`):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:replace="~{::title}">Default Title</title>
</head>
<body>
    <header th:replace="~{fragments/header :: header}">Header</header>

    <main>
        <div th:replace="~{::content}">Content goes here</div>
    </main>

    <footer th:replace="~{fragments/footer :: footer}">Footer</footer>
</body>
</html>
```

**Page Using Layout**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/base :: layout}">
<head>
    <title>My Page</title>
</head>
<body>
    <div th:fragment="content">
        <h1>My Page Content</h1>
        <p>This content will be inserted into the layout.</p>
    </div>
</body>
</html>
```

## Thymeleaf Features

### Expression Types

- **Variable**: `${variable}`
- **Selection**: `*{property}` (on selected object)
- **Message**: `#{message.key}` (i18n)
- **Link**: `@{/path}` (URL building)
- **Fragment**: `~{template :: fragment}` (template fragments)

### Utility Objects

Thymeleaf provides built-in utility objects:

```html
<!-- Date/Time -->
<span th:text="${#temporals.format(date, 'dd/MM/yyyy')}">Date</span>

<!-- Numbers -->
<span th:text="${#numbers.formatDecimal(number, 2, 2)}">Number</span>
<span th:text="${#numbers.formatCurrency(amount)}">Amount</span>

<!-- Strings -->
<span th:text="${#strings.toUpperCase(text)}">TEXT</span>
<span th:text="${#strings.substring(text, 0, 10)}">Substring</span>

<!-- Collections -->
<span th:text="${#lists.size(list)}">Size</span>
<span th:if="${#lists.isEmpty(list)}">Empty</span>

<!-- Objects -->
<span th:if="${#objects.nullSafe(obj, 'default')}">Value</span>
```

## Cache Management

```java
// Clear all cached templates
templatePort.clearCache();

// Clear specific template
templatePort.clearCache("email/welcome");

// Check if template exists (doesn't load it)
boolean exists = templatePort.exists("email/welcome");
```

## Error Handling

All operations return `Result<T>`:

```java
Result<TemplateResult> result = templatePort.render("email/welcome", context);

if (result.isOk()) {
    String html = result.getValue().getContent();
    sendEmail(html);
} else {
    Problem error = result.getError();

    switch (error.errorCode().code()) {
        case "TEMPLATE_NOT_FOUND":
            logger.error("Template not found: {}", error.message());
            break;
        case "TEMPLATE_PROCESSING_ERROR":
            logger.error("Template processing failed: {}", error.message());
            break;
        default:
            logger.error("Unexpected error: {}", error.message());
    }
}
```

### Error Codes

- `TEMPLATE_NOT_FOUND` - Template file doesn't exist
- `TEMPLATE_PROCESSING_ERROR` - Thymeleaf processing error (syntax, missing variable, etc.)
- `TEMPLATE_RENDER_ERROR` - Unexpected error during rendering

## Project Structure

```
src/main/resources/templates/
├── email/
│   ├── welcome.html
│   ├── password-reset.html
│   └── order-confirmation.html
├── reports/
│   ├── monthly-sales.html
│   └── user-activity.html
├── invoices/
│   └── standard.html
├── layouts/
│   └── base.html
└── fragments/
    ├── header.html
    └── footer.html
```

## Best Practices

1. **Use Layouts**: Create base layouts for consistent structure
2. **Extract Fragments**: Reuse common components (header, footer, etc.)
3. **Enable Caching**: Always enable caching in production
4. **Escape Variables**: Thymeleaf auto-escapes by default, use `th:utext` only when needed
5. **Use Utility Objects**: Leverage Thymeleaf's utility objects for formatting
6. **Validate Templates**: Check template existence before rendering
7. **Handle Errors**: Always handle Result errors, don't assume success
8. **Set Locale**: Always set locale for proper i18n and formatting

## Performance Tips

1. **Enable Template Caching**: Set `cacheable(true)` in production
2. **Set Appropriate TTL**: Balance between freshness and performance
3. **Use Fragments**: Fragment caching improves performance
4. **Avoid Complex Logic**: Keep template logic simple, do processing in Java
5. **Monitor Cache Hit Rate**: Clear cache only when necessary

## Thread Safety

- `ThymeleafTemplateAdapter` is thread-safe
- `TemplateContext` is immutable and thread-safe
- `TemplateResult` is immutable and thread-safe
- Template engine is thread-safe and can be shared

## Thymeleaf Version

This adapter uses Thymeleaf 3.1.2.RELEASE.

## References

- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Thymeleaf Tutorial](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)
- [Template Port Documentation](../commons-ports-template/README.md)
