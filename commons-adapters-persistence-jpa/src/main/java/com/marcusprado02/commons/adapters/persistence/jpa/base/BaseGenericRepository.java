package com.marcusprado02.commons.adapters.persistence.jpa.base;

import com.marcusprado02.commons.ports.persistence.contract.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

public abstract class BaseGenericRepository<E, ID> implements Repository<E, ID> {

  @PersistenceContext protected EntityManager entityManager;

  private final Class<E> entityClass;
  private final Class<ID> idClass;

  protected BaseGenericRepository(Class<E> entityClass, Class<ID> idClass) {
    this.entityClass = entityClass;
    this.idClass = idClass;
  }

  @Override
  public Optional<E> findById(ID id) {
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
  public void deleteById(ID id) {
    findById(id).ifPresent(entityManager::remove);
  }

  protected Class<E> getEntityClass() {
    return entityClass;
  }
}
