package com.marcusprado02.commons.ports.http;

@FunctionalInterface
public interface HttpResponseBodyMapper<T> {

  T map(byte[] body, HttpResponse<byte[]> response);
}
