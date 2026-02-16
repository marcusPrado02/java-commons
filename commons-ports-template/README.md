# Commons Ports - Template Engine

Platform-agnostic port interface for template engines (Thymeleaf, FreeMarker, Velocity, Mustache, etc.).

## Overview

This module provides a unified API for template rendering operations, independent of the underlying template engine implementation. It supports various use cases like email templating, report generation, HTML page rendering, and more.

## Core Components

### TemplatePort

Main interface for template operations:

```java
public interface TemplatePort {
  Result<TemplateResult> render(String templateName, TemplateContext context);
  Result<TemplateResult> renderString(String templateContent, TemplateContext context);
  boolean exists(String templateName);
  void clearCache();
  void clearCache(String templateName);
}
```

### TemplateContext

Immutable context with variables and locale:

```java
TemplateContext context = TemplateContext.builder()
    .variable("userName", "John Doe")
    .variable("order", orderDto)
    .variable("items", itemsList)
    .locale(Locale.US)
    .build();
```

### TemplateResult

Rendering result with content and metadata:

```java
TemplateResult result = TemplateResult.html("email/welcome", htmlContent);
String content = result.getContent();
byte[] bytes = result.getBytes();
String contentType = result.contentType(); // "text/html"
```

## Usage Examples

### Basic Template Rendering

```java
// Inject the port
private final TemplatePort templatePort;

public void sendWelcomeEmail(User user) {
  // Create context
  TemplateContext context = TemplateContext.builder()
      .variable("userName", user.getName())
      .variable("activationLink", generateActivationLink(user))
      .locale(user.getPreferredLocale())
      .build();

  // Render template
  Result<TemplateResult> result = templatePort.render("email/welcome", context);

  if (result.isOk()) {
    String html = result.getValue().getContent();
    emailService.send(user.getEmail(), "Welcome!", html);
  } else {
    logger.error("Failed to render welcome email: {}", result.getError());
  }
}
```

### String Template Rendering

```java
public String generateAdhocReport(Map<String, Object> data) {
  String template = """
      <html>
        <body>
          <h1>Report: [[${title}]]</h1>
          <p>Generated on: [[${date}]]</p>
          <ul>
            <li th:each="item : ${items}">[[${item}]]</li>
          </ul>
        </body>
      </html>
      """;

  TemplateContext context = TemplateContext.builder()
      .variables(data)
      .build();

  Result<TemplateResult> result = templatePort.renderString(template, context);

  return result.isOk() ? result.getValue().getContent() : "Error rendering report";
}
```

### Invoice Generation

```java
public byte[] generateInvoicePdf(Invoice invoice) {
  TemplateContext context = TemplateContext.builder()
      .variable("invoice", invoice)
      .variable("customer", invoice.getCustomer())
      .variable("items", invoice.getItems())
      .variable("totalAmount", invoice.getTotalAmount())
      .variable("dueDate", invoice.getDueDate())
      .locale(invoice.getCustomer().getLocale())
      .build();

  Result<TemplateResult> result = templatePort.render("invoices/standard", context);

  if (result.isOk()) {
    String html = result.getValue().getContent();
    return pdfConverter.convert(html);
  }

  throw new TemplateRenderingException(result.getError());
}
```

### Template Existence Check

```java
public boolean hasLocalizedTemplate(String baseName, Locale locale) {
  String templateName = String.format("%s_%s", baseName, locale.getLanguage());

  if (templatePort.exists(templateName)) {
    return true;
  }

  // Fallback to base template
  return templatePort.exists(baseName);
}
```

### Cache Management

```java
@Scheduled(cron = "0 0 * * * *") // Every hour
public void refreshTemplateCache() {
  logger.info("Clearing template cache...");
  templatePort.clearCache();
}

public void refreshSpecificTemplate(String templateName) {
  templatePort.clearCache(templateName);
}
```

## Common Use Cases

### Email Templates

```java
// Welcome email
context.variable("userName", user.getName());
context.variable("verificationLink", generateLink());
templatePort.render("email/welcome", context);

// Password reset
context.variable("resetToken", token);
context.variable("expiresIn", "24 hours");
templatePort.render("email/password-reset", context);

// Order confirmation
context.variable("orderNumber", order.getId());
context.variable("items", order.getItems());
context.variable("totalAmount", order.getTotal());
templatePort.render("email/order-confirmation", context);
```

### Reports

```java
// Monthly sales report
context.variable("month", "January 2026");
context.variable("salesData", salesList);
context.variable("totalRevenue", calculateRevenue());
context.variable("topProducts", getTopProducts());
templatePort.render("reports/monthly-sales", context);

// User activity report
context.variable("user", user);
context.variable("activities", activityLog);
context.variable("period", "Last 30 days");
templatePort.render("reports/user-activity", context);
```

### HTML Pages

```java
// Product catalog page
context.variable("products", productList);
context.variable("categories", categories);
context.variable("filters", activeFilters);
templatePort.render("pages/catalog", context);

// User dashboard
context.variable("user", currentUser);
context.variable("notifications", getNotifications());
context.variable("statistics", getUserStats());
templatePort.render("pages/dashboard", context);
```

## Implementations

### Thymeleaf

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-template-thymeleaf</artifactId>
</dependency>
```

See [commons-adapters-template-thymeleaf](../commons-adapters-template-thymeleaf) for Thymeleaf-specific features.

## Error Handling

All methods return `Result<T>` for consistent error handling:

```java
Result<TemplateResult> result = templatePort.render("email/welcome", context);

if (result.isOk()) {
  // Success path
  TemplateResult templateResult = result.getValue();
  processContent(templateResult.getContent());
} else {
  // Error path
  Problem error = result.getError();
  logger.error("Template rendering failed: {} - {}",
      error.errorCode(), error.message());

  // Handle specific errors
  if (error.errorCode().code().equals("TEMPLATE_NOT_FOUND")) {
    useDefaultTemplate();
  }
}
```

## Best Practices

1. **Use Builder Pattern**: Always use `TemplateContext.builder()` for readability
2. **Set Locale**: Always set locale for proper i18n and formatting
3. **Null Safety**: All context variables are nullable, handle them in templates
4. **Cache Management**: Clear cache after template modifications
5. **Error Handling**: Always handle Result errors, don't assume success
6. **Template Naming**: Use hierarchical names (e.g., "email/welcome", "reports/monthly")
7. **Content Type**: Use factory methods (`html()`, `text()`, `xml()`) when possible

## Thread Safety

- `TemplateContext` and `TemplateResult` are immutable and thread-safe
- `TemplatePort` implementations should be thread-safe
- Context builders are NOT thread-safe (create new builder per thread)

## Dependencies

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-kernel-result</artifactId>
</dependency>
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-kernel-errors</artifactId>
</dependency>
```
