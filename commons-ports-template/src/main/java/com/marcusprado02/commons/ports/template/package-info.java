/**
 * Template engine port interfaces and value objects.
 *
 * <p>This package provides a platform-agnostic API for template rendering operations. It supports
 * various template engines like Thymeleaf, FreeMarker, Velocity, Mustache, etc.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.template.TemplatePort} - Main interface for template
 *       operations
 *   <li>{@link com.marcusprado02.commons.ports.template.TemplateContext} - Immutable context with
 *       variables and locale
 *   <li>{@link com.marcusprado02.commons.ports.template.TemplateResult} - Rendering result with
 *       content and metadata
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create context with variables
 * TemplateContext context = TemplateContext.builder()
 *     .variable("userName", "John Doe")
 *     .variable("orderNumber", "#12345")
 *     .variable("totalAmount", 150.00)
 *     .locale(Locale.US)
 *     .build();
 *
 * // Render template
 * Result<TemplateResult> result = templatePort.render("email/order-confirmation", context);
 *
 * if (result.isOk()) {
 *     TemplateResult templateResult = result.getValue();
 *     String html = templateResult.getContent();
 *     emailService.send(userEmail, "Order Confirmation", html);
 * } else {
 *     logger.error("Failed to render template: {}", result.getError());
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>File-based and string-based template rendering
 *   <li>Locale-aware formatting and i18n
 *   <li>Template caching with clear cache support
 *   <li>Template existence checking
 *   <li>Multiple content types (HTML, text, XML, etc.)
 *   <li>Result pattern for error handling
 * </ul>
 *
 * <h2>Implementations</h2>
 *
 * <ul>
 *   <li>commons-adapters-template-thymeleaf - Thymeleaf 3.x implementation
 *   <li>commons-adapters-template-freemarker - FreeMarker implementation (planned)
 * </ul>
 *
 * @see com.marcusprado02.commons.ports.template.TemplatePort
 */
package com.marcusprado02.commons.ports.template;
