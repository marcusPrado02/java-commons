package com.marcusprado02.commons.app.multitenancy.isolation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

class DatabaseIsolationStrategyTest {

  private DatabaseIsolationStrategy strategy;

  @BeforeEach
  void setUp() {
    Function<String, String> urlGenerator = tenantId -> "jdbc:h2:mem:" + tenantId + ";DB_CLOSE_DELAY=-1";
    strategy = new DatabaseIsolationStrategy(urlGenerator, "sa", "");
    TenantContextHolder.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
    if (strategy != null) {
      strategy.destroy();
    }
  }

  @Test
  void shouldGetDataSourceForTenant() throws SQLException {
    TenantContextHolder.setContext(TenantContext.of("tenant1"));

    DataSource result = strategy.getDataSource();

    assertThat(result).isNotNull();
    // Verify we can get a connection
    try (Connection conn = result.getConnection()) {
      assertThat(conn).isNotNull();
    }
  }

  @Test
  void shouldReuseDataSourceForSameTenant() {
    TenantContextHolder.setContext(TenantContext.of("tenant1"));

    DataSource first = strategy.getDataSource();
    DataSource second = strategy.getDataSource();

    assertThat(first).isSameAs(second);
  }

  @Test
  void shouldCreateSeparateDataSourcesForDifferentTenants() {
    TenantContextHolder.setContext(TenantContext.of("tenant1"));
    DataSource ds1 = strategy.getDataSource();

    TenantContextHolder.setContext(TenantContext.of("tenant2"));
    DataSource ds2 = strategy.getDataSource();

    assertThat(ds1).isNotSameAs(ds2);
  }

  @Test
  void shouldThrowExceptionWhenNoTenantContext() {
    TenantContextHolder.clear();

    assertThatThrownBy(() -> strategy.getDataSource())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No tenant context");
  }

  @Test
  void shouldCloseAllDataSourcesOnDestroy() {
    // Create some data sources
    TenantContextHolder.setContext(TenantContext.of("tenant1"));
    strategy.getDataSource();

    TenantContextHolder.setContext(TenantContext.of("tenant2"));
    strategy.getDataSource();

    // Destroy should close all without exception
    assertThatCode(() -> strategy.destroy()).doesNotThrowAnyException();
  }
}
