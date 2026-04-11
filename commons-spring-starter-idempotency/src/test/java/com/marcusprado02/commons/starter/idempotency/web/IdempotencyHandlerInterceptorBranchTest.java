package com.marcusprado02.commons.starter.idempotency.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import com.marcusprado02.commons.starter.idempotency.DuplicateRequestStrategy;
import com.marcusprado02.commons.starter.idempotency.IdempotencyProperties;
import com.marcusprado02.commons.starter.idempotency.ResultRefStrategy;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdempotencyHandlerInterceptorBranchTest {

  private static final String HEADER = "Idempotency-Key";

  private IdempotencyHandlerInterceptor interceptor(
      IdempotencyStorePort store, DuplicateRequestStrategy strategy, ResultRefStrategy ref) {
    IdempotencyProperties props =
        new IdempotencyProperties(
            Duration.ofMinutes(1),
            new IdempotencyProperties.Web(true, HEADER, strategy, ref),
            new IdempotencyProperties.Aop(false));
    return new IdempotencyHandlerInterceptor(store, props);
  }

  @Test
  void shouldAllowGetRequests() throws Exception {
    IdempotencyHandlerInterceptor interceptor =
        interceptor(
            new InMemoryIdempotencyStore(),
            DuplicateRequestStrategy.CONFLICT,
            ResultRefStrategy.LOCATION_HEADER);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resource");
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isTrue();
  }

  @Test
  void shouldAllowRequestsWithNoIdempotencyHeader() throws Exception {
    IdempotencyHandlerInterceptor interceptor =
        interceptor(
            new InMemoryIdempotencyStore(),
            DuplicateRequestStrategy.CONFLICT,
            ResultRefStrategy.LOCATION_HEADER);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    // no header added
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isTrue();
  }

  @Test
  void shouldAllowDuplicateWhenStrategyIsAllow() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.ALLOW, ResultRefStrategy.LOCATION_HEADER);

    // first request acquires the key
    MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/orders");
    r1.addHeader(HEADER, "allow-key");
    interceptor.preHandle(r1, new MockHttpServletResponse(), new Object());

    // second request: key exists → ALLOW strategy returns true
    MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/orders");
    r2.addHeader(HEADER, "allow-key");
    MockHttpServletResponse resp2 = new MockHttpServletResponse();
    boolean result = interceptor.preHandle(r2, resp2, new Object());

    assertThat(result).isTrue();
    assertThat(resp2.getStatus()).isNotEqualTo(409);
  }

  @Test
  void shouldRejectInProgressKeyWithConflict() throws Exception {
    // Use mock to simulate find() returning empty but tryAcquire() returning false (race)
    IdempotencyStorePort store = mock(IdempotencyStorePort.class);
    when(store.find(any())).thenReturn(Optional.empty());
    when(store.tryAcquire(any(), any())).thenReturn(false);
    when(store.find(any())).thenReturn(Optional.empty()); // second find returns empty

    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.addHeader(HEADER, "race-key");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertThat(result).isFalse();
    assertThat(response.getStatus()).isEqualTo(409);
  }

  @Test
  void shouldAllowInProgressKeyWhenStrategyIsAllow() throws Exception {
    IdempotencyStorePort store = mock(IdempotencyStorePort.class);
    when(store.find(any())).thenReturn(Optional.empty());
    when(store.tryAcquire(any(), any())).thenReturn(false);

    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.ALLOW, ResultRefStrategy.LOCATION_HEADER);

    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/items/1");
    request.addHeader(HEADER, "race-allow");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = interceptor.preHandle(request, response, new Object());
    assertThat(result).isTrue();
  }

  @Test
  void afterCompletionShouldDoNothingWhenNotAcquired() {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    // No ATTR_ACQUIRED set on request
    interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
    // no exception → branches for missing attrs are covered
  }

  @Test
  void afterCompletionShouldMarkFailedOnException() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.addHeader(HEADER, "fail-key");
    interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    MockHttpServletResponse response = new MockHttpServletResponse();
    interceptor.afterCompletion(request, response, new Object(), new RuntimeException("boom"));

    IdempotencyKey key = new IdempotencyKey("fail-key");
    assertThat(store.find(key))
        .isPresent()
        .get()
        .extracting(r -> r.status())
        .isEqualTo(IdempotencyStatus.FAILED);
  }

  @Test
  void afterCompletionShouldMarkFailedOnErrorStatus() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.addHeader(HEADER, "error-key");
    interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(500);
    interceptor.afterCompletion(request, response, new Object(), null);

    IdempotencyKey key = new IdempotencyKey("error-key");
    assertThat(store.find(key))
        .isPresent()
        .get()
        .extracting(r -> r.status())
        .isEqualTo(IdempotencyStatus.FAILED);
  }

  @Test
  void afterCompletionShouldMarkCompletedWithNoResultRefWhenStrategyNone() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.NONE);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.addHeader(HEADER, "no-ref-key");
    interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(201);
    interceptor.afterCompletion(request, response, new Object(), null);

    IdempotencyKey key = new IdempotencyKey("no-ref-key");
    assertThat(store.find(key))
        .isPresent()
        .get()
        .extracting(r -> r.status())
        .isEqualTo(IdempotencyStatus.COMPLETED);
    assertThat(store.find(key).get().resultRef()).isNull();
  }

  @Test
  void rejectDuplicateShouldNotAddResultRefHeaderForInProgressRecord() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);

    // First request acquires key but does NOT complete it (leaves it IN_PROGRESS)
    MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/orders");
    r1.addHeader(HEADER, "in-progress");
    interceptor.preHandle(r1, new MockHttpServletResponse(), new Object());

    // Second request: key is IN_PROGRESS → rejectDuplicate with IN_PROGRESS record
    MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/orders");
    r2.addHeader(HEADER, "in-progress");
    MockHttpServletResponse resp2 = new MockHttpServletResponse();
    boolean result = interceptor.preHandle(r2, resp2, new Object());

    assertThat(result).isFalse();
    assertThat(resp2.getStatus()).isEqualTo(409);
    // No result-ref header because record is not COMPLETED
    assertThat(resp2.getHeader(IdempotencyHandlerInterceptor.HEADER_RESULT_REF)).isNull();
  }

  @Test
  void shouldHandlePatchAndDeleteAsMutationMethods() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyHandlerInterceptor interceptor =
        interceptor(store, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);

    MockHttpServletRequest patch = new MockHttpServletRequest("PATCH", "/items/1");
    patch.addHeader(HEADER, "patch-key");
    assertThat(interceptor.preHandle(patch, new MockHttpServletResponse(), new Object())).isTrue();

    MockHttpServletRequest delete = new MockHttpServletRequest("DELETE", "/items/1");
    delete.addHeader(HEADER, "delete-key");
    assertThat(interceptor.preHandle(delete, new MockHttpServletResponse(), new Object())).isTrue();
  }
}
