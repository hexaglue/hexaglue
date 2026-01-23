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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.IdFieldSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.plugin.jpa.util.NamingConventions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builder for transforming v5 AggregateRoot or Entity to EntitySpec model.
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
 *     .aggregateRoot(orderAggregate)
 *     .model(architecturalModel)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 4.0.0
 * @since 5.0.0 - Removed v4 DomainEntity support
 */
public final class EntitySpecBuilder {

    private AggregateRoot aggregateRoot;
    private Entity entity;

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
     * Sets the v5 aggregate root to transform.
     *
     * <p>Use this method for aggregate roots. For child entities, use {@link #entity(Entity)}.
     *
     * @param aggregateRoot the aggregate root from the v5 model
     * @return this builder
     * @since 5.0.0
     */
    public EntitySpecBuilder aggregateRoot(AggregateRoot aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        this.entity = null; // Clear entity if set
        return this;
    }

    /**
     * Sets the v5 child entity to transform.
     *
     * <p>Use this method for child entities. For aggregate roots, use {@link #aggregateRoot(AggregateRoot)}.
     *
     * @param entity the entity from the v5 model
     * @return this builder
     * @since 5.0.0
     */
    public EntitySpecBuilder entity(Entity entity) {
        this.entity = entity;
        this.aggregateRoot = null; // Clear aggregateRoot if set
        return this;
    }

    /**
     * Sets the architectural model for type resolution.
     *
     * @param model the architectural model
     * @return this builder
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
     * <p>This method performs the complete transformation to the EntitySpec model.
     *
     * @return an immutable EntitySpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if the entity has no identity
     */
    public EntitySpec build() {
        validateRequiredFields();

        if (aggregateRoot != null) {
            return buildFromAggregateRoot();
        }
        return buildFromEntity();
    }

    /**
     * Builds EntitySpec from v5 AggregateRoot.
     *
     * @since 5.0.0
     */
    private EntitySpec buildFromAggregateRoot() {
        Field identityField = aggregateRoot.identityField();
        TypeStructure structure = aggregateRoot.structure();

        String simpleName = aggregateRoot.id().simpleName();
        String className = simpleName + config.entitySuffix();
        String tableName = NamingConventions.toTableName(simpleName, config.tablePrefix());

        // Build IdFieldSpec from v5 Field
        TypeStructure identityTypeStructure =
                findTypeStructureV5(identityField.type().qualifiedName());
        IdFieldSpec idField = IdFieldSpec.from(identityField, identityTypeStructure);

        // Build properties and relations from v5 structure
        List<PropertyFieldSpec> properties = buildPropertySpecsV5(structure, identityField.name());
        List<RelationFieldSpec> relations = buildRelationSpecsV5(structure, identityField.name());

        return EntitySpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .tableName(tableName)
                .domainQualifiedName(aggregateRoot.id().qualifiedName())
                .idField(idField)
                .addProperties(properties)
                .addRelations(relations)
                .enableAuditing(config.enableAuditing())
                .enableOptimisticLocking(config.enableOptimisticLocking())
                .build();
    }

    /**
     * Builds EntitySpec from v5 Entity.
     *
     * @since 5.0.0
     */
    private EntitySpec buildFromEntity() {
        Optional<Field> identityFieldOpt = entity.identityField();
        if (identityFieldOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Entity " + entity.id().qualifiedName() + " has no identity field. Cannot generate JPA entity.");
        }

        Field identityField = identityFieldOpt.get();
        TypeStructure structure = entity.structure();

        String simpleName = entity.id().simpleName();
        String className = simpleName + config.entitySuffix();
        String tableName = NamingConventions.toTableName(simpleName, config.tablePrefix());

        // Build IdFieldSpec from v5 Field
        TypeStructure identityTypeStructure =
                findTypeStructureV5(identityField.type().qualifiedName());
        IdFieldSpec idField = IdFieldSpec.from(identityField, identityTypeStructure);

        // Build properties and relations from v5 structure
        List<PropertyFieldSpec> properties = buildPropertySpecsV5(structure, identityField.name());
        List<RelationFieldSpec> relations = buildRelationSpecsV5(structure, identityField.name());

        return EntitySpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .tableName(tableName)
                .domainQualifiedName(entity.id().qualifiedName())
                .idField(idField)
                .addProperties(properties)
                .addRelations(relations)
                .enableAuditing(config.enableAuditing())
                .enableOptimisticLocking(config.enableOptimisticLocking())
                .build();
    }

    /**
     * Finds TypeStructure for a qualified name using v5 domainIndex.
     *
     * @since 5.0.0
     */
    private TypeStructure findTypeStructureV5(String qualifiedName) {
        var domainIndexOpt = architecturalModel.domainIndex();
        if (domainIndexOpt.isPresent()) {
            var domainIndex = domainIndexOpt.get();

            // Check value objects (identity types are often value objects)
            var voOpt = domainIndex
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(qualifiedName))
                    .findFirst();
            if (voOpt.isPresent()) {
                return voOpt.get().structure();
            }

            // Check identifiers
            var idOpt = domainIndex
                    .identifiers()
                    .filter(id -> id.id().qualifiedName().equals(qualifiedName))
                    .findFirst();
            if (idOpt.isPresent()) {
                return idOpt.get().structure();
            }
        }
        return null;
    }

    /**
     * Builds property field specifications using v5 model.
     *
     * @param structure the type structure containing fields
     * @param identityFieldName the name of the identity field to exclude
     * @since 5.0.0
     */
    private List<PropertyFieldSpec> buildPropertySpecsV5(TypeStructure structure, String identityFieldName) {
        return structure.fields().stream()
                .filter(f -> !f.name().equals(identityFieldName))
                .filter(f -> !f.hasRole(FieldRole.IDENTITY))
                .filter(f -> !f.hasRole(FieldRole.COLLECTION))
                .filter(f -> !f.hasRole(FieldRole.AGGREGATE_REFERENCE))
                // Skip relation-type fields (they go to buildRelationSpecsV5)
                .filter(f -> !isRelationField(f))
                .map(f -> PropertyFieldSpec.fromV5(f, architecturalModel, embeddableMapping, infrastructurePackage))
                .collect(Collectors.toList());
    }

    /**
     * Builds relationship field specifications using v5 model.
     *
     * <p>The identity field is excluded from relations as it's handled by IdFieldSpec.</p>
     *
     * @param structure the type structure containing fields
     * @param identityFieldName the name of the identity field to exclude
     * @since 5.0.0
     */
    private List<RelationFieldSpec> buildRelationSpecsV5(TypeStructure structure, String identityFieldName) {
        return structure.fields().stream()
                .filter(f -> !f.name().equals(identityFieldName))
                .filter(f -> !f.hasRole(FieldRole.IDENTITY))
                .filter(f -> isRelationField(f))
                .map(f -> RelationFieldSpec.fromV5(f, architecturalModel, embeddableMapping))
                .collect(Collectors.toList());
    }

    /**
     * Determines if a field is a relation field (collection, reference, or embedded).
     *
     * <p>Identifiers (cross-aggregate references like CustomerId) are NOT treated as
     * relations - they should be unwrapped to their primitive type (e.g., UUID) and
     * handled as simple properties by PropertyFieldSpec.
     *
     * @since 5.0.0
     */
    private boolean isRelationField(Field field) {
        // Identifiers should NOT be treated as relations - they need to be unwrapped
        // to their primitive type (e.g., CustomerId → UUID)
        if (isIdentifierType(field)) {
            return false;
        }

        // Check roles
        if (field.hasRole(FieldRole.COLLECTION)
                || field.hasRole(FieldRole.AGGREGATE_REFERENCE)
                || field.hasRole(FieldRole.EMBEDDED)) {
            return true;
        }

        // Check JPA annotations
        return field.hasAnnotation("jakarta.persistence.OneToMany")
                || field.hasAnnotation("javax.persistence.OneToMany")
                || field.hasAnnotation("jakarta.persistence.ManyToOne")
                || field.hasAnnotation("javax.persistence.ManyToOne")
                || field.hasAnnotation("jakarta.persistence.OneToOne")
                || field.hasAnnotation("javax.persistence.OneToOne")
                || field.hasAnnotation("jakarta.persistence.ManyToMany")
                || field.hasAnnotation("javax.persistence.ManyToMany")
                || field.hasAnnotation("jakarta.persistence.ElementCollection")
                || field.hasAnnotation("javax.persistence.ElementCollection")
                || field.hasAnnotation("jakarta.persistence.Embedded")
                || field.hasAnnotation("javax.persistence.Embedded");
    }

    /**
     * Checks if a field's type is an Identifier in the domain model.
     *
     * <p>Identifier types (like CustomerId, OrderId) wrap primitive types (UUID, Long)
     * and should be unwrapped for JPA persistence, not embedded.
     *
     * @param field the field to check
     * @return true if the field type is a registered Identifier
     * @since 5.0.0
     */
    private boolean isIdentifierType(Field field) {
        var domainIndexOpt = architecturalModel.domainIndex();
        if (domainIndexOpt.isEmpty()) {
            return false;
        }
        var domainIndex = domainIndexOpt.get();
        String typeFqn = field.type().qualifiedName();
        return domainIndex.identifiers()
                .anyMatch(id -> id.id().qualifiedName().equals(typeFqn));
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        // Check that at least one entity source is provided
        if (aggregateRoot == null && entity == null) {
            throw new IllegalStateException("Either aggregateRoot or entity is required");
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
