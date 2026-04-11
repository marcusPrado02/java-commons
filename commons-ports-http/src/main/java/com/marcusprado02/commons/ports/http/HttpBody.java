package com.marcusprado02.commons.ports.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** HTTP request body variants: raw bytes, form-encoded, or multipart. */
public sealed interface HttpBody
    permits HttpBody.Bytes, HttpBody.FormUrlEncoded, HttpBody.Multipart {

  /**
   * Returns the MIME content-type for this body.
   *
   * @return the content-type string
   */
  String contentType();

  /** Raw byte-array body with an explicit content-type. */
  record Bytes(byte[] value, String contentType) implements HttpBody {
    /** Validates fields and applies default content-type on construction. */
    public Bytes {
      Objects.requireNonNull(value, "value must not be null");
      contentType =
          (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }

    /**
     * Creates a {@code Bytes} body with {@code application/octet-stream} content-type.
     *
     * @param value raw byte content
     */
    public Bytes(byte[] value) {
      this(value, "application/octet-stream");
    }

    // record accessor `contentType()` satisfies HttpBody
  }

  /** URL-encoded form data body ({@code application/x-www-form-urlencoded}). */
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

  /** Multipart form body ({@code multipart/form-data}). */
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

    /** A single part within a multipart body, with optional filename and content-type. */
    public record Part(String name, String filename, String contentType, byte[] value) {
      /** Validates fields and applies default content-type on construction. */
      public Part {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
        contentType =
            (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;
      }

      /**
       * Creates a part with {@code application/octet-stream} content-type and no filename.
       *
       * @param name field name
       * @param value raw byte content
       */
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
                Collections.unmodifiableList(
                    new ArrayList<>(values == null ? List.of() : values))));
    return Collections.unmodifiableMap(safe);
  }
}
