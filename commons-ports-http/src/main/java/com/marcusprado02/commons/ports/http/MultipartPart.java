package com.marcusprado02.commons.ports.http;

import java.util.Objects;
import java.util.Optional;

public record MultipartPart(
    String name, Optional<String> filename, Optional<String> contentType, byte[] content) {

  public MultipartPart {
    Objects.requireNonNull(name, "name must not be null");
    filename = (filename == null) ? Optional.empty() : filename;
    contentType = (contentType == null) ? Optional.empty() : contentType;
    Objects.requireNonNull(content, "content must not be null");
  }

  public static MultipartPart ofBytes(String name, byte[] content) {
    return new MultipartPart(name, Optional.empty(), Optional.empty(), content);
  }

  public static MultipartPart ofFile(String name, String filename, String contentType, byte[] content) {
    return new MultipartPart(name, Optional.of(filename), Optional.of(contentType), content);
  }
}
