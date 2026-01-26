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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ValueObjectMappingSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builder for transforming domain entities to MapperSpec model.
 *
 * <p>This builder creates MapStruct mapper interface specifications that convert
 * between domain objects and JPA entities. It analyzes the domain entity structure
 * and generates appropriate mapping annotations to handle:
 * <ul>
 *   <li>Identity field name differences (e.g., orderId → id)</li>
 *   <li>Wrapped identity types (e.g., OrderId → UUID)</li>
 *   <li>Embedded value objects</li>
 *   <li>Relationships with other domain types</li>
 *   <li>Ignored fields (audit fields, version fields)</li>
 * </ul>
 *
 * <p>Design decision: MapStruct provides compile-time type-safe mapping with
 * minimal runtime overhead. The builder generates explicit mapping annotations
 * for non-obvious conversions while relying on MapStruct's convention-based
 * mapping for simple field-to-field mappings.
 *
 * <h3>Generated Mapper Example:</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring", uses = {LineItemMapper.class})
 * public interface OrderMapper {
 *     @Mapping(target = "id", source = "orderId")
 *     @Mapping(target = "version", ignore = true)
 *     OrderEntity toEntity(Order domain);
 *
 *     @Mapping(target = "orderId", source = "id")
 *     Order toDomain(OrderEntity entity);
 * }
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MapperSpec spec = MapperSpecBuilder.builder()
 *     .aggregateRoot(orderAggregate)
 *     .model(architecturalModel)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 4.0.0
 */
public final class MapperSpecBuilder {

    private AggregateRoot aggregateRoot;
    private Entity entity;

    private ArchitecturalModel architecturalModel;
    private JpaConfig config;
    private String infrastructurePackage;
    private Map<String, String> embeddableMapping = Map.of();

    private MapperSpecBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new MapperSpecBuilder instance.
     *
     * @return a new builder instance
     */
    public static MapperSpecBuilder builder() {
        return new MapperSpecBuilder();
    }

    /**
     * Sets the JPA plugin configuration.
     *
     * @param config the JPA configuration
     * @return this builder
     */
    public MapperSpecBuilder config(JpaConfig config) {
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
    public MapperSpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the aggregate root to transform.
     *
     * @param aggregateRoot the aggregate root from the model
     * @return this builder
     * @since 5.0.0
     */
    public MapperSpecBuilder aggregateRoot(AggregateRoot aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        this.entity = null;
        return this;
    }

    /**
     * Sets the entity to transform.
     *
     * @param entity the entity from the model
     * @return this builder
     * @since 5.0.0
     */
    public MapperSpecBuilder entity(Entity entity) {
        this.entity = entity;
        this.aggregateRoot = null;
        return this;
    }

    /**
     * Sets the architectural model for type resolution.
     *
     * @param model the architectural model
     * @return this builder
     * @since 4.0.0
     */
    public MapperSpecBuilder model(ArchitecturalModel model) {
        this.architecturalModel = model;
        return this;
    }

    /**
     * Sets the mapping from domain VALUE_OBJECT types to JPA embeddable types.
     *
     * <p>This mapping is used to generate conversion methods for VALUE_OBJECTs
     * used in the entity (via @Embedded or @ElementCollection).
     *
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @return this builder
     */
    public MapperSpecBuilder embeddableMapping(Map<String, String> embeddableMapping) {
        this.embeddableMapping = embeddableMapping != null ? embeddableMapping : Map.of();
        return this;
    }

    /**
     * Builds the MapperSpec from the provided configuration.
     *
     * @return an immutable MapperSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     */
    public MapperSpec build() {
        validateRequiredFields();

        // Determine source type
        String simpleName;
        String qualifiedName;
        String identityFieldName;

        if (aggregateRoot != null) {
            simpleName = aggregateRoot.id().simpleName();
            qualifiedName = aggregateRoot.id().qualifiedName();
            identityFieldName = aggregateRoot.identityField().name();
        } else {
            simpleName = entity.id().simpleName();
            qualifiedName = entity.id().qualifiedName();
            identityFieldName = entity.identityField().map(Field::name).orElse("id");
        }

        String interfaceName = simpleName + config.mapperSuffix();
        TypeName domainTypeName = ClassName.bestGuess(qualifiedName);
        String entityClassName = simpleName + config.entitySuffix();
        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);

        List<MappingSpec> toEntityMappings = buildToEntityMappings(identityFieldName);
        List<MappingSpec> toDomainMappings = buildToDomainMappings(identityFieldName);

        MapperSpec.WrappedIdentitySpec wrappedIdentity = detectWrappedIdentity();
        List<ValueObjectMappingSpec> valueObjectMappings = detectValueObjectMappings();
        List<MapperSpec.EmbeddableMappingSpec> embeddableMappings = buildEmbeddableMappings();

        // BUG-009 fix: Detect entity relationships and add corresponding mappers
        List<ClassName> usedMappers = detectUsedMappers();

        return new MapperSpec(
                infrastructurePackage,
                interfaceName,
                domainTypeName,
                entityType,
                toEntityMappings,
                toDomainMappings,
                wrappedIdentity,
                valueObjectMappings,
                embeddableMappings,
                usedMappers);
    }

    /**
     * Builds the embeddable mapping specifications from the embeddableMapping.
     *
     * <p>For each VALUE_OBJECT that has a corresponding JPA embeddable, this creates
     * a specification that allows MapStruct to generate conversion methods.
     *
     * @return list of embeddable mapping specifications
     */
    private List<MapperSpec.EmbeddableMappingSpec> buildEmbeddableMappings() {
        List<MapperSpec.EmbeddableMappingSpec> mappings = new ArrayList<>();

        for (Map.Entry<String, String> entry : embeddableMapping.entrySet()) {
            String domainFqn = entry.getKey();
            String embeddableFqn = entry.getValue();

            // Extract simple names
            String domainSimpleName = domainFqn.substring(domainFqn.lastIndexOf('.') + 1);
            String embeddableSimpleName = embeddableFqn.substring(embeddableFqn.lastIndexOf('.') + 1);

            mappings.add(new MapperSpec.EmbeddableMappingSpec(
                    domainFqn, embeddableFqn, domainSimpleName, embeddableSimpleName));
        }

        return mappings;
    }

    /**
     * Builds mapping specifications for domain to entity conversion.
     *
     * @param identityFieldName the name of the identity field in the domain object
     * @return list of mapping specifications for toEntity method
     */
    private List<MappingSpec> buildToEntityMappings(String identityFieldName) {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (identityFieldName != null && !identityFieldName.equals("id")) {
            mappings.add(MappingSpec.direct("id", identityFieldName));
        }

        // Ignore version field if optimistic locking is enabled
        if (config.enableOptimisticLocking()) {
            mappings.add(MappingSpec.ignore("version"));
        }

        // Ignore audit fields if auditing is enabled (must match field names in JpaEntityCodegen)
        if (config.enableAuditing()) {
            mappings.add(MappingSpec.ignore("createdAt"));
            mappings.add(MappingSpec.ignore("updatedAt"));
        }

        return mappings;
    }

    /**
     * Builds mapping specifications for entity to domain conversion.
     *
     * @param identityFieldName the name of the identity field in the domain object
     * @return list of mapping specifications for toDomain method
     */
    private List<MappingSpec> buildToDomainMappings(String identityFieldName) {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (identityFieldName != null && !identityFieldName.equals("id")) {
            mappings.add(MappingSpec.direct(identityFieldName, "id"));
        }

        return mappings;
    }

    /**
     * Detects wrapped identity.
     *
     * @return the wrapped identity spec, or null if identity is not wrapped
     */
    private MapperSpec.WrappedIdentitySpec detectWrappedIdentity() {
        if (aggregateRoot != null) {
            return detectWrappedIdentityFromField(
                    aggregateRoot.identityField().type(),
                    aggregateRoot.identityField().wrappedType());
        }

        if (entity != null) {
            return entity.identityField()
                    .map(idField -> detectWrappedIdentityFromField(idField.type(), idField.wrappedType()))
                    .orElse(null);
        }

        return null;
    }

    /**
     * Detects wrapped identity from a field.
     *
     * @param idType the identity field type
     * @param wrappedType the wrapped type (if present)
     * @return the wrapped identity spec, or null if identity is not wrapped
     * @since 5.0.0
     */
    private MapperSpec.WrappedIdentitySpec detectWrappedIdentityFromField(
            io.hexaglue.syntax.TypeRef idType, Optional<io.hexaglue.syntax.TypeRef> wrappedType) {
        // If wrappedType is present in the Field, use it directly
        if (wrappedType.isPresent()) {
            String wrapperTypeName = idType.qualifiedName();
            String unwrappedTypeName = wrappedType.get().qualifiedName();
            // Infer accessor method from the simple name of the wrapped type
            String accessorMethod = inferAccessorMethod(idType.qualifiedName());
            return new MapperSpec.WrappedIdentitySpec(wrapperTypeName, unwrappedTypeName, accessorMethod);
        }

        // Fallback: look for v5 ValueObject in domainIndex
        if (architecturalModel.domainIndex().isPresent()) {
            var domainIndex = architecturalModel.domainIndex().get();
            var voOpt = domainIndex
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(idType.qualifiedName()))
                    .filter(io.hexaglue.arch.model.ValueObject::isSingleValue)
                    .findFirst();

            if (voOpt.isPresent()) {
                io.hexaglue.arch.model.ValueObject vo = voOpt.get();
                Field wrappedField = vo.wrappedField()
                        .orElseThrow(() -> new IllegalStateException("ValueObject "
                                + vo.id().qualifiedName() + " is marked as single-value but has no wrapped field"));
                String wrapperTypeName = vo.id().qualifiedName();
                String unwrappedTypeName = wrappedField.type().qualifiedName();
                String accessorMethod = wrappedField.name();
                return new MapperSpec.WrappedIdentitySpec(wrapperTypeName, unwrappedTypeName, accessorMethod);
            }
        }

        return null;
    }

    /**
     * Infers the accessor method name for a wrapper type.
     *
     * <p>For records, the accessor is typically "value" or the simple name in lowercase.</p>
     *
     * @param qualifiedName the wrapper type's qualified name
     * @return the likely accessor method name
     */
    private String inferAccessorMethod(String qualifiedName) {
        // Default to "value" which is common for ID wrappers
        return "value";
    }

    /**
     * Detects Value Objects used as properties.
     *
     * @return list of Value Object mapping specifications
     */
    private List<ValueObjectMappingSpec> detectValueObjectMappings() {
        if (aggregateRoot != null) {
            return detectValueObjectMappingsFromFields(
                    aggregateRoot.structure().fields(),
                    aggregateRoot.identityField().type().qualifiedName());
        }

        if (entity != null) {
            String identityTypeName =
                    entity.identityField().map(f -> f.type().qualifiedName()).orElse(null);
            return detectValueObjectMappingsFromFields(entity.structure().fields(), identityTypeName);
        }

        return List.of();
    }

    /**
     * Detects Value Objects used as properties from fields.
     *
     * @param fields the fields to scan
     * @param identityTypeName the identity type name to exclude
     * @return list of Value Object mapping specifications
     * @since 5.0.0
     */
    private List<ValueObjectMappingSpec> detectValueObjectMappingsFromFields(
            List<Field> fields, String identityTypeName) {
        Set<String> processedTypes = new HashSet<>();
        List<ValueObjectMappingSpec> mappings = new ArrayList<>();

        if (architecturalModel.domainIndex().isEmpty()) {
            return mappings;
        }

        var domainIndex = architecturalModel.domainIndex().get();

        for (Field field : fields) {
            String fieldTypeName = field.type().qualifiedName();

            // Skip if already processed
            if (processedTypes.contains(fieldTypeName)) {
                continue;
            }

            // Skip the entity's own identity type
            if (fieldTypeName.equals(identityTypeName)) {
                continue;
            }

            // Check if the field type is a single-value value object
            var voOpt = domainIndex
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(fieldTypeName))
                    .filter(io.hexaglue.arch.model.ValueObject::isSingleValue)
                    .findFirst();

            if (voOpt.isPresent()) {
                io.hexaglue.arch.model.ValueObject vo = voOpt.get();
                Field wrappedField = vo.wrappedField()
                        .orElseThrow(() -> new IllegalStateException("ValueObject "
                                + vo.id().qualifiedName() + " is marked as single-value but has no wrapped field"));
                String wrapperType = vo.id().qualifiedName();
                String simpleName = vo.id().simpleName();
                String unwrappedType = wrappedField.type().qualifiedName();
                String accessorMethod = wrappedField.name();
                // For v5, check if it's a record from structure
                boolean isRecord = vo.structure().isRecord();

                mappings.add(
                        new ValueObjectMappingSpec(wrapperType, simpleName, unwrappedType, accessorMethod, isRecord));
                processedTypes.add(fieldTypeName);
                continue;
            }

            // Check if the field type is a cross-aggregate Identifier (e.g., CustomerId in Order)
            var idOpt = domainIndex
                    .identifiers()
                    .filter(id -> id.id().qualifiedName().equals(fieldTypeName))
                    .findFirst();

            if (idOpt.isPresent()) {
                io.hexaglue.arch.model.Identifier identifier = idOpt.get();
                var wrappedType = identifier.wrappedType();
                if (wrappedType != null) {
                    String wrapperType = identifier.id().qualifiedName();
                    String simpleName = identifier.id().simpleName();
                    String unwrappedType = wrappedType.qualifiedName();
                    // For identifiers (records), use "value" as the accessor
                    String accessorMethod = "value";
                    boolean isRecord = identifier.structure().isRecord();

                    mappings.add(
                            new ValueObjectMappingSpec(wrapperType, simpleName, unwrappedType, accessorMethod, isRecord));
                    processedTypes.add(fieldTypeName);
                }
            }
        }

        // Also scan fields of embedded VALUE_OBJECTs
        for (String embeddableDomainFqn : embeddableMapping.keySet()) {
            domainIndex
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(embeddableDomainFqn))
                    .findFirst()
                    .ifPresent(vo -> {
                        for (Field voField : vo.structure().fields()) {
                            String voFieldTypeName = voField.type().qualifiedName();
                            if (processedTypes.contains(voFieldTypeName) || voFieldTypeName.equals(identityTypeName)) {
                                continue;
                            }

                            // Check if nested field is a single-value VALUE_OBJECT
                            var nestedVoOpt = domainIndex
                                    .valueObjects()
                                    .filter(nestedVo ->
                                            nestedVo.id().qualifiedName().equals(voFieldTypeName))
                                    .filter(io.hexaglue.arch.model.ValueObject::isSingleValue)
                                    .findFirst();

                            if (nestedVoOpt.isPresent()) {
                                io.hexaglue.arch.model.ValueObject nestedVo = nestedVoOpt.get();
                                Field nestedWrappedField = nestedVo.wrappedField()
                                        .orElseThrow(() -> new IllegalStateException(
                                                "ValueObject " + nestedVo.id().qualifiedName()
                                                        + " is marked as single-value but has no wrapped field"));
                                String wrapperType = nestedVo.id().qualifiedName();
                                String simpleName = nestedVo.id().simpleName();
                                String unwrappedType = nestedWrappedField.type().qualifiedName();
                                String accessorMethod = nestedWrappedField.name();
                                boolean isRecord = nestedVo.structure().isRecord();

                                mappings.add(new ValueObjectMappingSpec(
                                        wrapperType, simpleName, unwrappedType, accessorMethod, isRecord));
                                processedTypes.add(voFieldTypeName);
                                continue;
                            }

                            // Also check if nested field is an IDENTIFIER (e.g., ProductId wrapping UUID)
                            var nestedIdOpt = domainIndex
                                    .identifiers()
                                    .filter(id -> id.id().qualifiedName().equals(voFieldTypeName))
                                    .findFirst();

                            if (nestedIdOpt.isPresent()) {
                                io.hexaglue.arch.model.Identifier nestedId = nestedIdOpt.get();
                                var wrappedType = nestedId.wrappedType();
                                if (wrappedType != null) {
                                    String wrapperType = nestedId.id().qualifiedName();
                                    String simpleName = nestedId.id().simpleName();
                                    String unwrappedType = wrappedType.qualifiedName();
                                    // For identifiers, use "value" as the accessor (record convention)
                                    String accessorMethod = "value";
                                    boolean isRecord = nestedId.structure().isRecord();

                                    mappings.add(new ValueObjectMappingSpec(
                                            wrapperType, simpleName, unwrappedType, accessorMethod, isRecord));
                                    processedTypes.add(voFieldTypeName);
                                }
                            }
                        }
                    });
        }

        return mappings;
    }

    /**
     * Detects entity relationships and returns the list of mappers to use.
     *
     * <p>When an entity has a relationship to another entity (AGGREGATE_ROOT or ENTITY),
     * MapStruct needs to use the corresponding mapper to handle the conversion.
     * This method scans the fields and identifies entity relationships that require
     * a mapper dependency.
     *
     * <p>BUG-009 fix: Without this, mappers would fail to compile when the entity
     * has relationships to other entities (e.g., Lesson.course → Course/CourseEntity).
     *
     * @return list of mapper class names to include in the @Mapper(uses = {...}) annotation
     * @since 2.0.0
     */
    private List<ClassName> detectUsedMappers() {
        List<ClassName> usedMappers = new ArrayList<>();
        Set<String> processedTypes = new HashSet<>();

        if (architecturalModel.domainIndex().isEmpty()) {
            return usedMappers;
        }

        var domainIndex = architecturalModel.domainIndex().get();

        // Get the fields to scan
        List<Field> fields;
        String ownTypeFqn;
        if (aggregateRoot != null) {
            fields = aggregateRoot.structure().fields();
            ownTypeFqn = aggregateRoot.id().qualifiedName();
        } else {
            fields = entity.structure().fields();
            ownTypeFqn = entity.id().qualifiedName();
        }

        for (Field field : fields) {
            String fieldTypeFqn = field.elementType()
                    .map(t -> t.qualifiedName())
                    .orElse(field.type().qualifiedName());

            // Skip already processed types
            if (processedTypes.contains(fieldTypeFqn)) {
                continue;
            }

            // Skip self-reference
            if (fieldTypeFqn.equals(ownTypeFqn)) {
                continue;
            }

            // Check if the field type is an aggregate root
            boolean isAggregateRoot = domainIndex.aggregateRoots()
                    .anyMatch(agg -> agg.id().qualifiedName().equals(fieldTypeFqn));

            // Check if the field type is an entity
            boolean isEntity = domainIndex.entities()
                    .anyMatch(e -> e.id().qualifiedName().equals(fieldTypeFqn));

            if (isAggregateRoot || isEntity) {
                // Extract simple name and create mapper class name
                String simpleName = fieldTypeFqn.substring(fieldTypeFqn.lastIndexOf('.') + 1);
                String mapperClassName = simpleName + config.mapperSuffix();
                ClassName mapperClass = ClassName.get(infrastructurePackage, mapperClassName);
                usedMappers.add(mapperClass);
                processedTypes.add(fieldTypeFqn);
            }
        }

        return usedMappers;
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
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
