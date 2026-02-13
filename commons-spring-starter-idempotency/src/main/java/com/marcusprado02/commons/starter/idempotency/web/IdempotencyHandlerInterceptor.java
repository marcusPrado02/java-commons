package com.marcusprado02.commons.starter.idempotency.web;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.http.IdempotencyHttp;
import com.marcusprado02.commons.starter.idempotency.DuplicateRequestStrategy;
import com.marcusprado02.commons.starter.idempotency.IdempotencyProperties;
import com.marcusprado02.commons.starter.idempotency.ResultRefStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class IdempotencyHandlerInterceptor implements HandlerInterceptor {

  static final String ATTR_ACQUIRED =
      IdempotencyHandlerInterceptor.class.getName() + ".acquired";
  static final String ATTR_KEY = IdempotencyHandlerInterceptor.class.getName() + ".key";

  static final String HEADER_RESULT_REF = "Idempotency-Result-Ref";

  private final IdempotencyStorePort store;
  private final IdempotencyProperties properties;

  public IdempotencyHandlerInterceptor(IdempotencyStorePort store, IdempotencyProperties properties) {
    this.store = Objects.requireNonNull(store, "store must not be null");
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {

    if (!isMutationMethod(request.getMethod())) {
      return true;
    }

    String headerName = properties.web().headerName();
    String rawHeaderValue = request.getHeader(headerName);
    Optional<IdempotencyKey> maybeKey = IdempotencyHttp.resolveFromHeaderValue(rawHeaderValue);
    if (maybeKey.isEmpty()) {
      return true;
    }
    IdempotencyKey key = maybeKey.get();

    Optional<IdempotencyRecord> existing = store.find(key);
    if (existing.isPresent()) {
      if (properties.web().onDuplicate() == DuplicateRequestStrategy.ALLOW) {
        return true;
      }
      return rejectDuplicate(response, key, existing.get());
    }

    Duration ttl = properties.defaultTtl();
    boolean acquired = store.tryAcquire(key, ttl);
    if (!acquired) {
      if (properties.web().onDuplicate() == DuplicateRequestStrategy.ALLOW) {
        return true;
      }
      return rejectDuplicate(response, key, store.find(key).orElse(null));
    }

    request.setAttribute(ATTR_ACQUIRED, Boolean.TRUE);
    request.setAttribute(ATTR_KEY, key);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    Object acquired = request.getAttribute(ATTR_ACQUIRED);
    Object keyAttr = request.getAttribute(ATTR_KEY);
    if (!(acquired instanceof Boolean) || !((Boolean) acquired) || !(keyAttr instanceof IdempotencyKey)) {
      return;
    }
    IdempotencyKey key = (IdempotencyKey) keyAttr;

    if (ex != null) {
      store.markFailed(key, ex.getClass().getSimpleName());
      return;
    }

    int status = response.getStatus();
    if (status >= 200 && status < 400) {
      store.markCompleted(key, resolveResultRef(response));
    } else {
      store.markFailed(key, "HTTP_" + status);
    }
  }

  private boolean rejectDuplicate(HttpServletResponse response, IdempotencyKey key, IdempotencyRecord record)
      throws IOException {
    response.setStatus(HttpStatus.CONFLICT.value());
    response.setHeader(properties.web().headerName(), key.value());
    if (record != null && record.status() == IdempotencyStatus.COMPLETED && record.resultRef() != null) {
      response.setHeader(HEADER_RESULT_REF, record.resultRef());
    }
    return false;
  }

  private String resolveResultRef(HttpServletResponse response) {
    if (properties.web().resultRefStrategy() == ResultRefStrategy.NONE) {
      return null;
    }
    String location = response.getHeader(HttpHeaders.LOCATION);
    return (location == null || location.isBlank()) ? null : location;
  }

  private static boolean isMutationMethod(String method) {
    if (method == null) {
      return false;
    }
    String m = method.toUpperCase(Locale.ROOT);
    return m.equals("POST") || m.equals("PUT") || m.equals("PATCH") || m.equals("DELETE");
  }
}
