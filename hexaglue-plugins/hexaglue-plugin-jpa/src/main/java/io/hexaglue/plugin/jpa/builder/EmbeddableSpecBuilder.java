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

import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.EmbeddableSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.spi.ir.DomainType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for transforming SPI DomainType (VALUE_OBJECT) to EmbeddableSpec model.
 *
 * <p>This builder applies the transformation logic to convert domain value objects
 * into JPA embeddable specifications ready for JavaPoet code generation.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EmbeddableSpec spec = EmbeddableSpecBuilder.builder()
 *     .domainType(lineItemType)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .allTypes(allDomainTypes)
 *     .build();
 * }</pre>
 *
 * @since 3.0.0
 */
public final class EmbeddableSpecBuilder {

    private DomainType domainType;
    private JpaConfig config;
    private String infrastructurePackage;
    private List<DomainType> allTypes;
    private java.util.Map<String, String> embeddableMapping = java.util.Map.of();

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
     * Sets the domain type to transform.
     *
     * @param domainType the domain value object
     * @return this builder
     */
    public EmbeddableSpecBuilder domainType(DomainType domainType) {
        this.domainType = domainType;
        return this;
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
     * Sets the list of all domain types in the application.
     *
     * @param allTypes all domain types from the IR snapshot
     * @return this builder
     */
    public EmbeddableSpecBuilder allTypes(List<DomainType> allTypes) {
        this.allTypes = allTypes;
        return this;
    }

    /**
     * Sets the mapping from domain VALUE_OBJECT types to JPA embeddable types.
     *
     * <p>This mapping is used to substitute complex VALUE_OBJECTs
     * with their generated embeddable types in nested embeddables.
     *
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @return this builder
     */
    public EmbeddableSpecBuilder embeddableMapping(java.util.Map<String, String> embeddableMapping) {
        this.embeddableMapping = embeddableMapping != null ? embeddableMapping : java.util.Map.of();
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

        String className = domainType.simpleName() + config.embeddableSuffix();

        List<PropertyFieldSpec> properties = buildPropertySpecs();

        return EmbeddableSpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .domainQualifiedName(domainType.qualifiedName())
                .addProperties(properties)
                .build();
    }

    /**
     * Builds property field specifications from domain properties.
     *
     * <p>All properties of a value object are converted to PropertyFieldSpec.
     * Value objects do not have identity fields.
     *
     * <p>For complex VALUE_OBJECTs that have corresponding embeddables (in embeddableMapping),
     * the type is substituted with the embeddable type.
     *
     * @return list of property field specifications
     */
    private List<PropertyFieldSpec> buildPropertySpecs() {
        return domainType.properties().stream()
                .filter(prop -> !prop.hasRelation()) // Simple properties only
                .map(prop -> PropertyFieldSpec.from(prop, allTypes, embeddableMapping, infrastructurePackage))
                .collect(Collectors.toList());
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
