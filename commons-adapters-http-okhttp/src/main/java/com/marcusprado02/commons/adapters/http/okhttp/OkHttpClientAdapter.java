package com.marcusprado02.commons.adapters.http.okhttp;

import com.marcusprado02.commons.app.observability.TracerFacade;
import com.marcusprado02.commons.app.resilience.NoopResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import com.marcusprado02.commons.ports.http.HttpClientPort;
import com.marcusprado02.commons.ports.http.HttpInterceptor;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import com.marcusprado02.commons.ports.http.HttpResponse;
import com.marcusprado02.commons.ports.http.HttpStreamingResponse;
import com.marcusprado02.commons.ports.http.MultipartFormData;
import com.marcusprado02.commons.ports.http.MultipartPart;
import com.marcusprado02.commons.ports.http.StreamingHttpClientPort;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MultipartBody;

public final class OkHttpClientAdapter implements HttpClientPort, StreamingHttpClientPort {

  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.get("application/octet-stream");

  private final OkHttpClient client;
  private final ResilienceExecutor resilienceExecutor;
  private final ResiliencePolicySet resiliencePolicies;
  private final TracerFacade tracerFacade;
  private final List<HttpInterceptor> interceptors;

  private OkHttpClientAdapter(
      OkHttpClient client,
      ResilienceExecutor resilienceExecutor,
      ResiliencePolicySet resiliencePolicies,
      TracerFacade tracerFacade,
      List<HttpInterceptor> interceptors) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    this.resilienceExecutor =
        (resilienceExecutor == null) ? new NoopResilienceExecutor() : resilienceExecutor;
    this.resiliencePolicies =
        (resiliencePolicies == null)
            ? new ResiliencePolicySet(null, null, null, null)
            : resiliencePolicies;
    this.tracerFacade = tracerFacade;
    this.interceptors = List.copyOf(interceptors == null ? List.of() : interceptors);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public HttpResponse<byte[]> execute(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    HttpRequest interceptedRequest = applyRequestInterceptors(request);

    String operationName =
        interceptedRequest
            .name()
            .filter(s -> !s.isBlank())
            .orElseGet(() -> defaultOperationName(interceptedRequest));

    return inSpan(
        operationName,
        () ->
            resilienceExecutor.supply(
                operationName,
                resiliencePolicies,
                () -> {
                  HttpResponse<byte[]> response = doExecute(interceptedRequest);
                  return applyResponseInterceptors(interceptedRequest, response);
                }));
  }

  private HttpResponse<byte[]> doExecute(HttpRequest request) {
    OkHttpClient effectiveClient = applyPerRequestTimeout(client, request.timeout());
    Request okRequest = toOkHttpRequest(request);

    try (Response okResponse = effectiveClient.newCall(okRequest).execute()) {
      Map<String, List<String>> headers = toHeaderMap(okResponse.headers());
      byte[] body = readBody(okResponse.body());
      return new HttpResponse<>(okResponse.code(), headers, body);
    } catch (IOException ex) {
      throw new RuntimeException("HTTP request failed", ex);
    }
  }

  @Override
  public HttpStreamingResponse executeStream(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    HttpRequest interceptedRequest = applyRequestInterceptors(request);
    String operationName =
        interceptedRequest
            .name()
            .filter(s -> !s.isBlank())
            .orElseGet(() -> defaultOperationName(interceptedRequest));

    return inSpan(
        operationName,
        () ->
            resilienceExecutor.supply(
                operationName,
                resiliencePolicies,
                () -> {
                  OkHttpClient effectiveClient = applyPerRequestTimeout(client, interceptedRequest.timeout());
                  Request okRequest = toOkHttpRequest(interceptedRequest);
                  try {
                    Response okResponse = effectiveClient.newCall(okRequest).execute();
                    Map<String, List<String>> headers = toHeaderMap(okResponse.headers());
                    ResponseBody body = okResponse.body();
                    if (body == null) {
                      okResponse.close();
                      throw new RuntimeException("HTTP response body is null");
                    }
                    return new HttpStreamingResponse(
                        okResponse.code(), headers, body.byteStream(), okResponse::close);
                  } catch (IOException ex) {
                    throw new RuntimeException("HTTP request failed", ex);
                  }
                }));
  }

  private OkHttpClient applyPerRequestTimeout(OkHttpClient base, Optional<Duration> timeout) {
    if (timeout.isEmpty() || timeout.get().isNegative() || timeout.get().isZero()) {
      return base;
    }
    return base.newBuilder().callTimeout(timeout.get()).build();
  }

  private Request toOkHttpRequest(HttpRequest request) {
    Request.Builder builder = new Request.Builder().url(request.uri().toString());

    request
        .headers()
        .forEach(
            (key, values) -> {
              if (values == null) {
                return;
              }
              for (String value : values) {
                if (value != null) {
                  builder.addHeader(key, value);
                }
              }
            });

    String method = request.method().name();
    RequestBody body = buildRequestBody(request);
    builder.method(method, body);
    return builder.build();
  }

  private RequestBody buildRequestBody(HttpRequest request) {
    boolean mayHaveBody =
        request.method() == HttpMethod.POST
            || request.method() == HttpMethod.PUT
            || request.method() == HttpMethod.PATCH
            || request.method() == HttpMethod.DELETE;

    if (!mayHaveBody) {
      return null;
    }

    if (request.multipartForm().isPresent()) {
      MultipartFormData form = request.multipartForm().orElseThrow();
      MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
      for (MultipartPart part : form.parts()) {
        if (part.filename().isPresent()) {
          MediaType contentType =
              part.contentType().map(MediaType::get).orElse(DEFAULT_MEDIA_TYPE);
          builder.addFormDataPart(
              part.name(),
              part.filename().orElseThrow(),
              RequestBody.create(part.content(), contentType));
        } else {
          builder.addFormDataPart(
              part.name(),
              new String(part.content(), java.nio.charset.StandardCharsets.UTF_8));
        }
      }
      return builder.build();
    }

    byte[] safe = request.body().orElse(new byte[0]);
    return RequestBody.create(safe, DEFAULT_MEDIA_TYPE);
  }



  private byte[] readBody(ResponseBody body) throws IOException {
    if (body == null) {
      return new byte[0];
    }
    return body.bytes();
  }

  private Map<String, List<String>> toHeaderMap(Headers headers) {
    Map<String, List<String>> map = new LinkedHashMap<>();
    for (String name : headers.names()) {
      map.put(name, new ArrayList<>(headers.values(name)));
    }
    return map;
  }

  private HttpRequest applyRequestInterceptors(HttpRequest request) {
    HttpRequest current = request;
    for (HttpInterceptor interceptor : interceptors) {
      current = Objects.requireNonNull(interceptor.onRequest(current), "interceptor returned null request");
    }
    return current;
  }

  private HttpResponse<byte[]> applyResponseInterceptors(HttpRequest request, HttpResponse<byte[]> response) {
    HttpResponse<byte[]> current = response;
    for (HttpInterceptor interceptor : interceptors) {
      current =
          Objects.requireNonNull(
              interceptor.onResponse(request, current), "interceptor returned null response");
    }
    return current;
  }

  private <T> T inSpan(String spanName, java.util.function.Supplier<T> action) {
    if (tracerFacade == null) {
      return action.get();
    }
    return tracerFacade.inSpan(spanName, action);
  }

  private String defaultOperationName(HttpRequest request) {
    String host = request.uri().getHost();
    String safeHost = (host == null || host.isBlank()) ? "unknown-host" : host;
    return "http." + request.method().name().toLowerCase() + "." + safeHost;
  }

  public static final class Builder {
    private OkHttpClient client;
    private ResilienceExecutor resilienceExecutor;
    private ResiliencePolicySet resiliencePolicies;
    private TracerFacade tracerFacade;
    private final List<HttpInterceptor> interceptors = new ArrayList<>();

    private Duration connectTimeout;
    private Duration readTimeout;
    private Duration writeTimeout;
    private Duration callTimeout;

    private Builder() {}

    public Builder client(OkHttpClient client) {
      this.client = client;
      return this;
    }

    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    public Builder readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public Builder writeTimeout(Duration writeTimeout) {
      this.writeTimeout = writeTimeout;
      return this;
    }

    public Builder callTimeout(Duration callTimeout) {
      this.callTimeout = callTimeout;
      return this;
    }

    public Builder resilienceExecutor(ResilienceExecutor resilienceExecutor) {
      this.resilienceExecutor = resilienceExecutor;
      return this;
    }

    public Builder resiliencePolicies(ResiliencePolicySet resiliencePolicies) {
      this.resiliencePolicies = resiliencePolicies;
      return this;
    }

    public Builder tracerFacade(TracerFacade tracerFacade) {
      this.tracerFacade = tracerFacade;
      return this;
    }

    public Builder interceptor(HttpInterceptor interceptor) {
      if (interceptor != null) {
        this.interceptors.add(interceptor);
      }
      return this;
    }

    public OkHttpClientAdapter build() {
      OkHttpClient okClient = (client == null) ? new OkHttpClient() : client;
      OkHttpClient.Builder builder = okClient.newBuilder();

      if (connectTimeout != null) {
        builder.connectTimeout(connectTimeout);
      }
      if (readTimeout != null) {
        builder.readTimeout(readTimeout);
      }
      if (writeTimeout != null) {
        builder.writeTimeout(writeTimeout);
      }
      if (callTimeout != null) {
        builder.callTimeout(callTimeout);
      }

      return new OkHttpClientAdapter(
          builder.build(), resilienceExecutor, resiliencePolicies, tracerFacade, interceptors);
    }
  }
}
