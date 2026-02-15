package com.marcusprado02.commons.app.featureflags.spring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FeatureFlagAspectTest {

  @Mock private FeatureFlagService featureFlagService;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private MethodSignature signature;

  private FeatureFlagAspect aspect;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    aspect = new FeatureFlagAspect(featureFlagService);
    when(joinPoint.getSignature()).thenReturn(signature);
  }

  @Test
  void shouldProceedWhenFeatureEnabled() throws Throwable {
    FeatureFlag annotation =
        createAnnotation(
            "feature-key", FeatureFlag.FallbackStrategy.THROW_EXCEPTION, "", "");

    when(featureFlagService.isEnabled(eq("feature-key"), any(FeatureFlagContext.class)))
        .thenReturn(true);
    when(joinPoint.proceed()).thenReturn("result");

    Object result = aspect.aroundFeatureFlaggedMethod(joinPoint, annotation);

    assertThat(result).isEqualTo("result");
    verify(joinPoint).proceed();
  }

  @Test
  void shouldThrowExceptionWhenFeatureDisabledAndThrowStrategy() {
    FeatureFlag annotation =
        createAnnotation(
            "feature-key", FeatureFlag.FallbackStrategy.THROW_EXCEPTION, "", "");

    when(featureFlagService.isEnabled(eq("feature-key"), any(FeatureFlagContext.class)))
        .thenReturn(false);

    assertThatThrownBy(() -> aspect.aroundFeatureFlaggedMethod(joinPoint, annotation))
        .isInstanceOf(FeatureFlagDisabledException.class)
        .hasMessageContaining("feature-key");
  }

  @Test
  void shouldReturnNullWhenFeatureDisabledAndReturnNullStrategy() throws Throwable {
    FeatureFlag annotation =
        createAnnotation("feature-key", FeatureFlag.FallbackStrategy.RETURN_NULL, "", "");

    when(featureFlagService.isEnabled(eq("feature-key"), any(FeatureFlagContext.class)))
        .thenReturn(false);

    Object result = aspect.aroundFeatureFlaggedMethod(joinPoint, annotation);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnDefaultValueWhenFeatureDisabledAndReturnDefaultStrategy()
      throws Throwable {
    FeatureFlag annotation =
        createAnnotation(
            "feature-key", FeatureFlag.FallbackStrategy.RETURN_DEFAULT, "", "");

    when(featureFlagService.isEnabled(eq("feature-key"), any(FeatureFlagContext.class)))
        .thenReturn(false);
    when(signature.getReturnType()).thenReturn((Class) int.class);

    Object result = aspect.aroundFeatureFlaggedMethod(joinPoint, annotation);

    assertThat(result).isEqualTo(0);
  }

  private FeatureFlag createAnnotation(
      String key, FeatureFlag.FallbackStrategy fallback, String fallbackMethod, String userId) {
    return new FeatureFlag() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return FeatureFlag.class;
      }

      @Override
      public String key() {
        return key;
      }

      @Override
      public FallbackStrategy fallback() {
        return fallback;
      }

      @Override
      public String fallbackMethod() {
        return fallbackMethod;
      }

      @Override
      public String userId() {
        return userId;
      }
    };
  }
}
