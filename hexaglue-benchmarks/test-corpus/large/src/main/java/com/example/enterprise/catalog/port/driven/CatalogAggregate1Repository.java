package com.example.enterprise.catalog.port.driven;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1;
import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for CatalogAggregate1 persistence.
 */
public interface CatalogAggregate1Repository {
    CatalogAggregate1 save(CatalogAggregate1 entity);
    Optional<CatalogAggregate1> findById(CatalogAggregate1Id id);
    List<CatalogAggregate1> findAll();
    void deleteById(CatalogAggregate1Id id);
    boolean existsById(CatalogAggregate1Id id);
    long count();
}
