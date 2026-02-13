package com.marcusprado02.commons.ports.http;

public interface HttpClientPort {

	HttpResponse<byte[]> execute(HttpRequest request);
}
