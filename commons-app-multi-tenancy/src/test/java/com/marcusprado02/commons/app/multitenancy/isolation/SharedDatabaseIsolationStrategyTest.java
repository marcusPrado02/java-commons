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

class SharedDatabaseIsolationStrategyTest {

  @Mock private DataSource dataSource;

  private SharedDatabaseIsolationStrategy strategy;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    strategy = new SharedDatabaseIsolationStrategy(dataSource);
    TenantContextHolder.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  void shouldReturnSameDataSourceInstance() {
    TenantContextHolder.setContext(TenantContext.of("tenant123"));

    DataSource result1 = strategy.getDataSource();
    DataSource result2 = strategy.getDataSource();

    assertThat(result1).isSameAs(dataSource);
    assertThat(result2).isSameAs(dataSource);
    assertThat(result1).isSameAs(result2);
  }

  @Test
  void shouldReturnDataSourceEvenWithoutTenantContext() {
    // No tenant context set
    TenantContextHolder.clear();

    DataSource result = strategy.getDataSource();

    assertThat(result).isSameAs(dataSource);
  }

  @Test
  void shouldReturnSameDataSourceForDifferentTenants() {
    TenantContextHolder.setContext(TenantContext.of("tenant1"));
    DataSource result1 = strategy.getDataSource();

    TenantContextHolder.setContext(TenantContext.of("tenant2"));
    DataSource result2 = strategy.getDataSource();

    assertThat(result1).isSameAs(result2);
    assertThat(result1).isSameAs(dataSource);
  }

  @Test
  void shouldNotThrowOnDestroy() {
    assertThatCode(() -> strategy.destroy()).doesNotThrowAnyException();
  }
}
