package com.marcusprado02.commons.ports.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class HttpResponse<T> {

	private final int statusCode;
	private final Map<String, List<String>> headers;
	private final T body;

	public HttpResponse(int statusCode, Map<String, List<String>> headers, T body) {
		this.statusCode = statusCode;
		this.headers =
				Collections.unmodifiableMap(
						new LinkedHashMap<>(Objects.requireNonNull(headers, "headers must not be null")));
		this.body = body;
	}

	public int statusCode() {
		return statusCode;
	}

	public Map<String, List<String>> headers() {
		return headers;
	}

	public Optional<T> body() {
		return Optional.ofNullable(body);
	}

	public boolean isSuccessful() {
		return statusCode >= 200 && statusCode < 300;
	}
}
