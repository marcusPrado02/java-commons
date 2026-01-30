package com.marcusprado02.commons.adapters.web.spring.result;

import com.marcusprado02.commons.adapters.web.result.HttpResults;
import com.marcusprado02.commons.kernel.result.Result;
import org.springframework.http.ResponseEntity;

public final class SpringHttpResults {

  private SpringHttpResults() {}

  public static <T> ResponseEntity<?> from(Result<T> result, SpringHttpResultMapper mapper) {
    return SpringResultResponses.toResponseEntity(HttpResults.map(result, mapper));
  }
}
