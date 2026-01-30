package com.marcusprado02.commons.adapters.web.spring.result;

import com.marcusprado02.commons.adapters.web.result.HttpResultResponse;
import org.springframework.http.ResponseEntity;

public final class SpringResultResponses {

  private SpringResultResponses() {}

  public static ResponseEntity<?> toResponseEntity(HttpResultResponse resp) {
    return ResponseEntity.status(resp.status()).body(resp.body());
  }
}
