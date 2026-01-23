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

package io.hexaglue.plugin.jpa.model;

import com.palantir.javapoet.TypeName;
import java.util.List;

/**
 * Specification for generating a Spring Data JPA repository interface.
 *
 * <p>This record captures the information needed to generate a repository
 * interface that extends {@code JpaRepository<Entity, ID>}.
 *
 * <p>Design decision: Repository interfaces in Spring Data JPA are purely
 * declarative. We generate them as markers that extend JpaRepository and
 * optionally include custom query methods derived from the domain repository port.
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * package com.example.infrastructure.jpa;
 *
 * import org.springframework.data.jpa.repository.JpaRepository;
 * import java.util.UUID;
 *
 * public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
 *     // Spring Data JPA provides implementations for:
 *     // - save(entity)
 *     // - findById(id)
 *     // - findAll()
 *     // - delete(entity)
 *     // - etc.
 * }
 * }</pre>
 *
 * @param packageName the package for the generated repository interface
 * @param interfaceName the simple interface name (e.g., "OrderJpaRepository")
 * @param entityType the JavaPoet type of the entity class
 * @param idType the JavaPoet type of the entity's ID field
 * @param domainQualifiedName the fully qualified name of the domain aggregate root
 * @param derivedMethods custom query methods to generate (e.g., findByEmail, existsByStatus)
 * @since 2.0.0
 */
public record RepositorySpec(
        String packageName,
        String interfaceName,
        TypeName entityType,
        TypeName idType,
        String domainQualifiedName,
        List<DerivedMethodSpec> derivedMethods) {

    public RepositorySpec {
        derivedMethods = List.copyOf(derivedMethods);
    }

    /**
     * Returns the fully qualified interface name.
     *
     * @return packageName + "." + interfaceName
     */
    public String fullyQualifiedInterfaceName() {
        return packageName + "." + interfaceName;
    }

    /**
     * Returns the simple name of the domain aggregate.
     *
     * @return the simple name extracted from domainQualifiedName
     */
    public String domainSimpleName() {
        int lastDot = domainQualifiedName.lastIndexOf('.');
        return lastDot < 0 ? domainQualifiedName : domainQualifiedName.substring(lastDot + 1);
    }

    /**
     * Returns the simple name of the JPA entity.
     *
     * @return the simple name extracted from entityType
     * @since 2.0.0
     */
    public String entitySimpleName() {
        if (entityType instanceof com.palantir.javapoet.ClassName className) {
            return className.simpleName();
        }
        // Fallback: extract from toString() if not a ClassName
        String typeString = entityType.toString();
        int lastDot = typeString.lastIndexOf('.');
        return lastDot < 0 ? typeString : typeString.substring(lastDot + 1);
    }
}
