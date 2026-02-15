package com.marcusprado02.commons.app.multitenancy.resolver.servlet;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SubdomainTenantResolverTest {

  @Mock private HttpServletRequest request;

  public SubdomainTenantResolverTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldExtractTenantFromSubdomain() {
    SubdomainTenantResolver resolver = new SubdomainTenantResolver();
    when(request.getHeader("Host")).thenReturn("tenant123.example.com");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    TenantContext context = result.get();
    assertThat(context.tenantId()).isEqualTo("tenant123");
    assertThat(context.getDomain()).hasValue("tenant123.example.com");
  }

  @Test
  void shouldStripPort() {
    SubdomainTenantResolver resolver = new SubdomainTenantResolver();
    when(request.getHeader("Host")).thenReturn("tenant123.example.com:8080");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
    assertThat(result.get().getDomain()).hasValue("tenant123.example.com");
  }

  @Test
  void shouldReturnEmptyForTwoPartDomain() {
    SubdomainTenantResolver resolver = new SubdomainTenantResolver();
    when(request.getHeader("Host")).thenReturn("example.com");

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldStripPrefixes() {
    SubdomainTenantResolver resolver = new SubdomainTenantResolver("api-", "app-");
    when(request.getHeader("Host")).thenReturn("api-tenant123.example.com");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldStripSuffixes() {
    SubdomainTenantResolver resolver =
        new SubdomainTenantResolver(new String[0], new String[] {"-staging", "-dev"});
    when(request.getHeader("Host")).thenReturn("tenant123-staging.example.com");

    var result = resolver.resolve(request);

    assertThat(result).isPresent();
    assertThat(result.get().tenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldReturnEmptyWhenHostMissing() {
    SubdomainTenantResolver resolver = new SubdomainTenantResolver();
    when(request.getHeader("Host")).thenReturn(null);

    var result = resolver.resolve(request);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenTenantIdEmpty() {
    SubdomainTenantResolver resolver = new SubdomainTenantResolver("tenant123");
    when(request.getHeader("Host")).thenReturn("tenant123.example.com");

    var result = resolver.resolve(request);

    assertThat(result).isEmpty(); // tenant123 prefix strips entire subdomain
  }
}
