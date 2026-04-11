package com.marcusprado02.commons.starter.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Tracer.SpanInScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrometerTracerFacadeTest {

  private Tracer tracer;
  private Span span;
  private SpanInScope spanInScope;
  private MicrometerTracerFacade facade;

  @BeforeEach
  void setUp() {
    tracer = mock(Tracer.class);
    span = mock(Span.class);
    spanInScope = mock(SpanInScope.class);

    // nextSpan() returns Span; Span.name() and Span.start() return Span (builder-style)
    when(tracer.nextSpan()).thenReturn(span);
    when(span.name(anyString())).thenReturn(span);
    when(span.start()).thenReturn(span);
    when(tracer.withSpan(span)).thenReturn(spanInScope);
    when(span.error(any())).thenReturn(span);

    facade = new MicrometerTracerFacade(tracer);
  }

  @Test
  void runnableInSpanShouldExecuteActionAndEndSpan() {
    facade.inSpan("my-op", () -> {});
    verify(span).end();
  }

  @Test
  void runnableInSpanShouldRecordErrorAndRethrowOnRuntimeException() {
    RuntimeException ex = new RuntimeException("boom");
    assertThatThrownBy(
            () ->
                facade.inSpan(
                    "my-op",
                    () -> {
                      throw ex;
                    }))
        .isSameAs(ex);
    verify(span).error(ex);
    verify(span).end();
  }

  @Test
  void supplierInSpanShouldReturnResultAndEndSpan() {
    String result = facade.inSpan("my-op", () -> "result");
    assertThat(result).isEqualTo("result");
    verify(span).end();
  }

  @Test
  void supplierInSpanShouldRecordErrorAndRethrowOnRuntimeException() {
    RuntimeException ex = new RuntimeException("supplier-fail");
    assertThatThrownBy(
            () ->
                facade.inSpan(
                    "my-op",
                    () -> {
                      throw ex;
                    }))
        .isSameAs(ex);
    verify(span).error(ex);
    verify(span).end();
  }

  @Test
  void shouldUseDefaultSpanNameWhenNullIsPassed() {
    facade.inSpan(null, () -> {});
    verify(span).name("operation");
  }

  @Test
  void shouldUseDefaultSpanNameWhenBlankIsPassed() {
    facade.inSpan("   ", () -> {});
    verify(span).name("operation");
  }
}
