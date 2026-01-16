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
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.extraction.IdentityInfo;
import io.hexaglue.plugin.jpa.extraction.JpaAnnotationExtractor;
import io.hexaglue.plugin.jpa.extraction.PropertyInfo;
import io.hexaglue.plugin.jpa.extraction.RelationInfo;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.IdFieldSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.plugin.jpa.util.NamingConventions;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builder for transforming v4 DomainEntity to EntitySpec model.
 *
 * <p>This builder applies the transformation logic to convert domain model entities
 * into JPA entity specifications ready for JavaPoet code generation. It handles:
 * <ul>
 *   <li>Identity field transformation (wrapped identifiers, generation strategies)</li>
 *   <li>Property field transformation (simple properties, embedded value objects)</li>
 *   <li>Relationship field transformation (one-to-many, many-to-one, embedded, etc.)</li>
 *   <li>Naming convention application (entity class names, table names, column names)</li>
 *   <li>Configuration-based features (auditing, optimistic locking)</li>
 * </ul>
 *
 * <p>Design decision: The builder separates the concern of model transformation from
 * code generation. EntitySpec is a pure data structure, while this builder contains
 * the transformation logic with knowledge of both ArchitecturalModel types and JPA requirements.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EntitySpec spec = EntitySpecBuilder.builder()
 *     .domainEntity(orderEntity)
 *     .model(architecturalModel)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 4.0.0
 */
public final class EntitySpecBuilder {

    private DomainEntity domainEntity;
    private ArchitecturalModel architecturalModel;
    private JpaConfig config;
    private String infrastructurePackage;
    private Map<String, String> embeddableMapping = Map.of();

    private EntitySpecBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new EntitySpecBuilder instance.
     *
     * @return a new builder instance
     */
    public static EntitySpecBuilder builder() {
        return new EntitySpecBuilder();
    }

    /**
     * Sets the JPA plugin configuration.
     *
     * @param config the JPA configuration
     * @return this builder
     */
    public EntitySpecBuilder config(JpaConfig config) {
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
    public EntitySpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the v4 domain entity to transform.
     *
     * @param entity the domain entity from ArchitecturalModel
     * @return this builder
     * @since 4.0.0
     */
    public EntitySpecBuilder domainEntity(DomainEntity entity) {
        this.domainEntity = entity;
        return this;
    }

    /**
     * Sets the v4 architectural model for type resolution.
     *
     * @param model the architectural model
     * @return this builder
     * @since 4.0.0
     */
    public EntitySpecBuilder model(ArchitecturalModel model) {
        this.architecturalModel = model;
        return this;
    }

    /**
     * Sets the mapping from domain VALUE_OBJECT types to JPA embeddable types.
     *
     * <p>This mapping is used to replace domain types with generated embeddable types
     * in relationships (EMBEDDED and ELEMENT_COLLECTION).
     *
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @return this builder
     */
    public EntitySpecBuilder embeddableMapping(Map<String, String> embeddableMapping) {
        this.embeddableMapping = embeddableMapping != null ? embeddableMapping : Map.of();
        return this;
    }

    /**
     * Builds the EntitySpec from the provided configuration.
     *
     * <p>This method performs the complete transformation from the v4 DomainEntity
     * to the EntitySpec model. It validates that all required fields are set
     * and applies the transformation logic.
     *
     * @return an immutable EntitySpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if the domain entity has no identity
     */
    public EntitySpec build() {
        validateRequiredFields();

        if (!domainEntity.hasIdentity()) {
            throw new IllegalArgumentException("Domain entity "
                    + domainEntity.id().qualifiedName() + " has no identity. Cannot generate JPA entity.");
        }

        TypeSyntax syntax = domainEntity.syntax();
        if (syntax == null) {
            throw new IllegalStateException("Domain entity " + domainEntity.id().qualifiedName()
                    + " has no syntax. Cannot generate JPA entity.");
        }

        String simpleName = domainEntity.id().simpleName();
        String className = simpleName + config.entitySuffix();
        String tableName = NamingConventions.toTableName(simpleName, config.tablePrefix());

        // Extract identity using v4 extractor
        Optional<IdentityInfo> identityInfoOpt = JpaAnnotationExtractor.extractIdentity(syntax);
        if (identityInfoOpt.isEmpty()) {
            // Fallback to model's identity info
            identityInfoOpt =
                    Optional.of(IdentityInfo.simpleId(domainEntity.identityField(), domainEntity.identityType()));
        }

        IdentityInfo identityInfo = identityInfoOpt.get();
        TypeSyntax identityTypeSyntax = findTypeSyntax(identityInfo.idType().qualifiedName());
        IdFieldSpec idField = IdFieldSpec.from(identityInfo, identityTypeSyntax);

        // Extract properties and relations
        List<PropertyFieldSpec> properties = buildPropertySpecs(syntax);
        List<RelationFieldSpec> relations = buildRelationSpecs(syntax);

        return EntitySpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .tableName(tableName)
                .domainQualifiedName(domainEntity.id().qualifiedName())
                .idField(idField)
                .addProperties(properties)
                .addRelations(relations)
                .enableAuditing(config.enableAuditing())
                .enableOptimisticLocking(config.enableOptimisticLocking())
                .build();
    }

    /**
     * Finds TypeSyntax for a qualified name from the v4 model.
     */
    private TypeSyntax findTypeSyntax(String qualifiedName) {
        // Check value objects (identity types are often value objects)
        return architecturalModel
                .valueObjects()
                .filter(vo -> vo.id().qualifiedName().equals(qualifiedName))
                .map(vo -> vo.syntax())
                .findFirst()
                .orElse(null);
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
     * Builds relationship field specifications using v4 model.
     */
    private List<RelationFieldSpec> buildRelationSpecs(TypeSyntax syntax) {
        List<RelationInfo> relations = JpaAnnotationExtractor.extractRelations(syntax);
        return relations.stream()
                .map(rel -> RelationFieldSpec.from(rel, architecturalModel, embeddableMapping))
                .collect(Collectors.toList());
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        if (domainEntity == null) {
            throw new IllegalStateException("domainEntity is required");
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
