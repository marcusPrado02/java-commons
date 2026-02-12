package com.marcusprado02.commons.adapters.persistence.inmemory;

import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Implementação em memória de PageableRepository<E, ID>. */
public class InMemoryPageableRepository<E, ID> extends BaseInMemoryRepository<E, ID>
    implements PageableRepository<E, ID> {

  public InMemoryPageableRepository(IdExtractor<E, ID> idExtractor) {
    super(idExtractor);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, Specification<E> specification) {
    // Basic implementation: ignores specification for now
    return findAll(pageRequest);
  }

  @Override
  public PageResult<E> findAll(PageRequest pageRequest, SearchCriteria criteria) {
    // Basic implementation: ignores criteria for now
    return findAll(pageRequest);
  }

  @Override
  public PageResult<E> search(PageRequest pageRequest, Specification<E> spec, Sort sort) {
    // Basic implementation: ignores spec and sort for now
    return findAll(pageRequest);
  }

  public PageResult<E> findAll(PageRequest pageRequest) {
    List<E> all = new ArrayList<>(storage.values());

    // Ordenação simples se necessário (por nome de campo)
    // aqui deixamos sem ordenação por default
    List<E> sorted = all.stream().collect(Collectors.toList());

    int from = pageRequest.page() * pageRequest.size();
    int to = Math.min(from + pageRequest.size(), sorted.size());

    List<E> sub = sorted.subList(from, to);
    return new PageResult<>(sub, all.size(), pageRequest.page(), pageRequest.size());
  }
}
