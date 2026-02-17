package com.marcusprado02.commons.app.multitenancy.isolation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
  void shouldReturnDataSourceWithSchemaSwitch() throws SQLException {
    TenantContextHolder.setContext(TenantContext.of("tenant123"));

    DataSource result = strategy.getDataSource();

    assertThat(result).isNotNull();
    // The returned datasource should be a proxy that switches schema
    result.getConnection();
    verify(statement).execute("USE tenant123");
  }

  @Test
  void shouldSanitizeTenantIdForSchema() throws SQLException {
    TenantContextHolder.setContext(TenantContext.of("tenant-123_test"));

    DataSource result = strategy.getDataSource();

    result.getConnection();
    // Should sanitize tenant ID for safe schema name
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
    TenantContextHolder.setContext(TenantContext.of("tenant123"));
    when(statement.execute(anyString())).thenThrow(new SQLException("Schema not found"));

    DataSource result = strategy.getDataSource();

    // Should still return a connection even if schema switch fails
    assertThatCode(() -> result.getConnection()).doesNotThrowAnyException();
  }

  @Test
  void shouldUseCustomSchemaPattern() throws SQLException {
    strategy = new SchemaIsolationStrategy(dataSource, tenantId -> "app_" + tenantId + "_db");
    TenantContextHolder.setContext(TenantContext.of("tenant123"));

    DataSource result = strategy.getDataSource();

    result.getConnection();
    verify(statement).execute("USE app_tenant123_db");
  }
}
