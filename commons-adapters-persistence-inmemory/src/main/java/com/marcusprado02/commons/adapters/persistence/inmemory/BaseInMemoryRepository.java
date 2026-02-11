package com.marcusprado02.commons.adapters.persistence.inmemory;

import com.marcusprado02.commons.ports.persistence.contract.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação em memória de Repository<E, ID>.
 */
public class BaseInMemoryRepository<E, ID> implements Repository<E, ID> {

    protected final Map<ID, E> storage = new ConcurrentHashMap<>();
    private final IdExtractor<E, ID> idExtractor;

    public BaseInMemoryRepository(IdExtractor<E, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public Optional<E> findById(ID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public E save(E entity) {
        ID id = idExtractor.getId(entity);
        storage.put(id, entity);
        return entity;
    }

    @Override
    public void delete(E entity) {
        ID id = idExtractor.getId(entity);
        storage.remove(id);
    }

    @Override
    public void deleteById(ID id) {
        storage.remove(id);
    }
}
