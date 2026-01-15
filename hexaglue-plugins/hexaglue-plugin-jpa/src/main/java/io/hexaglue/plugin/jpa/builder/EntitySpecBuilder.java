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
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builder for transforming SPI DomainType to EntitySpec model.
 *
 * <p>This builder applies the transformation logic to convert domain model types
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
 * the transformation logic with knowledge of both SPI types and JPA requirements.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EntitySpec spec = EntitySpecBuilder.builder()
 *     .domainType(orderType)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .allTypes(allDomainTypes)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0
 */
public final class EntitySpecBuilder {

    // Legacy SPI fields
    private DomainType domainType;
    private List<DomainType> allTypes;

    // v4 model fields
    private DomainEntity domainEntity;
    private ArchitecturalModel architecturalModel;

    // Common fields
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
     * Sets the domain type to transform.
     *
     * @param domainType the domain aggregate root or entity
     * @return this builder
     * @deprecated Use {@link #domainEntity(DomainEntity)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public EntitySpecBuilder domainType(DomainType domainType) {
        this.domainType = domainType;
        return this;
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
     * Sets the list of all domain types in the application.
     *
     * <p>This is needed to resolve relationships and identify embedded value objects.
     *
     * @param allTypes all domain types from the IR snapshot
     * @return this builder
     * @deprecated Use {@link #model(ArchitecturalModel)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public EntitySpecBuilder allTypes(List<DomainType> allTypes) {
        this.allTypes = allTypes;
        return this;
    }

    /**
     * Sets the v4 domain entity to transform.
     *
     * <p>This is the preferred method for v4 model support. When set,
     * the builder will use the v4 extraction utilities.
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
     * <p>This is the preferred method for v4 model support. When set,
     * the builder will use the v4 extraction utilities.
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
     * <p>This method performs the complete transformation from either the legacy
     * SPI DomainType or v4 DomainEntity to the EntitySpec model. It validates
     * that all required fields are set and applies the transformation logic.
     *
     * <p>If v4 model is available (domainEntity and architecturalModel set),
     * uses the v4 extraction utilities. Otherwise falls back to legacy SPI.
     *
     * @return an immutable EntitySpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if the domain type has no identity
     */
    public EntitySpec build() {
        validateRequiredFields();

        // Use v4 model if available
        if (domainEntity != null && architecturalModel != null) {
            return buildFromV4Model();
        }

        // Fall back to legacy SPI
        return buildFromLegacyModel();
    }

    /**
     * Builds EntitySpec using v4 ArchitecturalModel.
     *
     * @return the built EntitySpec
     * @since 4.0.0
     */
    private EntitySpec buildFromV4Model() {
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
        List<PropertyFieldSpec> properties = buildPropertySpecsV4(syntax);
        List<RelationFieldSpec> relations = buildRelationSpecsV4(syntax);

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
     * Builds EntitySpec using legacy SPI model.
     *
     * @return the built EntitySpec
     */
    private EntitySpec buildFromLegacyModel() {
        if (!domainType.hasIdentity()) {
            throw new IllegalArgumentException(
                    "Domain type " + domainType.qualifiedName() + " has no identity. Cannot generate JPA entity.");
        }

        String className = domainType.simpleName() + config.entitySuffix();
        String tableName = NamingConventions.toTableName(domainType.simpleName(), config.tablePrefix());

        Identity identity = domainType.identity().orElseThrow();
        IdFieldSpec idField = IdFieldSpec.from(identity);

        List<PropertyFieldSpec> properties = buildPropertySpecs();
        List<RelationFieldSpec> relations = buildRelationSpecs();

        return EntitySpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .tableName(tableName)
                .domainQualifiedName(domainType.qualifiedName())
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
    private List<PropertyFieldSpec> buildPropertySpecsV4(TypeSyntax syntax) {
        List<PropertyInfo> properties = JpaAnnotationExtractor.extractProperties(syntax);
        return properties.stream()
                .map(prop -> PropertyFieldSpec.from(prop, architecturalModel))
                .collect(Collectors.toList());
    }

    /**
     * Builds relationship field specifications using v4 model.
     */
    private List<RelationFieldSpec> buildRelationSpecsV4(TypeSyntax syntax) {
        List<RelationInfo> relations = JpaAnnotationExtractor.extractRelations(syntax);
        return relations.stream()
                .map(rel -> RelationFieldSpec.from(rel, architecturalModel, embeddableMapping))
                .collect(Collectors.toList());
    }

    /**
     * Builds property field specifications from domain properties.
     *
     * <p>This method filters out identity fields and relationship fields,
     * converting only simple properties and embedded value objects to
     * PropertyFieldSpec instances.
     *
     * <p>Value Object detection: Each property's type is checked against the
     * list of all domain types. If the type is classified as VALUE_OBJECT,
     * the property will be marked for embedded treatment even if not explicitly
     * marked as embedded.
     *
     * @return list of property field specifications
     */
    private List<PropertyFieldSpec> buildPropertySpecs() {
        return domainType.properties().stream()
                .filter(prop -> !prop.isIdentity())
                .filter(prop -> !prop.hasRelation())
                .map(prop -> PropertyFieldSpec.from(prop, allTypes))
                .collect(Collectors.toList());
    }

    /**
     * Builds relationship field specifications from domain properties.
     *
     * <p>This method processes only properties that have relationships
     * (one-to-many, many-to-one, embedded, etc.) and converts them to
     * RelationFieldSpec instances.
     *
     * <p>Note: The SPI provides relationship information via {@link DomainProperty#relationInfo()}.
     * We need to convert from RelationInfo to the format expected by RelationFieldSpec.
     *
     * @return list of relationship field specifications
     */
    private List<RelationFieldSpec> buildRelationSpecs() {
        return domainType.properties().stream()
                .filter(DomainProperty::hasRelation)
                .map(this::convertToRelationFieldSpec)
                .collect(Collectors.toList());
    }

    /**
     * Converts a DomainProperty with RelationInfo to a RelationFieldSpec.
     *
     * <p>This bridges the gap between the SPI's RelationInfo model and the
     * plugin's RelationFieldSpec model. It creates a DomainRelation for compatibility
     * with the existing RelationFieldSpec.from() method.
     *
     * <p>For EMBEDDED and ELEMENT_COLLECTION relationships, the embeddableMapping is used
     * to replace domain VALUE_OBJECT types with their corresponding JPA embeddable types.
     *
     * @param property the domain property with relation info
     * @return the relation field specification
     */
    private RelationFieldSpec convertToRelationFieldSpec(DomainProperty property) {
        io.hexaglue.spi.ir.RelationInfo relationInfo = property.relationInfo();

        // Infer cascade and fetch strategies based on relation kind
        io.hexaglue.spi.ir.CascadeType cascade = inferCascadeType(relationInfo.kind());
        io.hexaglue.spi.ir.FetchType fetch = inferFetchType(relationInfo.kind());

        // Determine target kind from allTypes lookup
        io.hexaglue.spi.ir.DomainKind targetKind = findDomainKind(relationInfo.targetType());

        // Create a DomainRelation for compatibility with RelationFieldSpec.from()
        io.hexaglue.spi.ir.DomainRelation domainRelation = new io.hexaglue.spi.ir.DomainRelation(
                property.name(),
                relationInfo.kind(),
                relationInfo.targetType(),
                targetKind,
                relationInfo.mappedBy(),
                cascade,
                fetch,
                relationInfo.kind() == io.hexaglue.spi.ir.RelationKind.ONE_TO_MANY);

        // Use embeddableMapping to replace domain types with JPA embeddable types
        return RelationFieldSpec.from(domainRelation, embeddableMapping);
    }

    /**
     * Infers the cascade type based on the relation kind.
     *
     * @param kind the relation kind
     * @return the inferred cascade type
     */
    private io.hexaglue.spi.ir.CascadeType inferCascadeType(io.hexaglue.spi.ir.RelationKind kind) {
        return switch (kind) {
            case ONE_TO_MANY, ELEMENT_COLLECTION -> io.hexaglue.spi.ir.CascadeType.ALL;
            case EMBEDDED -> io.hexaglue.spi.ir.CascadeType.PERSIST;
            default -> io.hexaglue.spi.ir.CascadeType.NONE;
        };
    }

    /**
     * Infers the fetch type based on the relation kind.
     *
     * @param kind the relation kind
     * @return the inferred fetch type
     */
    private io.hexaglue.spi.ir.FetchType inferFetchType(io.hexaglue.spi.ir.RelationKind kind) {
        return switch (kind) {
            case EMBEDDED, ONE_TO_ONE -> io.hexaglue.spi.ir.FetchType.EAGER;
            default -> io.hexaglue.spi.ir.FetchType.LAZY;
        };
    }

    /**
     * Finds the DomainKind for a given type by looking it up in allTypes.
     *
     * @param qualifiedName the fully qualified type name
     * @return the domain kind, or VALUE_OBJECT as fallback
     */
    private io.hexaglue.spi.ir.DomainKind findDomainKind(String qualifiedName) {
        return allTypes.stream()
                .filter(type -> type.qualifiedName().equals(qualifiedName))
                .map(DomainType::kind)
                .findFirst()
                .orElse(io.hexaglue.spi.ir.DomainKind.VALUE_OBJECT);
    }

    /**
     * Validates that all required fields are set.
     *
     * <p>Supports both v4 model (domainEntity + architecturalModel) and
     * legacy model (domainType + allTypes).
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }

        // Check v4 model
        boolean hasV4Model = domainEntity != null && architecturalModel != null;

        // Check legacy model
        boolean hasLegacyModel = domainType != null && allTypes != null;

        if (!hasV4Model && !hasLegacyModel) {
            throw new IllegalStateException(
                    "Either v4 model (domainEntity + model) or legacy model (domainType + allTypes) is required");
        }
    }
}
