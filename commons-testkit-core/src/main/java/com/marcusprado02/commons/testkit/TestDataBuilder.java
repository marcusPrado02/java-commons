package com.marcusprado02.commons.testkit;

import java.util.function.Supplier;

/**
 * Base class for test data builders following the Test Data Builder pattern.
 *
 * <p>Extend this class to create fluent builders for your domain objects.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class UserBuilder extends TestDataBuilder<User> {
 *   private String id = TestIds.nextId("user");
 *   private String name = "Test User";
 *   private String email = RandomData.randomEmail();
 *
 *   public UserBuilder withId(String id) {
 *     this.id = id;
 *     return this;
 *   }
 *
 *   public UserBuilder withName(String name) {
 *     this.name = name;
 *     return this;
 *   }
 *
 *   @Override
 *   public User build() {
 *     return new User(id, name, email);
 *   }
 * }
 * }</pre>
 *
 * @param <T> the type of object to build
 */
public abstract class TestDataBuilder<T> implements Supplier<T> {

  /**
   * Builds the test object with the current configuration.
   *
   * @return the built object
   */
  public abstract T build();

  /**
   * Builds the test object (implements Supplier interface).
   *
   * @return the built object
   */
  @Override
  public T get() {
    return build();
  }

  /**
   * Builds and returns the object (alias for build()).
   *
   * @return the built object
   */
  public T create() {
    return build();
  }

  /**
   * Creates a new instance by cloning the current builder state.
   *
   * <p>Subclasses should override if they need custom cloning logic.
   *
   * @return a new builder with the same configuration
   */
  public TestDataBuilder<T> but() {
    return this;
  }
}
