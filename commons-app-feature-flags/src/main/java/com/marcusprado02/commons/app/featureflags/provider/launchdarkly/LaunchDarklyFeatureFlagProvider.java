package com.marcusprado02.commons.app.featureflags.provider.launchdarkly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.LDClient;
import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagProvider;
import com.marcusprado02.commons.app.featureflags.FeatureFlagValue;

/**
 * LaunchDarkly feature flag provider.
 *
 * <p>Wraps the LaunchDarkly Java SDK and provides integration with the feature flag abstraction.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * LaunchDarklyFeatureFlagProvider provider =
 *     new LaunchDarklyFeatureFlagProvider("sdk-key-123");
 *
 * FeatureFlagContext context = FeatureFlagContext.builder()
 *     .userId("user123")
 *     .attribute("email", "user@example.com")
 *     .build();
 *
 * boolean enabled = provider.isEnabled("new-checkout", context);
 * }</pre>
 */
public class LaunchDarklyFeatureFlagProvider implements FeatureFlagProvider {

  private final LDClient client;
  private final ObjectMapper objectMapper;

  public LaunchDarklyFeatureFlagProvider(String sdkKey) {
    this.client = new LDClient(sdkKey);
    this.objectMapper = new ObjectMapper();
  }

  public LaunchDarklyFeatureFlagProvider(LDClient client) {
    this.client = client;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public boolean isEnabled(String featureKey, FeatureFlagContext context) {
    LDContext ldContext = toLDContext(context);
    return client.boolVariation(featureKey, ldContext, false);
  }

  @Override
  public FeatureFlagValue getValue(String featureKey, FeatureFlagContext context) {
    LDContext ldContext = toLDContext(context);
    LDValue value = client.jsonValueVariation(featureKey, ldContext, LDValue.ofNull());
    return toFeatureFlagValue(value);
  }

  @Override
  public void shutdown() {
    try {
      client.close();
    } catch (Exception e) {
      // Ignore shutdown errors
    }
  }

  private LDContext toLDContext(FeatureFlagContext context) {
    var builder =
        LDContext.builder(context.userId() != null ? context.userId() : "anonymous");

    // Add session ID as attribute
    if (context.sessionId() != null) {
      builder.set("sessionId", context.sessionId());
    }

    // Add custom attributes
    context
        .attributes()
        .forEach(
            (key, value) -> {
              if (value instanceof String s) {
                builder.set(key, s);
              } else if (value instanceof Boolean b) {
                builder.set(key, b);
              } else if (value instanceof Number n) {
                builder.set(key, n.doubleValue());
              } else {
                builder.set(key, value.toString());
              }
            });

    return builder.build();
  }

  private FeatureFlagValue toFeatureFlagValue(LDValue value) {
    return switch (value.getType()) {
      case BOOLEAN -> FeatureFlagValue.of(value.booleanValue());
      case NUMBER -> {
        if (value.isInt()) {
          yield FeatureFlagValue.of(value.intValue());
        } else {
          yield FeatureFlagValue.of(value.doubleValue());
        }
      }
      case STRING -> FeatureFlagValue.of(value.stringValue());
      case ARRAY, OBJECT -> {
        try {
          JsonNode jsonNode = objectMapper.readTree(value.toJsonString());
          yield FeatureFlagValue.of(jsonNode);
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse JSON value", e);
        }
      }
      case NULL -> FeatureFlagValue.of(false);
    };
  }
}
