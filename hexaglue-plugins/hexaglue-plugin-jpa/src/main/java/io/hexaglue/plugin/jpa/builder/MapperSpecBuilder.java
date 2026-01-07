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

package io.hexaglue.plugin.jpa.builder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for transforming SPI DomainType to MapperSpec model.
 *
 * <p>This builder creates MapStruct mapper interface specifications that convert
 * between domain objects and JPA entities. It analyzes the domain type structure
 * and generates appropriate mapping annotations to handle:
 * <ul>
 *   <li>Identity field name differences (e.g., orderId → id)</li>
 *   <li>Wrapped identity types (e.g., OrderId → UUID)</li>
 *   <li>Embedded value objects</li>
 *   <li>Relationships with other domain types</li>
 *   <li>Ignored fields (audit fields, version fields)</li>
 * </ul>
 *
 * <p>Design decision: MapStruct provides compile-time type-safe mapping with
 * minimal runtime overhead. The builder generates explicit mapping annotations
 * for non-obvious conversions while relying on MapStruct's convention-based
 * mapping for simple field-to-field mappings.
 *
 * <h3>Generated Mapper Example:</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring", uses = {LineItemMapper.class})
 * public interface OrderMapper {
 *     @Mapping(target = "id", source = "orderId")
 *     @Mapping(target = "version", ignore = true)
 *     OrderEntity toEntity(Order domain);
 *
 *     @Mapping(target = "orderId", source = "id")
 *     Order toDomain(OrderEntity entity);
 * }
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MapperSpec spec = MapperSpecBuilder.builder()
 *     .domainType(orderType)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .allTypes(allDomainTypes)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0
 */
public final class MapperSpecBuilder {

    private DomainType domainType;
    private JpaConfig config;
    private String infrastructurePackage;
    private List<DomainType> allTypes;

    private MapperSpecBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new MapperSpecBuilder instance.
     *
     * @return a new builder instance
     */
    public static MapperSpecBuilder builder() {
        return new MapperSpecBuilder();
    }

    /**
     * Sets the domain type to transform.
     *
     * @param domainType the domain aggregate root or entity
     * @return this builder
     */
    public MapperSpecBuilder domainType(DomainType domainType) {
        this.domainType = domainType;
        return this;
    }

    /**
     * Sets the JPA plugin configuration.
     *
     * @param config the JPA configuration
     * @return this builder
     */
    public MapperSpecBuilder config(JpaConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Sets the infrastructure package name.
     *
     * <p>This is typically derived from the domain package by replacing
     * "domain" with "infrastructure.jpa".
     *
     * @param infrastructurePackage the package for generated JPA classes
     * @return this builder
     */
    public MapperSpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the list of all domain types in the application.
     *
     * <p>This is needed to identify relationships and determine which other
     * mappers this mapper depends on.
     *
     * @param allTypes all domain types from the IR snapshot
     * @return this builder
     */
    public MapperSpecBuilder allTypes(List<DomainType> allTypes) {
        this.allTypes = allTypes;
        return this;
    }

    /**
     * Builds the MapperSpec from the provided configuration.
     *
     * <p>This method performs the transformation from SPI DomainType to the
     * MapperSpec model. It analyzes the domain type structure to generate
     * appropriate mapping annotations for both toEntity and toDomain conversions.
     *
     * @return an immutable MapperSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     */
    public MapperSpec build() {
        validateRequiredFields();

        String interfaceName = domainType.simpleName() + config.mapperSuffix();

        TypeName domainTypeName = ClassName.bestGuess(domainType.qualifiedName());
        String entityClassName = domainType.simpleName() + config.entitySuffix();
        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);

        List<MappingSpec> toEntityMappings = buildToEntityMappings();
        List<MappingSpec> toDomainMappings = buildToDomainMappings();

        return new MapperSpec(
                infrastructurePackage, interfaceName, domainTypeName, entityType, toEntityMappings, toDomainMappings);
    }

    /**
     * Builds mapping specifications for domain to entity conversion.
     *
     * <p>This method analyzes the domain type and generates MapStruct mapping
     * annotations for cases where field names or types differ between domain
     * and entity representations.
     *
     * <p>Mapping rules:
     * <ul>
     *   <li>Identity field: Maps domain ID field name to "id" (e.g., orderId → id)</li>
     *   <li>Version field: Ignored when optimistic locking is enabled</li>
     *   <li>Audit fields: Ignored when auditing is enabled (createdDate, lastModifiedDate)</li>
     *   <li>Relationships: Handled by nested mappers (uses clause)</li>
     * </ul>
     *
     * @return list of mapping specifications for toEntity method
     */
    private List<MappingSpec> buildToEntityMappings() {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (domainType.hasIdentity()) {
            Identity identity = domainType.identity().orElseThrow();
            String identityFieldName = identity.fieldName();

            if (!identityFieldName.equals("id")) {
                mappings.add(MappingSpec.direct("id", identityFieldName));
            }
        }

        // Ignore version field if optimistic locking is enabled
        if (config.enableOptimisticLocking()) {
            mappings.add(MappingSpec.ignore("version"));
        }

        // Ignore audit fields if auditing is enabled
        if (config.enableAuditing()) {
            mappings.add(MappingSpec.ignore("createdDate"));
            mappings.add(MappingSpec.ignore("lastModifiedDate"));
        }

        return mappings;
    }

    /**
     * Builds mapping specifications for entity to domain conversion.
     *
     * <p>This method generates the reverse mappings for converting JPA entities
     * back to domain objects. It mirrors the toEntity mappings but in the
     * opposite direction.
     *
     * <p>Mapping rules:
     * <ul>
     *   <li>Identity field: Maps "id" to domain ID field name (e.g., id → orderId)</li>
     *   <li>All entity-specific fields (version, audit) are not mapped to domain</li>
     * </ul>
     *
     * @return list of mapping specifications for toDomain method
     */
    private List<MappingSpec> buildToDomainMappings() {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (domainType.hasIdentity()) {
            Identity identity = domainType.identity().orElseThrow();
            String identityFieldName = identity.fieldName();

            if (!identityFieldName.equals("id")) {
                mappings.add(MappingSpec.direct(identityFieldName, "id"));
            }
        }

        return mappings;
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        if (domainType == null) {
            throw new IllegalStateException("domainType is required");
        }
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }
        if (allTypes == null) {
            throw new IllegalStateException("allTypes is required");
        }
    }
}
