package com.marcusprado02.commons.kernel.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Coverage tests for commons-kernel-errors utility classes: ProblemBuilder, ErrorContext,
 * ErrorEnvelope, ProblemDetail, RFC7807ProblemDetail, ProblemThrower, DomainException hierarchy,
 * and StandardErrorCodes.
 */
class KernelErrorsCoverageTest {

  // ---- ProblemDetail ----

  @Test
  void problemDetail_of_creates_with_empty_meta() {
    ProblemDetail d = ProblemDetail.of("email", "invalid format");
    assertEquals("email", d.field());
    assertEquals("invalid format", d.message());
    assertTrue(d.meta().isEmpty());
  }

  @Test
  void problemDetail_with_meta_stores_entries() {
    ProblemDetail d = new ProblemDetail("age", "too young", Map.of("min", 18));
    assertEquals(18, d.meta().get("min"));
  }

  // ---- ProblemBuilder ----

  @Test
  void problemBuilder_of_string_builds_problem() {
    Problem p =
        ProblemBuilder.of("MY.CODE")
            .category(ErrorCategory.BUSINESS)
            .severity(Severity.ERROR)
            .message("msg")
            .build();

    assertEquals("MY.CODE", p.code().value());
    assertEquals(ErrorCategory.BUSINESS, p.category());
  }

  @Test
  void problemBuilder_validation_factory_sets_category_and_severity() {
    Problem p = ProblemBuilder.validation("V.CODE", "bad input").build();
    assertEquals(ErrorCategory.VALIDATION, p.category());
    assertEquals(Severity.ERROR, p.severity());
  }

  @Test
  void problemBuilder_business_factory() {
    Problem p = ProblemBuilder.business("B.CODE", "rule violation").build();
    assertEquals(ErrorCategory.BUSINESS, p.category());
  }

  @Test
  void problemBuilder_notFound_factory() {
    Problem p = ProblemBuilder.notFound("NF.CODE", "not found").build();
    assertEquals(ErrorCategory.NOT_FOUND, p.category());
  }

  @Test
  void problemBuilder_technical_factory_sets_critical_severity() {
    Problem p = ProblemBuilder.technical("T.CODE", "db error").build();
    assertEquals(ErrorCategory.TECHNICAL, p.category());
    assertEquals(Severity.CRITICAL, p.severity());
  }

  @Test
  void problemBuilder_with_details_and_meta() {
    Problem p =
        ProblemBuilder.validation("V.CODE", "fail")
            .detail("name", "too short")
            .detail("email", "invalid", Map.of("pattern", ".*@.*"))
            .meta("requestId", "abc-123")
            .meta(Map.of("traceId", "xyz"))
            .correlationId("corr-1")
            .traceId("trace-1")
            .userId("user-1")
            .tenantId("tenant-1")
            .helpUrl("https://docs.example.com")
            .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

    assertEquals(2, p.details().size());
    assertEquals("too short", p.details().get(0).message());
    assertEquals("abc-123", p.meta().get("requestId"));
  }

  @Test
  void problemBuilder_details_list() {
    List<ProblemDetail> list = List.of(ProblemDetail.of("f1", "m1"), ProblemDetail.of("f2", "m2"));
    Problem p =
        ProblemBuilder.validation("V.CODE", "fail").details(list).build();
    assertEquals(2, p.details().size());
  }

  @Test
  void problemBuilder_throwIt_throws_domain_exception() {
    assertThrows(
        DomainException.class,
        () ->
            ProblemBuilder.business("B.ERR", "boom")
                .category(ErrorCategory.BUSINESS)
                .severity(Severity.ERROR)
                .throwIt());
  }

  @Test
  void problemBuilder_throwAs_throws_specific_exception() {
    assertThrows(
        ValidationException.class,
        () ->
            ProblemBuilder.validation("V.ERR", "bad").throwAs(ValidationException::new));
  }

  @Test
  void problemBuilder_of_errorCode_object() {
    Problem p =
        ProblemBuilder.of(StandardErrorCodes.NOT_FOUND_ENTITY)
            .category(ErrorCategory.NOT_FOUND)
            .severity(Severity.ERROR)
            .message("missing")
            .build();
    assertEquals("NOT_FOUND.ENTITY", p.code().value());
  }

  @Test
  void problemBuilder_missing_category_throws() {
    assertThrows(
        NullPointerException.class,
        () -> ProblemBuilder.of("X").severity(Severity.ERROR).message("m").build());
  }

  // ---- ErrorContext ----

  @Test
  void errorContext_builder_stores_all_fields() {
    Map<String, Object> ctx =
        ErrorContext.builder()
            .correlationId("corr-1")
            .traceId("trace-1")
            .spanId("span-1")
            .userId("user-1")
            .tenantId("tenant-1")
            .sessionId("sess-1")
            .operation("createOrder")
            .resource("Order", "order-123")
            .httpMethod("POST")
            .httpPath("/api/orders")
            .ipAddress("127.0.0.1")
            .userAgent("test-agent")
            .environment("test")
            .appVersion("1.0.0")
            .helpUrl("https://docs.example.com")
            .exceptionClass(RuntimeException.class)
            .exceptionMessage("something failed")
            .put("customKey", "customValue")
            .putAll(Map.of("extra", "value"))
            .now()
            .build();

    assertEquals("corr-1", ctx.get("correlationId"));
    assertEquals("trace-1", ctx.get("traceId"));
    assertEquals("user-1", ctx.get("userId"));
    assertEquals("Order", ctx.get("resourceType"));
    assertEquals("order-123", ctx.get("resourceId"));
    assertEquals("customValue", ctx.get("customKey"));
    assertEquals("value", ctx.get("extra"));
    assertNotNull(ctx.get("timestamp"));
  }

  @Test
  void errorContext_empty_returns_empty_map() {
    assertTrue(ErrorContext.empty().isEmpty());
  }

  @Test
  void errorContext_timestamp_method() {
    Instant ts = Instant.parse("2026-01-01T00:00:00Z");
    Map<String, Object> ctx = ErrorContext.builder().timestamp(ts).build();
    assertEquals(ts, ctx.get("timestamp"));
  }

  @Test
  void errorContext_wrapProblem_creates_envelope_with_correlation() {
    Problem p = Problems.business("B.ERR", "error");
    ErrorEnvelope env =
        ErrorContext.builder().correlationId("corr-99").wrapProblem(p);
    assertEquals(p, env.problem());
    assertEquals("corr-99", env.correlationId());
    assertEquals("corr-99", env.context().get("correlationId"));
  }

  @Test
  void errorContext_put_requires_non_null_key() {
    assertThrows(NullPointerException.class, () -> ErrorContext.builder().put(null, "v"));
  }

  @Test
  void errorContext_putAll_requires_non_null_map() {
    assertThrows(NullPointerException.class, () -> ErrorContext.builder().putAll(null));
  }

  // ---- ErrorEnvelope ----

  @Test
  void errorEnvelope_of_creates_envelope_without_correlation() {
    Problem p = Problems.notFound("NF.X", "missing");
    ErrorEnvelope env = ErrorEnvelope.of(p);
    assertEquals(p, env.problem());
    assertNull(env.correlationId());
    assertTrue(env.context().isEmpty());
  }

  // ---- RFC7807ProblemDetail ----

  @Test
  void rfc7807_from_problem_sets_type_and_title() {
    Problem p = Problems.notFound("NOT_FOUND.ENTITY", "User not found");
    RFC7807ProblemDetail detail =
        RFC7807ProblemDetail.from(p).status(404).instance("/users/123").build();

    assertEquals("urn:problem-type:NOT_FOUND.ENTITY", detail.type().toString());
    assertEquals("User not found", detail.title());
    assertEquals(404, detail.status());
    assertEquals(URI.create("/users/123"), detail.instance());
  }

  @Test
  void rfc7807_from_problem_with_details_builds_detail_string() {
    Problem p =
        ProblemBuilder.validation("V.CODE", "fail")
            .detail("email", "invalid")
            .detail("name", "required")
            .build();
    RFC7807ProblemDetail detail = RFC7807ProblemDetail.from(p).status(400).build();

    assertNotNull(detail.detail());
    assertTrue(detail.detail().contains("email: invalid"));
    assertTrue(detail.detail().contains("name: required"));
  }

  @Test
  void rfc7807_builder_type_from_string() {
    RFC7807ProblemDetail d =
        RFC7807ProblemDetail.builder()
            .type("urn:type:test")
            .title("Test")
            .status(400)
            .detail("details here")
            .instance(URI.create("/res/1"))
            .extensions(Map.of("key", "val"))
            .build();

    assertEquals("urn:type:test", d.type().toString());
    assertEquals("val", d.extensions().get("key"));
  }

  @Test
  void rfc7807_null_extensions_defaults_to_empty_map() {
    RFC7807ProblemDetail d =
        new RFC7807ProblemDetail(null, null, null, null, null, null);
    assertNotNull(d.extensions());
    assertTrue(d.extensions().isEmpty());
  }

  // ---- ProblemThrower ----

  @Test
  void problemThrower_raise_throws_domain_exception() {
    Problem p = Problems.validation("V.ERR", "bad");
    DomainException ex = assertThrows(DomainException.class, () -> ProblemThrower.raise(p));
    assertEquals(p, ex.problem());
  }

  @Test
  void problemThrower_raise_null_throws_npe() {
    assertThrows(NullPointerException.class, () -> ProblemThrower.raise(null));
  }

  // ---- DomainException hierarchy ----

  @Test
  void domainException_with_cause() {
    Problem p = Problems.technical("T.ERR", "db error");
    RuntimeException cause = new RuntimeException("cause");
    DomainException ex = new DomainException(p, cause);
    assertEquals(p, ex.problem());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void all_exception_types_are_mapped() {
    assertInstanceOf(BusinessException.class, ProblemExceptions.from(
        Problem.of(ErrorCode.of("B"), ErrorCategory.BUSINESS, Severity.ERROR, "m")));
    assertInstanceOf(ConflictException.class, ProblemExceptions.from(
        Problem.of(ErrorCode.of("C"), ErrorCategory.CONFLICT, Severity.ERROR, "m")));
    assertInstanceOf(UnauthorizedException.class, ProblemExceptions.from(
        Problem.of(ErrorCode.of("U"), ErrorCategory.UNAUTHORIZED, Severity.ERROR, "m")));
    assertInstanceOf(ForbiddenException.class, ProblemExceptions.from(
        Problem.of(ErrorCode.of("F"), ErrorCategory.FORBIDDEN, Severity.ERROR, "m")));
    assertInstanceOf(TechnicalException.class, ProblemExceptions.from(
        Problem.of(ErrorCode.of("T"), ErrorCategory.TECHNICAL, Severity.CRITICAL, "m")));
  }

  // ---- StandardErrorCodes ----

  @Test
  void standardErrorCodes_constants_are_non_null_and_correct() {
    assertEquals("VALIDATION.FAILED", StandardErrorCodes.VALIDATION_FAILED.value());
    assertEquals("NOT_FOUND.ENTITY", StandardErrorCodes.NOT_FOUND_ENTITY.value());
    assertEquals("TECHNICAL.TIMEOUT", StandardErrorCodes.TECHNICAL_TIMEOUT.value());
    assertEquals(
        "INTEGRATION.COMMUNICATION_FAILURE",
        StandardErrorCodes.INTEGRATION_COMMUNICATION_FAILURE.value());
  }

  // ---- ResourceBundleMessageResolver ----

  @Test
  void resourceBundleResolver_falls_back_to_default_when_bundle_missing() {
    ResourceBundleMessageResolver resolver =
        new ResourceBundleMessageResolver("nonexistent/bundle");
    String msg = resolver.resolve("any.key", Locale.ENGLISH, "fallback message");
    assertEquals("fallback message", msg);
  }

  @Test
  void resourceBundleResolver_returns_key_when_no_default_and_bundle_missing() {
    ResourceBundleMessageResolver resolver =
        new ResourceBundleMessageResolver("nonexistent/bundle");
    String msg = resolver.resolve("some.key", Locale.ENGLISH, null);
    assertEquals("some.key", msg);
  }

  @Test
  void resourceBundleResolver_hasMessage_returns_false_when_bundle_missing() {
    ResourceBundleMessageResolver resolver =
        new ResourceBundleMessageResolver("nonexistent/bundle");
    assertFalse(resolver.hasMessage("any.key", Locale.ENGLISH));
  }

  @Test
  void resourceBundleResolver_fallback_with_args_formats_default() {
    ResourceBundleMessageResolver resolver =
        new ResourceBundleMessageResolver("nonexistent/bundle");
    String msg = resolver.resolve("k", Locale.ENGLISH, "Hello {0}", "World");
    assertEquals("Hello World", msg);
  }
}
