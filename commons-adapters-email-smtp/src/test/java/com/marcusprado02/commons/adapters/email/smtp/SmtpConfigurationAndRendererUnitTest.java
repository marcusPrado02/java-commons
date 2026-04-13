package com.marcusprado02.commons.adapters.email.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;

/** Unit tests for SmtpConfiguration validation branches and ThymeleafTemplateRenderer. */
class SmtpConfigurationAndRendererUnitTest {

  // ── SmtpConfiguration validation ─────────────────────────────────────────

  @Test
  void configShouldThrowWhenHostIsBlank() {
    assertThatThrownBy(
            () -> SmtpConfiguration.builder().host("  ").port(25).requireAuth(false).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("host");
  }

  @Test
  void configShouldThrowWhenPortIsZero() {
    assertThatThrownBy(
            () -> SmtpConfiguration.builder().host("localhost").port(0).requireAuth(false).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("port");
  }

  @Test
  void configShouldThrowWhenPortExceeds65535() {
    assertThatThrownBy(
            () ->
                SmtpConfiguration.builder()
                    .host("localhost")
                    .port(70000)
                    .requireAuth(false)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("port");
  }

  @Test
  void configShouldThrowWhenRequireAuthWithoutUsername() {
    assertThatThrownBy(
            () -> SmtpConfiguration.builder().host("localhost").port(25).requireAuth(true).build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void configShouldThrowWhenConnectionTimeoutIsNegative() {
    assertThatThrownBy(
            () ->
                SmtpConfiguration.builder()
                    .host("localhost")
                    .port(25)
                    .requireAuth(false)
                    .connectionTimeout(-1)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("connectionTimeout");
  }

  @Test
  void configShouldThrowWhenWriteTimeoutIsNegative() {
    assertThatThrownBy(
            () ->
                SmtpConfiguration.builder()
                    .host("localhost")
                    .port(25)
                    .requireAuth(false)
                    .writeTimeout(-1)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("writeTimeout");
  }

  @Test
  void toPropertiesShouldIncludeTlsSettingsWhenEnabled() {
    var config =
        SmtpConfiguration.builder()
            .host("smtp.example.com")
            .port(465)
            .requireAuth(false)
            .useTls(true)
            .build();

    var props = config.toProperties();

    assertThat(props.getProperty("mail.smtp.ssl.enable")).isEqualTo("true");
    assertThat(props.getProperty("mail.smtp.ssl.protocols")).contains("TLS");
  }

  @Test
  void toPropertiesShouldIncludeStartTlsSettingsWhenEnabled() {
    var config =
        SmtpConfiguration.builder()
            .host("smtp.example.com")
            .port(587)
            .requireAuth(false)
            .useStartTls(true)
            .build();

    var props = config.toProperties();

    assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
    assertThat(props.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
  }

  @Test
  void defaultsShouldCreateValidConfiguration() {
    var config = SmtpConfiguration.defaults();

    assertThat(config.host()).isEqualTo("localhost");
    assertThat(config.port()).isEqualTo(3025);
    assertThat(config.requireAuth()).isFalse();
  }

  // ── ThymeleafTemplateRenderer ─────────────────────────────────────────────

  @Test
  void builderWithDefaultsShouldCreateRenderer() {
    var renderer = ThymeleafTemplateRenderer.builder().build();

    assertThat(renderer).isNotNull();
  }

  @Test
  void withDefaultsShouldCreateRenderer() {
    var renderer = ThymeleafTemplateRenderer.withDefaults();

    assertThat(renderer).isNotNull();
  }

  @Test
  void builderWithCacheableTtlShouldCreateRenderer() {
    var renderer =
        ThymeleafTemplateRenderer.builder()
            .templatePrefix("/templates/")
            .templateSuffix(".html")
            .templateMode(TemplateMode.HTML)
            .characterEncoding("UTF-8")
            .cacheable(true)
            .cacheableTtlMs(3600_000L)
            .build();

    assertThat(renderer).isNotNull();
  }

  @Test
  void renderShouldThrowTemplateRenderingExceptionOnError() {
    var renderer = ThymeleafTemplateRenderer.withDefaults();

    assertThatThrownBy(() -> renderer.render("nonexistent-template", java.util.Map.of()))
        .isInstanceOf(TemplateRenderer.TemplateRenderingException.class)
        .hasMessageContaining("nonexistent-template");
  }
}
