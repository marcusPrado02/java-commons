package com.marcusprado02.commons.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LocaleContextTest {

  @AfterEach
  void tearDown() {
    LocaleContext.clear();
  }

  @Test
  void setLocale_shouldSetForCurrentThread() {
    LocaleContext.setLocale(Locale.FRENCH);

    Locale retrieved = LocaleContext.getLocale();

    assertThat(retrieved).isEqualTo(Locale.FRENCH);
  }

  @Test
  void getLocale_withoutSetting_shouldReturnDefault() {
    Locale locale = LocaleContext.getLocale();

    assertThat(locale).isEqualTo(LocaleContext.getDefaultLocale());
  }

  @Test
  void clear_shouldRemoveLocale() {
    LocaleContext.setLocale(Locale.GERMAN);

    LocaleContext.clear();
    Locale retrieved = LocaleContext.getLocale();

    assertThat(retrieved).isEqualTo(LocaleContext.getDefaultLocale());
  }

  @Test
  void getLocaleIfPresent_withSet_shouldReturnOptionalWithValue() {
    LocaleContext.setLocale(Locale.ITALIAN);

    var optional = LocaleContext.getLocaleIfPresent();

    assertThat(optional).isPresent().contains(Locale.ITALIAN);
  }

  @Test
  void getLocaleIfPresent_withoutSet_shouldReturnEmpty() {
    var optional = LocaleContext.getLocaleIfPresent();

    assertThat(optional).isEmpty();
  }

  @Test
  void setDefaultLocale_shouldChangeGlobalDefault() {
    Locale original = LocaleContext.getDefaultLocale();

    LocaleContext.setDefaultLocale(Locale.JAPANESE);

    assertThat(LocaleContext.getDefaultLocale()).isEqualTo(Locale.JAPANESE);
    assertThat(LocaleContext.getLocale()).isEqualTo(Locale.JAPANESE);

    // Restore
    LocaleContext.setDefaultLocale(original);
  }
}
