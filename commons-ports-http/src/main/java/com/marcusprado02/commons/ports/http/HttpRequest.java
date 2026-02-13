package com.marcusprado02.commons.ports.http;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class HttpRequest {

	private final String name;
	private final HttpMethod method;
	private final URI uri;
	private final Map<String, List<String>> headers;
	private final byte[] body;
	private final MultipartFormData multipartForm;
	private final Duration timeout;

	private HttpRequest(
			String name,
			HttpMethod method,
			URI uri,
			Map<String, List<String>> headers,
			byte[] body,
			MultipartFormData multipartForm,
			Duration timeout) {
		this.name = name;
		this.method = Objects.requireNonNull(method, "method must not be null");
		this.uri = Objects.requireNonNull(uri, "uri must not be null");
		this.headers = headers;
		this.body = body;
		this.multipartForm = multipartForm;
		this.timeout = timeout;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Optional<String> name() {
		return Optional.ofNullable(name);
	}

	public HttpMethod method() {
		return method;
	}

	public URI uri() {
		return uri;
	}

	public Map<String, List<String>> headers() {
		return headers;
	}

	public Optional<byte[]> body() {
		return Optional.ofNullable(body);
	}

	public Optional<MultipartFormData> multipartForm() {
		return Optional.ofNullable(multipartForm);
	}

	public Optional<Duration> timeout() {
		return Optional.ofNullable(timeout);
	}

	public static final class Builder {
		private String name;
		private HttpMethod method;
		private URI uri;
		private final Map<String, List<String>> headers = new LinkedHashMap<>();
		private byte[] body;
		private MultipartFormData multipartForm;
		private Duration timeout;

		private Builder() {}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder method(HttpMethod method) {
			this.method = method;
			return this;
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder header(String name, String value) {
			Objects.requireNonNull(name, "header name must not be null");
			Objects.requireNonNull(value, "header value must not be null");
			headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
			return this;
		}

		public Builder headers(Map<String, List<String>> headers) {
			Objects.requireNonNull(headers, "headers must not be null");
			this.headers.clear();
			headers.forEach(
					(key, values) -> this.headers.put(key, new ArrayList<>(values == null ? List.of() : values)));
			return this;
		}

		public Builder auth(HttpAuth auth) {
			Objects.requireNonNull(auth, "auth must not be null");
			return header("Authorization", auth.asAuthorizationHeaderValue());
		}

		public Builder body(byte[] body) {
			this.body = body;
			this.multipartForm = null;
			return this;
		}

		public Builder multipartForm(MultipartFormData multipartForm) {
			this.multipartForm = multipartForm;
			this.body = null;
			return this;
		}

		public Builder multipartPart(MultipartPart part) {
			MultipartFormData.Builder formBuilder =
					(multipartForm == null) ? MultipartFormData.builder() : MultipartFormData.builder();
			if (multipartForm != null) {
				multipartForm.parts().forEach(formBuilder::part);
			}
			formBuilder.part(part);
			return multipartForm(formBuilder.build());
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public HttpRequest build() {
			HttpMethod safeMethod = Objects.requireNonNull(method, "method must not be null");
			URI safeUri = Objects.requireNonNull(uri, "uri must not be null");

			Map<String, List<String>> safeHeaders = new LinkedHashMap<>();
			headers.forEach(
					(key, values) ->
							safeHeaders.put(
									key,
									Collections.unmodifiableList(
											new ArrayList<>(values == null ? List.of() : values))));

			return new HttpRequest(
					name,
					safeMethod,
					safeUri,
					Collections.unmodifiableMap(safeHeaders),
					body,
					multipartForm,
					timeout);
		}
	}
}
