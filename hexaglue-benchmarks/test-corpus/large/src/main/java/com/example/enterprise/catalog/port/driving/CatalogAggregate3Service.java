package com.example.enterprise.catalog.port.driving;

import com.example.enterprise.catalog.domain.model.CatalogAggregate3;
import com.example.enterprise.catalog.domain.model.CatalogAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for CatalogAggregate3 operations.
 */
public interface CatalogAggregate3Service {
    CatalogAggregate3Id create(CreateCatalogAggregate3Command command);
    CatalogAggregate3 get(CatalogAggregate3Id id);
    List<CatalogAggregate3> list();
    void update(UpdateCatalogAggregate3Command command);
    void delete(CatalogAggregate3Id id);
    void activate(CatalogAggregate3Id id);
    void complete(CatalogAggregate3Id id);
}
