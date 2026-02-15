package com.marcusprado02.commons.app.multitenancy;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TenantContextTest {

  @Test
  void shouldCreateSimpleTenantContext() {
    TenantContext context = TenantContext.of("tenant123");

    assertThat(context.tenantId()).isEqualTo("tenant123");
    assertThat(context.getName()).isEmpty();
    assertThat(context.getDomain()).isEmpty();
    assertThat(context.attributes()).isEmpty();
  }

  @Test
  void shouldCreateTenantContextWithName() {
    TenantContext context = TenantContext.of("tenant123", "Acme Corp");

    assertThat(context.tenantId()).isEqualTo("tenant123");
    assertThat(context.getName()).hasValue("Acme Corp");
    assertThat(context.getDomain()).isEmpty();
  }

  @Test
  void shouldBuildComplexTenantContext() {
    TenantContext context =
        TenantContext.builder()
            .tenantId("tenant123")
            .name("Acme Corp")
            .domain("acme.example.com")
            .attribute("plan", "premium")
            .attribute("region", "us-east-1")
            .attribute("active", true)
            .build();

    assertThat(context.tenantId()).isEqualTo("tenant123");
    assertThat(context.getName()).hasValue("Acme Corp");
    assertThat(context.getDomain()).hasValue("acme.example.com");
    assertThat(context.getAttribute("plan")).hasValue("premium");
    assertThat(context.getAttribute("region")).hasValue("us-east-1");
    assertThat(context.getAttribute("active")).hasValue(true);
    assertThat(context.getAttribute("missing")).isEmpty();
  }

  @Test
  void shouldGetTypedAttributes() {
    TenantContext context =
        TenantContext.builder()
            .tenantId("tenant123")
            .attribute("plan", "premium")
            .attribute("maxUsers", 100)
            .attribute("active", true)
            .build();

    assertThat(context.getAttribute("plan", String.class)).hasValue("premium");
    assertThat(context.getAttribute("maxUsers", Integer.class)).hasValue(100);
    assertThat(context.getAttribute("active", Boolean.class)).hasValue(true);
    assertThat(context.getAttribute("plan", Integer.class)).isEmpty();
  }

  @Test
  void shouldCreateImmutableAttributes() {
    TenantContext context =
        TenantContext.builder().tenantId("tenant123").attribute("key", "value").build();

    assertThatThrownBy(() -> context.attributes().put("new", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldRequireTenantId() {
    assertThatThrownBy(() -> TenantContext.builder().build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tenantId");
  }
}
