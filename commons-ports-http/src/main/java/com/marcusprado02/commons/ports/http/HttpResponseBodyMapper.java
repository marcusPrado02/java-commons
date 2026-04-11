package com.marcusprado02.commons.ports.http;

/** Maps a raw byte-array HTTP response body to a typed value of {@code T}. */
@FunctionalInterface
public interface HttpResponseBodyMapper<T> {

  T map(byte[] body, HttpResponse<byte[]> response);
}
