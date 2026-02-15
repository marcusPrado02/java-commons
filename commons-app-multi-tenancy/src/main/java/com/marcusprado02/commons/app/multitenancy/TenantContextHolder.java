package com.marcusprado02.commons.app.multitenancy;

/**
 * Thread-local tenant context holder.
 *
 * <p>Manages tenant context propagation within a single thread using ThreadLocal storage.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Set tenant context
 * TenantContext context = TenantContext.of("tenant123");
 * TenantContextHolder.setContext(context);
 *
 * try {
 *     // Use within operation
 *     String tenantId = TenantContextHolder.getCurrentTenantId();
 *     // ... business logic
 * } finally {
 *     // Always clean up
 *     TenantContextHolder.clear();
 * }
 * }</pre>
 */
public final class TenantContextHolder {

  private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();

  private TenantContextHolder() {
    // Utility class
  }

  /**
   * Sets the tenant context for the current thread.
   *
   * @param context tenant context
   */
  public static void setContext(TenantContext context) {
    contextHolder.set(context);
  }

  /**
   * Gets the tenant context for the current thread.
   *
   * @return tenant context or null if not set
   */
  public static TenantContext getContext() {
    return contextHolder.get();
  }

  /**
   * Gets the current tenant ID.
   *
   * @return tenant ID or null if no context is set
   */
  public static String getCurrentTenantId() {
    TenantContext context = getContext();
    return context != null ? context.tenantId() : null;
  }

  /**
   * Checks if a tenant context is currently set.
   *
   * @return true if context is set
   */
  public static boolean hasContext() {
    return getContext() != null;
  }

  /**
   * Clears the tenant context for the current thread.
   */
  public static void clear() {
    contextHolder.remove();
  }

  /**
   * Executes a runnable with the specified tenant context.
   *
   * @param context tenant context
   * @param runnable runnable to execute
   */
  public static void runWithContext(TenantContext context, Runnable runnable) {
    TenantContext previousContext = getContext();
    try {
      setContext(context);
      runnable.run();
    } finally {
      if (previousContext != null) {
        setContext(previousContext);
      } else {
        clear();
      }
    }
  }

  /**
   * Executes a supplier with the specified tenant context.
   *
   * @param context tenant context
   * @param supplier supplier to execute
   * @param <T> return type
   * @return supplier result
   */
  public static <T> T supplyWithContext(TenantContext context, java.util.function.Supplier<T> supplier) {
    TenantContext previousContext = getContext();
    try {
      setContext(context);
      return supplier.get();
    } finally {
      if (previousContext != null) {
        setContext(previousContext);
      } else {
        clear();
      }
    }
  }
}
