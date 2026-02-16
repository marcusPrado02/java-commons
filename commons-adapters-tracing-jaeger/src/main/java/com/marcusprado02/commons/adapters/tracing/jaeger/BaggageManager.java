package com.marcusprado02.commons.adapters.tracing.jaeger;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for managing OpenTelemetry baggage.
 *
 * <p>Baggage is contextual information that is carried across service boundaries. It's propagated
 * in-band along with trace context. Common use cases include:
 *
 * <ul>
 *   <li>User ID for multi-tenant applications
 *   <li>Feature flags
 *   <li>Request IDs
 *   <li>Correlation IDs
 *   <li>Authorization tokens
 * </ul>
 *
 * <p><b>Warning:</b> Baggage is propagated in HTTP headers, so avoid storing large amounts of data
 * or sensitive information. Keep total baggage size under 4KB to avoid HTTP header limits.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Set baggage
 * BaggageManager.set("user.id", "12345");
 * BaggageManager.set("tenant.id", "company-abc");
 *
 * // Get baggage in downstream service
 * Optional<String> userId = BaggageManager.get("user.id");
 *
 * // Execute code with baggage
 * BaggageManager.withBaggage(
 *     Map.of("correlation.id", "abc-123"),
 *     () -> {
 *       // Baggage is available in this scope
 *     });
 * }</pre>
 */
public final class BaggageManager {

  private BaggageManager() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Sets a baggage value in the current context.
   *
   * <p>The baggage will be propagated to downstream services.
   *
   * @param key the baggage key
   * @param value the baggage value
   * @return a new scope with the baggage set (must be closed)
   */
  public static Scope set(String key, String value) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");

    Baggage currentBaggage = Baggage.current();
    Baggage newBaggage = currentBaggage.toBuilder().put(key, value).build();

    return newBaggage.makeCurrent();
  }

  /**
   * Sets multiple baggage values in the current context.
   *
   * @param entries map of baggage key-value pairs
   * @return a new scope with the baggage set (must be closed)
   */
  public static Scope setAll(Map<String, String> entries) {
    Objects.requireNonNull(entries, "entries must not be null");

    Baggage currentBaggage = Baggage.current();
    BaggageBuilder builder = currentBaggage.toBuilder();

    entries.forEach(builder::put);

    Baggage newBaggage = builder.build();
    return newBaggage.makeCurrent();
  }

  /**
   * Gets a baggage value from the current context.
   *
   * @param key the baggage key
   * @return the baggage value, or empty if not found
   */
  public static Optional<String> get(String key) {
    Objects.requireNonNull(key, "key must not be null");

    String value = Baggage.current().getEntryValue(key);
    return Optional.ofNullable(value);
  }

  /**
   * Gets all baggage from the current context.
   *
   * @return map of all baggage key-value pairs
   */
  public static Map<String, String> getAll() {
    Map<String, String> result = new HashMap<>();
    Baggage.current().forEach((key, entry) -> result.put(key, entry.getValue()));
    return result;
  }

  /**
   * Removes a baggage value from the current context.
   *
   * @param key the baggage key to remove
   * @return a new scope with the baggage removed (must be closed)
   */
  public static Scope remove(String key) {
    Objects.requireNonNull(key, "key must not be null");

    Baggage currentBaggage = Baggage.current();
    Baggage newBaggage = currentBaggage.toBuilder().remove(key).build();

    return newBaggage.makeCurrent();
  }

  /**
   * Clears all baggage from the current context.
   *
   * @return a new scope with empty baggage (must be closed)
   */
  public static Scope clear() {
    return Baggage.empty().makeCurrent();
  }

  /**
   * Executes a runnable with baggage set for the duration of execution.
   *
   * <p>Example:
   *
   * <pre>{@code
   * BaggageManager.withBaggage(
   *     Map.of("user.id", "12345", "tenant.id", "company-abc"),
   *     () -> {
   *       // Code here has access to baggage
   *       callDownstreamService();
   *     });
   * }</pre>
   *
   * @param baggage map of baggage key-value pairs
   * @param runnable the code to execute
   */
  public static void withBaggage(Map<String, String> baggage, Runnable runnable) {
    Objects.requireNonNull(baggage, "baggage must not be null");
    Objects.requireNonNull(runnable, "runnable must not be null");

    try (Scope ignored = setAll(baggage)) {
      runnable.run();
    }
  }

  /**
   * Executes a supplier with baggage set for the duration of execution.
   *
   * <p>Example:
   *
   * <pre>{@code
   * String result = BaggageManager.withBaggage(
   *     Map.of("user.id", "12345"),
   *     () -> {
   *       return callDownstreamService();
   *     });
   * }</pre>
   *
   * @param baggage map of baggage key-value pairs
   * @param supplier the code to execute
   * @param <T> the return type
   * @return the result of the supplier
   */
  public static <T> T withBaggage(
      Map<String, String> baggage, java.util.function.Supplier<T> supplier) {
    Objects.requireNonNull(baggage, "baggage must not be null");
    Objects.requireNonNull(supplier, "supplier must not be null");

    try (Scope ignored = setAll(baggage)) {
      return supplier.get();
    }
  }

  /**
   * Executes a runnable with a single baggage entry set for the duration of execution.
   *
   * @param key the baggage key
   * @param value the baggage value
   * @param runnable the code to execute
   */
  public static void withBaggage(String key, String value, Runnable runnable) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(runnable, "runnable must not be null");

    try (Scope ignored = set(key, value)) {
      runnable.run();
    }
  }

  /**
   * Executes a supplier with a single baggage entry set for the duration of execution.
   *
   * @param key the baggage key
   * @param value the baggage value
   * @param supplier the code to execute
   * @param <T> the return type
   * @return the result of the supplier
   */
  public static <T> T withBaggage(
      String key, String value, java.util.function.Supplier<T> supplier) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(supplier, "supplier must not be null");

    try (Scope ignored = set(key, value)) {
      return supplier.get();
    }
  }

  /**
   * Gets the current baggage context.
   *
   * @return the current baggage
   */
  public static Baggage current() {
    return Baggage.current();
  }

  /**
   * Creates a new context with the given baggage.
   *
   * @param baggage the baggage to set
   * @return a new context with the baggage
   */
  public static Context withBaggage(Baggage baggage) {
    Objects.requireNonNull(baggage, "baggage must not be null");
    return Context.current().with(baggage);
  }

  /**
   * Returns the number of baggage entries in the current context.
   *
   * @return the size of the current baggage
   */
  public static int size() {
    return getAll().size();
  }

  /**
   * Checks if the current context has any baggage.
   *
   * @return true if baggage is empty, false otherwise
   */
  public static boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Checks if the current context contains the given baggage key.
   *
   * @param key the baggage key
   * @return true if the key exists, false otherwise
   */
  public static boolean contains(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return get(key).isPresent();
  }
}
