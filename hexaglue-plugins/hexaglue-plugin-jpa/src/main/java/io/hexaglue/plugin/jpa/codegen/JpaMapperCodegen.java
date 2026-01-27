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

package io.hexaglue.plugin.jpa.codegen;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ValueObjectMappingSpec;
import io.hexaglue.plugin.jpa.util.JpaAnnotations;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/**
 * Generates MapStruct mapper interfaces using JavaPoet.
 *
 * <p>This generator creates mapper interfaces that convert between domain objects
 * and JPA entities. MapStruct generates efficient type-safe implementations at
 * compile time, avoiding runtime reflection overhead.
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * package com.example.infrastructure.jpa;
 *
 * import javax.annotation.processing.Generated;
 * import org.mapstruct.Mapper;
 * import org.mapstruct.Mapping;
 *
 * @Generated(value = "io.hexaglue.plugin.jpa")
 * @Mapper(componentModel = "spring")
 * public interface OrderMapper {
 *
 *     @Mapping(target = "id", source = "orderId.value")
 *     OrderEntity toEntity(Order domain);
 *
 *     @Mapping(target = "orderId", source = "id")
 *     Order toDomain(OrderEntity entity);
 * }
 * }</pre>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li><b>Interface-based:</b> MapStruct generates implementation classes automatically</li>
 *   <li><b>Spring integration:</b> Uses {@code componentModel = "spring"} for dependency injection</li>
 *   <li><b>Bidirectional mapping:</b> Generates both {@code toEntity} and {@code toDomain} methods</li>
 *   <li><b>Custom mappings:</b> Supports {@code @Mapping} annotations for field name differences</li>
 *   <li><b>Type safety:</b> MapStruct validates mappings at compile time</li>
 *   <li><b>Stateless utility class:</b> All methods are static, no instance state</li>
 * </ul>
 *
 * <h3>Mapping Types:</h3>
 * <p>The generator supports three types of mappings via {@link MappingSpec}:
 * <ul>
 *   <li><b>Direct mappings:</b> {@code @Mapping(target = "id", source = "orderId")}</li>
 *   <li><b>Ignore mappings:</b> {@code @Mapping(target = "createdAt", ignore = true)}</li>
 *   <li><b>Expression mappings:</b> {@code @Mapping(target = "now", expression = "java(Instant.now())")}</li>
 * </ul>
 *
 * <h3>MapStruct Integration:</h3>
 * <p>Generated mappers benefit from MapStruct's capabilities:
 * <ul>
 *   <li>Type-safe compile-time validation</li>
 *   <li>Automatic nested object mapping</li>
 *   <li>Collection mapping (List, Set, etc.)</li>
 *   <li>Type conversion (String to UUID, etc.)</li>
 *   <li>No runtime reflection overhead</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class JpaMapperCodegen {

    /**
     * The generator identifier used in {@code @Generated} annotations.
     */
    private static final String GENERATOR_NAME = "io.hexaglue.plugin.jpa";

    private static final String PLUGIN_VERSION = Optional.ofNullable(
                    JpaMapperCodegen.class.getPackage().getImplementationVersion())
            .orElse("dev");

    private JpaMapperCodegen() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a MapStruct mapper interface from a MapperSpec.
     *
     * <p>The generated interface:
     * <ul>
     *   <li>Is public and marked as an interface</li>
     *   <li>Has {@code @Generated} annotation for documentation</li>
     *   <li>Has {@code @Mapper(componentModel = "spring")} for Spring integration</li>
     *   <li>Contains {@code toEntity()} method to convert domain → entity</li>
     *   <li>Contains {@code toDomain()} method to convert entity → domain</li>
     *   <li>Includes {@code @Mapping} annotations for custom field mappings</li>
     * </ul>
     *
     * <p><b>Method Signatures:</b>
     * <ul>
     *   <li>{@code EntityType toEntity(DomainType domain)} - converts domain to JPA entity</li>
     *   <li>{@code DomainType toDomain(EntityType entity)} - converts JPA entity to domain</li>
     * </ul>
     *
     * @param spec the mapper specification containing package, interface name, types, and mappings
     * @return the generated TypeSpec for the mapper interface
     * @throws IllegalArgumentException if spec is null
     */
    public static TypeSpec generate(MapperSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("MapperSpec cannot be null");
        }

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(spec.interfaceName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JpaAnnotations.generated(GENERATOR_NAME, PLUGIN_VERSION))
                .addAnnotation(buildMapperAnnotation(spec))
                .addJavadoc(
                        "MapStruct mapper for converting between {@link $L} domain objects and {@link $L} entities.\n\n",
                        spec.domainType(),
                        spec.entityType())
                .addJavadoc("<p>This interface provides bidirectional mapping:\n")
                .addJavadoc("<ul>\n")
                .addJavadoc("  <li>{@code toEntity()} - converts domain to JPA entity</li>\n")
                .addJavadoc("  <li>{@code toDomain()} - converts JPA entity to domain</li>\n")
                .addJavadoc("</ul>\n")
                .addJavadoc("\n")
                .addJavadoc("<p>MapStruct generates the implementation at compile time, ensuring type safety\n")
                .addJavadoc("and optimal performance without runtime reflection.\n");

        // Add toEntity method with custom mappings
        builder.addMethod(createToEntityMethod(spec));

        // Add toDomain method with custom mappings
        builder.addMethod(createToDomainMethod(spec));

        // Add identity conversion methods if identity is wrapped
        if (spec.wrappedIdentity() != null) {
            builder.addMethod(createMapToUnwrappedMethod(spec.wrappedIdentity()));
            builder.addMethod(createMapToWrappedMethod(spec.wrappedIdentity()));
        }

        // Add Value Object conversion methods
        if (spec.valueObjectMappings() != null) {
            for (ValueObjectMappingSpec voSpec : spec.valueObjectMappings()) {
                builder.addMethod(createValueObjectToUnwrappedMethod(voSpec));
                builder.addMethod(createValueObjectToWrappedMethod(voSpec));
            }
        }

        // Add Embeddable conversion methods (for complex VALUE_OBJECTs)
        if (spec.embeddableMappings() != null) {
            for (MapperSpec.EmbeddableMappingSpec embSpec : spec.embeddableMappings()) {
                builder.addMethod(createEmbeddableToEntityMethod(embSpec));
                builder.addMethod(createEmbeddableToDomainMethod(embSpec));
            }
        }

        return builder.build();
    }

    /**
     * Creates a method to convert domain VALUE_OBJECT to JPA embeddable.
     *
     * <p>This method allows MapStruct to automatically convert from domain types
     * to their JPA embeddable equivalents. MapStruct will generate the implementation
     * based on matching property names.
     *
     * <pre>{@code
     * LineItemEmbeddable toEmbeddable(LineItem domain);
     * }</pre>
     *
     * @param embSpec the embeddable mapping specification
     * @return the method spec for domain to embeddable conversion
     */
    private static MethodSpec createEmbeddableToEntityMethod(MapperSpec.EmbeddableMappingSpec embSpec) {
        ClassName domainClass = ClassName.bestGuess(embSpec.domainTypeFqn());
        ClassName embeddableClass = ClassName.bestGuess(embSpec.embeddableFqn());

        return MethodSpec.methodBuilder("toEmbeddable")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(embeddableClass)
                .addParameter(domainClass, "domain")
                .addJavadoc(
                        "Converts {@link $L} domain object to {@link $L} JPA embeddable.\n\n",
                        embSpec.domainSimpleName(),
                        embSpec.embeddableSimpleName())
                .addJavadoc("@param domain the domain VALUE_OBJECT to convert\n")
                .addJavadoc("@return the corresponding JPA embeddable\n")
                .build();
    }

    /**
     * Creates a method to convert JPA embeddable to domain VALUE_OBJECT.
     *
     * <p>This method allows MapStruct to automatically convert from JPA embeddables
     * to their domain equivalents. MapStruct will generate the implementation
     * based on matching property names.
     *
     * <pre>{@code
     * LineItem toDomain(LineItemEmbeddable embeddable);
     * }</pre>
     *
     * @param embSpec the embeddable mapping specification
     * @return the method spec for embeddable to domain conversion
     */
    private static MethodSpec createEmbeddableToDomainMethod(MapperSpec.EmbeddableMappingSpec embSpec) {
        ClassName domainClass = ClassName.bestGuess(embSpec.domainTypeFqn());
        ClassName embeddableClass = ClassName.bestGuess(embSpec.embeddableFqn());

        return MethodSpec.methodBuilder("toDomain")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(domainClass)
                .addParameter(embeddableClass, "embeddable")
                .addJavadoc(
                        "Converts {@link $L} JPA embeddable to {@link $L} domain object.\n\n",
                        embSpec.embeddableSimpleName(),
                        embSpec.domainSimpleName())
                .addJavadoc("@param embeddable the JPA embeddable to convert\n")
                .addJavadoc("@return the corresponding domain VALUE_OBJECT\n")
                .build();
    }

    /**
     * Generates a complete JavaFile for the mapper interface.
     *
     * <p>This method wraps the generated TypeSpec in a JavaFile with:
     * <ul>
     *   <li>The package declaration from the spec</li>
     *   <li>Automatic import management (JavaPoet handles this)</li>
     *   <li>4-space indentation (Palantir Java Format standard)</li>
     * </ul>
     *
     * <p>The resulting JavaFile can be written directly to disk using:
     * <pre>{@code
     * JavaFile javaFile = JpaMapperCodegen.generateFile(spec);
     * javaFile.writeTo(outputDirectory);
     * }</pre>
     *
     * @param spec the mapper specification
     * @return the generated JavaFile ready to be written
     * @throws IllegalArgumentException if spec is null
     */
    public static JavaFile generateFile(MapperSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("MapperSpec cannot be null");
        }

        TypeSpec mapperInterface = generate(spec);

        return JavaFile.builder(spec.packageName(), mapperInterface)
                .indent("    ") // 4 spaces for indentation
                .build();
    }

    // =====================================================================
    // Method generation
    // =====================================================================

    /**
     * Creates the {@code toEntity()} method with custom mappings.
     *
     * <p>This method converts domain objects to JPA entities. It:
     * <ul>
     *   <li>Takes a domain object as parameter</li>
     *   <li>Returns a JPA entity</li>
     *   <li>Applies {@code @Mapping} annotations from the spec</li>
     *   <li>Is abstract (MapStruct generates implementation)</li>
     * </ul>
     *
     * <p>Generated signature:
     * <pre>{@code
     * @Mapping(target = "id", source = "orderId.value")
     * OrderEntity toEntity(Order domain);
     * }</pre>
     *
     * @param spec the mapper specification
     * @return the {@code toEntity()} method spec
     */
    private static MethodSpec createToEntityMethod(MapperSpec spec) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(spec.entityType())
                .addParameter(spec.domainType(), "domain")
                .addJavadoc("Converts a domain object to a JPA entity.\n\n")
                .addJavadoc("@param domain the domain object to convert\n")
                .addJavadoc("@return the corresponding JPA entity\n");

        // Add @Mapping annotations for custom field mappings
        for (MappingSpec mapping : spec.toEntityMappings()) {
            methodBuilder.addAnnotation(buildMappingAnnotation(mapping));
        }

        return methodBuilder.build();
    }

    /**
     * Creates the {@code toDomain()} method with custom mappings.
     *
     * <p>This method converts JPA entities to domain objects. It:
     * <ul>
     *   <li>Takes a JPA entity as parameter</li>
     *   <li>Returns a domain object</li>
     *   <li>Applies {@code @Mapping} annotations from the spec</li>
     *   <li>Is abstract (MapStruct generates implementation)</li>
     * </ul>
     *
     * <p>Generated signature:
     * <pre>{@code
     * @Mapping(target = "orderId", source = "id")
     * Order toDomain(OrderEntity entity);
     * }</pre>
     *
     * @param spec the mapper specification
     * @return the {@code toDomain()} method spec
     */
    private static MethodSpec createToDomainMethod(MapperSpec spec) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toDomain")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(spec.domainType())
                .addParameter(spec.entityType(), "entity")
                .addJavadoc("Converts a JPA entity to a domain object.\n\n")
                .addJavadoc("@param entity the JPA entity to convert\n")
                .addJavadoc("@return the corresponding domain object\n");

        // Add @Mapping annotations for custom field mappings
        for (MappingSpec mapping : spec.toDomainMappings()) {
            methodBuilder.addAnnotation(buildMappingAnnotation(mapping));
        }

        return methodBuilder.build();
    }

    /**
     * Creates a method to unwrap identity values (e.g., TaskId → UUID).
     *
     * <p>This method allows MapStruct to automatically convert from wrapper types
     * to their underlying primitive types. For example:
     * <pre>{@code
     * default UUID map(TaskId id) {
     *     return id != null ? id.value() : null;
     * }
     * }</pre>
     *
     * @param wrappedIdentity the wrapped identity specification
     * @return the method spec for unwrapping
     */
    private static MethodSpec createMapToUnwrappedMethod(MapperSpec.WrappedIdentitySpec wrappedIdentity) {
        com.palantir.javapoet.ClassName wrapperClass =
                com.palantir.javapoet.ClassName.bestGuess(wrappedIdentity.wrapperType());
        com.palantir.javapoet.TypeName unwrappedType =
                com.palantir.javapoet.ClassName.bestGuess(wrappedIdentity.unwrappedType());

        return MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(unwrappedType)
                .addParameter(wrapperClass, "id")
                .addStatement("return id != null ? id.$L() : null", wrappedIdentity.accessorMethod())
                .addJavadoc(
                        "Unwraps {@link $L} to {@link $L}.\n\n",
                        wrappedIdentity.wrapperType(),
                        wrappedIdentity.unwrappedType())
                .addJavadoc("@param id the wrapped identity\n")
                .addJavadoc("@return the unwrapped value, or null if input is null\n")
                .build();
    }

    /**
     * Creates a method to wrap identity values (e.g., UUID → TaskId).
     *
     * <p>This method allows MapStruct to automatically convert from primitive types
     * to their wrapper types. For example:
     * <pre>{@code
     * default TaskId map(UUID id) {
     *     return id != null ? new TaskId(id) : null;
     * }
     * }</pre>
     *
     * @param wrappedIdentity the wrapped identity specification
     * @return the method spec for wrapping
     */
    private static MethodSpec createMapToWrappedMethod(MapperSpec.WrappedIdentitySpec wrappedIdentity) {
        com.palantir.javapoet.ClassName wrapperClass =
                com.palantir.javapoet.ClassName.bestGuess(wrappedIdentity.wrapperType());
        com.palantir.javapoet.TypeName unwrappedType =
                com.palantir.javapoet.ClassName.bestGuess(wrappedIdentity.unwrappedType());

        return MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(wrapperClass)
                .addParameter(unwrappedType, "id")
                .addStatement("return id != null ? new $T(id) : null", wrapperClass)
                .addJavadoc(
                        "Wraps {@link $L} into {@link $L}.\n\n",
                        wrappedIdentity.unwrappedType(),
                        wrappedIdentity.wrapperType())
                .addJavadoc("@param id the unwrapped value\n")
                .addJavadoc("@return the wrapped identity, or null if input is null\n")
                .build();
    }

    // =====================================================================
    // Value Object conversion methods
    // =====================================================================

    /**
     * Creates a method to unwrap Value Object values (e.g., Email → String).
     *
     * <p>This method allows MapStruct to automatically convert from Value Object types
     * to their underlying primitive types. For example:
     * <pre>{@code
     * default String map(Email email) {
     *     return email != null ? email.value() : null;
     * }
     * }</pre>
     *
     * @param voSpec the Value Object mapping specification
     * @return the method spec for unwrapping
     */
    private static MethodSpec createValueObjectToUnwrappedMethod(ValueObjectMappingSpec voSpec) {
        ClassName valueObjectClass = ClassName.bestGuess(voSpec.valueObjectType());
        TypeName primitiveType = toTypeName(voSpec.primitiveType());

        return MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(primitiveType)
                .addParameter(valueObjectClass, "vo")
                .addStatement("return vo != null ? vo.$L() : null", voSpec.accessorMethod())
                .addJavadoc("Unwraps {@link $L} to {@link $L}.\n\n", voSpec.simpleName(), voSpec.primitiveType())
                .addJavadoc("@param vo the Value Object to unwrap\n")
                .addJavadoc("@return the underlying value, or null if input is null\n")
                .build();
    }

    /**
     * Creates a method to wrap primitive values into Value Objects (e.g., String → Email).
     *
     * <p>This method allows MapStruct to automatically convert from primitive types
     * to their Value Object wrappers. For example:
     * <pre>{@code
     * default Email mapToEmail(String value) {
     *     return value != null ? new Email(value) : null;
     * }
     * }</pre>
     *
     * @param voSpec the Value Object mapping specification
     * @return the method spec for wrapping
     */
    private static MethodSpec createValueObjectToWrappedMethod(ValueObjectMappingSpec voSpec) {
        ClassName valueObjectClass = ClassName.bestGuess(voSpec.valueObjectType());
        TypeName primitiveType = toTypeName(voSpec.primitiveType());

        return MethodSpec.methodBuilder("mapTo" + voSpec.simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(valueObjectClass)
                .addParameter(primitiveType, "value")
                .addStatement("return value != null ? new $T(value) : null", valueObjectClass)
                .addJavadoc("Wraps {@link $L} into {@link $L}.\n\n", voSpec.primitiveType(), voSpec.simpleName())
                .addJavadoc("@param value the primitive value to wrap\n")
                .addJavadoc("@return the Value Object, or null if input is null\n")
                .build();
    }

    /**
     * Converts a type name string to a JavaPoet TypeName.
     *
     * <p>Handles primitive types by boxing them (e.g., "int" → Integer)
     * to support null checks in generated code. For reference types,
     * uses {@link ClassName#bestGuess(String)}.
     *
     * @param typeName the qualified type name
     * @return the corresponding TypeName
     */
    private static TypeName toTypeName(String typeName) {
        return switch (typeName) {
            case "int" -> ClassName.get(Integer.class);
            case "long" -> ClassName.get(Long.class);
            case "short" -> ClassName.get(Short.class);
            case "byte" -> ClassName.get(Byte.class);
            case "float" -> ClassName.get(Float.class);
            case "double" -> ClassName.get(Double.class);
            case "boolean" -> ClassName.get(Boolean.class);
            case "char" -> ClassName.get(Character.class);
            default -> ClassName.bestGuess(typeName);
        };
    }

    // =====================================================================
    // Annotation helpers
    // =====================================================================

    /**
     * Builds a {@code @Mapper} annotation with optional uses clause.
     *
     * <p>If the spec has usedMappers, generates:
     * <pre>{@code
     * @Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE, uses = {CourseMapper.class})
     * }</pre>
     *
     * <p>Otherwise generates the basic annotation without uses clause.
     *
     * @param spec the mapper specification
     * @return the @Mapper annotation spec
     * @since 2.0.0
     */
    private static AnnotationSpec buildMapperAnnotation(MapperSpec spec) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(ClassName.get("org.mapstruct", "Mapper"))
                .addMember("componentModel", "$S", "spring")
                .addMember("unmappedTargetPolicy", "$T.IGNORE", ClassName.get("org.mapstruct", "ReportingPolicy"));

        // BUG-009 fix: Add uses clause if there are entity relationship mappers
        if (spec.usedMappers() != null && !spec.usedMappers().isEmpty()) {
            // Build the uses array: {CourseMapper.class, TagMapper.class}
            StringBuilder usesValue = new StringBuilder("{");
            for (int i = 0; i < spec.usedMappers().size(); i++) {
                if (i > 0) {
                    usesValue.append(", ");
                }
                usesValue.append("$T.class");
            }
            usesValue.append("}");

            builder.addMember("uses", usesValue.toString(), spec.usedMappers().toArray());
        }

        return builder.build();
    }

    /**
     * Builds a {@code @Mapping} annotation from a MappingSpec.
     *
     * <p>Handles three types of mappings:
     * <ul>
     *   <li><b>Direct mapping:</b> {@code @Mapping(target = "id", source = "orderId")}</li>
     *   <li><b>Ignore mapping:</b> {@code @Mapping(target = "createdAt", ignore = true)}</li>
     *   <li><b>Expression mapping:</b> {@code @Mapping(target = "now", expression = "java(Instant.now())")}</li>
     * </ul>
     *
     * <p>The method delegates to {@link JpaAnnotations} utility methods for
     * consistent annotation generation.
     *
     * @param mapping the mapping specification
     * @return the {@code @Mapping} annotation spec
     * @throws IllegalArgumentException if mapping is null or invalid
     */
    private static AnnotationSpec buildMappingAnnotation(MappingSpec mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("MappingSpec cannot be null");
        }

        // Direct mapping: source and target both present, no expression
        if (mapping.isDirect()) {
            return JpaAnnotations.mapping(mapping.target(), mapping.source());
        }

        // Ignore mapping: source is null, expression contains "ignore"
        if (mapping.isIgnore()) {
            // Build ignore annotation manually since JpaAnnotations doesn't have ignore()
            return AnnotationSpec.builder(com.palantir.javapoet.ClassName.get("org.mapstruct", "Mapping"))
                    .addMember("target", "$S", mapping.target())
                    .addMember("ignore", "true")
                    .build();
        }

        // Expression mapping: expression is present
        if (mapping.expression() != null) {
            // Check if expression already contains "java(" wrapper
            String expr = mapping.expression();
            if (expr.contains("ignore")) {
                // This is an ignore mapping, build it directly
                return AnnotationSpec.builder(com.palantir.javapoet.ClassName.get("org.mapstruct", "Mapping"))
                        .addMember("target", "$S", mapping.target())
                        .addMember("ignore", "true")
                        .build();
            } else if (expr.startsWith("java(")) {
                // Expression already wrapped, build annotation manually
                return AnnotationSpec.builder(com.palantir.javapoet.ClassName.get("org.mapstruct", "Mapping"))
                        .addMember("target", "$S", mapping.target())
                        .addMember("expression", "$S", expr)
                        .build();
            } else {
                // Delegate to JpaAnnotations for proper wrapping
                return JpaAnnotations.mappingExpression(mapping.target(), expr);
            }
        }

        // Fallback: invalid mapping
        throw new IllegalArgumentException("Invalid MappingSpec: must have either source or expression");
    }
}
