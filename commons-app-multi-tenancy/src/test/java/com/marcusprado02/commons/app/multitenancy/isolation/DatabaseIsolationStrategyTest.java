package com.marcusprado02.commons.app.multitenancy.isolation;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseIsolationStrategyTest {

  private DatabaseIsolationStrategy strategy;

  @BeforeEach
  void setUp() {
    Function<String, String> urlGenerator =
        tenantId -> "jdbc:h2:mem:" + tenantId + ";DB_CLOSE_DELAY=-1";
    strategy = new DatabaseIsolationStrategy(urlGenerator, "sa", "");
  }

  @AfterEach
  void tearDown() {
    strategy.removeIsolation();
    if (strategy != null) {
      strategy.shutdown();
    }
  }

  @Test
  void shouldGetDataSourceForTenant() throws SQLException {
    strategy.applyIsolation("tenant1");

    DataSource result = strategy.getDataSource();

    assertThat(result).isNotNull();
    // Verify we can get a connection
    try (Connection conn = result.getConnection()) {
      assertThat(conn).isNotNull();
    }
  }

  @Test
  void shouldReuseDataSourceForSameTenant() {
    strategy.applyIsolation("tenant1");

    DataSource first = strategy.getDataSource();
    DataSource second = strategy.getDataSource();

    assertThat(first).isSameAs(second);
  }

  @Test
  void shouldCreateSeparateDataSourcesForDifferentTenants() {
    strategy.applyIsolation("tenant1");
    DataSource ds1 = strategy.getDataSource();

    strategy.applyIsolation("tenant2");
    DataSource ds2 = strategy.getDataSource();

    assertThat(ds1).isNotSameAs(ds2);
  }

  @Test
  void shouldThrowExceptionWhenNoTenantContext() {
    strategy.removeIsolation();

    assertThatThrownBy(() -> strategy.getDataSource())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No tenant context");
  }

  @Test
  void shouldCloseAllDataSourcesOnDestroy() {
    // Create some data sources
    strategy.applyIsolation("tenant1");
    strategy.getDataSource();

    strategy.applyIsolation("tenant2");
    strategy.getDataSource();

    // Shutdown should close all without exception
    assertThatCode(() -> strategy.shutdown()).doesNotThrowAnyException();
  }
}
