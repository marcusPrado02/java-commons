package com.marcusprado02.commons.adapters.persistence.inmemory;

import com.marcusprado02.commons.ports.persistence.contract.PageableRepository;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação em memória de PageableRepository<E, ID>.
 */
public class InMemoryPageableRepository<E, ID>
        extends BaseInMemoryRepository<E, ID>
        implements PageableRepository<E, ID> {

    public InMemoryPageableRepository(IdExtractor<E, ID> idExtractor) {
        super(idExtractor);
    }

    @Override
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
