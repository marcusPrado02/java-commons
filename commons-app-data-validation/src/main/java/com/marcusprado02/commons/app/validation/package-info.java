/**
 * Data validation abstractions for the commons library.
 *
 * <p>This module provides a flexible and extensible validation framework that integrates with the
 * {@code commons-kernel-result} module for type-safe error handling.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.validation.Validator} - Main validation interface
 *   <li>{@link com.marcusprado02.commons.app.validation.ValidationResult} - Validation outcome with
 *       violations
 *   <li>{@link com.marcusprado02.commons.app.validation.ValidationViolation} - Single validation
 *       error
 *   <li>{@link com.marcusprado02.commons.app.validation.FieldValidator} - Validates individual
 *       fields
 *   <li>{@link com.marcusprado02.commons.app.validation.CompositeValidator} - Combines multiple
 *       validators
 *   <li>{@link com.marcusprado02.commons.app.validation.ContextualValidator} - Validation with
 *       context
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * // Simple field validation
 * FieldValidator<User, String> emailValidator = FieldValidator.of(
 *     "email",
 *     User::getEmail,
 *     Validators.isEmail(),
 *     "Email must be valid"
 * );
 *
 * ValidationResult result = emailValidator.validate(user);
 * if (result.hasViolations()) {
 *     // Handle validation errors
 * }
 *
 * // Composite validation
 * Validator<User> userValidator = CompositeValidator.of(
 *     FieldValidator.of("email", User::getEmail, Validators.isEmail(), "Invalid email"),
 *     FieldValidator.of("name", User::getName, Validators.notBlank(), "Name is required"),
 *     FieldValidator.of("age", User::getAge, Validators.isPositive(), "Age must be positive")
 * );
 *
 * // Jakarta Bean Validation integration
 * Validator<User> jakartaValidator = JakartaValidatorAdapter.create();
 * ValidationResult result = jakartaValidator.validate(user);
 * }</pre>
 *
 * <h2>Common Validators</h2>
 *
 * <p>The {@link com.marcusprado02.commons.app.validation.Validators} class provides common
 * validation predicates:
 *
 * <ul>
 *   <li>{@code notNull()} - Value must not be null
 *   <li>{@code notBlank()} - String must not be blank
 *   <li>{@code isEmail()} - String must be valid email
 *   <li>{@code minLength(int)} - String minimum length
 *   <li>{@code maxLength(int)} - String maximum length
 *   <li>{@code isPositive()} - Number must be positive
 *   <li>{@code range(T, T)} - Value must be within range
 *   <li>{@code matches(String)} - String must match regex
 * </ul>
 *
 * <h2>Validation Severity</h2>
 *
 * <p>Violations can have different severity levels:
 *
 * <ul>
 *   <li>{@code ERROR} - Must be fixed before proceeding
 *   <li>{@code WARNING} - Should be addressed but doesn't prevent proceeding
 *   <li>{@code INFO} - Informational message
 * </ul>
 *
 * <h2>Contextual Validation</h2>
 *
 * <p>For validation that depends on external state:
 *
 * <pre>{@code
 * public class UniqueEmailValidator implements ContextualValidator<User> {
 *     private final UserRepository repository;
 *
 *     @Override
 *     public ValidationResult validate(User user, ValidationContext context) {
 *         String userId = context.get("userId", String.class);
 *         if (repository.existsByEmailAndIdNot(user.getEmail(), userId)) {
 *             return ValidationResult.invalid(
 *                 ValidationViolation.builder()
 *                     .field("email")
 *                     .message("Email already exists")
 *                     .build()
 *             );
 *         }
 *         return ValidationResult.valid();
 *     }
 * }
 *
 * ValidationContext context = ValidationContext.builder()
 *     .put("userId", "123")
 *     .build();
 *
 * ValidationResult result = validator.validate(user, context);
 * }</pre>
 *
 * <h2>Jakarta Bean Validation Integration</h2>
 *
 * <p>The module optionally integrates with Jakarta Bean Validation:
 *
 * <pre>{@code
 * public class User {
 *     @NotBlank
 *     @Email
 *     private String email;
 *
 *     @Size(min = 3, max = 50)
 *     private String name;
 * }
 *
 * Validator<User> validator = JakartaValidatorAdapter.create();
 * ValidationResult result = validator.validate(user);
 * }</pre>
 *
 * @see com.marcusprado02.commons.app.validation.Validator
 * @see com.marcusprado02.commons.app.validation.ValidationResult
 * @see com.marcusprado02.commons.app.validation.Validators
 */
package com.marcusprado02.commons.app.validation;
