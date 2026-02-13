package com.marcusprado02.commons.ports.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MultipartFormData {

  private final List<MultipartPart> parts;

  public MultipartFormData(List<MultipartPart> parts) {
    Objects.requireNonNull(parts, "parts must not be null");
    this.parts = List.copyOf(parts);
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<MultipartPart> parts() {
    return parts;
  }

  public static final class Builder {
    private final List<MultipartPart> parts = new ArrayList<>();

    private Builder() {}

    public Builder part(MultipartPart part) {
      if (part != null) {
        parts.add(part);
      }
      return this;
    }

    public Builder part(String name, byte[] content) {
      return part(MultipartPart.ofBytes(name, content));
    }

    public Builder file(String name, String filename, String contentType, byte[] content) {
      return part(MultipartPart.ofFile(name, filename, contentType, content));
    }

    public MultipartFormData build() {
      return new MultipartFormData(parts);
    }
  }
}
