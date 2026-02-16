/**
 * Internationalization (i18n) support for Java applications.
 *
 * <p>This package provides comprehensive i18n capabilities including:
 *
 * <ul>
 *   <li><strong>Message bundles</strong>: Load and resolve localized messages from resource bundles
 *   <li><strong>Locale detection</strong>: Parse and resolve locale from HTTP headers, cookies,
 *       etc.
 *   <li><strong>Formatting</strong>: Format currency, dates, times, and numbers according to locale
 *   <li><strong>Pluralization</strong>: Handle plural forms for different languages
 *   <li><strong>Thread-local context</strong>: Manage current user's locale in thread-safe manner
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>MessageSource</h3>
 *
 * <p>Interface for resolving localized messages with parameter substitution:
 *
 * <pre>{@code
 * MessageSource messages = new ResourceBundleMessageSource("messages");
 * Result<String> welcome = messages.getMessage("welcome.user",
 *     Locale.FRENCH,
 *     Map.of("name", "Jean"));
 * }</pre>
 *
 * <h3>LocaleContext</h3>
 *
 * <p>Thread-local storage for current user's locale:
 *
 * <pre>{@code
 * LocaleContext.setLocale(Locale.GERMAN);
 * Locale current = LocaleContext.getLocale();
 * LocaleContext.clear(); // Important in web apps
 * }</pre>
 *
 * <h3>LocaleFormatter</h3>
 *
 * <p>Format values according to locale conventions:
 *
 * <pre>{@code
 * LocaleFormatter formatter = new LocaleFormatter(Locale.FRANCE);
 * String price = formatter.formatCurrency(99.99, "EUR"); // "99,99 €"
 * String date = formatter.formatDate(LocalDate.now()); // "16/02/2026"
 * String number = formatter.formatNumber(1234567.89); // "1 234 567,89"
 * }</pre>
 *
 * <h3>LocaleResolver</h3>
 *
 * <p>Strategy for extracting locale from requests:
 *
 * <pre>{@code
 * LocaleResolver<HttpServletRequest> resolver =
 *     request -> AcceptLanguageParser.parseFirst(request.getHeader("Accept-Language"));
 * }</pre>
 *
 * <h2>Message Bundle Format</h2>
 *
 * <p>Standard Java properties format with pluralization support:
 *
 * <pre>
 * # messages.properties (default/English)
 * welcome.user=Welcome, {name}!
 * items.one=You have {count} item
 * items.other=You have {count} items
 *
 * # messages_fr.properties (French)
 * welcome.user=Bienvenue, {name}!
 * items.one=Vous avez {count} article
 * items.other=Vous avez {count} articles
 * </pre>
 *
 * <h2>Web Application Integration</h2>
 *
 * <p>Example servlet filter for locale management:
 *
 * <pre>{@code
 * public class LocaleFilter implements Filter {
 *   private final LocaleResolver<HttpServletRequest> resolver =
 *       request -> AcceptLanguageParser.parseFirst(request.getHeader("Accept-Language"));
 *
 *   @Override
 *   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
 *     HttpServletRequest request = (HttpServletRequest) req;
 *     Locale locale = resolver.resolve(request);
 *     if (locale != null) {
 *       LocaleContext.setLocale(locale);
 *     }
 *     try {
 *       chain.doFilter(req, res);
 *     } finally {
 *       LocaleContext.clear();
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Spring Integration Example</h2>
 *
 * <pre>{@code
 * @Configuration
 * public class I18nConfig {
 *   @Bean
 *   public MessageSource messageSource() {
 *     return new ResourceBundleMessageSource("messages");
 *   }
 *
 *   @Bean
 *   public LocaleFormatter localeFormatter() {
 *     return new LocaleFormatter(LocaleContext.getLocale());
 *   }
 * }
 * }</pre>
 *
 * @see com.marcusprado02.commons.app.i18n.MessageSource
 * @see com.marcusprado02.commons.app.i18n.LocaleContext
 * @see com.marcusprado02.commons.app.i18n.LocaleFormatter
 * @see com.marcusprado02.commons.app.i18n.LocaleResolver
 * @since 1.0.0
 */
package com.marcusprado02.commons.app.i18n;
