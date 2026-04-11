package com.marcusprado02.commons.platform.http;

import com.marcusprado02.commons.platform.context.RequestContextSnapshot;

/** Writes request context values as HTTP headers via a {@link HeaderSink}. */
public interface ContextHeaderWriter {

  void write(RequestContextSnapshot ctx, HeaderSink sink);

  /** Accepts individual HTTP header name/value pairs. */
  interface HeaderSink {
    void set(String name, String value);
  }
}
