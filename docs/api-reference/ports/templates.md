# Port: Templates

## Visão Geral

`commons-ports-templates` define contratos para renderização de templates (HTML, email, PDF), abstraindo engines como Thymeleaf e Freemarker.

**Quando usar:**
- Emails HTML
- PDFs dinâmicos
- Páginas HTML server-side
- Relatórios
- Documentos com templates

**Implementações disponíveis:**
- `commons-adapters-template-thymeleaf` - Thymeleaf

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-templates</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-template-thymeleaf</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 📝 TemplateEngine Interface

### Core Methods

```java
public interface TemplateEngine {
    
    /**
     * Renderiza template com variáveis.
     */
    Result<String> render(
        String templateName,
        Map<String, Object> variables
    );
    
    /**
     * Renderiza template com locale.
     */
    Result<String> render(
        String templateName,
        Map<String, Object> variables,
        Locale locale
    );
    
    /**
     * Verifica se template existe.
     */
    boolean exists(String templateName);
}
```

### Template Context

```java
public record TemplateContext(
    Map<String, Object> variables,
    Locale locale
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder variable(String key, Object value);
        public Builder variables(Map<String, Object> variables);
        public Builder locale(Locale locale);
        public TemplateContext build();
    }
    
    public static TemplateContext of(Map<String, Object> variables) {
        return new TemplateContext(variables, Locale.getDefault());
    }
}
```

---

## 📧 Email Templates

### Welcome Email Template

```html
<!-- templates/emails/welcome.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Welcome!</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #4CAF50;
            color: white;
            padding: 20px;
            text-align: center;
        }
        .content {
            padding: 20px;
        }
        .button {
            display: inline-block;
            background-color: #4CAF50;
            color: white;
            padding: 12px 30px;
            text-decoration: none;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Welcome to <span th:text="${companyName}">Our Company</span>!</h1>
        </div>
        
        <div class="content">
            <p>Hi <span th:text="${userName}">User</span>,</p>
            
            <p>Thank you for joining us! We're excited to have you onboard.</p>
            
            <p>To get started, please verify your email address by clicking the button below:</p>
            
            <p style="text-align: center; margin: 30px 0;">
                <a th:href="${activationLink}" class="button">Verify Email</a>
            </p>
            
            <p>If you didn't create this account, please ignore this email.</p>
            
            <p>
                Best regards,<br/>
                The <span th:text="${companyName}">Company</span> Team
            </p>
        </div>
    </div>
</body>
</html>
```

### Email Service

```java
@Service
public class WelcomeEmailService {
    
    private final TemplateEngine templateEngine;
    private final EmailSender emailSender;
    
    public Result<Void> sendWelcomeEmail(User user) {
        // Render template
        Map<String, Object> variables = Map.of(
            "userName", user.name(),
            "companyName", "My Company",
            "activationLink", buildActivationLink(user)
        );
        
        Result<String> htmlResult = templateEngine.render(
            "emails/welcome",
            variables
        );
        
        if (htmlResult.isError()) {
            return htmlResult.mapToVoid();
        }
        
        // Send email
        Email email = Email.builder()
            .from("welcome@mycompany.com", "My Company")
            .to(user.email(), user.name())
            .subject("Welcome to My Company!")
            .html(htmlResult.get())
            .text(extractTextFromHtml(htmlResult.get()))
            .build();
        
        return emailSender.send(email).mapToVoid();
    }
    
    private String buildActivationLink(User user) {
        String token = user.activationToken();
        return "https://mycompany.com/activate?token=" + token;
    }
}
```

---

## 📋 Order Confirmation Template

### Template

```html
<!-- templates/emails/order-confirmation.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Order Confirmation</title>
</head>
<body>
    <div class="container">
        <h1>Order Confirmation</h1>
        
        <p>Hi <span th:text="${customerName}">Customer</span>,</p>
        
        <p>Thank you for your order! Your order has been confirmed.</p>
        
        <h2>Order Details</h2>
        <table>
            <tr>
                <td><strong>Order ID:</strong></td>
                <td th:text="${orderId}">12345</td>
            </tr>
            <tr>
                <td><strong>Date:</strong></td>
                <td th:text="${#temporals.format(orderDate, 'MMM dd, yyyy')}">Jan 01, 2024</td>
            </tr>
        </table>
        
        <h2>Items</h2>
        <table border="1" style="border-collapse: collapse; width: 100%;">
            <thead>
                <tr>
                    <th>Product</th>
                    <th>Quantity</th>
                    <th>Price</th>
                    <th>Subtotal</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="item : ${items}">
                    <td th:text="${item.productName}">Product</td>
                    <td th:text="${item.quantity}">1</td>
                    <td th:text="${#numbers.formatCurrency(item.price)}">$10.00</td>
                    <td th:text="${#numbers.formatCurrency(item.subtotal)}">$10.00</td>
                </tr>
            </tbody>
            <tfoot>
                <tr>
                    <td colspan="3"><strong>Total:</strong></td>
                    <td><strong th:text="${#numbers.formatCurrency(total)}">$100.00</strong></td>
                </tr>
            </tfoot>
        </table>
        
        <p>
            <a th:href="${trackingLink}">Track your order</a>
        </p>
    </div>
</body>
</html>
```

### Service

```java
@Service
public class OrderEmailService {
    
    private final TemplateEngine templateEngine;
    private final EmailSender emailSender;
    
    public Result<Void> sendOrderConfirmation(Order order, User user) {
        // Prepare variables
        Map<String, Object> variables = Map.of(
            "customerName", user.name(),
            "orderId", order.id().value(),
            "orderDate", order.createdAt(),
            "items", order.items().stream()
                .map(this::mapOrderItem)
                .toList(),
            "total", order.total().amount(),
            "trackingLink", buildTrackingLink(order)
        );
        
        // Render template
        Result<String> htmlResult = templateEngine.render(
            "emails/order-confirmation",
            variables
        );
        
        if (htmlResult.isError()) {
            return htmlResult.mapToVoid();
        }
        
        // Send email
        Email email = Email.builder()
            .from("orders@mycompany.com", "My Company Orders")
            .to(user.email(), user.name())
            .subject("Order Confirmation #" + order.id().value())
            .html(htmlResult.get())
            .build();
        
        return emailSender.send(email).mapToVoid();
    }
    
    private Map<String, Object> mapOrderItem(OrderItem item) {
        return Map.of(
            "productName", item.productName(),
            "quantity", item.quantity(),
            "price", item.price().amount(),
            "subtotal", item.subtotal().amount()
        );
    }
}
```

---

## 🌍 Internationalization

### Localized Templates

```html
<!-- templates/emails/password-reset_en.html -->
<p>Hi <span th:text="${userName}">User</span>,</p>
<p>You requested a password reset. Click the button below to reset your password:</p>
<a th:href="${resetLink}">Reset Password</a>

<!-- templates/emails/password-reset_pt.html -->
<p>Olá <span th:text="${userName}">Usuário</span>,</p>
<p>Você solicitou a redefinição de senha. Clique no botão abaixo para redefinir:</p>
<a th:href="${resetLink}">Redefinir Senha</a>
```

### Localized Service

```java
@Service
public class PasswordResetEmailService {
    
    private final TemplateEngine templateEngine;
    private final EmailSender emailSender;
    
    public Result<Void> sendPasswordReset(User user, Locale locale) {
        // Render with locale
        Map<String, Object> variables = Map.of(
            "userName", user.name(),
            "resetLink", buildResetLink(user)
        );
        
        Result<String> htmlResult = templateEngine.render(
            "emails/password-reset",
            variables,
            locale
        );
        
        if (htmlResult.isError()) {
            return htmlResult.mapToVoid();
        }
        
        // Subject depends on locale
        String subject = locale.getLanguage().equals("pt")
            ? "Redefinição de Senha"
            : "Password Reset";
        
        Email email = Email.builder()
            .from("security@mycompany.com")
            .to(user.email(), user.name())
            .subject(subject)
            .html(htmlResult.get())
            .build();
        
        return emailSender.send(email).mapToVoid();
    }
}
```

---

## 📊 Report Templates

### Monthly Report Template

```html
<!-- templates/reports/monthly-report.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Monthly Report</title>
    <style>
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        .summary { background-color: #f2f2f2; font-weight: bold; }
    </style>
</head>
<body>
    <h1 th:text="${'Monthly Report - ' + #temporals.format(month, 'MMMM yyyy')}">Monthly Report</h1>
    
    <h2>Summary</h2>
    <table>
        <tr>
            <td>Total Orders:</td>
            <td th:text="${summary.totalOrders}">100</td>
        </tr>
        <tr>
            <td>Total Revenue:</td>
            <td th:text="${#numbers.formatCurrency(summary.totalRevenue)}">$10,000</td>
        </tr>
        <tr>
            <td>Average Order Value:</td>
            <td th:text="${#numbers.formatCurrency(summary.averageOrderValue)}">$100</td>
        </tr>
    </table>
    
    <h2>Top Products</h2>
    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th>Units Sold</th>
                <th>Revenue</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="product : ${topProducts}">
                <td th:text="${product.name}">Product Name</td>
                <td th:text="${product.unitsSold}">10</td>
                <td th:text="${#numbers.formatCurrency(product.revenue)}">$1,000</td>
            </tr>
        </tbody>
    </table>
    
    <h2>Sales by Category</h2>
    <table>
        <thead>
            <tr>
                <th>Category</th>
                <th>Orders</th>
                <th>Revenue</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="category : ${categories}">
                <td th:text="${category.name}">Category</td>
                <td th:text="${category.orders}">10</td>
                <td th:text="${#numbers.formatCurrency(category.revenue)}">$1,000</td>
            </tr>
        </tbody>
    </table>
</body>
</html>
```

### Report Service

```java
@Service
public class MonthlyReportService {
    
    private final TemplateEngine templateEngine;
    private final DocumentGenerator pdfGenerator;
    private final FileStorage fileStorage;
    
    public Result<FileLocation> generateMonthlyReport(YearMonth month) {
        // Gather data
        ReportData data = gatherReportData(month);
        
        // Render HTML
        Map<String, Object> variables = Map.of(
            "month", month.atDay(1),
            "summary", data.summary(),
            "topProducts", data.topProducts(),
            "categories", data.categories()
        );
        
        Result<String> htmlResult = templateEngine.render(
            "reports/monthly-report",
            variables
        );
        
        if (htmlResult.isError()) {
            return htmlResult.mapError();
        }
        
        // Convert HTML to PDF
        Result<byte[]> pdfResult = pdfGenerator.fromHtml(htmlResult.get());
        
        if (pdfResult.isError()) {
            return pdfResult.mapError();
        }
        
        // Store PDF
        String key = String.format(
            "reports/monthly/%s/%s-report.pdf",
            month.getYear(),
            month.getMonth().toString().toLowerCase()
        );
        
        return fileStorage.store(key, pdfResult.get());
    }
}
```

---

## 💼 Invoice Template

### Template

```html
<!-- templates/invoices/invoice.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Invoice</title>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>INVOICE</h1>
            <p>Invoice #<span th:text="${invoiceNumber}">INV-001</span></p>
            <p>Date: <span th:text="${#temporals.format(invoiceDate, 'MMM dd, yyyy')}">Jan 01, 2024</span></p>
        </div>
        
        <div class="parties">
            <div class="from">
                <h3>From:</h3>
                <p th:text="${company.name}">Company Name</p>
                <p th:text="${company.address}">Address</p>
                <p th:text="${company.taxId}">Tax ID</p>
            </div>
            
            <div class="to">
                <h3>Bill To:</h3>
                <p th:text="${customer.name}">Customer Name</p>
                <p th:text="${customer.address}">Address</p>
                <p th:if="${customer.taxId != null}" th:text="${customer.taxId}">Tax ID</p>
            </div>
        </div>
        
        <table>
            <thead>
                <tr>
                    <th>Description</th>
                    <th>Quantity</th>
                    <th>Unit Price</th>
                    <th>Amount</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="item : ${items}">
                    <td th:text="${item.description}">Item</td>
                    <td th:text="${item.quantity}">1</td>
                    <td th:text="${#numbers.formatCurrency(item.unitPrice)}">$10.00</td>
                    <td th:text="${#numbers.formatCurrency(item.amount)}">$10.00</td>
                </tr>
            </tbody>
            <tfoot>
                <tr>
                    <td colspan="3">Subtotal:</td>
                    <td th:text="${#numbers.formatCurrency(subtotal)}">$100.00</td>
                </tr>
                <tr>
                    <td colspan="3">Tax (<span th:text="${taxRate}">10</span>%):</td>
                    <td th:text="${#numbers.formatCurrency(tax)}">$10.00</td>
                </tr>
                <tr class="total">
                    <td colspan="3"><strong>Total:</strong></td>
                    <td><strong th:text="${#numbers.formatCurrency(total)}">$110.00</strong></td>
                </tr>
            </tfoot>
        </table>
        
        <div class="footer">
            <p>Payment is due within 30 days.</p>
            <p th:if="${paymentInstructions != null}" th:text="${paymentInstructions}">Payment instructions</p>
        </div>
    </div>
</body>
</html>
```

---

## 🧪 Testing

### Mock Template Engine

```java
public class MockTemplateEngine implements TemplateEngine {
    
    private final Map<String, String> templates = new HashMap<>();
    
    @Override
    public Result<String> render(
        String templateName,
        Map<String, Object> variables
    ) {
        String template = templates.get(templateName);
        
        if (template == null) {
            return Result.error(Error.of(
                "TEMPLATE_NOT_FOUND",
                "Template not found: " + templateName
            ));
        }
        
        // Simple variable replacement
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            rendered = rendered.replace(placeholder, String.valueOf(entry.getValue()));
        }
        
        return Result.ok(rendered);
    }
    
    @Override
    public boolean exists(String templateName) {
        return templates.containsKey(templateName);
    }
    
    public void mockTemplate(String name, String content) {
        templates.put(name, content);
    }
}
```

### Test Example

```java
class WelcomeEmailServiceTest {
    
    private MockTemplateEngine templateEngine;
    private MockEmailSender emailSender;
    private WelcomeEmailService welcomeService;
    
    @BeforeEach
    void setUp() {
        templateEngine = new MockTemplateEngine();
        emailSender = new MockEmailSender();
        welcomeService = new WelcomeEmailService(templateEngine, emailSender);
        
        // Mock template
        templateEngine.mockTemplate(
            "emails/welcome",
            "<h1>Welcome ${userName}!</h1><a href=\"${activationLink}\">Activate</a>"
        );
    }
    
    @Test
    void shouldSendWelcomeEmail() {
        // Given
        User user = User.create("john@example.com", "John Doe");
        
        // When
        Result<Void> result = welcomeService.sendWelcomeEmail(user);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        List<Email> sent = emailSender.getSentEmails();
        assertThat(sent).hasSize(1);
        
        Email email = sent.get(0);
        assertThat(email.htmlBody()).contains("Welcome John Doe!");
        assertThat(email.htmlBody()).contains("Activate");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use template inheritance/layouts
templateEngine.render("layouts/main", variables);

// ✅ Escape HTML por segurança
<span th:text="${userName}">  // Auto-escaped

// ✅ Cache templates em production
@Configuration
public class ThymeleafConfig {
    @Bean
    public ThymeleafTemplateEngine engine() {
        return ThymeleafTemplateEngine.builder()
            .cacheable(true)
            .build();
    }
}

// ✅ Organize templates por feature
templates/
  emails/
  reports/
  invoices/

// ✅ Use locale para i18n
templateEngine.render(name, variables, locale);
```

### ❌ DON'T

```java
// ❌ NÃO use HTML sem escape
<span th:utext="${userInput}">  // ❌ XSS vulnerability!

// ❌ NÃO coloque lógica complexa no template
<span th:text="${user.orders.stream().filter(...).count()}">  // ❌ No!

// ❌ NÃO misture linguagens
// Use Thymeleaf OU Freemarker, não ambos

// ❌ NÃO hardcode strings
<p>Welcome!</p>  // ❌ Use i18n: th:text="#{welcome}"

// ❌ NÃO renderize templates grandes síncronamente
CompletableFuture.supplyAsync(() -> 
    templateEngine.render(template, variables)
);
```

---

## Ver Também

- [Thymeleaf Adapter](../../../commons-adapters-template-thymeleaf/) - Implementation
- [Communication](./communication.md) - Email sending
- [Documents](./documents.md) - PDF generation
- [I18n](../app-i18n.md) - Internationalization
