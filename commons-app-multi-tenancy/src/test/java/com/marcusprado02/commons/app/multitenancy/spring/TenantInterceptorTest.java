package com.marcusprado02.commons.app.multitenancy.spring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.method.HandlerMethod;

import java.util.Optional;

class TenantInterceptorTest {

  @Mock private TenantResolver<HttpServletRequest> tenantResolver;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private HandlerMethod handler;

  private TenantInterceptor interceptor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    interceptor = new TenantInterceptor(tenantResolver);
    TenantContextHolder.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  void shouldSetTenantContextInPreHandle() {
    TenantContext tenantContext = TenantContext.of("tenant123");
    when(tenantResolver.resolve(request)).thenReturn(Optional.of(tenantContext));

    boolean result = interceptor.preHandle(request, response, handler);

    assertThat(result).isTrue();
    assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("tenant123");
  }

  @Test
  void shouldProceedWhenTenantNotResolved() {
    when(tenantResolver.resolve(request)).thenReturn(Optional.empty());

    boolean result = interceptor.preHandle(request, response, handler);

    assertThat(result).isTrue();
    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldClearContextInAfterCompletion() {
    TenantContext tenantContext = TenantContext.of("tenant123");
    when(tenantResolver.resolve(request)).thenReturn(Optional.of(tenantContext));

    // Simulate request processing
    interceptor.preHandle(request, response, handler);
    interceptor.afterCompletion(request, response, handler, null);

    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldClearContextEvenWithException() {
    TenantContext tenantContext = TenantContext.of("tenant123");
    when(tenantResolver.resolve(request)).thenReturn(Optional.of(tenantContext));

    // Simulate request processing with exception
    interceptor.preHandle(request, response, handler);
    Exception ex = new RuntimeException("Test exception");
    interceptor.afterCompletion(request, response, handler, ex);

    assertThat(TenantContextHolder.hasContext()).isFalse();
  }

  @Test
  void shouldRestorePreviousContext() {
    TenantContext originalContext = TenantContext.of("original");
    TenantContext newContext = TenantContext.of("tenant123");

    // Set original context
    TenantContextHolder.setContext(originalContext);

    when(tenantResolver.resolve(request)).thenReturn(Optional.of(newContext));

    // Simulate full request cycle
    interceptor.preHandle(request, response, handler);
    assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("tenant123");

    interceptor.afterCompletion(request, response, handler, null);
    assertThat(TenantContextHolder.getCurrentTenantId()).isEqualTo("original");
  }
}
