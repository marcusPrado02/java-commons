package com.marcusprado02.commons.adapters.web.spring.webflux.client;

import com.marcusprado02.commons.adapters.web.spring.client.OutboundContextHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/** WebClient exchange filter that propagates outbound context headers to reactive HTTP requests. */
public final class WebClientContextExchangeFilter {

  private WebClientContextExchangeFilter() {}

  /**
   * Returns an {@link ExchangeFilterFunction} that propagates context headers.
   *
   * @return the exchange filter function
   */
  public static ExchangeFilterFunction propagateContextHeaders() {
    return (request, next) -> {
      ClientRequest mutated =
          ClientRequest.from(request)
              .headers(
                  h ->
                      OutboundContextHeaders.currentHeaders()
                          .forEach(
                              (k, v) -> {
                                if (!h.containsKey(k)) {
                                  h.add(k, v);
                                }
                              }))
              .build();

      return next.exchange(mutated);
    };
  }
}
