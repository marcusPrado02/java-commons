package com.marcusprado02.commons.ports.template;

import java.util.*;

/**
 * Context for template rendering with variables and configuration.
 *
 * <p>Immutable value object containing all data needed for template rendering.
 *
 * <p>Example:
 *
 * <pre>{@code
 * TemplateContext context = TemplateContext.builder()
 *     .variable("user", userDto)
 *     .variable("order", orderDto)
 *     .variable("items", itemsList)
 *     .locale(Locale.US)
 *     .build();
 * }</pre>
 *
 * @param variables template variables (immutable map)
 * @param locale locale for formatting and i18n
 */
public record TemplateContext(Map<String, Object> variables, Locale locale) {

  public TemplateContext {
    variables = Map.copyOf(variables);
    if (locale == null) {
      locale = Locale.getDefault();
    }
  }

  /**
   * Creates a new builder.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets a variable value.
   *
   * @param key variable name
   * @return variable value or null if not present
   */
  public Object getVariable(String key) {
    return variables.get(key);
  }

  /**
   * Checks if a variable exists.
   *
   * @param key variable name
   * @return true if variable exists
   */
  public boolean hasVariable(String key) {
    return variables.containsKey(key);
  }

  /**
   * Gets all variable names.
   *
   * @return set of variable names
   */
  public Set<String> getVariableNames() {
    return variables.keySet();
  }

  /** Builder for TemplateContext. */
  public static class Builder {
    private final Map<String, Object> variables = new HashMap<>();
    private Locale locale = Locale.getDefault();

    /**
     * Adds a variable to the context.
     *
     * @param name variable name
     * @param value variable value
     * @return this builder
     */
    public Builder variable(String name, Object value) {
      Objects.requireNonNull(name, "Variable name cannot be null");
      this.variables.put(name, value);
      return this;
    }

    /**
     * Adds multiple variables.
     *
     * @param variables map of variables
     * @return this builder
     */
    public Builder variables(Map<String, Object> variables) {
      Objects.requireNonNull(variables, "Variables map cannot be null");
      this.variables.putAll(variables);
      return this;
    }

    /**
     * Sets the locale for formatting and i18n.
     *
     * @param locale locale
     * @return this builder
     */
    public Builder locale(Locale locale) {
      this.locale = locale;
      return this;
    }

    /**
     * Builds the context.
     *
     * @return template context
     */
    public TemplateContext build() {
      return new TemplateContext(variables, locale);
    }
  }

  /** Creates an empty context with default locale. */
  public static TemplateContext empty() {
    return new TemplateContext(Map.of(), Locale.getDefault());
  }

  /** Creates a context with a single variable. */
  public static TemplateContext of(String name, Object value) {
    return builder().variable(name, value).build();
  }
}
