package com.marcusprado02.commons.app.multitenancy.resolver;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import org.junit.jupiter.api.Test;
import java.util.Optional;

class CompositeTenantResolverTest {

  @Test
  void shouldReturnFirstSuccessfulResolution() {
    TenantResolver<String> resolver1 = mock(TenantResolver.class);
    TenantResolver<String> resolver2 = mock(TenantResolver.class);

    when(resolver1.getPriority()).thenReturn(10);
    when(resolver2.getPriority()).thenReturn(20);
    when(resolver1.resolve("request")).thenReturn(Optional.empty());
    when(resolver2.resolve("request"))
        .thenReturn(Optional.of(TenantContext.of("tenant123")));

    CompositeTenantResolver<String> composite =
        CompositeTenantResolver.<String>builder()
            .resolver(resolver2) // Added second, but has lower priority
            .resolver(resolver1) // Added first, but has higher priority
            .build();

    Optional<TenantContext> result = composite.resolve("request");

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");

    // Verify resolver1 was called first (due to priority)
    verify(resolver1).resolve("request");
    verify(resolver2).resolve("request");
  }

  @Test
  void shouldReturnEmptyWhenNoResolverSucceeds() {
    TenantResolver<String> resolver1 = mock(TenantResolver.class);
    TenantResolver<String> resolver2 = mock(TenantResolver.class);

    when(resolver1.resolve("request")).thenReturn(Optional.empty());
    when(resolver2.resolve("request")).thenReturn(Optional.empty());

    CompositeTenantResolver<String> composite =
        CompositeTenantResolver.<String>builder()
            .resolver(resolver1)
            .resolver(resolver2)
            .build();

    Optional<TenantContext> result = composite.resolve("request");

    assertThat(result).isEmpty();
  }

  @Test
  void shouldStopWhenFirstResolverSucceeds() {
    TenantResolver<String> resolver1 = mock(TenantResolver.class);
    TenantResolver<String> resolver2 = mock(TenantResolver.class);

    when(resolver1.getPriority()).thenReturn(10);
    when(resolver2.getPriority()).thenReturn(20);
    when(resolver1.resolve("request"))
        .thenReturn(Optional.of(TenantContext.of("tenant123")));

    CompositeTenantResolver<String> composite =
        CompositeTenantResolver.<String>builder()
            .resolver(resolver1)
            .resolver(resolver2)
            .build();

    Optional<TenantContext> result = composite.resolve("request");

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");

    // Verify only first resolver was called
    verify(resolver1).resolve("request");
    verify(resolver2, never()).resolve("request");
  }

  @Test
  void shouldSortResolversByPriority() {
    TenantResolver<String> lowPriority = mock(TenantResolver.class);
    TenantResolver<String> highPriority = mock(TenantResolver.class);

    when(lowPriority.getPriority()).thenReturn(100);
    when(highPriority.getPriority()).thenReturn(1);
    when(lowPriority.resolve("request")).thenReturn(Optional.empty());
    when(highPriority.resolve("request"))
        .thenReturn(Optional.of(TenantContext.of("tenant123")));

    // Add in reverse priority order
    CompositeTenantResolver<String> composite =
        CompositeTenantResolver.<String>builder()
            .resolver(lowPriority) // Added first but has low priority
            .resolver(highPriority) // Added second but has high priority
            .build();

    Optional<TenantContext> result = composite.resolve("request");

    assertThat(result).isPresent();

    // Verify high priority resolver was called first
    verify(highPriority).resolve("request");
    verify(lowPriority, never()).resolve("request");
  }
}
