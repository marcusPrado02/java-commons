package com.marcusprado02.commons.kernel.errors;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * RFC 7807 Problem Details for HTTP APIs representation.
 *
 * <p>This record provides compliance with RFC 7807 (Problem Details for HTTP APIs), which defines a
 * standard way to carry machine-readable details of errors in HTTP response bodies.
 *
 * <p>RFC 7807 fields:
 *
 * <ul>
 *   <li><b>type</b>: A URI reference that identifies the problem type
 *   <li><b>title</b>: A short, human-readable summary of the problem type
 *   <li><b>status</b>: The HTTP status code
 *   <li><b>detail</b>: A human-readable explanation specific to this occurrence
 *   <li><b>instance</b>: A URI reference identifying the specific occurrence
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * RFC7807ProblemDetail problemDetail = RFC7807ProblemDetail.from(problem)
 *     .instance(URI.create("/orders/123"))
 *     .status(404)
 *     .build();
 * }</pre>
 *
 * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807</a>
 */
public record RFC7807ProblemDetail(
    URI type,
    String title,
    Integer status,
    String detail,
    URI instance,
    Map<String, Object> extensions) {

  public RFC7807ProblemDetail {
    extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
  }

  /**
   * Creates a Builder from a Problem.
   *
   * @param problem the problem
   * @return a new builder
   */
  public static Builder from(Problem problem) {
    Objects.requireNonNull(problem, "problem");
    return builder()
        .type(URI.create("urn:problem-type:" + problem.code().value()))
        .title(problem.message())
        .detail(buildDetailFromProblem(problem))
        .extensions(problem.meta());
  }

  /**
   * Creates a new Builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  private static String buildDetailFromProblem(Problem problem) {
    if (problem.details() == null || problem.details().isEmpty()) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    for (ProblemDetail detail : problem.details()) {
      if (sb.length() > 0) {
        sb.append("; ");
      }
      sb.append(detail.field()).append(": ").append(detail.message());
    }
    return sb.toString();
  }

  /** Builder for RFC7807ProblemDetail. */
  public static final class Builder {
    private URI type;
    private String title;
    private Integer status;
    private String detail;
    private URI instance;
    private Map<String, Object> extensions = Map.of();

    private Builder() {}

    public Builder type(URI type) {
      this.type = type;
      return this;
    }

    public Builder type(String type) {
      this.type = URI.create(type);
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder status(Integer status) {
      this.status = status;
      return this;
    }

    public Builder detail(String detail) {
      this.detail = detail;
      return this;
    }

    public Builder instance(URI instance) {
      this.instance = instance;
      return this;
    }

    public Builder instance(String instance) {
      this.instance = URI.create(instance);
      return this;
    }

    public Builder extensions(Map<String, Object> extensions) {
      this.extensions = extensions;
      return this;
    }

    public RFC7807ProblemDetail build() {
      return new RFC7807ProblemDetail(type, title, status, detail, instance, extensions);
    }
  }
}
