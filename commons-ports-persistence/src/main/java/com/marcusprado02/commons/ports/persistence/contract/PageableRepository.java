package com.marcusprado02.commons.ports.persistence.contract;

import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;

/** Repository with pagination support. */
public interface PageableRepository<E, ID> extends Repository<E, ID> {

  PageResult<E> findAll(PageRequest pageRequest);
}
