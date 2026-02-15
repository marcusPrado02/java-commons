package com.marcusprado02.commons.app.multitenancy.resolver.servlet;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HeaderTenantResolverTest {

  @Mock private HttpServletRequest request;

  public HeaderTenantResolverTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldResolveTenantFromHeader() {
    HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-ID");
    when(request.getHeader("X-Tenant-ID")).thenReturn("tenant123");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldReturnEmptyWhenHeaderMissing() {
    HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-ID");
    when(request.getHeader("X-Tenant-ID")).thenReturn(null);

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenHeaderEmpty() {
    HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-ID");
    when(request.getHeader("X-Tenant-ID")).thenReturn("");

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldTrimHeaderValue() {
    HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-ID");
    when(request.getHeader("X-Tenant-ID")).thenReturn("  tenant123  ");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldHaveHighPriority() {
    HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-ID");

    assertThat(resolver.getPriority()).isEqualTo(10);
  }
}
