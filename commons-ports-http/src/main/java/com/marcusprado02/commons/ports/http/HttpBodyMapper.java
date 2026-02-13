package com.marcusprado02.commons.ports.http;

@FunctionalInterface
public interface HttpBodyMapper<T> {

  T map(HttpResponse<byte[]> response);
}
