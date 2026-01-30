package com.marcusprado02.commons.platform.http;

import com.marcusprado02.commons.platform.context.RequestContextSnapshot;

public interface ContextHeaderWriter {

  void write(RequestContextSnapshot ctx, HeaderSink sink);

  interface HeaderSink {
    void set(String name, String value);
  }
}
