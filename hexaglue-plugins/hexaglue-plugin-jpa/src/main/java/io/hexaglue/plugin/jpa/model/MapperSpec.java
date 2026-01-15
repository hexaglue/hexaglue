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
 * @param valueObjectMappings information about Value Objects that need conversion methods
 * @param embeddableMappings information about VALUE_OBJECTs that map to JPA embeddables
 * @since 2.0.0
 */
public record MapperSpec(
        String packageName,
        String interfaceName,
        TypeName domainType,
        TypeName entityType,
        List<MappingSpec> toEntityMappings,
        List<MappingSpec> toDomainMappings,
        WrappedIdentitySpec wrappedIdentity,
        List<ValueObjectMappingSpec> valueObjectMappings,
        List<EmbeddableMappingSpec> embeddableMappings) {

    /**
     * Specification for a VALUE_OBJECT that maps to a JPA embeddable class.
     *
     * <p>This is used for complex VALUE_OBJECTs (multiple properties) that need
     * generated @Embeddable classes. MapStruct will generate the conversion methods
     * automatically based on matching property names.
     *
     * @param domainTypeFqn the fully qualified name of the domain VALUE_OBJECT
     * @param embeddableFqn the fully qualified name of the generated embeddable
     * @param domainSimpleName the simple name of the domain type
     * @param embeddableSimpleName the simple name of the embeddable class
     * @since 3.0.0
     */
    public record EmbeddableMappingSpec(
            String domainTypeFqn,
            String embeddableFqn,
            String domainSimpleName,
            String embeddableSimpleName) {}

    /**
     * Specification for a wrapped identity type that needs conversion methods.
     *
     * @param wrapperType the fully qualified name of the wrapper type (e.g., "TaskId")
     * @param unwrappedType the fully qualified name of the unwrapped type (e.g., "UUID")
     * @param accessorMethod the method to access the unwrapped value (e.g., "value")
     */
    public record WrappedIdentitySpec(String wrapperType, String unwrappedType, String accessorMethod) {}

    /**
     * Specification for a Value Object type that needs conversion methods in the mapper.
     *
     * <p>Value Objects are immutable types without identity. In JPA mapping, they are
     * typically embedded as components of an entity. MapStruct needs explicit conversion
     * methods to handle the mapping between Value Objects and their primitive representations.
     *
     * <p>For example, an {@code Email} value object record with a single {@code value} field
     * would generate:
     * <pre>{@code
     * default String map(Email email) {
     *     return email != null ? email.value() : null;
     * }
     *
     * default Email mapToEmail(String value) {
     *     return value != null ? new Email(value) : null;
     * }
     * }</pre>
     *
     * @param valueObjectType the fully qualified name of the Value Object type (e.g., "com.example.Email")
     * @param simpleName the simple name of the Value Object (e.g., "Email")
     * @param primitiveType the fully qualified name of the underlying primitive type (e.g., "java.lang.String")
     * @param accessorMethod the method to access the underlying value (e.g., "value" for records)
     * @param isRecord true if the Value Object is a Java record
     * @since 3.0.0
     */
    public record ValueObjectMappingSpec(
            String valueObjectType,
            String simpleName,
            String primitiveType,
            String accessorMethod,
            boolean isRecord) {

        /**
         * Creates a ValueObjectMappingSpec from a DomainType representing a Value Object or Identifier.
         *
         * <p>Both VALUE_OBJECT and IDENTIFIER types are supported because they use the same
         * single-property wrapper pattern and need similar conversion methods. IDENTIFIER types
         * are commonly used for inter-aggregate references (foreign keys like CustomerId).
         *
         * @param domainType the domain type representing the Value Object or Identifier
         * @return a ValueObjectMappingSpec if the type is a simple wrapper, null otherwise
         */
        public static ValueObjectMappingSpec from(DomainType domainType) {
            // Accept both VALUE_OBJECT and IDENTIFIER types
            boolean isWrapperType = domainType.isValueObject()
                    || domainType.kind() == io.hexaglue.spi.ir.DomainKind.IDENTIFIER;
            if (!isWrapperType) {
                return null;
            }

            // Only support types with a single property (simple wrappers)
            if (domainType.properties().size() != 1) {
                return null;
            }

            var property = domainType.properties().get(0);
            String primitiveType = property.type().qualifiedName();

            // Determine accessor method based on whether it's a record
            String accessorMethod = domainType.isRecord() ? property.name() : "get" + capitalize(property.name());

            return new ValueObjectMappingSpec(
                    domainType.qualifiedName(),
                    domainType.simpleName(),
                    primitiveType,
                    accessorMethod,
                    domainType.isRecord());
        }

        private static String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
    }

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
     * <p>Note: This method cannot detect Value Objects since it doesn't have
     * access to all domain types. Use {@link MapperSpecBuilder} for full
     * Value Object support.
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
                entityPackage,
                interfaceName,
                domainTypeName,
                entityType,
                toEntityMappings,
                toDomainMappings,
                null,
                List.of(),
                List.of());
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
