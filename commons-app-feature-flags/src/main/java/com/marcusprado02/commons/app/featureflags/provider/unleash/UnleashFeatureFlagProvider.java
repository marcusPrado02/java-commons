package com.marcusprado02.commons.app.featureflags.provider.unleash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagProvider;
import com.marcusprado02.commons.app.featureflags.FeatureFlagValue;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.Variant;
import java.util.Optional;

/**
 * Unleash feature flag provider.
 *
 * <p>Wraps the Unleash Java SDK and provides integration with the feature flag abstraction.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * UnleashConfig config = UnleashConfig.builder()
 *     .appName("my-app")
 *     .instanceId("instance-1")
 *     .unleashAPI("https://unleash.example.com/api/")
 *     .apiKey("*:*.unleash-api-key")
 *     .build();
 *
 * Unleash unleash = new DefaultUnleash(config);
 * UnleashFeatureFlagProvider provider = new UnleashFeatureFlagProvider(unleash);
 *
 * FeatureFlagContext context = FeatureFlagContext.forUser("user123");
 * boolean enabled = provider.isEnabled("new-checkout", context);
 * }</pre>
 */
public class UnleashFeatureFlagProvider implements FeatureFlagProvider {

  private final Unleash unleash;
  private final ObjectMapper objectMapper;

  public UnleashFeatureFlagProvider(Unleash unleash) {
    this.unleash = unleash;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public boolean isEnabled(String featureKey, FeatureFlagContext context) {
    UnleashContext unleashContext = toUnleashContext(context);
    return unleash.isEnabled(featureKey, unleashContext);
  }

  @Override
  public FeatureFlagValue getValue(String featureKey, FeatureFlagContext context) {
    UnleashContext unleashContext = toUnleashContext(context);
    Variant variant = unleash.getVariant(featureKey, unleashContext);

    if (variant == null || !variant.isEnabled()) {
      return FeatureFlagValue.of(false);
    }

    return toFeatureFlagValue(variant);
  }

  private UnleashContext toUnleashContext(FeatureFlagContext context) {
    var builder = UnleashContext.builder();

    // Set user ID
    if (context.userId() != null) {
      builder.userId(context.userId());
    }

    // Set session ID
    if (context.sessionId() != null) {
      builder.sessionId(context.sessionId());
    }

    // Add custom properties
    context
        .attributes()
        .forEach(
            (key, value) -> {
              builder.addProperty(key, value.toString());
            });

    return builder.build();
  }

  private FeatureFlagValue toFeatureFlagValue(Variant variant) {
    var payload = variant.getPayload();

    if (payload.isEmpty()) {
      // No payload, just return enabled status
      return FeatureFlagValue.of(variant.isEnabled());
    }

    String payloadValue = payload.get().getValue();

    // Try to parse as different types
    try {
      // Try boolean
      if (payloadValue.equalsIgnoreCase("true") || payloadValue.equalsIgnoreCase("false")) {
        return FeatureFlagValue.of(Boolean.parseBoolean(payloadValue));
      }

      // Try number
      if (payloadValue.matches("-?\\d+")) {
        return FeatureFlagValue.of(Long.parseLong(payloadValue));
      }

      if (payloadValue.matches("-?\\d+\\.\\d+")) {
        return FeatureFlagValue.of(Double.parseDouble(payloadValue));
      }

      // Try JSON
      if (payloadValue.startsWith("{") || payloadValue.startsWith("[")) {
        JsonNode jsonNode = objectMapper.readTree(payloadValue);
        return FeatureFlagValue.of(jsonNode);
      }

      // Default: string
      return FeatureFlagValue.of(payloadValue);
    } catch (Exception e) {
      // If parsing fails, return as string
      return FeatureFlagValue.of(payloadValue);
    }
  }
}
