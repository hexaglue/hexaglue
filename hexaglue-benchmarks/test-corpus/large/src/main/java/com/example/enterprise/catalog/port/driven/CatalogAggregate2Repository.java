package com.example.enterprise.catalog.port.driven;

import com.example.enterprise.catalog.domain.model.CatalogAggregate2;
import com.example.enterprise.catalog.domain.model.CatalogAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for CatalogAggregate2 persistence.
 */
public interface CatalogAggregate2Repository {
    CatalogAggregate2 save(CatalogAggregate2 entity);
    Optional<CatalogAggregate2> findById(CatalogAggregate2Id id);
    List<CatalogAggregate2> findAll();
    void deleteById(CatalogAggregate2Id id);
    boolean existsById(CatalogAggregate2Id id);
    long count();
}
