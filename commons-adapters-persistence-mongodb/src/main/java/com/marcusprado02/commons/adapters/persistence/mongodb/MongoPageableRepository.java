package com.marcusprado02.commons.adapters.persistence.mongodb;

import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * MongoDB implementation of PageableRepository using MongoTemplate.
 *
 * @param <E> Entity type
 * @param <ID> ID type
 */
public class MongoPageableRepository<E, ID> implements PageableRepository<E, ID> {

  private final MongoTemplate mongoTemplate;
  private final Class<E> entityClass;
  private final MongoQueryBuilder<E> queryBuilder;

  public MongoPageableRepository(MongoTemplate mongoTemplate, Class<E> entityClass) {
    this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate must not be null");
    this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
    this.queryBuilder = new MongoQueryBuilder<>();
  }

  @Override
  public E save(E entity) {
    Objects.requireNonNull(entity, "entity must not be null");
    return mongoTemplate.save(entity);
  }

  @Override
  public Optional<E> findById(ID id) {
    Objects.requireNonNull(id, "id must not be null");
    return Optional.ofNullable(mongoTemplate.findById(id, entityClass));
  }

  public List<E> findAll() {
    return mongoTemplate.findAll(entityClass);
  }

  @Override
  public void delete(E entity) {
    Objects.requireNonNull(entity, "entity must not be null");
    mongoTemplate.remove(entity);
  }

  @Override
  public void deleteById(ID id) {
    Objects.requireNonNull(id, "id must not be null");
    E entity = findById(id).orElseThrow(
        () -> new IllegalArgumentException("Entity with id " + id + " not found"));
    mongoTemplate.remove(entity);
  }

  public boolean existsById(ID id) {
    Objects.requireNonNull(id, "id must not be null");
    return findById(id).isPresent();
  }

  public long count() {
    return mongoTemplate.count(new Query(), entityClass);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, Specification<E> specification) {
    // Specification is JPA-specific, fallback to findAll without filter
    return findAll(pageRequest);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, SearchCriteria criteria) {
    Objects.requireNonNull(pageRequest, "pageRequest must not be null");

    Query query = new Query();

    if (criteria != null && !criteria.filters().isEmpty()) {
      queryBuilder.applyFilters(query, criteria);
    }

    // Apply pagination
    int skip = pageRequest.page() * pageRequest.size();
    query.skip(skip).limit(pageRequest.size());

    // Execute query
    List<E> results = mongoTemplate.find(query, entityClass);

    // Count total (without pagination)
    Query countQuery = new Query();
    if (criteria != null && !criteria.filters().isEmpty()) {
      queryBuilder.applyFilters(countQuery, criteria);
    }
    long total = mongoTemplate.count(countQuery, entityClass);

    return new PageResult<>(results, total, pageRequest.page(), pageRequest.size());
  }

  @Override
  public PageResult<E> search(PageRequest pageRequest, Specification<E> spec, Sort sort) {
    Objects.requireNonNull(pageRequest, "pageRequest must not be null");

    Query query = new Query();

    // Apply sorting
    if (sort != null && !sort.orders().isEmpty()) {
      queryBuilder.applySort(query, sort);
    }

    // Apply pagination
    int skip = pageRequest.page() * pageRequest.size();
    query.skip(skip).limit(pageRequest.size());

    // Execute query
    List<E> results = mongoTemplate.find(query, entityClass);
    long total = mongoTemplate.count(new Query(), entityClass);

    return new PageResult<>(results, total, pageRequest.page(), pageRequest.size());
  }

  public PageResult<E> findAll(PageRequest pageRequest) {
    Objects.requireNonNull(pageRequest, "pageRequest must not be null");

    Query query = new Query();
    int skip = pageRequest.page() * pageRequest.size();
    query.skip(skip).limit(pageRequest.size());

    List<E> results = mongoTemplate.find(query, entityClass);
    long total = mongoTemplate.count(new Query(), entityClass);

    return new PageResult<>(results, total, pageRequest.page(), pageRequest.size());
  }
}
