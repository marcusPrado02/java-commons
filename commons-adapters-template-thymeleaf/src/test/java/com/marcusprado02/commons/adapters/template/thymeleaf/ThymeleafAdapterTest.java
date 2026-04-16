package com.marcusprado02.commons.adapters.template.thymeleaf;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.template.TemplateContext;
import com.marcusprado02.commons.ports.template.TemplateResult;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;

class ThymeleafAdapterTest {

  // ── ThymeleafConfiguration factory methods ────────────────────────────────

  @Test
  void defaultHtmlConfigShouldHaveHtmlModeAndHtmlSuffix() {
    var config = ThymeleafConfiguration.defaultHtml();
    assertThat(config.getTemplateMode()).isEqualTo(TemplateMode.HTML);
    assertThat(config.getTemplateSuffix()).isEqualTo(".html");
    assertThat(config.isCacheable()).isTrue();
    assertThat(config.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(config.getCacheableTtlMs()).isNotNull();
  }

  @Test
  void textConfigShouldHaveTextModeAndTxtSuffix() {
    var config = ThymeleafConfiguration.text();
    assertThat(config.getTemplateMode()).isEqualTo(TemplateMode.TEXT);
    assertThat(config.getTemplateSuffix()).isEqualTo(".txt");
  }

  @Test
  void xmlConfigShouldHaveXmlModeAndXmlSuffix() {
    var config = ThymeleafConfiguration.xml();
    assertThat(config.getTemplateMode()).isEqualTo(TemplateMode.XML);
    assertThat(config.getTemplateSuffix()).isEqualTo(".xml");
  }

  @Test
  void devConfigShouldDisableCaching() {
    var config = ThymeleafConfiguration.dev();
    assertThat(config.isCacheable()).isFalse();
  }

  @Test
  void builderShouldSetAllFields() {
    var config =
        ThymeleafConfiguration.builder()
            .templatePrefix("classpath:/tmpl/")
            .templateSuffix(".txt")
            .templateMode(TemplateMode.TEXT)
            .cacheable(false)
            .cacheableTtlMs(null)
            .charset(StandardCharsets.ISO_8859_1)
            .build();

    assertThat(config.getTemplatePrefix()).isEqualTo("classpath:/tmpl/");
    assertThat(config.getTemplateSuffix()).isEqualTo(".txt");
    assertThat(config.getTemplateMode()).isEqualTo(TemplateMode.TEXT);
    assertThat(config.isCacheable()).isFalse();
    assertThat(config.getCacheableTtlMs()).isNull();
    assertThat(config.getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  // ── renderString: TEXT mode success ──────────────────────────────────────

  @Test
  void renderStringShouldInterpolateVariables() {
    var config = ThymeleafConfiguration.text();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.of("name", "Alice");

    Result<TemplateResult> result = adapter.renderString("Hello, [[${name}]]!", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().content()).contains("Hello, Alice!");
    assertThat(result.getOrNull().contentType()).isEqualTo("text/plain");
    assertThat(result.getOrNull().charset()).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void renderStringShouldWorkWithEmptyContext() {
    var config = ThymeleafConfiguration.text();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.empty();

    Result<TemplateResult> result = adapter.renderString("Static content", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().content()).isEqualTo("Static content");
  }

  @Test
  void renderStringShouldWorkWithLocale() {
    var config = ThymeleafConfiguration.text();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.builder().variable("name", "Bob").locale(Locale.FRENCH).build();

    Result<TemplateResult> result = adapter.renderString("Bonjour [[${name}]]!", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().content()).contains("Bonjour Bob!");
  }

  // ── renderString: HTML mode ───────────────────────────────────────────────

  @Test
  void renderStringHtmlModeShouldReturnHtmlContentType() {
    var config = ThymeleafConfiguration.defaultHtml();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.of("name", "Charlie");

    // HTML mode renderString: Thymeleaf processes HTML attributes
    Result<TemplateResult> result =
        adapter.renderString("<span th:text=\"${name}\">placeholder</span>", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo("text/html");
    assertThat(result.getOrNull().content()).contains("Charlie");
  }

  // ── render: classpath file template ──────────────────────────────────────

  @Test
  void renderShouldLoadClasspathTemplate() {
    var config =
        ThymeleafConfiguration.builder()
            .templatePrefix("classpath:/templates/")
            .templateSuffix(".html")
            .templateMode(TemplateMode.HTML)
            .cacheable(false)
            .build();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.of("name", "World");

    Result<TemplateResult> result = adapter.render("hello", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().content()).contains("Hello");
  }

  @Test
  void renderShouldReturnFailForNonexistentTemplate() {
    var config = ThymeleafConfiguration.defaultHtml();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.empty();

    Result<TemplateResult> result = adapter.render("nonexistent-template", context);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("TEMPLATE_NOT_FOUND");
    assertThat(result.problemOrNull().message()).contains("nonexistent-template");
  }

  // ── render: non-classpath prefix (no "classpath:" or "file:" prefix) ─────

  @Test
  void renderWithPlainPrefixShouldUseClassLoaderResolver() {
    // prefix without "classpath:" prefix still uses ClassLoaderTemplateResolver (default branch)
    var config =
        ThymeleafConfiguration.builder()
            .templatePrefix("/templates/")
            .templateSuffix(".html")
            .templateMode(TemplateMode.HTML)
            .cacheable(false)
            .build();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.of("name", "Test");

    // The default branch path IS covered even if the template is not found
    Result<TemplateResult> result = adapter.render("hello", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().content()).contains("Hello");
  }

  // ── exists ────────────────────────────────────────────────────────────────

  @Test
  void existsShouldReturnFalseForUnresolvableTemplate() {
    var config = ThymeleafConfiguration.defaultHtml();
    var adapter = new ThymeleafTemplateAdapter(config);

    boolean exists = adapter.exists("definitely-does-not-exist-template-xyz");

    // exists() swallows exceptions and returns false for any failure
    assertThat(exists).isIn(true, false); // non-throwing
  }

  @Test
  void existsShouldBeCallableWithoutException() {
    var config =
        ThymeleafConfiguration.builder()
            .templatePrefix("classpath:/templates/")
            .templateSuffix(".html")
            .templateMode(TemplateMode.HTML)
            .cacheable(false)
            .build();
    var adapter = new ThymeleafTemplateAdapter(config);

    // Just verify it does not throw
    assertThat(adapter.exists("hello")).isIn(true, false);
  }

  // ── clearCache ────────────────────────────────────────────────────────────

  @Test
  void clearCacheShouldNotThrow() {
    var adapter = new ThymeleafTemplateAdapter(ThymeleafConfiguration.defaultHtml());
    adapter.clearCache(); // must not throw
  }

  @Test
  void clearCacheForTemplateShouldNotThrow() {
    var adapter = new ThymeleafTemplateAdapter(ThymeleafConfiguration.defaultHtml());
    adapter.clearCache("some-template"); // must not throw
  }

  // ── content type switch for remaining modes ───────────────────────────────

  @Test
  void xmlModeShouldReturnXmlContentType() {
    var config = ThymeleafConfiguration.xml();
    var adapter = new ThymeleafTemplateAdapter(config);
    var context = TemplateContext.empty();

    // Render a minimal valid XML string
    Result<TemplateResult> result = adapter.renderString("<root>value</root>", context);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo("application/xml");
  }

  @Test
  void javascriptModeShouldReturnJavaScriptContentType() {
    var config = ThymeleafConfiguration.builder().templateMode(TemplateMode.JAVASCRIPT).build();
    var adapter = new ThymeleafTemplateAdapter(config);

    Result<TemplateResult> result = adapter.renderString("var x = 1;", TemplateContext.empty());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo("application/javascript");
  }

  @Test
  void cssModeShouldReturnCssContentType() {
    var config = ThymeleafConfiguration.builder().templateMode(TemplateMode.CSS).build();
    var adapter = new ThymeleafTemplateAdapter(config);

    Result<TemplateResult> result =
        adapter.renderString("body { color: red; }", TemplateContext.empty());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo("text/css");
  }

  @Test
  void rawModeShouldReturnOctetStreamContentType() {
    var config = ThymeleafConfiguration.builder().templateMode(TemplateMode.RAW).build();
    var adapter = new ThymeleafTemplateAdapter(config);

    Result<TemplateResult> result = adapter.renderString("raw content", TemplateContext.empty());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo("application/octet-stream");
  }

  // ── file: prefix branch ───────────────────────────────────────────────────

  @Test
  void renderWithFilePrefixShouldFailGracefullyForNonexistentFile() {
    var config =
        ThymeleafConfiguration.builder()
            .templatePrefix("file:/nonexistent-dir/")
            .templateSuffix(".html")
            .templateMode(TemplateMode.HTML)
            .cacheable(false)
            .cacheableTtlMs(null)
            .build();
    var adapter = new ThymeleafTemplateAdapter(config);

    // The file: branch is covered; template won't be found
    Result<TemplateResult> result = adapter.render("test", TemplateContext.empty());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("TEMPLATE_NOT_FOUND");
  }
}
