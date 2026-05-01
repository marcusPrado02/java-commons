package com.marcusprado02.commons.starter.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import com.marcusprado02.commons.starter.idempotency.exception.DuplicateIdempotencyKeyException;
import com.marcusprado02.commons.starter.idempotency.exception.IdempotencyInProgressException;
import com.marcusprado02.commons.starter.idempotency.web.IdempotencyHandlerInterceptor;
import com.marcusprado02.commons.starter.idempotency.web.IdempotencyWebExceptionHandler;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdempotencyBranchTest {

  private static final String HEADER = "Idempotency-Key";

  private IdempotencyHandlerInterceptor interceptor(
      DuplicateRequestStrategy strategy, ResultRefStrategy ref) {
    IdempotencyProperties props =
        new IdempotencyProperties(
            Duration.ofMinutes(1),
            new IdempotencyProperties.Web(true, HEADER, strategy, ref),
            new IdempotencyProperties.Aop(false));
    return new IdempotencyHandlerInterceptor(new InMemoryIdempotencyStore(), props);
  }

  // --- IdempotencyProperties null-defaulting ---

  @Test
  void idempotencyProperties_null_defaultTtl_defaults_to_five_minutes() {
    IdempotencyProperties props = new IdempotencyProperties(null, null, null);
    assertEquals(Duration.ofMinutes(5), props.defaultTtl());
  }

  @Test
  void idempotencyProperties_null_web_defaults_to_standard_config() {
    IdempotencyProperties props = new IdempotencyProperties(null, null, null);
    assertNotNull(props.web());
    assertEquals("Idempotency-Key", props.web().headerName());
    assertEquals(DuplicateRequestStrategy.CONFLICT, props.web().onDuplicate());
    assertEquals(ResultRefStrategy.LOCATION_HEADER, props.web().resultRefStrategy());
  }

  @Test
  void idempotencyProperties_null_aop_defaults_to_disabled() {
    IdempotencyProperties props = new IdempotencyProperties(null, null, null);
    assertNotNull(props.aop());
    assertFalse(props.aop().enabled());
  }

  @Test
  void idempotencyPropertiesWeb_null_headerName_defaults_to_idempotency_key() {
    IdempotencyProperties.Web web = new IdempotencyProperties.Web(true, null, null, null);
    assertEquals("Idempotency-Key", web.headerName());
  }

  @Test
  void idempotencyPropertiesWeb_blank_headerName_defaults_to_idempotency_key() {
    IdempotencyProperties.Web web =
        new IdempotencyProperties.Web(true, "   ", DuplicateRequestStrategy.CONFLICT, null);
    assertEquals("Idempotency-Key", web.headerName());
  }

  @Test
  void idempotencyPropertiesWeb_null_onDuplicate_defaults_to_conflict() {
    IdempotencyProperties.Web web = new IdempotencyProperties.Web(true, HEADER, null, null);
    assertEquals(DuplicateRequestStrategy.CONFLICT, web.onDuplicate());
  }

  @Test
  void idempotencyPropertiesWeb_null_resultRefStrategy_defaults_to_location_header() {
    IdempotencyProperties.Web web =
        new IdempotencyProperties.Web(true, HEADER, DuplicateRequestStrategy.CONFLICT, null);
    assertEquals(ResultRefStrategy.LOCATION_HEADER, web.resultRefStrategy());
  }

  // --- Exception getters ---

  @Test
  void duplicateIdempotencyKeyException_getters_with_resultRef() {
    DuplicateIdempotencyKeyException ex =
        new DuplicateIdempotencyKeyException("key-x", "/orders/99");
    assertEquals("key-x", ex.getKey());
    assertEquals("/orders/99", ex.getResultRef());
    assertTrue(ex.getMessage().contains("key-x"));
  }

  @Test
  void duplicateIdempotencyKeyException_getters_null_resultRef() {
    DuplicateIdempotencyKeyException ex = new DuplicateIdempotencyKeyException("key-y", null);
    assertEquals("key-y", ex.getKey());
    assertNull(ex.getResultRef());
  }

  @Test
  void idempotencyInProgressException_getKey() {
    IdempotencyInProgressException ex = new IdempotencyInProgressException("key-z");
    assertEquals("key-z", ex.getKey());
    assertTrue(ex.getMessage().contains("key-z"));
  }

  // --- IdempotencyWebExceptionHandler ---

  @Test
  void handleDuplicate_without_resultRef_returns_conflict_no_ref_body() {
    IdempotencyWebExceptionHandler handler = new IdempotencyWebExceptionHandler();
    DuplicateIdempotencyKeyException ex = new DuplicateIdempotencyKeyException("k1", null);

    ResponseEntity<Map<String, Object>> resp = handler.handleDuplicate(ex);

    assertEquals(409, resp.getStatusCode().value());
    assertEquals("DUPLICATE_IDEMPOTENCY_KEY", resp.getBody().get("error"));
    assertEquals("k1", resp.getBody().get("key"));
    assertFalse(resp.getBody().containsKey("resultRef"));
    assertNull(resp.getHeaders().getFirst("Idempotency-Result-Ref"));
  }

  @Test
  void handleDuplicate_with_resultRef_includes_header_and_body() {
    IdempotencyWebExceptionHandler handler = new IdempotencyWebExceptionHandler();
    DuplicateIdempotencyKeyException ex = new DuplicateIdempotencyKeyException("k2", "/orders/42");

    ResponseEntity<Map<String, Object>> resp = handler.handleDuplicate(ex);

    assertEquals(409, resp.getStatusCode().value());
    assertEquals("/orders/42", resp.getBody().get("resultRef"));
    assertEquals("/orders/42", resp.getHeaders().getFirst("Idempotency-Result-Ref"));
  }

  @Test
  void handleInProgress_returns_conflict_with_key() {
    IdempotencyWebExceptionHandler handler = new IdempotencyWebExceptionHandler();
    IdempotencyInProgressException ex = new IdempotencyInProgressException("k3");

    ResponseEntity<Map<String, Object>> resp = handler.handleInProgress(ex);

    assertEquals(409, resp.getStatusCode().value());
    assertEquals("IDEMPOTENCY_IN_PROGRESS", resp.getBody().get("error"));
    assertEquals("k3", resp.getBody().get("key"));
  }

  // --- IdempotencyHandlerInterceptor afterCompletion guard branches ---

  @Test
  void afterCompletion_acquired_false_does_nothing() {
    IdempotencyHandlerInterceptor interceptor =
        interceptor(DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.setAttribute(
        IdempotencyHandlerInterceptor.class.getName() + ".acquired", Boolean.FALSE);
    request.setAttribute(
        IdempotencyHandlerInterceptor.class.getName() + ".key", new IdempotencyKey("x"));
    interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
  }

  @Test
  void afterCompletion_key_not_idempotency_key_does_nothing() {
    IdempotencyHandlerInterceptor interceptor =
        interceptor(DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.setAttribute(IdempotencyHandlerInterceptor.class.getName() + ".acquired", Boolean.TRUE);
    request.setAttribute(
        IdempotencyHandlerInterceptor.class.getName() + ".key", "not-a-key-object");
    interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
  }

  @Test
  void afterCompletion_blank_location_header_stores_null_result_ref() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    IdempotencyProperties props =
        new IdempotencyProperties(
            Duration.ofMinutes(1),
            new IdempotencyProperties.Web(
                true, HEADER, DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER),
            new IdempotencyProperties.Aop(false));
    IdempotencyHandlerInterceptor interceptor = new IdempotencyHandlerInterceptor(store, props);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.addHeader(HEADER, "blank-loc");
    interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(201);
    response.setHeader(HttpHeaders.LOCATION, "   ");
    interceptor.afterCompletion(request, response, new Object(), null);

    assertThat(store.find(new IdempotencyKey("blank-loc")))
        .isPresent()
        .get()
        .extracting(IdempotencyRecord::resultRef)
        .isNull();
  }
}
