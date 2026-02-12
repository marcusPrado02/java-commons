package com.marcusprado02.commons.adapters.persistence.jpa.repository;

import com.marcusprado02.commons.adapters.persistence.jpa.base.BaseGenericRepository;
import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import com.marcusprado02.commons.ports.persistence.specification.SpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
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
  public PageResult<E> findAll(PageRequest pageRequest, Specification<E> specification) {

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // count
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<E> countRoot = countQuery.from(getEntityClass());
    countQuery
        .select(cb.count(countRoot))
        .where(specification.toPredicate(countRoot, countQuery, cb));
    long total = entityManager.createQuery(countQuery).getSingleResult();

    // select
    CriteriaQuery<E> selectQuery = cb.createQuery(getEntityClass());
    Root<E> root = selectQuery.from(getEntityClass());
    selectQuery.select(root).where(specification.toPredicate(root, selectQuery, cb));

    TypedQuery<E> typedQuery = entityManager.createQuery(selectQuery);
    typedQuery.setFirstResult(pageRequest.page() * pageRequest.size());
    typedQuery.setMaxResults(pageRequest.size());

    List<E> content = typedQuery.getResultList();

    return new PageResult<>(content, total, pageRequest.page(), pageRequest.size());
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, SearchCriteria criteria) {

    Specification<E> spec = new SpecificationBuilder<E>().build(criteria);

    return findAll(pageRequest, spec);
  }

  @Override
  public PageResult<E> search(PageRequest pageRequest, Specification<E> spec, Sort sort) {

    var cb = entityManager.getCriteriaBuilder();

    // -- COUNT
    var countQuery = cb.createQuery(Long.class);
    var countRoot = countQuery.from(getEntityClass());
    countQuery.select(cb.count(countRoot));
    if (spec != null) {
      countQuery.where(spec.toPredicate(countRoot, countQuery, cb));
    }
    long total = entityManager.createQuery(countQuery).getSingleResult();

    // -- SELECT
    var selectQuery = cb.createQuery(getEntityClass());
    var root = selectQuery.from(getEntityClass());
    selectQuery.select(root);

    if (spec != null) {
      selectQuery.where(spec.toPredicate(root, selectQuery, cb));
    }

    // ORDENAÇÃO DINÂMICA
    if (sort != null) {
      var jpaOrders =
          sort.orders().stream()
              .map(
                  o ->
                      o.direction() == Order.Direction.ASC
                          ? cb.asc(root.get(o.field()))
                          : cb.desc(root.get(o.field())))
              .toList();
      selectQuery.orderBy(jpaOrders);
    }

    var typedQuery = entityManager.createQuery(selectQuery);

    typedQuery.setFirstResult(pageRequest.page() * pageRequest.size());
    typedQuery.setMaxResults(pageRequest.size());

    var content = typedQuery.getResultList();

    return new PageResult<>(content, total, pageRequest.page(), pageRequest.size());
  }
}
