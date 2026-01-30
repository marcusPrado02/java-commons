package com.marcusprado02.commons.adapters.web.spring.client;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;

public final class RestTemplateContextInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    var headers = request.getHeaders();
    OutboundContextHeaders.currentHeaders()
        .forEach(
            (k, v) -> {
              if (!headers.containsKey(k)) {
                headers.add(k, v);
              }
            });

    return execution.execute(request, body);
  }
}
