package com.marcusprado02.commons.ports.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class HttpStreamingResponse implements Closeable {

  private final int statusCode;
  private final Map<String, List<String>> headers;
  private final InputStream body;
  private final Closeable closer;

  public HttpStreamingResponse(
      int statusCode,
      Map<String, List<String>> headers,
      InputStream body,
      Closeable closer) {
    this.statusCode = statusCode;
    this.headers =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(headers, "headers must not be null")));
    this.body = Objects.requireNonNull(body, "body must not be null");
    this.closer = closer;
  }

  public int statusCode() {
    return statusCode;
  }

  public Map<String, List<String>> headers() {
    return headers;
  }

  public InputStream body() {
    return body;
  }

  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }

  public Optional<String> firstHeader(String name) {
    List<String> values = headers.get(name);
    if (values == null || values.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(values.getFirst());
  }

  @Override
  public void close() throws IOException {
    try {
      body.close();
    } finally {
      if (closer != null) {
        closer.close();
      }
    }
  }
}
