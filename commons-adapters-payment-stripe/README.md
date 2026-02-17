# Commons Adapters - Payment Stripe

Stripe payment processing adapter for the Commons Platform.

## Features

- **Payment Processing**: Create, retrieve, confirm, and cancel payments
- **Subscription Management**: Create and manage recurring subscriptions
- **Refund Operations**: Full and partial refunds for completed payments
- **Result Pattern**: Consistent error handling with Result<T>
- **Stripe API Integration**: Built on official Stripe Java SDK 26.13.0

## Installation

### Maven

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-payment-stripe</artifactId>
</dependency>
```

### Gradle

```gradle
implementation 'com.marcusprado02.commons:commons-adapters-payment-stripe'
```

## Quick Start

### 1. Payment Processing

```java
// Initialize Stripe service with your API key
var paymentService = StripePaymentService.create("sk_test_...");

// Create a payment intent
var result = paymentService.createPayment(
    BigDecimal.valueOf(1000), // $10.00 in cents
    "usd",
    "cus_customer123",
    "pm_payment_method",
    "Order payment",
    Map.of("order_id", "ord_456")
);

if (result.isOk()) {
    var payment = result.getOrNull();
    System.out.println("Payment created: " + payment.id());
    System.out.println("Status: " + payment.status());
}
```

### 2. Subscription Management

```java
var subscriptionService = StripeSubscriptionService.create("sk_test_...");

// Create recurring subscription
var result = subscriptionService.createSubscription(
    "cus_customer123",
    "price_monthly",
    "pm_payment_method",
    Map.of("plan", "premium")
);

if (result.isOk()) {
    var subscription = result.getOrNull();
    System.out.println("Subscription: " + subscription.id());
    System.out.println("Status: " + subscription.status());
}
```

### 3. Refund Processing

```java
var refundService = StripeRefundService.create("sk_test_...");

// Create full refund
var result = refundService.createRefund(
    "pi_payment123",
    null, // null for full refund
    "requested_by_customer",
    Map.of()
);

// Or partial refund
var partialResult = refundService.createRefund(
    "pi_payment123",
    BigDecimal.valueOf(500), // Refund $5.00
    "duplicate",
    Map.of()
);
```

## Payment Statuses

| Status | Description |
|--------|-------------|
| `PENDING` | Payment created but not processed |
| `PROCESSING` | Payment being processed |
| `REQUIRES_ACTION` | Requires customer action (e.g., 3D Secure) |
| `SUCCEEDED` | Payment completed successfully |
| `FAILED` | Payment failed |
| `CANCELED` | Payment canceled |
| `REFUNDED` | Payment refunded |

## Payment Operations

### Create Payment

```java
Result<Payment> createPayment(
    BigDecimal amount,
    String currency,
    String customerId,
    String paymentMethodId,
    String description,
    Map<String, String> metadata
);
```

### Get Payment

```java
Result<Payment> getPayment(String paymentId);
```

### List Payments

```java
Result<List<Payment>> listPayments(String customerId, int limit);
```

### Confirm Payment

```java
Result<Payment> confirmPayment(String paymentId);
```

### Cancel Payment

```java
Result<Payment> cancelPayment(String paymentId);
```

## Subscription Operations

### Create Subscription

```java
Result<Subscription> createSubscription(
    String customerId,
    String priceId,
    String paymentMethodId,
    Map<String, String> metadata
);
```

### Get Subscription

```java
Result<Subscription> getSubscription(String subscriptionId);
```

### List Subscriptions

```java
Result<List<Subscription>> listSubscriptions(String customerId);
```

### Cancel Subscription

```java
Result<Subscription> cancelSubscription(
    String subscriptionId,
    boolean immediately
);
```

## Refund Operations

### Create Refund

```java
Result<Refund> createRefund(
    String paymentId,
    BigDecimal amount, // null for full refund
    String reason,
    Map<String, String> metadata
);
```

### Get Refund

```java
Result<Refund> getRefund(String refundId);
```

### List Refunds

```java
Result<List<Refund>> listRefunds(String paymentId);
```

## Error Handling

All methods return `Result<T>` for consistent error handling:

```java
var result = paymentService.createPayment(...);

if (result.isFail()) {
    var problem = result.problemOrNull();
    System.err.println("Error: " + problem.message());
    System.err.println("Code: " + problem.code().value());

    switch (problem.category()) {
        case NOT_FOUND -> handleNotFound();
        case BUSINESS -> handleBusinessError();
        case TECHNICAL -> handleTechnicalError();
    }
}
```

### Error Codes

| Error Code | Description |
|------------|-------------|
| `PAYMENT.CREATE_FAILED` | Failed to create payment |
| `PAYMENT.NOT_FOUND` | Payment not found |
| `PAYMENT.CANCEL_FAILED` | Failed to cancel payment |
| `PAYMENT.CONFIRM_FAILED` | Failed to confirm payment |
| `SUBSCRIPTION.CREATE_FAILED` | Failed to create subscription |
| `SUBSCRIPTION.NOT_FOUND` | Subscription not found |
| `SUBSCRIPTION.CANCEL_FAILED` | Failed to cancel subscription |
| `REFUND.CREATE_FAILED` | Failed to create refund |
| `REFUND.NOT_FOUND` | Refund not found |

## Spring Boot Integration

```java
@Configuration
public class PaymentConfiguration {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Bean
    public PaymentService paymentService() {
        return StripePaymentService.create(stripeApiKey);
    }

    @Bean
    public SubscriptionService subscriptionService() {
        return StripeSubscriptionService.create(stripeApiKey);
    }

    @Bean
    public RefundService refundService() {
        return StripeRefundService.create(stripeApiKey);
    }
}
```

## Testing

For testing, use Stripe's test mode with test API keys:
- Test keys start with `sk_test_`
- Use test card numbers: `4242 4242 4242 4242`
- Stripe provides a comprehensive testing guide

```java
// Test mode
var service = StripePaymentService.create("sk_test_...");

// Test payment with test card
var result = service.createPayment(
    BigDecimal.valueOf(1000),
    "usd",
    "cus_test",
    "pm_card_visa",
    "Test payment",
    Map.of("test", "true")
);
```

## Dependencies

- **Stripe Java SDK**: 26.13.0
- **commons-ports-payment**: Port interfaces
- **commons-kernel-result**: Result<T> pattern
- **commons-kernel-errors**: Error handling

## Limitations

- ⏳ Webhook support (planned)
- ⏳ PaymentMethod CRUD operations (partially implemented)
- ⏳ Advanced subscription features (trials, metered billing)
- ⏳ Dispute management (planned)
- ⏳ Customer management (planned)

## Future Enhancements

- Webhook event handling
- Customer portal support
- Checkout session creation
- Payment links
- Terminal payments
- Connect (marketplace) support
- Invoice management
- Tax calculation
- Fraud detection integration

## Resources

- [Stripe API Documentation](https://stripe.com/docs/api)
- [Stripe Java SDK](https://github.com/stripe/stripe-java)
- [Stripe Testing Guide](https://stripe.com/docs/testing)
- [Stripe Test Cards](https://stripe.com/docs/testing#cards)

## License

This project is licensed under the MIT License - see [LICENSE](../LICENSE) for details.
