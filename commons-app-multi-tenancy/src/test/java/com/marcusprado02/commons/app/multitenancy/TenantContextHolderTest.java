package com.marcusprado02.commons.app.multitenancy;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextHolderTest {

  @AfterEach
  void cleanUp() {
    TenantContextHolder.clear();
  }

  @Test
  void shouldSetAndGetContext() {
    TenantContext context = TenantContext.of("tenant123");
    TenantContextHolder.setContext(context);

    assertThat(TenantContextHolder.getContext()).isEqualTo(context);
    assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("tenant123");
    assertThat(TenantContextHolder.hasContext()).isTrue();
  }

  @Test
  void shouldReturnNullWhenNoContext() {
    assertThat(TenantContextHolder.getContext()).isNull();
    assertThat(TenantContextHolder.getCurrentTenantId()).isNull();
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldClearContext() {
    TenantContext context = TenantContext.of("tenant123");
    TenantContextHolder.setContext(context);

    TenantContextHolder.clear();

    assertThat(TenantContextHolder.getContext()).isNull();
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldRunWithContext() {
    TenantContext context = TenantContext.of("tenant123");
    final String[] capturedTenantId = new String[1];

    TenantContextHolder.runWithContext(
        context,
        () -> {
          capturedTenantId[0] = TenantContextHolder.getCurrentTenantId();
        });

    assertThat(capturedTenantId[0]).isEqualTo("tenant123");
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldSupplyWithContext() {
    TenantContext context = TenantContext.of("tenant123");

    String result =
        TenantContextHolder.supplyWithContext(
            context, TenantContextHolder::getCurrentTenantId);

    assertThat(result).isEqualTo("tenant123");
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldRestorePreviousContext() {
    TenantContext originalContext = TenantContext.of("original");
    TenantContext tempContext = TenantContext.of("temporary");

    TenantContextHolder.setContext(originalContext);

    TenantContextHolder.runWithContext(
        tempContext,
        () -> {
          assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("temporary");
        });

    assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("original");
  }
}
