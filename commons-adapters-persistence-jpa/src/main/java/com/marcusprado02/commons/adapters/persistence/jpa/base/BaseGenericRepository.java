package com.marcusprado02.commons.adapters.persistence.jpa.base;

import com.marcusprado02.commons.ports.persistence.contract.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

/** Base JPA repository providing common CRUD operations. */
public abstract class BaseGenericRepository<E, I> implements Repository<E, I> {

  @PersistenceContext protected EntityManager entityManager;

  private final Class<E> entityClass;
  private final Class<I> idClass;

  protected BaseGenericRepository(Class<E> entityClass, Class<I> idClass) {
    this.entityClass = entityClass;
    this.idClass = idClass;
  }

  @Override
  public Optional<E> findById(I id) {
    return Optional.ofNullable(entityManager.find(entityClass, id));
  }

  @Override
  public E save(E entity) {
    return entityManager.merge(entity);
  }

  @Override
  public void delete(E entity) {
    entityManager.remove(entity);
  }

  @Override
  public void deleteById(I id) {
    findById(id).ifPresent(entityManager::remove);
  }

  protected Class<E> getEntityClass() {
    return entityClass;
  }
}
