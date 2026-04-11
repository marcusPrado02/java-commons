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
 * MongoDB implementation of {@code PageableRepository} using {@code MongoTemplate}.
 *
 * @param <E> the entity type
 * @param <I> the ID type
 */
public class MongoPageableRepository<E, I> implements PageableRepository<E, I> {

  private final MongoTemplate mongoTemplate;
  private final Class<E> entityClass;
  private final MongoQueryBuilder<E> queryBuilder;

  /**
   * Creates a new repository backed by the given {@code MongoTemplate} and entity class.
   *
   * @param mongoTemplate the Mongo template to use for database operations
   * @param entityClass the class of the entity managed by this repository
   */
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
  public Optional<E> findById(I id) {
    Objects.requireNonNull(id, "id must not be null");
    return Optional.ofNullable(mongoTemplate.findById(id, entityClass));
  }

  /**
   * Returns all entities in the collection.
   *
   * @return a list of all entities
   */
  public List<E> findAll() {
    return mongoTemplate.findAll(entityClass);
  }

  /**
   * Returns a page of entities without applying any filter.
   *
   * @param pageRequest the page request
   * @return a page result containing entities for the requested page
   */
  public PageResult<E> findAll(PageRequest pageRequest) {
    Objects.requireNonNull(pageRequest, "pageRequest must not be null");

    Query query = new Query();
    int skip = pageRequest.page() * pageRequest.size();
    query.skip(skip).limit(pageRequest.size());

    List<E> results = mongoTemplate.find(query, entityClass);
    long total = mongoTemplate.count(new Query(), entityClass);

    return new PageResult<>(results, total, pageRequest.page(), pageRequest.size());
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, Specification<E> specification) {
    // Specification is JPA-specific; fall back to findAll without filter
    return findAll(pageRequest);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, SearchCriteria criteria) {
    Objects.requireNonNull(pageRequest, "pageRequest must not be null");

    Query query = new Query();

    if (criteria != null && !criteria.filters().isEmpty()) {
      queryBuilder.applyFilters(query, criteria);
    }

    int skip = pageRequest.page() * pageRequest.size();
    query.skip(skip).limit(pageRequest.size());

    List<E> results = mongoTemplate.find(query, entityClass);

    Query countQuery = new Query();
    if (criteria != null && !criteria.filters().isEmpty()) {
      queryBuilder.applyFilters(countQuery, criteria);
    }
    long total = mongoTemplate.count(countQuery, entityClass);

    return new PageResult<>(results, total, pageRequest.page(), pageRequest.size());
  }

  @Override
  public void delete(E entity) {
    Objects.requireNonNull(entity, "entity must not be null");
    mongoTemplate.remove(entity);
  }

  @Override
  public void deleteById(I id) {
    Objects.requireNonNull(id, "id must not be null");
    E entity =
        findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Entity with id " + id + " not found"));
    mongoTemplate.remove(entity);
  }

  /**
   * Returns whether an entity with the given ID exists.
   *
   * @param id the entity ID
   * @return {@code true} if an entity with the given ID exists, {@code false} otherwise
   */
  public boolean existsById(I id) {
    Objects.requireNonNull(id, "id must not be null");
    return findById(id).isPresent();
  }

  /**
   * Returns the total number of entities in the collection.
   *
   * @return the entity count
   */
  public long count() {
    return mongoTemplate.count(new Query(), entityClass);
  }

  @Override
  public PageResult<E> search(PageRequest pageRequest, Specification<E> spec, Sort sort) {
    Objects.requireNonNull(pageRequest, "pageRequest must not be null");

    Query query = new Query();

    if (sort != null && !sort.orders().isEmpty()) {
      queryBuilder.applySort(query, sort);
    }

    int skip = pageRequest.page() * pageRequest.size();
    query.skip(skip).limit(pageRequest.size());

    List<E> results = mongoTemplate.find(query, entityClass);
    long total = mongoTemplate.count(new Query(), entityClass);

    return new PageResult<>(results, total, pageRequest.page(), pageRequest.size());
  }
}
