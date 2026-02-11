package com.marcusprado02.commons.ports.persistence.contract;

import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.specification.SearchCriteria;
import com.marcusprado02.commons.ports.persistence.specification.Specification;

/** Repository with pagination support. */
public interface PageableRepository<E, ID> extends Repository<E, ID> {

  PageResult<E> findAll(PageRequest pageRequest, Specification<E> specification);

PageResult<E> findAll(PageRequest pageRequest, SearchCriteria criteria);
}
