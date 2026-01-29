package com.marcusprado02.commons.adapters.web.spring.filter;

import com.marcusprado02.commons.app.observability.RequestContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

public final class RequestContextFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    try {
      chain.doFilter(request, response);
    } finally {
      // Segurança extra: se algum código criou contexto e esqueceu, a gente limpa.
      RequestContext.clear();
    }
  }
}
