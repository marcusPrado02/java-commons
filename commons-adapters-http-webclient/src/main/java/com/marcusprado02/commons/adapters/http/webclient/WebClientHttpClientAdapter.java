package com.marcusprado02.commons.adapters.http.webclient;

import com.marcusprado02.commons.ports.http.HttpInterceptor;
import com.marcusprado02.commons.ports.http.HttpRequest;
import com.marcusprado02.commons.ports.http.HttpResponse;
import com.marcusprado02.commons.ports.http.ReactiveHttpClientPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public final class WebClientHttpClientAdapter implements ReactiveHttpClientPort {

  private final WebClient webClient;
  private final List<HttpInterceptor> interceptors;
  private final Function<ContextView, Map<String, String>> contextHeadersProvider;

  private WebClientHttpClientAdapter(
      WebClient webClient,
      List<HttpInterceptor> interceptors,
      Function<ContextView, Map<String, String>> contextHeadersProvider) {
    this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
    this.interceptors = List.copyOf(interceptors == null ? List.of() : interceptors);
    this.contextHeadersProvider = contextHeadersProvider;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Mono<HttpResponse<byte[]>> execute(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    return Mono.deferContextual(
        contextView -> {
          HttpRequest requestWithContext = applyContextHeaders(request, contextView);
          HttpRequest interceptedRequest = applyRequestInterceptors(requestWithContext);

          Mono<HttpResponse<byte[]>> mono = doExecute(interceptedRequest);

          Optional<Duration> timeout = interceptedRequest.timeout();
          if (timeout.isPresent() && !timeout.get().isNegative() && !timeout.get().isZero()) {
            mono = mono.timeout(timeout.get());
          }

          return mono.map(response -> applyResponseInterceptors(interceptedRequest, response));
        });
  }

  private Mono<HttpResponse<byte[]>> doExecute(HttpRequest request) {
    return webClient
        .method(toSpringMethod(request))
        .uri(request.uri())
        .headers(headers -> applyHeaders(headers, request))
        .body(buildBodyInserter(request))
        .exchangeToMono(this::toResponse);
  }

  private Mono<HttpResponse<byte[]>> toResponse(ClientResponse response) {
    int statusCode = response.statusCode().value();
    return response
        .bodyToMono(byte[].class)
        .defaultIfEmpty(new byte[0])
        .map(body -> new HttpResponse<>(statusCode, response.headers().asHttpHeaders(), body));
  }

  private HttpMethod toSpringMethod(HttpRequest request) {
    return HttpMethod.valueOf(request.method().name());
  }

  private void applyHeaders(HttpHeaders target, HttpRequest request) {
    request
        .headers()
        .forEach(
            (key, values) -> {
              if (values == null) {
                return;
              }
              for (String value : values) {
                if (value != null) {
                  target.add(key, value);
                }
              }
            });
  }

  private BodyInserter<?, ? super ClientHttpRequest> buildBodyInserter(HttpRequest request) {
    return BodyInserters.fromValue(request.body().orElse(new byte[0]));
  }

  private HttpRequest applyContextHeaders(HttpRequest request, ContextView contextView) {
    if (contextHeadersProvider == null) {
      return request;
    }
    Map<String, String> contextHeaders = contextHeadersProvider.apply(contextView);
    if (contextHeaders == null || contextHeaders.isEmpty()) {
      return request;
    }

    HttpRequest.Builder builder = HttpRequest.builder();
    request.name().ifPresent(builder::name);
    builder.method(request.method());
    builder.uri(request.uri());
    builder.headers(request.headers());
    request.body().ifPresent(builder::body);
    request.timeout().ifPresent(builder::timeout);

    contextHeaders.forEach(
        (key, value) -> {
          if (key != null && value != null) {
            builder.header(key, value);
          }
        });

    return builder.build();
  }

  private HttpRequest applyRequestInterceptors(HttpRequest request) {
    HttpRequest current = request;
    for (HttpInterceptor interceptor : interceptors) {
      current =
          Objects.requireNonNull(
              interceptor.onRequest(current), "interceptor returned null request");
    }
    return current;
  }

  private HttpResponse<byte[]> applyResponseInterceptors(
      HttpRequest request, HttpResponse<byte[]> response) {
    HttpResponse<byte[]> current = response;
    for (HttpInterceptor interceptor : interceptors) {
      current =
          Objects.requireNonNull(
              interceptor.onResponse(request, current), "interceptor returned null response");
    }
    return current;
  }

  public static final class Builder {
    private WebClient webClient;
    private WebClient.Builder webClientBuilder;
    private final List<HttpInterceptor> interceptors = new ArrayList<>();
    private Function<ContextView, Map<String, String>> contextHeadersProvider;

    private Builder() {}

    public Builder webClient(WebClient webClient) {
      this.webClient = webClient;
      return this;
    }

    public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
      this.webClientBuilder = webClientBuilder;
      return this;
    }

    public Builder interceptor(HttpInterceptor interceptor) {
      if (interceptor != null) {
        interceptors.add(interceptor);
      }
      return this;
    }

    public Builder contextHeadersProvider(
        Function<ContextView, Map<String, String>> contextHeadersProvider) {
      this.contextHeadersProvider = contextHeadersProvider;
      return this;
    }

    public WebClientHttpClientAdapter build() {
      WebClient safeClient =
          (webClient != null)
              ? webClient
              : (webClientBuilder != null) ? webClientBuilder.build() : WebClient.builder().build();
      return new WebClientHttpClientAdapter(safeClient, interceptors, contextHeadersProvider);
    }
  }
}
