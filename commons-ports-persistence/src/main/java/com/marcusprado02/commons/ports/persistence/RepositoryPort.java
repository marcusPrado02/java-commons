package com.marcusprado02.commons.ports.persistence;

public interface RepositoryPort<T, ID>
    extends ReadRepositoryPort<T, ID>, WriteRepositoryPort<T, ID> {}
