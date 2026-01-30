package com.marcusprado02.commons.adapters.web.problem;

import com.marcusprado02.commons.adapters.web.envelope.ApiMeta;
import java.util.Map;

public record HttpProblemResponse(
    int status, String code, String message, Map<String, Object> details, ApiMeta meta) {

  public HttpProblemResponse {
    details = details == null ? Map.of() : Map.copyOf(details);
    meta = meta == null ? ApiMeta.empty() : meta;
  }

  public static HttpProblemResponse of(int status, String code, String message) {
    return new HttpProblemResponse(status, code, message, Map.of(), ApiMeta.empty());
  }
}
