package com.marcusprado02.commons.app.webhooks;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * HTTP client abstraction for delivering webhooks.
 *
 * <p>Implementations send HTTP POST requests to webhook endpoints with the event payload.
 */
public interface WebhookHttpClient {

  /**
   * Sends a webhook HTTP POST request.
   *
   * @param request the webhook request
   * @return result containing the HTTP response
   */
  Result<WebhookHttpResponse> send(WebhookHttpRequest request);
}
