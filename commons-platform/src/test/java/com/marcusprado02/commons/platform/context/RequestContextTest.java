package com.marcusprado02.commons.platform.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RequestContextTest {

  @Test
  void request_context_is_marker_composition() {
    assertTrue(RequestContext.class.isInterface());
  }
}
