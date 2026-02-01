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
 * @Mapper(componentModel = "spring", uses = {CourseMapper.class})
 * public interface LessonMapper {
 *     @Mapping(target = "id", source = "lessonId")
 *     LessonEntity toEntity(Lesson domain);
 *
 *     @Mapping(target = "lessonId", source = "id")
 *     Lesson toDomain(LessonEntity entity);
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
 * @param usedMappers list of other mappers this mapper should delegate to for entity relationships
 * @param reconstitutionSpec specification for generating a default toDomain() method using a factory method,
 *        or null to fall back to MapStruct's abstract method generation
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
        List<EmbeddableMappingSpec> embeddableMappings,
        List<ClassName> usedMappers,
        ReconstitutionSpec reconstitutionSpec) {

    /**
     * Canonical constructor with defensive copies.
     */
    public MapperSpec {
        toEntityMappings = List.copyOf(toEntityMappings);
        toDomainMappings = List.copyOf(toDomainMappings);
        valueObjectMappings = List.copyOf(valueObjectMappings);
        embeddableMappings = List.copyOf(embeddableMappings);
        usedMappers = usedMappers == null ? List.of() : List.copyOf(usedMappers);
    }

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
            String domainTypeFqn, String embeddableFqn, String domainSimpleName, String embeddableSimpleName) {}

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
            String valueObjectType, String simpleName, String primitiveType, String accessorMethod, boolean isRecord) {}

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
     * Specification for generating a {@code default toDomain()} method that calls
     * a domain factory method (e.g., {@code reconstitute()}) instead of relying
     * on MapStruct's abstract method generation.
     *
     * <p>This is needed when domain objects follow DDD rich model patterns with
     * private constructors and no setters. MapStruct cannot instantiate such objects
     * via its default strategy (no-arg constructor + setters).
     *
     * @param factoryMethodName the name of the static factory method (e.g., "reconstitute")
     * @param domainTypeQualifiedName the fully qualified name of the domain type
     * @param parameters the ordered list of parameter specifications for the factory call
     * @since 5.0.0
     */
    public record ReconstitutionSpec(
            String factoryMethodName, String domainTypeQualifiedName, List<ReconstitutionParameterSpec> parameters) {

        /**
         * Canonical constructor with defensive copy.
         */
        public ReconstitutionSpec {
            parameters = List.copyOf(parameters);
        }
    }

    /**
     * Specification for a single parameter in a reconstitution factory method call.
     *
     * <p>Each parameter maps to an entity getter expression. The {@link ConversionKind}
     * determines how the getter value is transformed before being passed to the factory.
     *
     * @param parameterName the parameter name in the factory method
     * @param parameterTypeQualifiedName the fully qualified type of the parameter
     * @param entityFieldName the corresponding field name on the JPA entity
     * @param conversionKind how to convert the entity field value to the parameter type
     * @since 5.0.0
     */
    public record ReconstitutionParameterSpec(
            String parameterName,
            String parameterTypeQualifiedName,
            String entityFieldName,
            ConversionKind conversionKind) {}

    /**
     * Describes how an entity field value is converted when passed to
     * the domain reconstitution factory method.
     *
     * @since 5.0.0
     */
    public enum ConversionKind {

        /** Direct pass-through, no conversion needed (e.g., {@code entity.getFirstName()}). */
        DIRECT,

        /** Wrapped identity conversion via existing {@code map()} helper (e.g., {@code map(entity.getId())}). */
        WRAPPED_IDENTITY,

        /** Single-value Value Object conversion via {@code mapToXxx()} helper (e.g., {@code mapToEmail(entity.getEmail())}). */
        VALUE_OBJECT,

        /** Multi-value Value Object embedded conversion via overloaded {@code toDomain()} (e.g., {@code toDomain(entity.getAmount())}). */
        EMBEDDED_VALUE_OBJECT,

        /** Entity relationship, delegated via {@code @Mapper(uses={})} (e.g., {@code entity.getItems()}). */
        ENTITY_RELATION
    }
}
