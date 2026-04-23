package com.marcusprado02.commons.kernel.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class KernelCoreStubsTest {

  @Test
  void utility_classes_are_final_with_private_constructor() throws Exception {
    List<Class<?>> stubs =
        List.of(
            Collections2.class,
            Dates.class,
            Numbers.class,
            Objects2.class,
            Preconditions.class,
            Strings.class,
            Uuids.class);

    for (Class<?> clazz : stubs) {
      assertTrue(Modifier.isFinal(clazz.getModifiers()), clazz.getSimpleName() + " must be final");
      var ctor = clazz.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(ctor.getModifiers()),
          clazz.getSimpleName() + " constructor must be private");
      ctor.setAccessible(true);
      ctor.newInstance();
    }
  }
}
