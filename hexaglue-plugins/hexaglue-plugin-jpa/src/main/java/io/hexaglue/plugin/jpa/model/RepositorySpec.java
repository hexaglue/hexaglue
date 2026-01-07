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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.spi.ir.DomainType;

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
 * @since 2.0.0
 */
public record RepositorySpec(
        String packageName, String interfaceName, TypeName entityType, TypeName idType, String domainQualifiedName) {

    /**
     * Creates a RepositorySpec from a SPI DomainType and JpaConfig.
     *
     * <p>This factory method derives the repository metadata from the domain
     * aggregate root and applies naming conventions from the configuration.
     *
     * <p>Naming convention:
     * <ul>
     *   <li>Interface name: {DomainName} + {repositorySuffix}</li>
     *   <li>Package: {entityPackage} (same as entity)</li>
     * </ul>
     *
     * @param domainType the aggregate root domain type
     * @param config the JPA plugin configuration
     * @return a RepositorySpec ready for code generation
     * @throws IllegalArgumentException if the domain type has no identity
     */
    public static RepositorySpec from(DomainType domainType, JpaConfig config) {
        if (!domainType.hasIdentity()) {
            throw new IllegalArgumentException(
                    "Domain type " + domainType.qualifiedName() + " has no identity. Cannot generate repository.");
        }

        // Derive entity class name
        String entityClassName = domainType.simpleName() + config.entitySuffix();
        String entityPackage = JpaModelUtils.deriveInfrastructurePackage(domainType.packageName());

        // Derive repository interface name
        String interfaceName = domainType.simpleName() + config.repositorySuffix();

        // Resolve entity and ID types
        TypeName entityType = ClassName.get(entityPackage, entityClassName);
        TypeName idType = resolveIdType(domainType);

        return new RepositorySpec(entityPackage, interfaceName, entityType, idType, domainType.qualifiedName());
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
     * Resolves the ID type from the domain type's identity.
     *
     * <p>Uses the unwrapped type for JPA compatibility. Wrapped identity types
     * (e.g., {@code record OrderId(UUID value)}) are unwrapped to their underlying
     * type (e.g., {@code UUID}) for use in the JpaRepository generic parameter.
     *
     * @param domainType the domain aggregate root
     * @return the JavaPoet TypeName of the ID type
     */
    private static TypeName resolveIdType(DomainType domainType) {
        String idTypeName = domainType
                .identity()
                .orElseThrow(() -> new IllegalArgumentException("Domain type has no identity"))
                .unwrappedType()
                .qualifiedName();

        return ClassName.bestGuess(idTypeName);
    }
}
