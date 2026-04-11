package com.marcusprado02.commons.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marcusprado02.commons.adapters.web.envelope.ApiEnvelope;
import com.marcusprado02.commons.adapters.web.envelope.ApiEnvelopeWithContext;
import com.marcusprado02.commons.adapters.web.envelope.ApiMeta;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.adapters.web.rest.GenericSearchController;
import com.marcusprado02.commons.adapters.web.rest.PageableResponse;
import com.marcusprado02.commons.adapters.web.result.HttpResultResponse;
import com.marcusprado02.commons.adapters.web.result.HttpResultResponseWithContext;
import com.marcusprado02.commons.adapters.web.result.HttpResults;
import com.marcusprado02.commons.adapters.web.result.HttpResultsWithContext;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WebModuleTest {

  // ── ApiMeta ──────────────────────────────────────────────────────────────

  @Test
  void apiMetaEmptyHasNullIds() {
    ApiMeta meta = ApiMeta.empty();
    assertNull(meta.correlationId());
    assertNull(meta.tenantId());
    assertNotNull(meta.timestamp());
  }

  @Test
  void apiMetaWithAttributesReturnsNewInstance() {
    ApiMeta meta = ApiMeta.empty();
    ApiMeta enriched = meta.withAttributes(Map.of("k", "v"));
    assertEquals("v", enriched.attributes().get("k"));
  }

  @Test
  void apiMetaNullAttributesDefaultsToEmptyMap() {
    ApiMeta meta = new ApiMeta(null, null, null, null, null, null);
    assertNotNull(meta.attributes());
    assertTrue(meta.attributes().isEmpty());
  }

  // ── ApiEnvelope ──────────────────────────────────────────────────────────

  @Test
  void apiEnvelopeSuccessOfDataOnly() {
    ApiEnvelope.Success<String> s = ApiEnvelope.Success.of("hello");
    assertEquals("hello", s.data());
    assertNotNull(s.meta());
  }

  @Test
  void apiEnvelopeSuccessOfDataAndMeta() {
    ApiMeta meta = ApiMeta.empty();
    ApiEnvelope.Success<String> s = ApiEnvelope.Success.of("data", meta);
    assertEquals("data", s.data());
    assertEquals(meta, s.meta());
  }

  @Test
  void apiEnvelopeSuccessNullMetaDefaultsToEmpty() {
    ApiEnvelope.Success<String> s = new ApiEnvelope.Success<>("x", null);
    assertNotNull(s.meta());
  }

  @Test
  void apiEnvelopeFailureOfErrorOnly() {
    ApiEnvelope.Failure f = ApiEnvelope.Failure.of("err");
    assertEquals("err", f.error());
    assertNotNull(f.meta());
  }

  @Test
  void apiEnvelopeFailureOfErrorAndMeta() {
    ApiMeta meta = ApiMeta.empty();
    ApiEnvelope.Failure f = ApiEnvelope.Failure.of("err", meta);
    assertEquals("err", f.error());
    assertEquals(meta, f.meta());
  }

  @Test
  void apiEnvelopeFailureNullMetaDefaultsToEmpty() {
    ApiEnvelope.Failure f = new ApiEnvelope.Failure("e", null);
    assertNotNull(f.meta());
  }

  // ── ApiEnvelopeWithContext ────────────────────────────────────────────────

  @Test
  void apiEnvelopeWithContextSuccessFactory() {
    ApiEnvelopeWithContext.Success<String> s =
        ApiEnvelopeWithContext.Success.of("data", "corr-1", "tenant-1", "actor-1");
    assertEquals("data", s.data());
    assertEquals("corr-1", s.correlationId());
    assertNotNull(s.timestamp());
  }

  @Test
  void apiEnvelopeWithContextSuccessNullMetaDefaultsToEmpty() {
    ApiEnvelopeWithContext.Success<String> s =
        new ApiEnvelopeWithContext.Success<>("d", "c", "t", "a", null, null);
    assertNotNull(s.meta());
  }

  @Test
  void apiEnvelopeWithContextFailureFactory() {
    ApiEnvelopeWithContext.Failure f =
        ApiEnvelopeWithContext.Failure.of("err", "corr-1", "tenant-1", "actor-1");
    assertEquals("err", f.error());
    assertEquals("tenant-1", f.tenantId());
  }

  @Test
  void apiEnvelopeWithContextFailureNullMetaDefaultsToEmpty() {
    ApiEnvelopeWithContext.Failure f =
        new ApiEnvelopeWithContext.Failure("e", "c", "t", "a", null, null);
    assertNotNull(f.meta());
  }

  // ── HttpProblemResponse ───────────────────────────────────────────────────

  @Test
  void httpProblemResponseNullDetailsDefaultsToEmptyMap() {
    HttpProblemResponse r = new HttpProblemResponse(400, "ERR", "msg", null, null);
    assertNotNull(r.details());
    assertTrue(r.details().isEmpty());
    assertNotNull(r.meta());
  }

  @Test
  void httpProblemResponseOfFactory() {
    HttpProblemResponse r = HttpProblemResponse.of(404, "NOT_FOUND", "not found");
    assertEquals(404, r.status());
    assertEquals("NOT_FOUND", r.code());
    assertTrue(r.details().isEmpty());
  }

  // ── HttpResults ───────────────────────────────────────────────────────────

  @Test
  void httpResultsMapOkBranch() {
    Result<String> ok = Result.ok("value");
    HttpResultResponse response = HttpResults.map(ok, new StubResultMapper());
    assertEquals(200, response.status());
  }

  @Test
  void httpResultsMapFailBranch() {
    Result<String> fail = Result.fail(buildProblem());
    HttpResultResponse response = HttpResults.map(fail, new StubResultMapper());
    assertEquals(500, response.status());
  }

  // ── HttpResultsWithContext ─────────────────────────────────────────────────

  @Test
  void httpResultsWithContextMapOkBranch() {
    Result<String> ok = Result.ok("value");
    HttpResultResponseWithContext r =
        HttpResultsWithContext.map(ok, new StubContextMapper(), "c", "t", "a");
    assertEquals(200, r.status());
  }

  @Test
  void httpResultsWithContextMapFailBranch() {
    Result<String> fail = Result.fail(buildProblem());
    HttpResultResponseWithContext r =
        HttpResultsWithContext.map(fail, new StubContextMapper(), "c", "t", "a");
    assertEquals(500, r.status());
  }

  // ── PageResponse ──────────────────────────────────────────────────────────

  @Test
  void pageResponseStoresFields() {
    PageResponse<String> resp = new PageResponse<>(List.of("a", "b"), 0, 2, 5L, 3);
    assertEquals(2, resp.items().size());
    assertEquals(5L, resp.totalItems());
    assertEquals(3, resp.totalPages());
  }

  // ── PageableResponse ──────────────────────────────────────────────────────

  @Test
  void pageableResponseThrowsOnNullContent() {
    assertThrows(IllegalArgumentException.class, () -> new PageableResponse<>(null, 0, 0, 1));
  }

  @Test
  void pageableResponseThrowsOnNegativeTotalElements() {
    assertThrows(IllegalArgumentException.class, () -> new PageableResponse<>(List.of(), -1, 0, 1));
  }

  @Test
  void pageableResponseThrowsOnNegativePage() {
    assertThrows(IllegalArgumentException.class, () -> new PageableResponse<>(List.of(), 0, -1, 1));
  }

  @Test
  void pageableResponseThrowsOnZeroSize() {
    assertThrows(IllegalArgumentException.class, () -> new PageableResponse<>(List.of(), 0, 0, 0));
  }

  @Test
  void pageableResponseValid() {
    PageableResponse<String> r = new PageableResponse<>(List.of("a"), 1L, 0, 10);
    assertEquals(1, r.content().size());
    assertEquals(1L, r.totalElements());
  }

  // ── GenericSearchController ────────────────────────────────────────────────

  @Test
  void searchControllerReturnsPageableResponse() {
    GenericSearchController<String, Long> ctrl =
        new GenericSearchController<>("/items", new StubRepository(List.of("a", "b", "c")));
    PageableResponse<String> resp = ctrl.search(Map.of());
    assertNotNull(resp);
    assertEquals(3, resp.totalElements());
    assertEquals("/items", ctrl.getBasePath());
  }

  @Test
  void searchControllerRespectsPaginationParams() {
    GenericSearchController<String, Long> ctrl =
        new GenericSearchController<>("/items", new StubRepository(List.of("a")));
    PageableResponse<String> resp = ctrl.search(Map.of("page", "2", "size", "5"));
    assertNotNull(resp);
  }

  @Test
  void searchControllerEnforcesMaxSize() {
    GenericSearchController<String, Long> ctrl =
        new GenericSearchController<>("/items", new StubRepository(List.of()));
    PageableResponse<String> resp = ctrl.search(Map.of("size", "999"));
    assertEquals(100, resp.size());
  }

  @Test
  void searchControllerAppliesFilterWhenPresent() {
    GenericSearchController<String, Long> ctrl =
        new GenericSearchController<>("/items", new StubRepository(List.of("alice")));
    PageableResponse<String> resp = ctrl.search(Map.of("filter", "name:eq:alice"));
    assertNotNull(resp);
  }

  @Test
  void searchControllerHandlesInvalidPaginationParams() {
    GenericSearchController<String, Long> ctrl =
        new GenericSearchController<>("/items", new StubRepository(List.of("x")));
    // invalid page/size values should fall back to defaults
    PageableResponse<String> resp = ctrl.search(Map.of("page", "abc", "size", "xyz"));
    assertNotNull(resp);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static Problem buildProblem() {
    return Problem.of(
        ErrorCode.of("TEST.ERR"), ErrorCategory.TECHNICAL, Severity.ERROR, "test problem");
  }

  private static final class StubResultMapper
      implements com.marcusprado02.commons.adapters.web.result.HttpResultMapper {
    @Override
    public <T> HttpResultResponse mapOk(Result.Ok<T> ok) {
      return new HttpResultResponse(200, ApiEnvelope.Success.of(ok.value()));
    }

    @Override
    public HttpResultResponse mapFail(Result.Fail<?> fail) {
      return new HttpResultResponse(500, ApiEnvelope.Failure.of(fail.problem()));
    }

    @Override
    public com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse mapProblem(
        Problem problem) {
      return HttpProblemResponse.of(500, "ERR", problem.message());
    }
  }

  private static final class StubContextMapper
      implements com.marcusprado02.commons.adapters.web.result.HttpResultMapperWithContext {
    @Override
    public <T> HttpResultResponseWithContext mapOk(
        Result.Ok<T> ok, String correlationId, String tenantId, String actorId) {
      return new HttpResultResponseWithContext(
          200, ApiEnvelopeWithContext.Success.of(ok.value(), correlationId, tenantId, actorId));
    }

    @Override
    public HttpResultResponseWithContext mapFail(
        Result.Fail<?> fail, String correlationId, String tenantId, String actorId) {
      return new HttpResultResponseWithContext(
          500, ApiEnvelopeWithContext.Failure.of(fail.problem(), correlationId, tenantId, actorId));
    }

    @Override
    public HttpResultResponseWithContext mapProblem(
        Problem problem, String correlationId, String tenantId, String actorId) {
      return new HttpResultResponseWithContext(
          500, ApiEnvelopeWithContext.Failure.of(problem, correlationId, tenantId, actorId));
    }
  }

  private static final class StubRepository implements PageableRepository<String, Long> {

    private final List<String> data;

    StubRepository(List<String> data) {
      this.data = new ArrayList<>(data);
    }

    @Override
    public Optional<String> findById(Long id) {
      return Optional.empty();
    }

    @Override
    public String save(String entity) {
      return entity;
    }

    @Override
    public void delete(String entity) {}

    @Override
    public void deleteById(Long id) {}

    @Override
    public PageResult<String> findAll(PageRequest req, Specification<String> spec) {
      return new PageResult<>(data, data.size(), req.page(), req.size());
    }

    @Override
    public PageResult<String> findAll(PageRequest req, SearchCriteria criteria) {
      return new PageResult<>(data, data.size(), req.page(), req.size());
    }

    @Override
    public PageResult<String> search(PageRequest req, Specification<String> spec, Sort sort) {
      return new PageResult<>(data, data.size(), req.page(), req.size());
    }
  }
}
