package com.marcusprado02.commons.ports.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface HttpBody permits HttpBody.Bytes, HttpBody.FormUrlEncoded, HttpBody.Multipart {

  String contentType();

  record Bytes(byte[] value, String contentType) implements HttpBody {
    public Bytes {
      Objects.requireNonNull(value, "value must not be null");
      contentType =
          (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }

    public Bytes(byte[] value) {
      this(value, "application/octet-stream");
    }

    // record accessor `contentType()` satisfies HttpBody
  }

  record FormUrlEncoded(Map<String, List<String>> fields) implements HttpBody {
    public FormUrlEncoded {
      Objects.requireNonNull(fields, "fields must not be null");
      fields = deepUnmodifiableCopy(fields);
    }

    @Override
    public String contentType() {
      return "application/x-www-form-urlencoded";
    }
  }

  record Multipart(List<Part> parts) implements HttpBody {
    public Multipart {
      Objects.requireNonNull(parts, "parts must not be null");
      parts = List.copyOf(parts);
    }

    @Override
    public String contentType() {
      // boundary is provided by the underlying client implementation
      return "multipart/form-data";
    }

    public record Part(String name, String filename, String contentType, byte[] value) {
      public Part {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
        contentType = (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
      }

      public Part(String name, byte[] value) {
        this(name, null, "application/octet-stream", value);
      }
    }
  }

  private static Map<String, List<String>> deepUnmodifiableCopy(Map<String, List<String>> input) {
    Map<String, List<String>> safe = new LinkedHashMap<>();
    input.forEach(
        (key, values) ->
            safe.put(
                key,
                Collections.unmodifiableList(new ArrayList<>(values == null ? List.of() : values))));
    return Collections.unmodifiableMap(safe);
  }
}
