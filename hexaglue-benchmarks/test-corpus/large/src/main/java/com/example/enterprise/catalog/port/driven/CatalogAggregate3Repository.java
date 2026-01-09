package com.example.enterprise.catalog.port.driven;

import com.example.enterprise.catalog.domain.model.CatalogAggregate3;
import com.example.enterprise.catalog.domain.model.CatalogAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for CatalogAggregate3 persistence.
 */
public interface CatalogAggregate3Repository {
    CatalogAggregate3 save(CatalogAggregate3 entity);
    Optional<CatalogAggregate3> findById(CatalogAggregate3Id id);
    List<CatalogAggregate3> findAll();
    void deleteById(CatalogAggregate3Id id);
    boolean existsById(CatalogAggregate3Id id);
    long count();
}
