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
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import io.hexaglue.plugin.jpa.util.JpaAnnotations;
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
                .addAnnotation(JpaAnnotations.generated(GENERATOR_NAME))
                .addAnnotation(JpaAnnotations.mapper())
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
                .addJavadoc("and optimal performance without runtime reflection.\n")
                .addJavadoc("\n@since 1.0.0\n");

        // Add toEntity method with custom mappings
        builder.addMethod(createToEntityMethod(spec));

        // Add toDomain method with custom mappings
        builder.addMethod(createToDomainMethod(spec));

        return builder.build();
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

    // =====================================================================
    // Annotation helpers
    // =====================================================================

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
