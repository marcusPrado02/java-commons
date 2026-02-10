package com.marcusprado02.commons.adapters.persistence.jpa.repository;

import com.marcusprado02.commons.adapters.persistence.jpa.base.BaseGenericRepository;
import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;

public class PageableJpaRepository<E, ID> extends BaseGenericRepository<E, ID>
    implements PageableRepository<E, ID> {

  public PageableJpaRepository(Class<E> entityClass, Class<ID> idClass) {
    super(entityClass, idClass);
  }

  /** Inject the EntityManager and return this repository instance */
  public PageableJpaRepository<E, ID> withEntityManager(EntityManager em) {
    this.entityManager = em;
    return this;
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest) {
    var cb = entityManager.getCriteriaBuilder();

    // Build count query
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<E> countRoot = countQuery.from(getEntityClass());
    countQuery.select(cb.count(countRoot));
    long total = entityManager.createQuery(countQuery).getSingleResult();

    // Build select query
    CriteriaQuery<E> criteriaQuery = cb.createQuery(getEntityClass());
    Root<E> root = criteriaQuery.from(getEntityClass());
    criteriaQuery.select(root);

    TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);
    typedQuery.setFirstResult(pageRequest.page() * pageRequest.size());
    typedQuery.setMaxResults(pageRequest.size());

    List<E> content = typedQuery.getResultList();

    return new PageResult<>(content, total, pageRequest.page(), pageRequest.size());
  }
}
