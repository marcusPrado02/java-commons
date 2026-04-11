package com.marcusprado02.commons.adapters.web.jaxrs.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

class ContextPropagationFilterTest {

  @Mock private ClientRequestContext requestContext;

  private ContextPropagationFilter filter;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    filter = new ContextPropagationFilter();
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    MDC.clear();
  }

  @AfterEach
  void tearDown() throws Exception {
    MDC.clear();
    mocks.close();
  }

  @Test
  void shouldNotThrowWhenMdcIsEmpty() {
    assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotThrowWithCorrelationIdInMdc() throws IOException {
    MDC.put("correlationId", "test-corr-id");

    assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotThrowWithAllContextHeadersInMdc() throws IOException {
    MDC.put("correlationId", "corr-1");
    MDC.put("tenantId", "tenant-1");
    MDC.put("actorId", "actor-1");

    assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotAddHeaderForBlankMdcValue() throws IOException {
    MDC.put("correlationId", "  ");

    filter.filter(requestContext);

    verify(requestContext, never()).getHeaders();
  }
}
