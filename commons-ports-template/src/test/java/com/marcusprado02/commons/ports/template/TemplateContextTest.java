package com.marcusprado02.commons.ports.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateContextTest {

  @Test
  void templateContext_builder_with_variables_and_locale() {
    TemplateContext ctx = TemplateContext.builder()
        .variable("user", "Alice")
        .variable("count", 42)
        .locale(Locale.US)
        .build();

    assertEquals("Alice", ctx.getVariable("user"));
    assertEquals(42, ctx.getVariable("count"));
    assertEquals(Locale.US, ctx.locale());
    assertTrue(ctx.hasVariable("user"));
    assertFalse(ctx.hasVariable("missing"));
    assertEquals(2, ctx.getVariableNames().size());
  }

  @Test
  void templateContext_empty_returns_empty_context() {
    TemplateContext ctx = TemplateContext.empty();
    assertTrue(ctx.variables().isEmpty());
    assertNotNull(ctx.locale());
  }

  @Test
  void templateContext_of_single_variable() {
    TemplateContext ctx = TemplateContext.of("key", "value");
    assertEquals("value", ctx.getVariable("key"));
  }

  @Test
  void templateContext_null_locale_defaults_to_system_default() {
    TemplateContext ctx = new TemplateContext(Map.of(), null);
    assertNotNull(ctx.locale());
    assertEquals(Locale.getDefault(), ctx.locale());
  }

  @Test
  void templateContext_builder_variables_map() {
    TemplateContext ctx = TemplateContext.builder()
        .variables(Map.of("a", "1", "b", "2"))
        .build();
    assertEquals(2, ctx.variables().size());
  }

  @Test
  void templateContext_builder_null_variables_map_throws() {
    assertThrows(NullPointerException.class,
        () -> TemplateContext.builder().variables(null));
  }

  @Test
  void templateContext_builder_null_variable_name_throws() {
    assertThrows(NullPointerException.class,
        () -> TemplateContext.builder().variable(null, "v"));
  }
}
