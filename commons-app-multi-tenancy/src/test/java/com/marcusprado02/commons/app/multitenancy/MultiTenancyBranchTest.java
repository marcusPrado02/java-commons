package com.marcusprado02.commons.app.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.app.multitenancy.isolation.DatabaseIsolationStrategy;
import com.marcusprado02.commons.app.multitenancy.isolation.SchemaIsolationStrategy;
import com.marcusprado02.commons.app.multitenancy.spring.TenantAutoConfiguration;
import com.marcusprado02.commons.app.multitenancy.spring.TenantFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MultiTenancyBranchTest {

  @AfterEach
  void clearTenantContext() {
    TenantContextHolder.clear();
  }

  // --- TenantAutoConfiguration ---

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class));

  @Test
  void tenantAutoConfiguration_registersTenantResolverBean() {
    contextRunner.run(ctx -> assertThat(ctx.getBean("tenantResolver")).isNotNull());
  }

  @Test
  void tenantAutoConfiguration_registersTenantFilterBean() {
    contextRunner.run(
        ctx -> assertThat(ctx.getBean("tenantFilter")).isInstanceOf(TenantFilter.class));
  }

  // --- TenantFilter non-HttpServletRequest branch ---

  @Test
  void tenantFilter_nonHttpRequest_passesThrough() throws ServletException, IOException {
    TenantFilter filter = new TenantFilter(request -> null);
    ServletRequest nonHttp = mock(ServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(nonHttp, response, chain);

    verify(chain).doFilter(nonHttp, response);
  }

  // --- TenantContext.Builder.attributes(Map) ---

  @Test
  void tenantContextBuilder_withAttributesMap() {
    Map<String, Object> attrs = Map.of("plan", "premium", "region", "us-east-1");
    TenantContext ctx = TenantContext.builder().tenantId("t1").attributes(attrs).build();

    assertThat(ctx.getAttribute("plan")).hasValue("premium");
    assertThat(ctx.getAttribute("region")).hasValue("us-east-1");
  }

  // --- DatabaseIsolationStrategy extra branches ---

  @Test
  void databaseStrategy_fourArgConstructor_works() {
    com.zaxxer.hikari.HikariConfig cfg = new com.zaxxer.hikari.HikariConfig();
    cfg.setMaximumPoolSize(5);
    DatabaseIsolationStrategy strategy =
        new DatabaseIsolationStrategy(
            tenantId -> "jdbc:h2:mem:" + tenantId + ";DB_CLOSE_DELAY=-1", "sa", "", cfg);
    assertThat(strategy.getIsolationType())
        .isEqualTo(
            com.marcusprado02.commons.app.multitenancy.isolation.TenantIsolationStrategy
                .IsolationType.DATABASE_PER_TENANT);
    strategy.shutdown();
  }

  @Test
  void databaseStrategy_getCurrentDataSource_returnsNullWhenNoTenant() {
    DatabaseIsolationStrategy strategy =
        new DatabaseIsolationStrategy(
            tenantId -> "jdbc:h2:mem:" + tenantId + ";DB_CLOSE_DELAY=-1", "sa", "");
    strategy.removeIsolation();

    assertThat(strategy.getCurrentDataSource()).isNull();
    strategy.shutdown();
  }

  @Test
  void databaseStrategy_removeTenant_noOpWhenAbsent() {
    DatabaseIsolationStrategy strategy =
        new DatabaseIsolationStrategy(
            tenantId -> "jdbc:h2:mem:" + tenantId + ";DB_CLOSE_DELAY=-1", "sa", "");

    assertThatCode(() -> strategy.removeTenant("nonexistent")).doesNotThrowAnyException();
    strategy.shutdown();
  }

  // --- SchemaIsolationStrategy extra branches ---

  @Test
  void schemaStrategy_getCurrentSchema_returnsNullWhenNoTenant() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);
    when(ds.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);
    TenantContextHolder.clear();

    assertThat(strategy.getCurrentSchema()).isNull();
  }

  @Test
  void schemaStrategy_getCurrentSchema_returnsSchemaWhenTenantSet() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);
    when(ds.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);
    TenantContextHolder.setContext(TenantContext.of("tenant1"));

    assertThat(strategy.getCurrentSchema()).isEqualTo("tenant_tenant1");
  }

  @Test
  void schemaStrategy_createSchemaIfNotExists_executesCreateStatement() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);
    when(ds.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);
    strategy.createSchemaIfNotExists("tenant1");

    verify(stmt).execute("CREATE SCHEMA IF NOT EXISTS tenant_tenant1");
  }

  @Test
  void schemaStrategy_createSchemaIfNotExists_throwsOnSqlException() throws SQLException {
    DataSource ds = mock(DataSource.class);
    when(ds.getConnection()).thenThrow(new SQLException("no connection"));

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);

    assertThatThrownBy(() -> strategy.createSchemaIfNotExists("tenant1"))
        .isInstanceOf(SchemaIsolationStrategy.TenantIsolationException.class)
        .hasMessageContaining("tenant1");
  }

  @Test
  void schemaStrategy_dropSchema_executesDropStatement() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);
    when(ds.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);
    strategy.dropSchema("tenant1");

    verify(stmt).execute("DROP SCHEMA IF EXISTS tenant_tenant1 CASCADE");
  }

  @Test
  void schemaStrategy_dropSchema_throwsOnSqlException() throws SQLException {
    DataSource ds = mock(DataSource.class);
    when(ds.getConnection()).thenThrow(new SQLException("no connection"));

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);

    assertThatThrownBy(() -> strategy.dropSchema("tenant1"))
        .isInstanceOf(SchemaIsolationStrategy.TenantIsolationException.class)
        .hasMessageContaining("tenant1");
  }

  @Test
  void schemaStrategy_applyIsolation_setSchema_fallsBackToSetSchema() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);
    when(ds.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);
    doThrow(new SQLException("not supported"))
        .when(stmt)
        .execute(org.mockito.ArgumentMatchers.startsWith("SET search_path"));
    doThrow(new SQLException("not supported"))
        .when(stmt)
        .execute(org.mockito.ArgumentMatchers.startsWith("USE"));

    SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(ds);
    assertThatCode(() -> strategy.applyIsolation("tenant1")).doesNotThrowAnyException();
  }
}
