package com.marcusprado02.commons.app.featureflags;

public class FallbackTarget {

  public String guarded() {
    return "main-result";
  }

  public String fallback() {
    return "fallback-result";
  }
}
