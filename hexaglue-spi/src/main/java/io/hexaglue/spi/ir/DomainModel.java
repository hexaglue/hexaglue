/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.spi.ir;

import io.hexaglue.arch.ElementKind;
import java.util.List;
import java.util.Optional;

/**
 * The domain model extracted from the application.
 *
 * @param types all domain types (aggregates, entities, value objects, etc.)
 * @deprecated Use {@link io.hexaglue.arch.ArchitecturalModel} with its query API instead.
 *             For example: {@code model.query().aggregates()} or {@code model.query().valueObjects()}.
 *             Scheduled for removal in v5.0.0.
 * @see io.hexaglue.arch.ArchitecturalModel
 * @see io.hexaglue.arch.query.ArchitectureQuery
 */
@Deprecated(forRemoval = true, since = "4.0.0")
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
    public List<DomainType> typesOfKind(ElementKind kind) {
        return types.stream().filter(t -> t.kind() == kind).toList();
    }

    /**
     * Returns all aggregate roots.
     */
    public List<DomainType> aggregateRoots() {
        return typesOfKind(ElementKind.AGGREGATE_ROOT);
    }

    /**
     * Returns all entities (including aggregate roots).
     */
    public List<DomainType> entities() {
        return types.stream()
                .filter(t -> t.kind() == ElementKind.ENTITY || t.kind() == ElementKind.AGGREGATE_ROOT)
                .toList();
    }

    /**
     * Returns all value objects.
     */
    public List<DomainType> valueObjects() {
        return typesOfKind(ElementKind.VALUE_OBJECT);
    }
}
