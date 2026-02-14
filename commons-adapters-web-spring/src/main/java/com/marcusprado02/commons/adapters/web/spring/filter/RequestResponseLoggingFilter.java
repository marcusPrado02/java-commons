package com.marcusprado02.commons.adapters.web.spring.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filter that logs HTTP requests and responses with detailed information.
 *
 * <p>Logs:
 *
 * <ul>
 *   <li>Request method, URI, query string, headers
 *   <li>Response status code, headers
 *   <li>Request/response duration
 *   <li>Correlation ID (if present)
 * </ul>
 *
 * <p><strong>Configuration:</strong>
 *
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter() {
 *   FilterRegistrationBean<RequestResponseLoggingFilter> bean = new FilterRegistrationBean<>();
 *   bean.setFilter(new RequestResponseLoggingFilter());
 *   bean.setOrder(10); // After correlation ID filter
 *   return bean;
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> Set logging level to DEBUG for this class to see logs:
 *
 * <pre>
 * logging.level.com.marcusprado02.commons.adapters.web.spring.filter.RequestResponseLoggingFilter=DEBUG
 * </pre>
 */
public final class RequestResponseLoggingFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  private static final List<String> HEADERS_TO_LOG =
      List.of("X-Correlation-Id", "X-Tenant-Id", "Content-Type", "Accept", "User-Agent");

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!log.isDebugEnabled()) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

    long startTime = System.currentTimeMillis();

    try {
      logRequest(requestWrapper);
      chain.doFilter(requestWrapper, responseWrapper);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      logResponse(responseWrapper, duration);
      responseWrapper.copyBodyToResponse();
    }
  }

  private void logRequest(HttpServletRequest request) {
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String queryString = request.getQueryString();
    String headers = getRelevantHeaders(request);

    String fullUri = queryString != null ? uri + "?" + queryString : uri;

    log.debug(">>> HTTP Request: {} {} | Headers: {}", method, fullUri, headers);
  }

  private void logResponse(HttpServletResponse response, long duration) {
    int status = response.getStatus();
    String statusText = getStatusText(status);
    String headers = getRelevantResponseHeaders(response);

    log.debug(
        "<<< HTTP Response: {} {} | Duration: {}ms | Headers: {}",
        status,
        statusText,
        duration,
        headers);
  }

  private String getRelevantHeaders(HttpServletRequest request) {
    return Collections.list(request.getHeaderNames()).stream()
        .filter(HEADERS_TO_LOG::contains)
        .map(name -> name + "=" + request.getHeader(name))
        .collect(Collectors.joining(", "));
  }

  private String getRelevantResponseHeaders(HttpServletResponse response) {
    return response.getHeaderNames().stream()
        .filter(HEADERS_TO_LOG::contains)
        .map(name -> name + "=" + response.getHeader(name))
        .collect(Collectors.joining(", "));
  }

  private String getStatusText(int status) {
    try {
      HttpStatus httpStatus = HttpStatus.valueOf(status);
      return httpStatus.getReasonPhrase();
    } catch (IllegalArgumentException e) {
      return "Unknown";
    }
  }
}
