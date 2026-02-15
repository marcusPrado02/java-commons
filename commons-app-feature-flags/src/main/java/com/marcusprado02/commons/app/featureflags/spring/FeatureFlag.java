package com.marcusprado02.commons.app.featureflags.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to guard method execution with a feature flag.
 *
 * <p>When applied to a method, the method will only execute if the specified feature flag is
 * enabled. If disabled, the fallback strategy is used.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Service
 * public class PaymentService {
 *
 *     @FeatureFlag(key = "new-payment-flow", fallback = FallbackStrategy.THROW_EXCEPTION)
 *     public PaymentResult processPayment(Payment payment) {
 *         // New payment flow
 *         return newPaymentProcessor.process(payment);
 *     }
 *
 *     @FeatureFlag(key = "discount-feature", fallback = FallbackStrategy.RETURN_NULL)
 *     public Discount calculateDiscount(Order order) {
 *         // Calculate discount
 *         return discountCalculator.calculate(order);
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureFlag {

  /**
   * Feature flag key.
   *
   * @return feature key
   */
  String key();

  /**
   * Fallback strategy when feature is disabled.
   *
   * @return fallback strategy
   */
  FallbackStrategy fallback() default FallbackStrategy.THROW_EXCEPTION;

  /**
   * Name of a bean method to call as fallback (when fallback = CALL_METHOD).
   *
   * @return fallback method name
   */
  String fallbackMethod() default "";

  /**
   * User ID expression (SpEL supported).
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code userId = "#userId"} - from method parameter
   *   <li>{@code userId = "#order.userId"} - from object property
   *   <li>{@code userId = "@securityContext.currentUserId()"} - from bean method
   * </ul>
   *
   * @return user ID expression
   */
  String userId() default "";

  enum FallbackStrategy {
    /** Throw FeatureFlagDisabledException. */
    THROW_EXCEPTION,

    /** Return null. */
    RETURN_NULL,

    /** Return default value for primitive types (0, false, etc.), null for objects. */
    RETURN_DEFAULT,

    /** Call fallback method. */
    CALL_METHOD
  }
}
