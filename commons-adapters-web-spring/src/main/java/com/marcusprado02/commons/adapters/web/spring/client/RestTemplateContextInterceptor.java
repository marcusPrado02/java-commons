package com.marcusprado02.commons.adapters.web.spring.client;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link org.springframework.http.client.ClientHttpRequestInterceptor} that propagates context
 * headers to outbound REST requests.
 */
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
