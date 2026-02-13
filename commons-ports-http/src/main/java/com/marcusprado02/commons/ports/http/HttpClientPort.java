package com.marcusprado02.commons.ports.http;

public interface HttpClientPort {

	HttpResponse<byte[]> execute(HttpRequest request);

	default <T> HttpResponse<T> execute(HttpRequest request, HttpBodyMapper<T> mapper) {
		HttpResponse<byte[]> raw = execute(request);
		T mapped = mapper.map(raw);
		return new HttpResponse<>(raw.statusCode(), raw.headers(), mapped);
	}
}
