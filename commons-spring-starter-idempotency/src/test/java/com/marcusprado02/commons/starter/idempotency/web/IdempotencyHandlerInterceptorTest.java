package com.marcusprado02.commons.starter.idempotency.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import com.marcusprado02.commons.starter.idempotency.DuplicateRequestStrategy;
import com.marcusprado02.commons.starter.idempotency.IdempotencyProperties;
import com.marcusprado02.commons.starter.idempotency.ResultRefStrategy;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdempotencyHandlerInterceptorTest {

  @Test
  void shouldRejectDuplicateRequestsAndExposeResultRef() throws Exception {
    IdempotencyStorePort store = new InMemoryIdempotencyStore();
    IdempotencyProperties props =
        new IdempotencyProperties(
            Duration.ofMinutes(1),
            new IdempotencyProperties.Web(
                true, "Idempotency-Key", DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER),
            new IdempotencyProperties.Aop(false));

    IdempotencyHandlerInterceptor interceptor = new IdempotencyHandlerInterceptor(store, props);

    MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/orders");
    r1.addHeader("Idempotency-Key", "k1");
    MockHttpServletResponse resp1 = new MockHttpServletResponse();

    boolean allowed = interceptor.preHandle(r1, resp1, new Object());
    assertThat(allowed).isTrue();

    resp1.setStatus(201);
    resp1.setHeader(HttpHeaders.LOCATION, "/orders/123");
    interceptor.afterCompletion(r1, resp1, new Object(), null);

    assertThat(store.find(new IdempotencyKey("k1"))).isPresent();

    MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/orders");
    r2.addHeader("Idempotency-Key", "k1");
    MockHttpServletResponse resp2 = new MockHttpServletResponse();
    boolean allowed2 = interceptor.preHandle(r2, resp2, new Object());

    assertThat(allowed2).isFalse();
    assertThat(resp2.getStatus()).isEqualTo(409);
    assertThat(resp2.getHeader(IdempotencyHandlerInterceptor.HEADER_RESULT_REF))
        .isEqualTo("/orders/123");
  }
}
