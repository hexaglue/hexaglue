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
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builder for transforming SPI DomainType to MapperSpec model.
 *
 * <p>This builder creates MapStruct mapper interface specifications that convert
 * between domain objects and JPA entities. It analyzes the domain type structure
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
 *     .domainType(orderType)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .allTypes(allDomainTypes)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0
 */
public final class MapperSpecBuilder {

    // Legacy SPI fields
    private DomainType domainType;
    private List<DomainType> allTypes;

    // v4 model fields
    private DomainEntity domainEntity;
    private ArchitecturalModel architecturalModel;

    // Common fields
    private JpaConfig config;
    private String infrastructurePackage;
    private java.util.Map<String, String> embeddableMapping = java.util.Map.of();

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
     * Sets the domain type to transform.
     *
     * @param domainType the domain aggregate root or entity
     * @return this builder
     * @deprecated Use {@link #domainEntity(DomainEntity)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public MapperSpecBuilder domainType(DomainType domainType) {
        this.domainType = domainType;
        return this;
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
     * Sets the list of all domain types in the application.
     *
     * <p>This is needed to identify relationships and determine which other
     * mappers this mapper depends on.
     *
     * @param allTypes all domain types from the IR snapshot
     * @return this builder
     * @deprecated Use {@link #model(ArchitecturalModel)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public MapperSpecBuilder allTypes(List<DomainType> allTypes) {
        this.allTypes = allTypes;
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
    public MapperSpecBuilder embeddableMapping(java.util.Map<String, String> embeddableMapping) {
        this.embeddableMapping = embeddableMapping != null ? embeddableMapping : java.util.Map.of();
        return this;
    }

    /**
     * Builds the MapperSpec from the provided configuration.
     *
     * <p>If v4 model is available (domainEntity and architecturalModel set),
     * uses v4 model. Otherwise falls back to legacy SPI.
     *
     * @return an immutable MapperSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     */
    public MapperSpec build() {
        validateRequiredFields();

        // Use v4 model if available
        if (domainEntity != null && architecturalModel != null) {
            return buildFromV4Model();
        }

        // Fall back to legacy SPI
        return buildFromLegacyModel();
    }

    /**
     * Builds MapperSpec using v4 ArchitecturalModel.
     *
     * @return the built MapperSpec
     * @since 4.0.0
     */
    private MapperSpec buildFromV4Model() {
        String simpleName = domainEntity.id().simpleName();
        String interfaceName = simpleName + config.mapperSuffix();

        TypeName domainTypeName = ClassName.bestGuess(domainEntity.id().qualifiedName());
        String entityClassName = simpleName + config.entitySuffix();
        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);

        List<MappingSpec> toEntityMappings = buildToEntityMappingsV4();
        List<MappingSpec> toDomainMappings = buildToDomainMappingsV4();

        MapperSpec.WrappedIdentitySpec wrappedIdentity = detectWrappedIdentityV4();
        List<ValueObjectMappingSpec> valueObjectMappings = detectValueObjectMappingsV4();
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
     * Builds MapperSpec using legacy SPI model.
     *
     * @return the built MapperSpec
     */
    private MapperSpec buildFromLegacyModel() {
        String interfaceName = domainType.simpleName() + config.mapperSuffix();

        TypeName domainTypeName = ClassName.bestGuess(domainType.qualifiedName());
        String entityClassName = domainType.simpleName() + config.entitySuffix();
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

        for (java.util.Map.Entry<String, String> entry : embeddableMapping.entrySet()) {
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
     * <p>This method analyzes the domain type and generates MapStruct mapping
     * annotations for cases where field names or types differ between domain
     * and entity representations.
     *
     * <p>Mapping rules:
     * <ul>
     *   <li>Identity field: Maps domain ID field name to "id" (e.g., orderId → id)</li>
     *   <li>Wrapped identity: Uses accessor method (e.g., orderId.value() → id)</li>
     *   <li>Version field: Ignored when optimistic locking is enabled</li>
     *   <li>Audit fields: Ignored when auditing is enabled (createdDate, lastModifiedDate)</li>
     *   <li>Relationships: Handled by nested mappers (uses clause)</li>
     * </ul>
     *
     * @return list of mapping specifications for toEntity method
     */
    private List<MappingSpec> buildToEntityMappings() {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id" or if it's wrapped
        if (domainType.hasIdentity()) {
            Identity identity = domainType.identity().orElseThrow();
            String identityFieldName = identity.fieldName();

            // If identity is wrapped, we need to access the underlying value
            // MapStruct will use the map() method we generate
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
     * <p>This method generates the reverse mappings for converting JPA entities
     * back to domain objects. It mirrors the toEntity mappings but in the
     * opposite direction.
     *
     * <p>Mapping rules:
     * <ul>
     *   <li>Identity field: Maps "id" to domain ID field name (e.g., id → orderId)</li>
     *   <li>All entity-specific fields (version, audit) are not mapped to domain</li>
     * </ul>
     *
     * @return list of mapping specifications for toDomain method
     */
    private List<MappingSpec> buildToDomainMappings() {
        List<MappingSpec> mappings = new ArrayList<>();

        // Map identity field if name differs from "id"
        if (domainType.hasIdentity()) {
            Identity identity = domainType.identity().orElseThrow();
            String identityFieldName = identity.fieldName();

            if (!identityFieldName.equals("id")) {
                mappings.add(MappingSpec.direct(identityFieldName, "id"));
            }
        }

        return mappings;
    }

    /**
     * Builds mapping specifications for domain to entity conversion using v4 model.
     *
     * @return list of mapping specifications for toEntity method
     * @since 4.0.0
     */
    private List<MappingSpec> buildToEntityMappingsV4() {
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
     * Builds mapping specifications for entity to domain conversion using v4 model.
     *
     * @return list of mapping specifications for toDomain method
     * @since 4.0.0
     */
    private List<MappingSpec> buildToDomainMappingsV4() {
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
     * @since 4.0.0
     */
    private MapperSpec.WrappedIdentitySpec detectWrappedIdentityV4() {
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
     * @since 4.0.0
     */
    private List<ValueObjectMappingSpec> detectValueObjectMappingsV4() {
        Set<String> processedTypes = new HashSet<>();
        List<ValueObjectMappingSpec> mappings = new ArrayList<>();

        // Exclude the entity's own identity type (handled by detectWrappedIdentityV4)
        String identityTypeName =
                domainEntity.hasIdentity() ? domainEntity.identityType().qualifiedName() : null;

        // Scan all fields of the domain entity
        if (domainEntity.syntax() != null) {
            scanFieldsForValueObjectsV4(domainEntity.syntax().fields(), identityTypeName, processedTypes, mappings);
        }

        // Also scan fields of embedded VALUE_OBJECTs
        for (String embeddableDomainFqn : embeddableMapping.keySet()) {
            architecturalModel
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(embeddableDomainFqn))
                    .filter(vo -> vo.syntax() != null)
                    .findFirst()
                    .ifPresent(vo -> scanFieldsForValueObjectsV4(
                            vo.syntax().fields(), identityTypeName, processedTypes, mappings));
        }

        return mappings;
    }

    /**
     * Scans a list of fields for Value Object wrapper types using v4 model.
     */
    private void scanFieldsForValueObjectsV4(
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

            // Skip the entity's own identity type (handled by detectWrappedIdentityV4)
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
     * Detects if the identity type is wrapped (e.g., TaskId wrapping UUID).
     *
     * <p>This method analyzes the domain type's identity to determine if it uses
     * a custom wrapper type that needs special handling in MapStruct conversions.
     *
     * <p>When the identity is wrapped (e.g., {@code record TaskId(UUID value)}),
     * MapStruct needs explicit conversion methods to map between the wrapper type
     * and the underlying primitive type.
     *
     * @return the wrapped identity spec, or null if identity is not wrapped
     */
    private MapperSpec.WrappedIdentitySpec detectWrappedIdentity() {
        if (!domainType.hasIdentity()) {
            return null;
        }

        Identity identity = domainType.identity().orElseThrow();

        if (!identity.isWrapped()) {
            return null;
        }

        // Extract wrapper and unwrapped types
        String wrapperType = identity.type().qualifiedName();
        String unwrappedType = identity.unwrappedType().qualifiedName();

        // For records, the accessor method is typically "value()"
        // For classes, it might be "getValue()" or "get" + fieldName
        String accessorMethod = determineAccessorMethod(identity);

        return new MapperSpec.WrappedIdentitySpec(wrapperType, unwrappedType, accessorMethod);
    }

    /**
     * Determines the accessor method name for unwrapping the identity value.
     *
     * <p>For records, this is typically the component name (e.g., "value" for {@code record TaskId(UUID value)}).
     * For classes, this follows JavaBeans convention (e.g., "getValue").
     *
     * @param identity the identity to analyze
     * @return the accessor method name (without parentheses)
     */
    private String determineAccessorMethod(Identity identity) {
        // For records, the accessor is the component name (typically "value")
        // This is a conventional pattern used in DDD value objects
        if (identity.wrapperKind() == io.hexaglue.spi.ir.IdentityWrapperKind.RECORD) {
            return "value";
        }

        // For classes, follow JavaBeans convention
        return "getValue";
    }

    /**
     * Detects Value Objects and Identifier types used as properties in the domain type
     * and its embedded VALUE_OBJECTs.
     *
     * <p>This method scans all properties of the domain type and identifies those
     * whose type is classified as VALUE_OBJECT or IDENTIFIER in allTypes. For each simple
     * wrapper type (single property pattern), a ValueObjectMappingSpec is created.
     *
     * <p>IDENTIFIER types are included because they are used for inter-aggregate references
     * (foreign keys) and need the same conversion methods as Value Objects.
     *
     * <p>For embedded VALUE_OBJECTs (those in embeddableMapping), this method also scans
     * their properties to detect wrapper types that need conversion methods.
     *
     * <p>Complex Value Objects (multiple properties) are not supported for automatic
     * mapping generation - they require custom MapStruct mappings.
     *
     * @return list of Value Object mapping specifications
     */
    private List<ValueObjectMappingSpec> detectValueObjectMappings() {
        Set<String> processedTypes = new HashSet<>();
        List<ValueObjectMappingSpec> mappings = new ArrayList<>();

        // Exclude the entity's own identity type (handled by detectWrappedIdentity)
        String identityTypeName =
                domainType.identity().map(id -> id.type().qualifiedName()).orElse(null);

        // Scan all properties of the domain type
        scanPropertiesForValueObjects(domainType.properties(), identityTypeName, processedTypes, mappings);

        // Also scan properties of embedded VALUE_OBJECTs (those in embeddableMapping)
        for (String embeddableDomainFqn : embeddableMapping.keySet()) {
            DomainType embeddableDomainType = allTypes.stream()
                    .filter(type -> type.qualifiedName().equals(embeddableDomainFqn))
                    .findFirst()
                    .orElse(null);

            if (embeddableDomainType != null) {
                scanPropertiesForValueObjects(
                        embeddableDomainType.properties(), identityTypeName, processedTypes, mappings);
            }
        }

        return mappings;
    }

    /**
     * Scans a list of properties for Value Object and Identifier wrapper types.
     *
     * @param properties the properties to scan
     * @param identityTypeName the entity's own identity type (to exclude)
     * @param processedTypes set of already processed type names
     * @param mappings the list to add new mappings to
     */
    private void scanPropertiesForValueObjects(
            List<DomainProperty> properties,
            String identityTypeName,
            Set<String> processedTypes,
            List<ValueObjectMappingSpec> mappings) {

        for (DomainProperty property : properties) {
            // Skip identity properties (handled separately by detectWrappedIdentity)
            if (property.isIdentity()) {
                continue;
            }

            String propertyTypeName = property.type().qualifiedName();

            // Skip if already processed (avoid duplicate conversion methods)
            if (processedTypes.contains(propertyTypeName)) {
                continue;
            }

            // Skip the entity's own identity type (handled by detectWrappedIdentity)
            if (propertyTypeName.equals(identityTypeName)) {
                continue;
            }

            // Find the DomainType for this property's type
            DomainType propertyDomainType = allTypes.stream()
                    .filter(type -> type.qualifiedName().equals(propertyTypeName))
                    .findFirst()
                    .orElse(null);

            // Accept both VALUE_OBJECT and IDENTIFIER types for mapping generation
            // IDENTIFIER types are used for inter-aggregate references (foreign keys)
            if (propertyDomainType != null && isSimpleWrapperType(propertyDomainType)) {
                // Create a ValueObjectMappingSpec for this wrapper type
                ValueObjectMappingSpec spec = ValueObjectMappingSpec.from(propertyDomainType);
                if (spec != null) {
                    mappings.add(spec);
                    processedTypes.add(propertyTypeName);
                }
            }
        }
    }

    /**
     * Determines if a domain type is a simple wrapper type that needs conversion methods.
     *
     * <p>A simple wrapper type has exactly one property and is either:
     * <ul>
     *   <li>A VALUE_OBJECT - immutable wrapper around a primitive</li>
     *   <li>An IDENTIFIER - typed identity wrapper (e.g., CustomerId)</li>
     * </ul>
     *
     * @param type the domain type to check
     * @return true if the type is a simple wrapper needing conversion methods
     */
    private boolean isSimpleWrapperType(DomainType type) {
        if (type == null || type.properties().size() != 1) {
            return false;
        }
        return type.isValueObject() || type.kind() == io.hexaglue.spi.ir.DomainKind.IDENTIFIER;
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
