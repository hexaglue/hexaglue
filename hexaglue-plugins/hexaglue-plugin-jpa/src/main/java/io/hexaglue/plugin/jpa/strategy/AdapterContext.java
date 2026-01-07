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

package io.hexaglue.plugin.jpa.strategy;

import com.palantir.javapoet.TypeName;

/**
 * Context information for adapter method generation.
 *
 * <p>This immutable record encapsulates all the metadata needed by method
 * generation strategies to produce correct adapter implementations. It provides
 * type information and field names that strategies use when generating method bodies.
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Immutability: Record ensures thread-safety and clarity</li>
 *   <li>Context Object Pattern: Avoids passing many individual parameters</li>
 *   <li>Type Safety: Uses JavaPoet TypeName for compile-time correctness</li>
 *   <li>Naming: Field names match the adapter's actual field declarations</li>
 * </ul>
 *
 * <h3>Usage in Generated Code:</h3>
 * <p>For an Order aggregate, this context would contain:
 * <pre>{@code
 * AdapterContext(
 *     domainClass = Order.class,
 *     entityClass = OrderEntity.class,
 *     repositoryFieldName = "repository",
 *     mapperFieldName = "mapper"
 * )
 * }</pre>
 *
 * <p>Which enables strategies to generate code like:
 * <pre>{@code
 * var entity = mapper.toEntity(domain);  // Uses mapperFieldName
 * var saved = repository.save(entity);   // Uses repositoryFieldName
 * return mapper.toDomain(saved);         // Uses mapperFieldName
 * }</pre>
 *
 * @param domainClass the JavaPoet type of the domain aggregate (e.g., Order)
 * @param entityClass the JavaPoet type of the JPA entity (e.g., OrderEntity)
 * @param repositoryFieldName the name of the repository field in the adapter (typically "repository")
 * @param mapperFieldName the name of the mapper field in the adapter (typically "mapper")
 * @since 2.0.0
 */
public record AdapterContext(
        TypeName domainClass, TypeName entityClass, String repositoryFieldName, String mapperFieldName) {

    /**
     * Creates an AdapterContext with all required metadata.
     *
     * @param domainClass the domain type
     * @param entityClass the entity type
     * @param repositoryFieldName the repository field name
     * @param mapperFieldName the mapper field name
     * @throws IllegalArgumentException if any parameter is null or if field names are empty
     */
    public AdapterContext {
        if (domainClass == null) {
            throw new IllegalArgumentException("Domain class cannot be null");
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        if (repositoryFieldName == null || repositoryFieldName.isEmpty()) {
            throw new IllegalArgumentException("Repository field name cannot be null or empty");
        }
        if (mapperFieldName == null || mapperFieldName.isEmpty()) {
            throw new IllegalArgumentException("Mapper field name cannot be null or empty");
        }
    }
}
