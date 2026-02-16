package com.marcusprado02.commons.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceBundleMessageSourceTest {

  private MessageSource messageSource;

  @BeforeEach
  void setUp() {
    messageSource = new ResourceBundleMessageSource("test-messages");
  }

  @AfterEach
  void tearDown() {
    messageSource.reload();
  }

  @Test
  void getMessage_withValidKey_shouldReturnMessage() {
    Result<String> result = messageSource.getMessage("welcome", Locale.ENGLISH);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("Welcome!");
  }

  @Test
  void getMessage_withInvalidKey_shouldReturnError() {
    Result<String> result = messageSource.getMessage("invalid.key", Locale.ENGLISH);

    assertThat(result.isOk()).isFalse();
  }

  @Test
  void getMessage_withParameters_shouldSubstitute() {
    Result<String> result =
        messageSource.getMessage("welcome.user", Locale.ENGLISH, Map.of("name", "John"));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("Welcome, John!");
  }

  @Test
  void getMessageOrDefault_withInvalidKey_shouldReturnDefault() {
    String result =
        messageSource.getMessageOrDefault("invalid.key", Locale.ENGLISH, "Default message");

    assertThat(result).isEqualTo("Default message");
  }

  @Test
  void getPluralMessage_withOne_shouldReturnSingular() {
    Result<String> result = messageSource.getPluralMessage("items", 1, Locale.ENGLISH);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("1 item");
  }

  @Test
  void getPluralMessage_withMany_shouldReturnPlural() {
    Result<String> result = messageSource.getPluralMessage("items", 5, Locale.ENGLISH);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("5 items");
  }

  @Test
  void hasMessage_withValidKey_shouldReturnTrue() {
    boolean exists = messageSource.hasMessage("welcome", Locale.ENGLISH);

    assertThat(exists).isTrue();
  }

  @Test
  void hasMessage_withInvalidKey_shouldReturnFalse() {
    boolean exists = messageSource.hasMessage("invalid.key", Locale.ENGLISH);

    assertThat(exists).isFalse();
  }
}
