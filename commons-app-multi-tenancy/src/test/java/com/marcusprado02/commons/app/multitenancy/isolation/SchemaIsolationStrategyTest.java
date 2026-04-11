package com.marcusprado02.commons.app.multitenancy.isolation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SchemaIsolationStrategyTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private Statement statement;

  private SchemaIsolationStrategy strategy;

  @BeforeEach
  void setUp() throws SQLException {
    MockitoAnnotations.openMocks(this);
    strategy = new SchemaIsolationStrategy(dataSource);
    TenantContextHolder.clear();

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  void shouldReturnDataSourceWithSchemaSwitch() {
    TenantContextHolder.setContext(TenantContext.of("tenant123"));

    DataSource result = strategy.getDataSource();

    assertThat(result).isNotNull();
  }

  @Test
  void shouldSanitizeTenantIdForSchema() throws SQLException {
    // Use a sanitizing schema generator: replace non-alphanumeric chars with underscores
    strategy =
        new SchemaIsolationStrategy(
            dataSource, tenantId -> tenantId.replaceAll("[^a-zA-Z0-9_]", "_"));

    // Stub SET search_path to throw so fallback to USE is exercised
    doThrow(new SQLException("not supported"))
        .when(statement)
        .execute(startsWith("SET search_path"));

    // tenant-123_test -> tenant_123_test after sanitization
    strategy.applyIsolation("tenant-123_test");

    verify(statement).execute("USE tenant_123_test");
  }

  @Test
  void shouldThrowExceptionWhenNoTenantContext() {
    TenantContextHolder.clear();

    assertThatThrownBy(() -> strategy.getDataSource())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No tenant context");
  }

  @Test
  void shouldHandleSqlExceptionsGracefully() throws SQLException {
    when(statement.execute(anyString())).thenThrow(new SQLException("Schema not found"));
    doThrow(new SQLException("Schema not found")).when(connection).setSchema(anyString());

    assertThatThrownBy(() -> strategy.applyIsolation("tenant123"))
        .isInstanceOf(SchemaIsolationStrategy.TenantIsolationException.class)
        .hasMessageContaining("tenant123");
  }

  @Test
  void shouldUseCustomSchemaPattern() throws SQLException {
    strategy = new SchemaIsolationStrategy(dataSource, tenantId -> "app_" + tenantId + "_db");

    // Stub SET search_path to throw so fallback to USE is exercised
    doThrow(new SQLException("not supported"))
        .when(statement)
        .execute(startsWith("SET search_path"));

    strategy.applyIsolation("tenant123");

    verify(statement).execute("USE app_tenant123_db");
  }
}
