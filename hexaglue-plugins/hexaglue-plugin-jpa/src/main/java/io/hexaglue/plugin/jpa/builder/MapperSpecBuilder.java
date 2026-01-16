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
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ValueObjectMappingSpec;
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder for transforming v4 DomainEntity to MapperSpec model.
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
 *     .domainEntity(orderEntity)
 *     .model(architecturalModel)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 4.0.0
 */
public final class MapperSpecBuilder {

    private DomainEntity domainEntity;
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
     * Sets the v4 domain entity to transform.
     *
     * @param entity the domain entity from ArchitecturalModel
     * @return this builder
     * @since 4.0.0
     */
    public MapperSpecBuilder domainEntity(DomainEntity entity) {
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

        String simpleName = domainEntity.id().simpleName();
        String interfaceName = simpleName + config.mapperSuffix();

        TypeName domainTypeName = ClassName.bestGuess(domainEntity.id().qualifiedName());
        String entityClassName = simpleName + config.entitySuffix();
        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);

        List<MappingSpec> toEntityMappings = buildToEntityMappings();
        List<MappingSpec> toDomainMappings = buildToDomainMappings();

        MapperSpec.WrappedIdentitySpec wrappedIdentity = detectWrappedIdentity();
        List<ValueObjectMappingSpec> valueObjectMappings = detectValueObjectMappings();
        List<MapperSpec.EmbeddableMappingSpec> embeddableMappings = buildEmbeddableMappings();

        return new MapperSpec(
                infrastructurePackage,
                interfaceName,
                domainTypeName,
                entityType,
                toEntityMappings,
                toDomainMappings,
                wrappedIdentity,
                valueObjectMappings,
                embeddableMappings);
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
     * @return list of mapping specifications for toEntity method
     */
    private List<MappingSpec> buildToEntityMappings() {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (domainEntity.hasIdentity()) {
            String identityFieldName = domainEntity.identityField();
            if (!identityFieldName.equals("id")) {
                mappings.add(MappingSpec.direct("id", identityFieldName));
            }
        }

        // Ignore version field if optimistic locking is enabled
        if (config.enableOptimisticLocking()) {
            mappings.add(MappingSpec.ignore("version"));
        }

        // Ignore audit fields if auditing is enabled
        if (config.enableAuditing()) {
            mappings.add(MappingSpec.ignore("createdDate"));
            mappings.add(MappingSpec.ignore("lastModifiedDate"));
        }

        return mappings;
    }

    /**
     * Builds mapping specifications for entity to domain conversion.
     *
     * @return list of mapping specifications for toDomain method
     */
    private List<MappingSpec> buildToDomainMappings() {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (domainEntity.hasIdentity()) {
            String identityFieldName = domainEntity.identityField();
            if (!identityFieldName.equals("id")) {
                mappings.add(MappingSpec.direct(identityFieldName, "id"));
            }
        }

        return mappings;
    }

    /**
     * Detects wrapped identity using v4 model.
     *
     * @return the wrapped identity spec, or null if identity is not wrapped
     */
    private MapperSpec.WrappedIdentitySpec detectWrappedIdentity() {
        if (!domainEntity.hasIdentity()) {
            return null;
        }

        TypeRef idType = domainEntity.identityType();

        // Check if identity type is a value object wrapper
        var voOpt = architecturalModel
                .valueObjects()
                .filter(vo -> vo.id().qualifiedName().equals(idType.qualifiedName()))
                .filter(vo -> vo.componentFields().size() == 1)
                .filter(vo -> vo.syntax() != null)
                .findFirst();

        if (voOpt.isEmpty()) {
            return null;
        }

        ValueObject vo = voOpt.get();
        FieldSyntax wrappedField = vo.syntax().fields().get(0);
        String wrapperType = vo.id().qualifiedName();
        String unwrappedType = wrappedField.type().qualifiedName();
        String accessorMethod = wrappedField.name(); // For records, accessor is field name

        return new MapperSpec.WrappedIdentitySpec(wrapperType, unwrappedType, accessorMethod);
    }

    /**
     * Detects Value Objects used as properties using v4 model.
     *
     * @return list of Value Object mapping specifications
     */
    private List<ValueObjectMappingSpec> detectValueObjectMappings() {
        Set<String> processedTypes = new HashSet<>();
        List<ValueObjectMappingSpec> mappings = new ArrayList<>();

        // Exclude the entity's own identity type (handled by detectWrappedIdentity)
        String identityTypeName =
                domainEntity.hasIdentity() ? domainEntity.identityType().qualifiedName() : null;

        // Scan all fields of the domain entity
        if (domainEntity.syntax() != null) {
            scanFieldsForValueObjects(domainEntity.syntax().fields(), identityTypeName, processedTypes, mappings);
        }

        // Also scan fields of embedded VALUE_OBJECTs
        for (String embeddableDomainFqn : embeddableMapping.keySet()) {
            architecturalModel
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(embeddableDomainFqn))
                    .filter(vo -> vo.syntax() != null)
                    .findFirst()
                    .ifPresent(vo -> scanFieldsForValueObjects(
                            vo.syntax().fields(), identityTypeName, processedTypes, mappings));
        }

        return mappings;
    }

    /**
     * Scans a list of fields for Value Object wrapper types using v4 model.
     */
    private void scanFieldsForValueObjects(
            List<FieldSyntax> fields,
            String identityTypeName,
            Set<String> processedTypes,
            List<ValueObjectMappingSpec> mappings) {

        for (FieldSyntax field : fields) {
            // Skip static fields
            if (field.isStatic()) {
                continue;
            }

            String fieldTypeName = field.type().qualifiedName();

            // Skip if already processed (avoid duplicate conversion methods)
            if (processedTypes.contains(fieldTypeName)) {
                continue;
            }

            // Skip the entity's own identity type (handled by detectWrappedIdentity)
            if (fieldTypeName.equals(identityTypeName)) {
                continue;
            }

            // Check if the field type is a simple wrapper value object
            var voOpt = architecturalModel
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(fieldTypeName))
                    .filter(vo -> vo.componentFields().size() == 1)
                    .filter(vo -> vo.syntax() != null)
                    .findFirst();

            if (voOpt.isPresent()) {
                ValueObject vo = voOpt.get();
                FieldSyntax wrappedField = vo.syntax().fields().get(0);
                String wrapperType = vo.id().qualifiedName();
                String simpleName = vo.id().simpleName();
                String unwrappedType = wrappedField.type().qualifiedName();
                String accessorMethod = wrappedField.name();
                boolean isRecord = vo.syntax().isRecord();

                mappings.add(
                        new ValueObjectMappingSpec(wrapperType, simpleName, unwrappedType, accessorMethod, isRecord));
                processedTypes.add(fieldTypeName);
            }
        }
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
