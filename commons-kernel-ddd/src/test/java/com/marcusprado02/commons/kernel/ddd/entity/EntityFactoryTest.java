package com.marcusprado02.commons.kernel.ddd.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EntityFactoryTest {

  @Test
  void shouldCreateEntityWithStaticMethod() {
    TenantId tenantId = TenantId.of("tenant-1");
    ActorId actorId = ActorId.of("user-123");
    Instant now = Instant.now();

    TestEntity entity =
        EntityFactory.create(
            tenantId,
            actorId,
            now,
            (tid, stamp) -> new TestEntity(TestEntityId.of("id-1"), tid, "Test", stamp));

    assertEquals(TestEntityId.of("id-1"), entity.id());
    assertEquals(tenantId, entity.tenantId());
    assertEquals("Test", entity.name);
    assertEquals(actorId, entity.audit().created().by());
  }

  // Test entity
  static class TestEntity extends Entity<TestEntityId> {
    private final String name;

    public TestEntity(TestEntityId id, TenantId tenantId, String name, AuditStamp created) {
      super(id, tenantId, created);
      this.name = name;
    }
  }

  record TestEntityId(String value) {
    static TestEntityId of(String value) {
      return new TestEntityId(value);
    }
  }
}
