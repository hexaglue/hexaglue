package com.example.enterprise.catalog.port.driving;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1;
import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for CatalogAggregate1 operations.
 */
public interface CatalogAggregate1Service {
    CatalogAggregate1Id create(CreateCatalogAggregate1Command command);
    CatalogAggregate1 get(CatalogAggregate1Id id);
    List<CatalogAggregate1> list();
    void update(UpdateCatalogAggregate1Command command);
    void delete(CatalogAggregate1Id id);
    void activate(CatalogAggregate1Id id);
    void complete(CatalogAggregate1Id id);
}
