package com.marcusprado02.commons.adapters.tracing.jaeger;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for adding semantic attributes to spans.
 *
 * <p>Provides convenience methods for adding common attributes following OpenTelemetry semantic
 * conventions and Jaeger best practices.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Span span = tracer.spanBuilder("database-query").startSpan();
 *
 * SpanAttributes.builder()
 *     .component("postgresql")
 *     .dbSystem("postgresql")
 *     .dbStatement("SELECT * FROM users WHERE id = ?")
 *     .dbUser("app_user")
 *     .addTo(span);
 * }</pre>
 */
public final class SpanAttributes {

  // Semantic convention attribute keys
  private static final AttributeKey<String> COMPONENT = AttributeKey.stringKey("component");
  private static final AttributeKey<String> ERROR_KIND = AttributeKey.stringKey("error.kind");
  private static final AttributeKey<String> ERROR_MESSAGE = AttributeKey.stringKey("error.message");
  private static final AttributeKey<String> ERROR_STACK = AttributeKey.stringKey("error.stack");

  // Database attributes
  private static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system");
  private static final AttributeKey<String> DB_CONNECTION_STRING =
      AttributeKey.stringKey("db.connection_string");
  private static final AttributeKey<String> DB_USER = AttributeKey.stringKey("db.user");
  private static final AttributeKey<String> DB_NAME = AttributeKey.stringKey("db.name");
  private static final AttributeKey<String> DB_STATEMENT = AttributeKey.stringKey("db.statement");
  private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");

  // HTTP attributes
  private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
  private static final AttributeKey<String> HTTP_URL = AttributeKey.stringKey("http.url");
  private static final AttributeKey<Long> HTTP_STATUS_CODE =
      AttributeKey.longKey("http.status_code");
  private static final AttributeKey<String> HTTP_USER_AGENT =
      AttributeKey.stringKey("http.user_agent");

  // Messaging attributes
  private static final AttributeKey<String> MESSAGE_SYSTEM =
      AttributeKey.stringKey("messaging.system");
  private static final AttributeKey<String> MESSAGE_DESTINATION =
      AttributeKey.stringKey("messaging.destination");
  private static final AttributeKey<String> MESSAGE_OPERATION =
      AttributeKey.stringKey("messaging.operation");
  private static final AttributeKey<String> MESSAGE_ID =
      AttributeKey.stringKey("messaging.message_id");

  // RPC attributes
  private static final AttributeKey<String> RPC_SYSTEM = AttributeKey.stringKey("rpc.system");
  private static final AttributeKey<String> RPC_SERVICE = AttributeKey.stringKey("rpc.service");
  private static final AttributeKey<String> RPC_METHOD = AttributeKey.stringKey("rpc.method");

  private SpanAttributes() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Marks a span as representing an error.
   *
   * @param span the span to mark as error
   * @param throwable the throwable that caused the error
   */
  public static void recordError(Span span, Throwable throwable) {
    Objects.requireNonNull(span, "span must not be null");
    Objects.requireNonNull(throwable, "throwable must not be null");

    span.setStatus(StatusCode.ERROR, throwable.getMessage());
    span.recordException(throwable);
  }

  /**
   * Marks a span as representing an error with a custom message.
   *
   * @param span the span to mark as error
   * @param errorMessage the error message
   */
  public static void recordError(Span span, String errorMessage) {
    Objects.requireNonNull(span, "span must not be null");
    Objects.requireNonNull(errorMessage, "errorMessage must not be null");

    span.setStatus(StatusCode.ERROR, errorMessage);
    span.setAttribute(ERROR_MESSAGE, errorMessage);
  }

  /**
   * Sets the component name for a span.
   *
   * @param span the span
   * @param component the component name (e.g., "postgresql", "redis", "http-client")
   */
  public static void setComponent(Span span, String component) {
    Objects.requireNonNull(span, "span must not be null");
    if (component != null && !component.isBlank()) {
      span.setAttribute(COMPONENT, component);
    }
  }

  /** Builder for constructing span attributes. */
  public static final class Builder {
    private final Map<AttributeKey<?>, Object> attributes = new HashMap<>();

    private Builder() {}

    /**
     * Sets the component name.
     *
     * @param component component name (e.g., "postgresql", "redis")
     * @return this builder
     */
    public Builder component(String component) {
      if (component != null && !component.isBlank()) {
        attributes.put(COMPONENT, component);
      }
      return this;
    }

    // Database attributes

    /**
     * Sets the database system identifier.
     *
     * @param system database system (e.g., "postgresql", "mysql", "mongodb")
     * @return this builder
     */
    public Builder dbSystem(String system) {
      if (system != null && !system.isBlank()) {
        attributes.put(DB_SYSTEM, system);
      }
      return this;
    }

    /**
     * Sets the database name.
     *
     * @param name database name
     * @return this builder
     */
    public Builder dbName(String name) {
      if (name != null && !name.isBlank()) {
        attributes.put(DB_NAME, name);
      }
      return this;
    }

    /**
     * Sets the database statement (SQL query).
     *
     * @param statement the database statement
     * @return this builder
     */
    public Builder dbStatement(String statement) {
      if (statement != null && !statement.isBlank()) {
        attributes.put(DB_STATEMENT, statement);
      }
      return this;
    }

    /**
     * Sets the database operation type.
     *
     * @param operation operation type (e.g., "SELECT", "INSERT", "UPDATE", "DELETE")
     * @return this builder
     */
    public Builder dbOperation(String operation) {
      if (operation != null && !operation.isBlank()) {
        attributes.put(DB_OPERATION, operation);
      }
      return this;
    }

    /**
     * Sets the database user.
     *
     * @param user database user
     * @return this builder
     */
    public Builder dbUser(String user) {
      if (user != null && !user.isBlank()) {
        attributes.put(DB_USER, user);
      }
      return this;
    }

    /**
     * Sets the database connection string (sensitive - typically excluded from tracing).
     *
     * @param connectionString connection string
     * @return this builder
     */
    public Builder dbConnectionString(String connectionString) {
      if (connectionString != null && !connectionString.isBlank()) {
        attributes.put(DB_CONNECTION_STRING, connectionString);
      }
      return this;
    }

    // HTTP attributes

    /**
     * Sets the HTTP method.
     *
     * @param method HTTP method (e.g., "GET", "POST", "PUT", "DELETE")
     * @return this builder
     */
    public Builder httpMethod(String method) {
      if (method != null && !method.isBlank()) {
        attributes.put(HTTP_METHOD, method);
      }
      return this;
    }

    /**
     * Sets the HTTP URL.
     *
     * @param url the URL
     * @return this builder
     */
    public Builder httpUrl(String url) {
      if (url != null && !url.isBlank()) {
        attributes.put(HTTP_URL, url);
      }
      return this;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param statusCode HTTP status code
     * @return this builder
     */
    public Builder httpStatusCode(int statusCode) {
      attributes.put(HTTP_STATUS_CODE, (long) statusCode);
      return this;
    }

    /**
     * Sets the HTTP user agent.
     *
     * @param userAgent user agent string
     * @return this builder
     */
    public Builder httpUserAgent(String userAgent) {
      if (userAgent != null && !userAgent.isBlank()) {
        attributes.put(HTTP_USER_AGENT, userAgent);
      }
      return this;
    }

    // Messaging attributes

    /**
     * Sets the messaging system identifier.
     *
     * @param system messaging system (e.g., "kafka", "rabbitmq", "sqs")
     * @return this builder
     */
    public Builder messagingSystem(String system) {
      if (system != null && !system.isBlank()) {
        attributes.put(MESSAGE_SYSTEM, system);
      }
      return this;
    }

    /**
     * Sets the messaging destination (topic/queue name).
     *
     * @param destination destination name
     * @return this builder
     */
    public Builder messagingDestination(String destination) {
      if (destination != null && !destination.isBlank()) {
        attributes.put(MESSAGE_DESTINATION, destination);
      }
      return this;
    }

    /**
     * Sets the messaging operation.
     *
     * @param operation operation (e.g., "send", "receive", "process")
     * @return this builder
     */
    public Builder messagingOperation(String operation) {
      if (operation != null && !operation.isBlank()) {
        attributes.put(MESSAGE_OPERATION, operation);
      }
      return this;
    }

    /**
     * Sets the message ID.
     *
     * @param messageId message ID
     * @return this builder
     */
    public Builder messagingMessageId(String messageId) {
      if (messageId != null && !messageId.isBlank()) {
        attributes.put(MESSAGE_ID, messageId);
      }
      return this;
    }

    // RPC attributes

    /**
     * Sets the RPC system identifier.
     *
     * @param system RPC system (e.g., "grpc", "thrift")
     * @return this builder
     */
    public Builder rpcSystem(String system) {
      if (system != null && !system.isBlank()) {
        attributes.put(RPC_SYSTEM, system);
      }
      return this;
    }

    /**
     * Sets the RPC service name.
     *
     * @param service service name
     * @return this builder
     */
    public Builder rpcService(String service) {
      if (service != null && !service.isBlank()) {
        attributes.put(RPC_SERVICE, service);
      }
      return this;
    }

    /**
     * Sets the RPC method name.
     *
     * @param method method name
     * @return this builder
     */
    public Builder rpcMethod(String method) {
      if (method != null && !method.isBlank()) {
        attributes.put(RPC_METHOD, method);
      }
      return this;
    }

    // Generic attributes

    /**
     * Adds a custom string attribute.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this builder
     */
    public Builder attr(String key, String value) {
      if (key != null && !key.isBlank() && value != null) {
        attributes.put(AttributeKey.stringKey(key), value);
      }
      return this;
    }

    /**
     * Adds a custom long attribute.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this builder
     */
    public Builder attr(String key, long value) {
      if (key != null && !key.isBlank()) {
        attributes.put(AttributeKey.longKey(key), value);
      }
      return this;
    }

    /**
     * Adds a custom boolean attribute.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this builder
     */
    public Builder attr(String key, boolean value) {
      if (key != null && !key.isBlank()) {
        attributes.put(AttributeKey.booleanKey(key), value);
      }
      return this;
    }

    /**
     * Adds a custom double attribute.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this builder
     */
    public Builder attr(String key, double value) {
      if (key != null && !key.isBlank()) {
        attributes.put(AttributeKey.doubleKey(key), value);
      }
      return this;
    }

    /**
     * Builds the attributes and adds them to the given span.
     *
     * @param span the span to add attributes to
     */
    @SuppressWarnings("unchecked")
    public void addTo(Span span) {
      Objects.requireNonNull(span, "span must not be null");
      attributes.forEach(
          (key, value) -> {
            if (value instanceof String) {
              span.setAttribute((AttributeKey<String>) key, (String) value);
            } else if (value instanceof Long) {
              span.setAttribute((AttributeKey<Long>) key, (Long) value);
            } else if (value instanceof Boolean) {
              span.setAttribute((AttributeKey<Boolean>) key, (Boolean) value);
            } else if (value instanceof Double) {
              span.setAttribute((AttributeKey<Double>) key, (Double) value);
            }
          });
    }

    /**
     * Builds the attributes as an {@link Attributes} object.
     *
     * @return the attributes
     */
    @SuppressWarnings("unchecked")
    public Attributes build() {
      AttributesBuilder builder = Attributes.builder();
      attributes.forEach(
          (key, value) -> {
            if (value instanceof String) {
              builder.put((AttributeKey<String>) key, (String) value);
            } else if (value instanceof Long) {
              builder.put((AttributeKey<Long>) key, (Long) value);
            } else if (value instanceof Boolean) {
              builder.put((AttributeKey<Boolean>) key, (Boolean) value);
            } else if (value instanceof Double) {
              builder.put((AttributeKey<Double>) key, (Double) value);
            }
          });
      return builder.build();
    }
  }
}
