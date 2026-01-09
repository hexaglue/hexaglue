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
import java.util.List;

/**
 * Specification for generating a MapStruct mapper interface.
 *
 * <p>This record captures the information needed to generate a mapper that
 * converts between domain objects and JPA entities. Uses MapStruct for
 * compile-time type-safe mapping generation.
 *
 * <p>Design decision: Mappers provide clean separation between domain and
 * persistence layers. MapStruct generates efficient implementations at
 * compile time, avoiding runtime reflection overhead.
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * package com.example.infrastructure.jpa;
 *
 * import org.mapstruct.Mapper;
 * import org.mapstruct.Mapping;
 *
 * @Mapper(componentModel = "spring")
 * public interface OrderMapper {
 *     @Mapping(target = "id", source = "orderId")
 *     OrderEntity toEntity(Order domain);
 *
 *     @Mapping(target = "orderId", source = "id")
 *     Order toDomain(OrderEntity entity);
 * }
 * }</pre>
 *
 * @param packageName the package for the generated mapper interface
 * @param interfaceName the simple interface name (e.g., "OrderMapper")
 * @param domainType the JavaPoet type of the domain object
 * @param entityType the JavaPoet type of the JPA entity
 * @param toEntityMappings the mappings for domain → entity conversion
 * @param toDomainMappings the mappings for entity → domain conversion
 * @param wrappedIdentity information about wrapped identity type, if present
 * @since 2.0.0
 */
public record MapperSpec(
        String packageName,
        String interfaceName,
        TypeName domainType,
        TypeName entityType,
        List<MappingSpec> toEntityMappings,
        List<MappingSpec> toDomainMappings,
        WrappedIdentitySpec wrappedIdentity) {

    /**
     * Specification for a wrapped identity type that needs conversion methods.
     *
     * @param wrapperType the fully qualified name of the wrapper type (e.g., "TaskId")
     * @param unwrappedType the fully qualified name of the unwrapped type (e.g., "UUID")
     * @param accessorMethod the method to access the unwrapped value (e.g., "value")
     */
    public record WrappedIdentitySpec(String wrapperType, String unwrappedType, String accessorMethod) {}

    /**
     * A single field mapping specification for MapStruct.
     *
     * <p>Represents a {@code @Mapping} annotation in the generated code.
     *
     * @param target the target field name
     * @param source the source field name (null for ignore)
     * @param expression the MapStruct expression (null for direct mapping)
     */
    public record MappingSpec(String target, String source, String expression) {

        /**
         * Creates a direct field mapping.
         *
         * @param target the target field name
         * @param source the source field name
         * @return a direct mapping spec
         */
        public static MappingSpec direct(String target, String source) {
            return new MappingSpec(target, source, null);
        }

        /**
         * Creates an ignore mapping.
         *
         * @param target the target field to ignore
         * @return an ignore mapping spec
         */
        public static MappingSpec ignore(String target) {
            return new MappingSpec(target, null, "ignore = true");
        }

        /**
         * Creates an expression mapping.
         *
         * @param target the target field name
         * @param expression the MapStruct expression
         * @return an expression mapping spec
         */
        public static MappingSpec expression(String target, String expression) {
            return new MappingSpec(target, null, expression);
        }

        /**
         * Returns true if this is a direct mapping (no expression).
         *
         * @return true if expression is null and source is not null
         */
        public boolean isDirect() {
            return expression == null && source != null;
        }

        /**
         * Returns true if this is an ignore mapping.
         *
         * @return true if source is null and expression contains "ignore"
         */
        public boolean isIgnore() {
            return source == null && expression != null && expression.contains("ignore");
        }
    }

    /**
     * Creates a MapperSpec from a SPI DomainType and JpaConfig.
     *
     * <p>This factory method derives the mapper metadata and infers basic
     * field mappings. Complex mappings (nested objects, collections) may
     * require custom logic.
     *
     * <p>Naming convention:
     * <ul>
     *   <li>Interface name: {DomainName} + {mapperSuffix}</li>
     *   <li>Package: {entityPackage} (same as entity)</li>
     * </ul>
     *
     * @param domainType the domain aggregate root
     * @param config the JPA plugin configuration
     * @return a MapperSpec ready for code generation
     */
    public static MapperSpec from(DomainType domainType, JpaConfig config) {
        // Derive package and names
        String entityPackage = JpaModelUtils.deriveInfrastructurePackage(domainType.packageName());
        String interfaceName = domainType.simpleName() + config.mapperSuffix();

        // Resolve types
        TypeName domainTypeName = ClassName.bestGuess(domainType.qualifiedName());
        String entityClassName = domainType.simpleName() + config.entitySuffix();
        TypeName entityType = ClassName.get(entityPackage, entityClassName);

        // Derive mappings (basic implementation - can be enhanced)
        List<MappingSpec> toEntityMappings = inferToEntityMappings(domainType);
        List<MappingSpec> toDomainMappings = inferToDomainMappings(domainType);

        return new MapperSpec(
                entityPackage, interfaceName, domainTypeName, entityType, toEntityMappings, toDomainMappings, null);
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
     * Returns true if this mapper has any custom mappings.
     *
     * @return true if there are mappings for either direction
     */
    public boolean hasCustomMappings() {
        return !toEntityMappings.isEmpty() || !toDomainMappings.isEmpty();
    }

    /**
     * Infers mappings for domain → entity conversion.
     *
     * <p>This is a placeholder for basic mapping logic. In a full implementation,
     * this would analyze the domain type's properties and generate appropriate
     * MapStruct mapping annotations.
     *
     * <p>Current implementation: Returns empty list (MapStruct will use defaults).
     *
     * @param domainType the domain type
     * @return list of mapping specs (currently empty)
     */
    private static List<MappingSpec> inferToEntityMappings(DomainType domainType) {
        // TODO: Implement intelligent mapping inference based on:
        // - Identity field name differences (orderId → id)
        // - Wrapped types (OrderId → UUID)
        // - Embedded value objects
        // - Collections
        return List.of();
    }

    /**
     * Infers mappings for entity → domain conversion.
     *
     * <p>This is a placeholder for basic mapping logic. In a full implementation,
     * this would analyze the entity's fields and generate appropriate
     * MapStruct mapping annotations.
     *
     * <p>Current implementation: Returns empty list (MapStruct will use defaults).
     *
     * @param domainType the domain type
     * @return list of mapping specs (currently empty)
     */
    private static List<MappingSpec> inferToDomainMappings(DomainType domainType) {
        // TODO: Implement intelligent mapping inference
        return List.of();
    }
}
