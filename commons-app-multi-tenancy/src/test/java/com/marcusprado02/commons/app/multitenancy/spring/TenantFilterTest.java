package com.marcusprado02.commons.app.multitenancy.spring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Optional;

class TenantFilterTest {

  @Mock private TenantResolver<HttpServletRequest> tenantResolver;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private TenantFilter filter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    filter = new TenantFilter(tenantResolver);
    TenantContextHolder.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  void shouldSetTenantContextWhenResolved() throws ServletException, IOException {
    TenantContext tenantContext = TenantContext.of("tenant123");
    when(tenantResolver.resolve(request)).thenReturn(Optional.of(tenantContext));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    // Context should be cleared after filter chain
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldProceedWhenTenantNotResolved() throws ServletException, IOException {
    when(tenantResolver.resolve(request)).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldClearContextEvenWhenExceptionOccurs() throws ServletException, IOException {
    TenantContext tenantContext = TenantContext.of("tenant123");
    when(tenantResolver.resolve(request)).thenReturn(Optional.of(tenantContext));
    doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
        .isInstanceOf(ServletException.class);

    // Context should still be cleared
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldRestorePreviousContext() throws ServletException, IOException {
    TenantContext originalContext = TenantContext.of("original");
    TenantContext newContext = TenantContext.of("tenant123");

    // Set original context
    TenantContextHolder.setContext(originalContext);

    when(tenantResolver.resolve(request)).thenReturn(Optional.of(newContext));

    filter.doFilter(request, response, filterChain);

    // Original context should be restored
    assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("original");
  }
}
