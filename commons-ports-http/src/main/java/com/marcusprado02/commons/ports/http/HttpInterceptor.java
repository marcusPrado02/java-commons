package com.marcusprado02.commons.ports.http;

import java.util.Objects;

public interface HttpInterceptor {

	default HttpRequest onRequest(HttpRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		return request;
	}

	default HttpResponse<byte[]> onResponse(HttpRequest request, HttpResponse<byte[]> response) {
		Objects.requireNonNull(request, "request must not be null");
		Objects.requireNonNull(response, "response must not be null");
		return response;
	}
}
