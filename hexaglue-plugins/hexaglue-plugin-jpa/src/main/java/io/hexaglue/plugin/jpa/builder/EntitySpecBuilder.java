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
import io.hexaglue.plugin.jpa.model.AttributeOverride;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.IdFieldSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.plugin.jpa.util.NamingConventions;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Map<String, String> entityMapping = Map.of();
    private Map<String, String> bidirectionalMappings = Map.of();

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
     * Sets the mapping from domain AGGREGATE_ROOT and ENTITY types to JPA entity types.
     *
     * <p>This mapping is used to replace domain types with generated entity types
     * in entity relationships (ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY, ONE_TO_ONE).
     *
     * <p>BUG-008 fix: Without this mapping, generated entities would reference domain types
     * instead of JPA entity types, causing Hibernate errors like "targets the type 'X' which
     * is not an '@Entity' type".
     *
     * @param entityMapping map from domain FQN to entity FQN
     * @return this builder
     * @since 2.0.0
     */
    public EntitySpecBuilder entityMapping(Map<String, String> entityMapping) {
        this.entityMapping = entityMapping != null ? entityMapping : Map.of();
        return this;
    }

    /**
     * Sets the bidirectional relationship mappings detected by {@link io.hexaglue.plugin.jpa.util.BidirectionalDetector}.
     *
     * <p>This mapping is used to automatically set {@code mappedBy} on the inverse side
     * of bidirectional relationships. Key format: "{typeFqn}#{fieldName}", value: mappedBy field name.
     *
     * @param bidirectionalMappings map from inverse field key to owning field name
     * @return this builder
     * @since 5.0.0
     */
    public EntitySpecBuilder bidirectionalMappings(Map<String, String> bidirectionalMappings) {
        this.bidirectionalMappings = bidirectionalMappings != null ? bidirectionalMappings : Map.of();
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
        String typeFqn = aggregateRoot.id().qualifiedName();
        List<PropertyFieldSpec> properties = buildPropertySpecsV5(structure, identityField.name());
        List<RelationFieldSpec> relations = buildRelationSpecsV5(structure, identityField.name(), typeFqn);

        return EntitySpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .tableName(tableName)
                .domainQualifiedName(typeFqn)
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
        String typeFqn = entity.id().qualifiedName();
        List<PropertyFieldSpec> properties = buildPropertySpecsV5(structure, identityField.name());
        List<RelationFieldSpec> relations = buildRelationSpecsV5(structure, identityField.name(), typeFqn);

        return EntitySpec.builder()
                .packageName(infrastructurePackage)
                .className(className)
                .tableName(tableName)
                .domainQualifiedName(typeFqn)
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
     * <p>This method also handles detection of embedded field conflicts (multiple fields
     * of the same embedded type) and generates appropriate {@code @AttributeOverride}
     * annotations to avoid column name conflicts.
     *
     * @param structure the type structure containing fields
     * @param identityFieldName the name of the identity field to exclude
     * @since 5.0.0
     */
    private List<PropertyFieldSpec> buildPropertySpecsV5(TypeStructure structure, String identityFieldName) {
        List<PropertyFieldSpec> properties = structure.fields().stream()
                .filter(f -> !f.name().equals(identityFieldName))
                .filter(f -> !f.hasRole(FieldRole.IDENTITY))
                .filter(f -> !f.hasRole(FieldRole.COLLECTION))
                .filter(f -> !f.hasRole(FieldRole.AGGREGATE_REFERENCE))
                // Skip relation-type fields (they go to buildRelationSpecsV5)
                .filter(f -> !isRelationField(f))
                .map(f -> PropertyFieldSpec.fromV5(f, architecturalModel, embeddableMapping, infrastructurePackage))
                .collect(Collectors.toList());

        // Post-process to add @AttributeOverrides for embedded fields with conflicts
        return addAttributeOverridesForConflicts(properties);
    }

    /**
     * Detects embedded field conflicts and adds @AttributeOverride annotations.
     *
     * <p>This method adds {@code @AttributeOverride} annotations in two cases:
     * <ol>
     *   <li>When multiple embedded fields of the same type exist (column name conflicts)</li>
     *   <li>When embedded attributes have SQL reserved word names (e.g., "value", "key")</li>
     * </ol>
     *
     * <p>For type conflicts, attribute overrides use field-name-prefixed column names.
     * For SQL reserved words, the {@link NamingConventions#toColumnName} method handles
     * the renaming (e.g., "value" → "value_col").
     *
     * @param properties the list of property field specs
     * @return the updated list with attribute overrides added where needed
     * @since 2.0.0
     */
    private List<PropertyFieldSpec> addAttributeOverridesForConflicts(List<PropertyFieldSpec> properties) {
        // Group embedded properties by type
        Map<String, List<PropertyFieldSpec>> embeddedByType = new HashMap<>();
        for (PropertyFieldSpec prop : properties) {
            if (prop.shouldBeEmbedded() && !prop.hasAttributeOverrides()) {
                embeddedByType.computeIfAbsent(prop.typeQualifiedName(), k -> new ArrayList<>()).add(prop);
            }
        }

        // Build list of types that need overrides:
        // 1. Types with multiple instances (column name conflicts)
        // 2. Types with SQL reserved word attribute names
        Map<String, List<String>> typeToAttributeNames = new HashMap<>();
        for (Map.Entry<String, List<PropertyFieldSpec>> entry : embeddedByType.entrySet()) {
            List<String> attributeNames = findEmbeddableAttributeNames(entry.getKey());
            if (!attributeNames.isEmpty()) {
                boolean hasMultipleInstances = entry.getValue().size() > 1;
                boolean hasReservedWordAttributes = attributeNames.stream()
                        .anyMatch(attr -> !NamingConventions.toColumnName(attr).equals(NamingConventions.toSnakeCase(attr)));

                if (hasMultipleInstances || hasReservedWordAttributes) {
                    typeToAttributeNames.put(entry.getKey(), attributeNames);
                }
            }
        }

        // If no overrides needed, return as-is
        if (typeToAttributeNames.isEmpty()) {
            return properties;
        }

        // Create new list with overrides added
        List<PropertyFieldSpec> result = new ArrayList<>();
        for (PropertyFieldSpec prop : properties) {
            if (prop.shouldBeEmbedded() && typeToAttributeNames.containsKey(prop.typeQualifiedName())) {
                // Create attribute overrides for this field
                List<String> attributeNames = typeToAttributeNames.get(prop.typeQualifiedName());
                boolean hasMultipleInstances = embeddedByType.get(prop.typeQualifiedName()).size() > 1;
                List<AttributeOverride> overrides = new ArrayList<>();

                for (String attrName : attributeNames) {
                    String columnName;
                    if (hasMultipleInstances) {
                        // Multiple instances: prefix with field name
                        columnName = NamingConventions.toSnakeCase(prop.fieldName())
                                + "_" + NamingConventions.toColumnName(attrName);
                    } else {
                        // Single instance with reserved word: just use safe column name
                        columnName = NamingConventions.toColumnName(attrName);
                    }
                    overrides.add(new AttributeOverride(attrName, columnName));
                }

                // Create new PropertyFieldSpec with overrides
                result.add(new PropertyFieldSpec(
                        prop.fieldName(),
                        prop.javaType(),
                        prop.nullability(),
                        prop.columnName(),
                        prop.isEmbedded(),
                        prop.isValueObject(),
                        prop.isEnum(),
                        prop.typeQualifiedName(),
                        prop.isWrappedForeignKey(),
                        prop.unwrappedType(),
                        prop.wrapperAccessorMethod(),
                        overrides));
            } else {
                result.add(prop);
            }
        }

        return result;
    }

    /**
     * Finds the attribute names of an embeddable type.
     *
     * <p>This method looks up the type in the domain index and retrieves its field names.
     * These field names correspond to the attributes that may need to be overridden.
     *
     * @param typeQualifiedName the fully qualified name of the embeddable type
     * @return list of attribute names, or empty list if not found
     * @since 2.0.0
     */
    private List<String> findEmbeddableAttributeNames(String typeQualifiedName) {
        var domainIndexOpt = architecturalModel.domainIndex();
        if (domainIndexOpt.isEmpty()) {
            return List.of();
        }
        var domainIndex = domainIndexOpt.get();

        // Check if it's mapped to an embeddable type
        String lookupType = embeddableMapping.getOrDefault(typeQualifiedName, typeQualifiedName);

        // If it's mapped to an infrastructure embeddable, find the source VALUE_OBJECT
        String resolvedDomainType = typeQualifiedName;
        for (Map.Entry<String, String> entry : embeddableMapping.entrySet()) {
            if (entry.getValue().equals(lookupType)) {
                resolvedDomainType = entry.getKey();
                break;
            }
        }

        // Make effectively final for lambda
        final String domainType = resolvedDomainType;

        // Look up the VALUE_OBJECT in domain index
        var voOpt = domainIndex.valueObjects()
                .filter(vo -> vo.id().qualifiedName().equals(domainType))
                .findFirst();

        if (voOpt.isPresent()) {
            return voOpt.get().structure().fields().stream()
                    .map(Field::name)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    /**
     * Builds relationship field specifications using v5 model.
     *
     * <p>The identity field is excluded from relations as it's handled by IdFieldSpec.</p>
     *
     * <p>This method also handles detection of embedded field conflicts (multiple EMBEDDED
     * relations of the same type) and generates appropriate {@code @AttributeOverride}
     * annotations to avoid column name conflicts.
     *
     * <p>Also applies bidirectional mappings to set {@code mappedBy} on inverse sides
     * of bidirectional relationships (BUG-003 fix).
     *
     * @param structure the type structure containing fields
     * @param identityFieldName the name of the identity field to exclude
     * @param typeFqn the fully qualified name of the current type (for bidirectional lookup)
     * @since 5.0.0
     */
    private List<RelationFieldSpec> buildRelationSpecsV5(
            TypeStructure structure, String identityFieldName, String typeFqn) {
        List<RelationFieldSpec> relations = structure.fields().stream()
                .filter(f -> !f.name().equals(identityFieldName))
                .filter(f -> !f.hasRole(FieldRole.IDENTITY))
                .filter(f -> isRelationField(f))
                .map(f -> {
                    // BUG-008 fix: Pass entityMapping to replace domain types with entity types
                    RelationFieldSpec spec = RelationFieldSpec.fromV5(f, architecturalModel, embeddableMapping, entityMapping);
                    // BUG-003 fix: Apply bidirectional mappings if this is an inverse side
                    String key = typeFqn + "#" + f.name();
                    String mappedByValue = bidirectionalMappings.get(key);
                    if (mappedByValue != null && spec.mappedBy() == null) {
                        // Create new spec with mappedBy set
                        return spec.withMappedBy(mappedByValue);
                    }
                    return spec;
                })
                .collect(Collectors.toList());

        // Post-process to add @AttributeOverrides for embedded relations with conflicts
        return addAttributeOverridesForRelationConflicts(relations);
    }

    /**
     * Detects embedded relation conflicts and adds @AttributeOverride annotations.
     *
     * <p>This method adds {@code @AttributeOverride} annotations in two cases:
     * <ol>
     *   <li>When multiple EMBEDDED relations of the same type exist (column name conflicts)</li>
     *   <li>When embedded attributes have SQL reserved word names (e.g., "value", "key")</li>
     * </ol>
     *
     * @param relations the list of relation field specs
     * @return the updated list with attribute overrides added where needed
     * @since 2.0.0
     */
    private List<RelationFieldSpec> addAttributeOverridesForRelationConflicts(List<RelationFieldSpec> relations) {
        // Group EMBEDDED relations by target type
        Map<String, List<RelationFieldSpec>> embeddedByType = new HashMap<>();
        for (RelationFieldSpec rel : relations) {
            if (rel.kind() == io.hexaglue.spi.ir.RelationKind.EMBEDDED && !rel.hasAttributeOverrides()) {
                // Extract simple type name from targetType (e.g., "AddressEmbeddable")
                String targetTypeName = rel.targetType().toString();
                embeddedByType.computeIfAbsent(targetTypeName, k -> new ArrayList<>()).add(rel);
            }
        }

        // Build list of types that need overrides:
        // 1. Types with multiple instances (column name conflicts)
        // 2. Types with SQL reserved word attribute names
        Map<String, List<String>> typeToAttributeNames = new HashMap<>();
        for (Map.Entry<String, List<RelationFieldSpec>> entry : embeddedByType.entrySet()) {
            // Find the domain type from embeddable mapping
            String embeddableFqn = entry.getKey();
            String domainType = findDomainTypeFromEmbeddable(embeddableFqn);
            List<String> attributeNames = findEmbeddableAttributeNames(domainType);

            if (!attributeNames.isEmpty()) {
                boolean hasMultipleInstances = entry.getValue().size() > 1;
                boolean hasReservedWordAttributes = attributeNames.stream()
                        .anyMatch(attr -> !NamingConventions.toColumnName(attr).equals(NamingConventions.toSnakeCase(attr)));

                if (hasMultipleInstances || hasReservedWordAttributes) {
                    typeToAttributeNames.put(embeddableFqn, attributeNames);
                }
            }
        }

        // If no overrides needed, return as-is
        if (typeToAttributeNames.isEmpty()) {
            return relations;
        }

        // Create new list with overrides added
        List<RelationFieldSpec> result = new ArrayList<>();
        for (RelationFieldSpec rel : relations) {
            String targetTypeName = rel.targetType().toString();
            if (rel.kind() == io.hexaglue.spi.ir.RelationKind.EMBEDDED
                    && typeToAttributeNames.containsKey(targetTypeName)) {
                // Create attribute overrides for this relation
                List<String> attributeNames = typeToAttributeNames.get(targetTypeName);
                boolean hasMultipleInstances = embeddedByType.get(targetTypeName).size() > 1;
                List<AttributeOverride> overrides = new ArrayList<>();

                for (String attrName : attributeNames) {
                    String columnName;
                    if (hasMultipleInstances) {
                        // Multiple instances: prefix with field name
                        columnName = NamingConventions.toSnakeCase(rel.fieldName())
                                + "_" + NamingConventions.toColumnName(attrName);
                    } else {
                        // Single instance with reserved word: just use safe column name
                        columnName = NamingConventions.toColumnName(attrName);
                    }
                    overrides.add(new AttributeOverride(attrName, columnName));
                }

                // Create new RelationFieldSpec with overrides
                result.add(new RelationFieldSpec(
                        rel.fieldName(),
                        rel.targetType(),
                        rel.kind(),
                        rel.targetKind(),
                        rel.mappedBy(),
                        rel.cascade(),
                        rel.fetch(),
                        rel.orphanRemoval(),
                        overrides));
            } else {
                result.add(rel);
            }
        }

        return result;
    }

    /**
     * Finds the domain type FQN from an embeddable type FQN.
     *
     * @param embeddableFqn the embeddable type fully qualified name
     * @return the domain type FQN, or the embeddable FQN if not found
     * @since 2.0.0
     */
    private String findDomainTypeFromEmbeddable(String embeddableFqn) {
        for (Map.Entry<String, String> entry : embeddableMapping.entrySet()) {
            if (entry.getValue().equals(embeddableFqn)) {
                return entry.getKey();
            }
        }
        return embeddableFqn;
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

        // Enums should NOT be treated as relations - they use @Enumerated(EnumType.STRING)
        if (isEnumType(field)) {
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
     * Checks if a field's type is an enum in the domain model.
     *
     * <p>Enum types classified as VALUE_OBJECT should be persisted with
     * {@code @Enumerated(EnumType.STRING)}, not as embedded relations.
     *
     * @param field the field to check
     * @return true if the field type is a VALUE_OBJECT with TypeNature.ENUM
     * @since 2.0.0
     */
    private boolean isEnumType(Field field) {
        var domainIndexOpt = architecturalModel.domainIndex();
        if (domainIndexOpt.isEmpty()) {
            return false;
        }
        var domainIndex = domainIndexOpt.get();
        String typeFqn = field.type().qualifiedName();
        return domainIndex.valueObjects()
                .filter(vo -> vo.id().qualifiedName().equals(typeFqn))
                .anyMatch(vo -> vo.structure() != null
                        && vo.structure().nature() == io.hexaglue.arch.model.TypeNature.ENUM);
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
