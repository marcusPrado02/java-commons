package com.marcusprado02.commons.adapters.web.spring.filter;

import com.marcusprado02.commons.app.observability.ContextKeys;
import com.marcusprado02.commons.app.observability.CorrelationId;
import com.marcusprado02.commons.app.observability.RequestContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;

public final class CorrelationIdFilter implements Filter {

  public static final String REQUEST_ATTRIBUTE = "commons.correlation_id";

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String incoming = request.getHeader(WebHeaders.X_CORRELATION_ID);
    String correlationId =
        (incoming == null || incoming.isBlank()) ? CorrelationId.newId() : incoming.trim();

    request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
    response.setHeader(WebHeaders.X_CORRELATION_ID, correlationId);

    RequestContext.put(ContextKeys.CORRELATION_ID, correlationId);
    MDC.put(ContextKeys.CORRELATION_ID, correlationId);

    try {
      chain.doFilter(req, res);
    } finally {
      MDC.remove(ContextKeys.CORRELATION_ID);
      RequestContext.clear();
    }
  }
}
