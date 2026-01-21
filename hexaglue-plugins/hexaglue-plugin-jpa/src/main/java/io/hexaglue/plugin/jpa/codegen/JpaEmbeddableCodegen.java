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

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.model.EmbeddableSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.util.JpaAnnotations;
import io.hexaglue.plugin.jpa.util.NamingConventions;
import javax.lang.model.element.Modifier;

/**
 * JavaPoet-based code generator for JPA embeddable classes.
 *
 * <p>This generator produces JPA embeddable classes for domain value objects.
 * Embeddable classes are used for:
 * <ul>
 *   <li>Embedded fields ({@code @Embedded} in entities)</li>
 *   <li>Element collections ({@code @ElementCollection} in entities)</li>
 * </ul>
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * @Generated("io.hexaglue.plugin.jpa")
 * @Embeddable
 * public class LineItemEmbeddable {
 *
 *     @Column(name = "product_name")
 *     private String productName;
 *
 *     @Column(name = "quantity")
 *     private int quantity;
 *
 *     @Column(name = "unit_price")
 *     private BigDecimal unitPrice;
 *
 *     public LineItemEmbeddable() {}
 *
 *     // getters and setters...
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public final class JpaEmbeddableCodegen {

    /**
     * Generator identifier used in {@code @Generated} annotations.
     */
    private static final String GENERATOR_NAME = "io.hexaglue.plugin.jpa";

    private JpaEmbeddableCodegen() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a complete JPA embeddable class from an embeddable specification.
     *
     * @param spec the embeddable specification containing all generation metadata
     * @return a TypeSpec representing the complete embeddable class
     * @throws IllegalArgumentException if spec is null
     */
    public static TypeSpec generate(EmbeddableSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("EmbeddableSpec cannot be null");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JpaAnnotations.generated(GENERATOR_NAME))
                .addAnnotation(JpaAnnotations.embeddable());

        // Add property fields
        for (PropertyFieldSpec property : spec.properties()) {
            addPropertyField(builder, property);
        }

        // Add no-args constructor (required by JPA)
        builder.addMethod(createNoArgsConstructor());

        // Add getters and setters for all fields
        addAccessors(builder, spec);

        return builder.build();
    }

    /**
     * Generates a complete JavaFile for the embeddable.
     *
     * @param spec the embeddable specification
     * @return a JavaFile ready to be written to disk
     * @throws IllegalArgumentException if spec is null
     */
    public static JavaFile generateFile(EmbeddableSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("EmbeddableSpec cannot be null");
        }

        return JavaFile.builder(spec.packageName(), generate(spec))
                .indent("    ") // 4 spaces
                .build();
    }

    /**
     * Adds a property field with appropriate JPA annotation.
     *
     * <p>The annotation depends on the property type:
     * <ul>
     *   <li>Enum types: {@code @Enumerated(EnumType.STRING)} + {@code @Column}</li>
     *   <li>Embedded types (complex VALUE_OBJECTs): {@code @Embedded}</li>
     *   <li>Simple types: {@code @Column}</li>
     * </ul>
     *
     * @param builder the class builder
     * @param property the property field specification
     */
    private static void addPropertyField(TypeSpec.Builder builder, PropertyFieldSpec property) {
        TypeName fieldType = property.effectiveJpaType();
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, property.fieldName(), Modifier.PRIVATE);

        if (property.isEnum()) {
            // Enum types need @Enumerated(EnumType.STRING)
            fieldBuilder.addAnnotation(JpaAnnotations.enumerated());
            fieldBuilder.addAnnotation(JpaAnnotations.column(property.columnName(), property.nullability()));
        } else if (property.shouldBeEmbedded()) {
            // Embedded types (complex VALUE_OBJECTs like Money) need @Embedded
            fieldBuilder.addAnnotation(JpaAnnotations.embedded());
        } else {
            // Simple types (including unwrapped single-value VOs like Quantity -> int)
            fieldBuilder.addAnnotation(JpaAnnotations.column(property.columnName(), property.nullability()));
        }

        builder.addField(fieldBuilder.build());
    }

    /**
     * Creates a public no-args constructor required by JPA.
     *
     * @return a MethodSpec for the no-args constructor
     */
    private static MethodSpec createNoArgsConstructor() {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
    }

    /**
     * Adds getters and setters for all fields.
     *
     * @param builder the class builder
     * @param spec the embeddable specification
     */
    private static void addAccessors(TypeSpec.Builder builder, EmbeddableSpec spec) {
        for (PropertyFieldSpec property : spec.properties()) {
            TypeName propertyType = property.effectiveJpaType();
            builder.addMethod(createGetter(property.fieldName(), propertyType));
            builder.addMethod(createSetter(property.fieldName(), propertyType));
        }
    }

    /**
     * Creates a getter method for a field.
     *
     * @param fieldName the field name
     * @param type the field type
     * @return a MethodSpec for the getter
     */
    private static MethodSpec createGetter(String fieldName, TypeName type) {
        String methodName = "get" + NamingConventions.capitalize(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return $N", fieldName)
                .build();
    }

    /**
     * Creates a setter method for a field.
     *
     * @param fieldName the field name
     * @param type the field type
     * @return a MethodSpec for the setter
     */
    private static MethodSpec createSetter(String fieldName, TypeName type) {
        String methodName = "set" + NamingConventions.capitalize(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(type, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }
}
