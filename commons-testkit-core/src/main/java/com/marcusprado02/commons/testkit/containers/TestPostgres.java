package com.marcusprado02.commons.testkit.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured PostgreSQL test container.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Container
 * static PostgreSQLContainer<?> postgres = TestPostgres.container();
 *
 * // Configure datasource with:
 * String url = postgres.getJdbcUrl();
 * String username = postgres.getUsername();
 * String password = postgres.getPassword();
 * }</pre>
 */
public final class TestPostgres {

  private static final String DEFAULT_IMAGE = "postgres:16-alpine";
  private static final String DEFAULT_DATABASE = "testdb";
  private static final String DEFAULT_USERNAME = "test";
  private static final String DEFAULT_PASSWORD = "test";

  private TestPostgres() {}

  /** Creates a PostgreSQL container with default settings (postgres:16-alpine). */
  public static PostgreSQLContainer<?> container() {
    return new PostgreSQLContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
        .withDatabaseName(DEFAULT_DATABASE)
        .withUsername(DEFAULT_USERNAME)
        .withPassword(DEFAULT_PASSWORD)
        .withReuse(true);
  }

  /** Creates a PostgreSQL container with a specific version. */
  public static PostgreSQLContainer<?> container(String version) {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:" + version))
        .withDatabaseName(DEFAULT_DATABASE)
        .withUsername(DEFAULT_USERNAME)
        .withPassword(DEFAULT_PASSWORD)
        .withReuse(true);
  }

  /** Creates a PostgreSQL container with custom configuration. */
  public static PostgreSQLContainer<?> container(
      String image, String database, String username, String password) {
    return new PostgreSQLContainer<>(DockerImageName.parse(image))
        .withDatabaseName(database)
        .withUsername(username)
        .withPassword(password)
        .withReuse(true);
  }
}
