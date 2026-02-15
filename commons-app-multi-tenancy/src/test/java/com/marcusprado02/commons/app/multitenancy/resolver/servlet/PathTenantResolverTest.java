package com.marcusprado02.commons.app.multitenancy.resolver.servlet;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PathTenantResolverTest {

  @Mock private HttpServletRequest request;

  public PathTenantResolverTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldExtractTenantFromDefaultPattern() {
    PathTenantResolver resolver = new PathTenantResolver();
    when(request.getRequestURI()).thenReturn("/tenant123/api/users");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldExtractTenantFromCustomPattern() {
    PathTenantResolver resolver = new PathTenantResolver("/api/v1/([^/]+)/.*");
    when(request.getRequestURI()).thenReturn("/api/v1/tenant123/users");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldReturnEmptyWhenPatternDoesNotMatch() {
    PathTenantResolver resolver = new PathTenantResolver();
    when(request.getRequestURI()).thenReturn("/api/users"); // No tenant in path

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenTenantIdIsEmpty() {
    PathTenantResolver resolver = new PathTenantResolver("/([^/]*)/.*");
    when(request.getRequestURI()).thenReturn("//api/users"); // Empty tenant

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenURIIsNull() {
    PathTenantResolver resolver = new PathTenantResolver();
    when(request.getRequestURI()).thenReturn(null);

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldHaveLowPriority() {
    PathTenantResolver resolver = new PathTenantResolver();

    assertThat(resolver.getPriority()).isEqualTo(30);
  }
}
