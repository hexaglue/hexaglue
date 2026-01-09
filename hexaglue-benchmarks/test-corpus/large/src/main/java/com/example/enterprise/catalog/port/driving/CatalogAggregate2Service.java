package com.example.enterprise.catalog.port.driving;

import com.example.enterprise.catalog.domain.model.CatalogAggregate2;
import com.example.enterprise.catalog.domain.model.CatalogAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for CatalogAggregate2 operations.
 */
public interface CatalogAggregate2Service {
    CatalogAggregate2Id create(CreateCatalogAggregate2Command command);
    CatalogAggregate2 get(CatalogAggregate2Id id);
    List<CatalogAggregate2> list();
    void update(UpdateCatalogAggregate2Command command);
    void delete(CatalogAggregate2Id id);
    void activate(CatalogAggregate2Id id);
    void complete(CatalogAggregate2Id id);
}
