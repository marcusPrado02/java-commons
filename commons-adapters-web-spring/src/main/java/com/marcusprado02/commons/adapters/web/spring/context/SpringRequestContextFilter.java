package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import com.marcusprado02.commons.platform.http.ContextHeaderWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.web.filter.OncePerRequestFilter;

public final class SpringRequestContextFilter extends OncePerRequestFilter {

  private final SpringRequestContextResolver resolver;

  private final ContextHeaderWriter contextHeaderWriter;

  public SpringRequestContextFilter(
      SpringRequestContextResolver resolver, ContextHeaderWriter contextHeaderWriter) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.contextHeaderWriter = Objects.requireNonNull(contextHeaderWriter, "contextHeaderWriter");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    RequestContextSnapshot ctx = resolver.resolve(request);
    contextHeaderWriter.write(ctx, response::setHeader);

    try {
      SpringRequestContextHolder.set(ctx);
      MdcContextBinder.bind(ctx);

      // OTel baggage (se disponível)
      try {
        var baggage = com.marcusprado02.commons.adapters.otel.baggage.BaggageBinder.bind(ctx);
        baggage.makeCurrent();
      } catch (NoClassDefFoundError ignored) {
        // OTel não presente
      }
      // Propaga correlation id de volta para o client (boa prática)
      if (ctx.correlationId() != null) {
        response.setHeader("X-Correlation-Id", ctx.correlationId());
      }

      filterChain.doFilter(request, response);
    } finally {
      MdcContextBinder.clear();
      SpringRequestContextHolder.clear();
    }
  }
}
