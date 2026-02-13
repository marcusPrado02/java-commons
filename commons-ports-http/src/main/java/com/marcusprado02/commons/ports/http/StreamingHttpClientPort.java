package com.marcusprado02.commons.ports.http;

public interface StreamingHttpClientPort {

  HttpStreamingResponse executeStream(HttpRequest request);
}
