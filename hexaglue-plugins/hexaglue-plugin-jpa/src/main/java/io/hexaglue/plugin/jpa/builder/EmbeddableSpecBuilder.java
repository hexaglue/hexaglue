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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.extraction.JpaAnnotationExtractor;
import io.hexaglue.plugin.jpa.extraction.PropertyInfo;
import io.hexaglue.plugin.jpa.model.EmbeddableSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder for transforming v4 ValueObject to EmbeddableSpec model.
 *
 * <p>This builder applies the transformation logic to convert domain value objects
 * into JPA embeddable specifications ready for JavaPoet code generation.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EmbeddableSpec spec = EmbeddableSpecBuilder.builder()
 *     .valueObject(lineItemVO)
 *     .model(architecturalModel)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 4.0.0
 */
public final class EmbeddableSpecBuilder {

    private ValueObject valueObject;
    private ArchitecturalModel architecturalModel;
    private JpaConfig config;
    private String infrastructurePackage;
    private Map<String, String> embeddableMapping = Map.of();

    private EmbeddableSpecBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new EmbeddableSpecBuilder instance.
     *
     * @return a new builder instance
     */
    public static EmbeddableSpecBuilder builder() {
        return new EmbeddableSpecBuilder();
    }

    /**
     * Sets the JPA plugin configuration.
     *
     * @param config the JPA configuration
     * @return this builder
     */
    public EmbeddableSpecBuilder config(JpaConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Sets the infrastructure package name.
     *
     * @param infrastructurePackage the package for generated JPA classes
     * @return this builder
     */
    public EmbeddableSpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the v4 value object to transform.
     *
     * @param vo the value object from ArchitecturalModel
     * @return this builder
     * @since 4.0.0
     */
    public EmbeddableSpecBuilder valueObject(ValueObject vo) {
        this.valueObject = vo;
        return this;
    }

    /**
     * Sets the v4 architectural model for type resolution.
     *
     * @param model the architectural model
     * @return this builder
     * @since 4.0.0
     */
    public EmbeddableSpecBuilder model(ArchitecturalModel model) {
        this.architecturalModel = model;
        return this;
    }

    /**
     * Sets the mapping from domain VALUE_OBJECT types to JPA embeddable types.
     *
     * <p>This mapping is used to substitute VALUE_OBJECT field types with their
     * corresponding JPA embeddable types when generating code.
     *
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @return this builder
     */
    public EmbeddableSpecBuilder embeddableMapping(Map<String, String> embeddableMapping) {
        this.embeddableMapping = embeddableMapping != null ? embeddableMapping : Map.of();
        return this;
    }

    /**
     * Builds the EmbeddableSpec from the provided configuration.
     *
     * @return an immutable EmbeddableSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     */
    public EmbeddableSpec build() {
        validateRequiredFields();

        TypeSyntax syntax = valueObject.syntax();
        if (syntax == null) {
            throw new IllegalStateException("Value object " + valueObject.id().qualifiedName()
                    + " has no syntax. Cannot generate JPA embeddable.");
        }

        String simpleName = valueObject.id().simpleName();
        String className = simpleName + config.embeddableSuffix();

        List<PropertyFieldSpec> properties = buildPropertySpecs(syntax);

        return EmbeddableSpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .domainQualifiedName(valueObject.id().qualifiedName())
                .addProperties(properties)
                .build();
    }

    /**
     * Builds property field specifications using v4 model.
     */
    private List<PropertyFieldSpec> buildPropertySpecs(TypeSyntax syntax) {
        List<PropertyInfo> properties = JpaAnnotationExtractor.extractProperties(syntax);
        return properties.stream()
                .map(prop -> PropertyFieldSpec.from(prop, architecturalModel))
                .collect(Collectors.toList());
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        if (valueObject == null) {
            throw new IllegalStateException("valueObject is required");
        }
        if (architecturalModel == null) {
            throw new IllegalStateException("model (ArchitecturalModel) is required");
        }
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }
    }
}
