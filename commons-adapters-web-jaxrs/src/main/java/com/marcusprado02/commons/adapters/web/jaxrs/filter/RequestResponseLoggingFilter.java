package com.marcusprado02.commons.adapters.web.jaxrs.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS filter that logs HTTP requests and responses with detailed information.
 *
 * <p>Logs:
 *
 * <ul>
 *   <li>Request method, path, query parameters, relevant headers
 *   <li>Response status code, relevant headers
 *   <li>Request/response duration
 * </ul>
 *
 * <p><strong>Note:</strong> Set logging level to DEBUG for this class to see logs:
 *
 * <pre>
 * logging.level.com.marcusprado02.commons.adapters.web.jaxrs.filter.RequestResponseLoggingFilter=DEBUG
 * </pre>
 */
@Provider
public class RequestResponseLoggingFilter
    implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  private static final String START_TIME_PROPERTY = "commons.request_start_time";

  private static final List<String> HEADERS_TO_LOG =
      List.of("X-Correlation-Id", "X-Tenant-Id", "Content-Type", "Accept", "User-Agent");

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (!log.isDebugEnabled()) {
      return;
    }

    requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

    String method = requestContext.getMethod();
    String path = requestContext.getUriInfo().getPath();
    String query = requestContext.getUriInfo().getRequestUri().getQuery();
    String headers = getRelevantHeaders(requestContext);

    String fullPath = query != null ? path + "?" + query : path;

    log.debug(">>> JAX-RS Request: {} {} | Headers: {}", method, fullPath, headers);
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    if (!log.isDebugEnabled()) {
      return;
    }

    Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
    long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

    int status = responseContext.getStatus();
    String headers = getRelevantResponseHeaders(responseContext);

    log.debug("<<< JAX-RS Response: {} | Duration: {}ms | Headers: {}", status, duration, headers);
  }

  private String getRelevantHeaders(ContainerRequestContext requestContext) {
    StringBuilder sb = new StringBuilder();
    for (String headerName : HEADERS_TO_LOG) {
      String value = requestContext.getHeaderString(headerName);
      if (value != null) {
        if (!sb.isEmpty()) {
          sb.append(", ");
        }
        sb.append(headerName).append("=").append(value);
      }
    }
    return sb.toString();
  }

  private String getRelevantResponseHeaders(ContainerResponseContext responseContext) {
    StringBuilder sb = new StringBuilder();
    for (String headerName : HEADERS_TO_LOG) {
      Object value = responseContext.getHeaders().getFirst(headerName);
      if (value != null) {
        if (!sb.isEmpty()) {
          sb.append(", ");
        }
        sb.append(headerName).append("=").append(value);
      }
    }
    return sb.toString();
  }
}
