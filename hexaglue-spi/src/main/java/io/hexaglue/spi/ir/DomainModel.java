package io.hexaglue.spi.ir;

import java.util.List;
import java.util.Optional;

/**
 * The domain model extracted from the application.
 *
 * @param types all domain types (aggregates, entities, value objects, etc.)
 */
public record DomainModel(List<DomainType> types) {

    /**
     * Finds a domain type by its qualified name.
     *
     * @param qualifiedName the fully qualified class name
     * @return the domain type, or empty if not found
     */
    public Optional<DomainType> findByQualifiedName(String qualifiedName) {
        return types.stream()
                .filter(t -> t.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    /**
     * Returns all types of a specific kind.
     *
     * @param kind the domain kind to filter by
     * @return list of matching types
     */
    public List<DomainType> typesOfKind(DomainKind kind) {
        return types.stream().filter(t -> t.kind() == kind).toList();
    }

    /**
     * Returns all aggregate roots.
     */
    public List<DomainType> aggregateRoots() {
        return typesOfKind(DomainKind.AGGREGATE_ROOT);
    }

    /**
     * Returns all entities (including aggregate roots).
     */
    public List<DomainType> entities() {
        return types.stream()
                .filter(t -> t.kind() == DomainKind.ENTITY || t.kind() == DomainKind.AGGREGATE_ROOT)
                .toList();
    }

    /**
     * Returns all value objects.
     */
    public List<DomainType> valueObjects() {
        return typesOfKind(DomainKind.VALUE_OBJECT);
    }
}
