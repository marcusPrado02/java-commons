package com.marcusprado02.commons.adapters.persistence.jpa.transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.function.Supplier;

/** Utilitário para execução de código dentro de transação JPA. */
public final class Transactional {

  private Transactional() {}

  /**
   * Executa uma ação dentro de uma transação, retornando um valor. Se lançar exceção, faz rollback.
   */
  public static <T> T run(EntityManager em, Supplier<T> action) {
    EntityTransaction tx = em.getTransaction();
    boolean active = tx.isActive();

    try {
      if (!active) tx.begin();
      T result = action.get();
      if (!active) tx.commit();
      return result;
    } catch (RuntimeException e) {
      if (!active && tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /** Executa uma ação dentro de uma transação sem retorno (void). */
  public static void run(EntityManager em, Runnable action) {
    run(
        em,
        () -> {
          action.run();
          return null;
        });
  }
}
