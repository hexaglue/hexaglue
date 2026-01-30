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

package io.hexaglue.plugin.livingdoc.content;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.SourceReference;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Selects domain content from ArchitecturalModel and converts it to documentation models.
 *
 * @since 4.0.0
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 * @since 5.0.0 - Migrated to v5 ArchType API with DomainIndex
 */
public final class DomainContentSelector {

    private final ArchitecturalModel model;
    private final DomainIndex domainIndex;

    /**
     * Creates a selector using v5 ArchitecturalModel with DomainIndex.
     *
     * <p>The {@link DomainIndex} is cached at construction time to avoid
     * repeated lookups on every selection method call.</p>
     *
     * @param model the architectural model
     * @throws IllegalStateException if the model does not contain a DomainIndex
     * @since 4.0.0
     * @since 5.0.0 - Migrated to v5 API, cached DomainIndex
     */
    public DomainContentSelector(ArchitecturalModel model) {
        this.model = model;
        this.domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
    }

    public List<DomainTypeDoc> selectAggregateRoots() {
        return domainIndex.aggregateRoots().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectEntities() {
        return domainIndex.entities().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectValueObjects() {
        return domainIndex.valueObjects().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectIdentifiers() {
        return domainIndex.identifiers().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectDomainEvents() {
        return domainIndex.domainEvents().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectDomainServices() {
        return domainIndex.domainServices().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectApplicationServices() {
        return model.typeRegistry()
                .map(registry ->
                        registry.all(ApplicationService.class).map(this::toDoc).toList())
                .orElse(List.of());
    }

    public List<DomainTypeDoc> selectAllTypes() {
        List<DomainTypeDoc> all = new ArrayList<>();
        all.addAll(selectAggregateRoots());
        all.addAll(selectEntities());
        all.addAll(selectValueObjects());
        all.addAll(selectIdentifiers());
        all.addAll(selectDomainEvents());
        all.addAll(selectDomainServices());
        all.addAll(selectApplicationServices());
        return all;
    }

    private DomainTypeDoc toDoc(AggregateRoot agg) {
        TypeStructure structure = agg.structure();
        String packageName = extractPackageName(agg.id().qualifiedName());
        String simpleName = agg.id().simpleName();
        boolean isRecord = structure.isRecord();
        String construct = structure.nature().name().toLowerCase();

        // AggregateRoot always has an identity field (required field, not Optional)
        IdentityDoc identityDoc = toIdentityDoc(agg);

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.AGGREGATE_ROOT,
                toSpiConfidenceLevel(agg.classification()),
                construct,
                isRecord,
                identityDoc,
                extractProperties(structure),
                List.of(), // Relations not tracked in v5 yet
                toDebugInfo(agg.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private DomainTypeDoc toDoc(Entity entity) {
        TypeStructure structure = entity.structure();
        String packageName = extractPackageName(entity.id().qualifiedName());
        String simpleName = entity.id().simpleName();
        boolean isRecord = structure.isRecord();
        String construct = structure.nature().name().toLowerCase();

        IdentityDoc identityDoc = null;
        if (entity.identityField().isPresent()) {
            identityDoc = toIdentityDoc(entity);
        }

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.ENTITY,
                toSpiConfidenceLevel(entity.classification()),
                construct,
                isRecord,
                identityDoc,
                extractProperties(structure),
                List.of(), // Relations not tracked in v5 yet
                toDebugInfo(entity.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private DomainTypeDoc toDoc(ValueObject vo) {
        TypeStructure structure = vo.structure();
        String packageName = extractPackageName(vo.id().qualifiedName());
        String simpleName = vo.id().simpleName();
        boolean isRecord = structure.isRecord();
        String construct = structure.nature().name().toLowerCase();

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.VALUE_OBJECT,
                toSpiConfidenceLevel(vo.classification()),
                construct,
                isRecord,
                null,
                extractProperties(structure),
                List.of(),
                toDebugInfo(vo.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private DomainTypeDoc toDoc(Identifier id) {
        TypeStructure structure = id.structure();
        String packageName = extractPackageName(id.id().qualifiedName());
        String simpleName = id.id().simpleName();
        boolean isRecord = structure.isRecord();
        String construct = structure.nature().name().toLowerCase();

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.IDENTIFIER,
                toSpiConfidenceLevel(id.classification()),
                construct,
                isRecord,
                null,
                extractProperties(structure),
                List.of(),
                toDebugInfo(id.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private DomainTypeDoc toDoc(DomainEvent event) {
        TypeStructure structure = event.structure();
        String packageName = extractPackageName(event.id().qualifiedName());
        String simpleName = event.id().simpleName();
        boolean isRecord = structure.isRecord();
        String construct = structure.nature().name().toLowerCase();

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.DOMAIN_EVENT,
                toSpiConfidenceLevel(event.classification()),
                construct,
                isRecord,
                null,
                extractProperties(structure),
                List.of(),
                toDebugInfo(event.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private DomainTypeDoc toDoc(DomainService service) {
        TypeStructure structure = service.structure();
        String packageName = extractPackageName(service.id().qualifiedName());
        String simpleName = service.id().simpleName();
        String construct = structure.nature().name().toLowerCase();

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.DOMAIN_SERVICE,
                toSpiConfidenceLevel(service.classification()),
                construct,
                false,
                null,
                extractProperties(structure),
                List.of(),
                toDebugInfo(service.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private DomainTypeDoc toDoc(ApplicationService service) {
        TypeStructure structure = service.structure();
        String packageName = extractPackageName(service.id().qualifiedName());
        String simpleName = service.id().simpleName();
        String construct = structure.nature().name().toLowerCase();

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.APPLICATION_SERVICE,
                toSpiConfidenceLevel(service.classification()),
                construct,
                false,
                null,
                extractProperties(structure),
                List.of(),
                toDebugInfo(service.id().qualifiedName(), structure),
                structure.documentation().orElse(null));
    }

    private IdentityDoc toIdentityDoc(AggregateRoot agg) {
        // AggregateRoot.identityField() returns Field directly (required, not Optional)
        Field idField = agg.identityField();
        String fieldName = idField.name();
        TypeRef idType = idField.type();

        String typeName = idType.simpleName();
        String unwrappedTypeName =
                idField.wrappedType().map(TypeRef::simpleName).orElse(typeName);

        return new IdentityDoc(
                fieldName,
                typeName,
                unwrappedTypeName,
                "AUTO", // Strategy not tracked in v5 yet
                "DIRECT",
                false,
                false,
                null);
    }

    private IdentityDoc toIdentityDoc(Entity entity) {
        Field idField =
                entity.identityField().orElseThrow(() -> new IllegalStateException("Entity must have identity field"));
        String fieldName = idField.name();
        TypeRef idType = idField.type();

        String typeName = idType.simpleName();
        String unwrappedTypeName =
                idField.wrappedType().map(TypeRef::simpleName).orElse(typeName);

        return new IdentityDoc(
                fieldName,
                typeName,
                unwrappedTypeName,
                "AUTO", // Strategy not tracked in v5 yet
                "DIRECT",
                false,
                false,
                null);
    }

    private List<PropertyDoc> extractProperties(TypeStructure structure) {
        return structure.fields().stream().map(this::toPropertyDoc).collect(Collectors.toList());
    }

    private PropertyDoc toPropertyDoc(Field field) {
        TypeRef type = field.type();
        String typeName = type.qualifiedName();
        List<String> typeArguments = !type.typeArguments().isEmpty()
                ? type.typeArguments().stream().map(TypeRef::qualifiedName).collect(Collectors.toList())
                : List.of();

        return new PropertyDoc(
                field.name(),
                typeName,
                determineCardinality(type),
                determineNullability(type),
                field.isIdentity(),
                false, // isEmbedded
                true, // isSimple
                !typeArguments.isEmpty(),
                typeArguments,
                null,
                field.documentation().orElse(null));
    }

    /**
     * Determines the nullability of a type.
     *
     * <p>Java primitives cannot be null, so they are always NON_NULL.
     * Reference types are UNKNOWN unless annotated.</p>
     *
     * @param type the type to check
     * @return "NON_NULL" for primitives, "UNKNOWN" for reference types
     */
    private String determineNullability(TypeRef type) {
        // Primitive types can never be null in Java
        if (type.isPrimitive()) {
            return "NON_NULL";
        }
        // Reference types: we cannot determine without annotations
        return "UNKNOWN";
    }

    /**
     * Determines the cardinality of a type.
     *
     * <p>Collection types (List, Set, Collection) have COLLECTION cardinality.
     * All other types have SINGLE cardinality.</p>
     *
     * @param type the type to check
     * @return "COLLECTION" for collection types, "SINGLE" otherwise
     */
    private String determineCardinality(TypeRef type) {
        String qualifiedName = type.qualifiedName();
        // Check for common collection types
        if (qualifiedName.equals("java.util.List")
                || qualifiedName.equals("java.util.Set")
                || qualifiedName.equals("java.util.Collection")
                || qualifiedName.equals("java.util.Iterable")
                || qualifiedName.startsWith("java.util.List")
                || qualifiedName.startsWith("java.util.Set")
                || qualifiedName.startsWith("java.util.Collection")) {
            return "COLLECTION";
        }
        return "SINGLE";
    }

    private DebugInfo toDebugInfo(String qualifiedName, TypeStructure structure) {
        // Use actual source location from TypeStructure when available, fall back to derived path
        Optional<SourceReference> srcLoc = structure.sourceLocation();
        String sourceFile = srcLoc.map(SourceReference::filePath)
                .orElseGet(() -> deriveSourceFilePath(qualifiedName).orElse(null));
        int lineStart = srcLoc.map(SourceReference::lineStart).orElse(0);
        int lineEnd = srcLoc.map(SourceReference::lineEnd).orElse(0);

        // B6: Store annotation qualified names (renderer adds "@" prefix and simplifies)
        List<String> annotations =
                structure.annotations().stream().map(a -> a.qualifiedName()).collect(Collectors.toList());

        return new DebugInfo(qualifiedName, annotations, sourceFile, lineStart, lineEnd);
    }

    /**
     * Derives the expected source file path from a fully qualified class name.
     *
     * <p>For example, "com.example.domain.Order" becomes "com/example/domain/Order.java"
     *
     * @param qualifiedName the fully qualified class name
     * @return the expected source file path, or empty if qualifiedName is null/empty
     * @since 5.0.0
     */
    private Optional<String> deriveSourceFilePath(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return Optional.empty();
        }
        // Replace dots with path separators and add .java extension
        return Optional.of(qualifiedName.replace('.', '/') + ".java");
    }

    private io.hexaglue.arch.model.ir.ConfidenceLevel toSpiConfidenceLevel(ClassificationTrace trace) {
        if (trace == null) {
            return io.hexaglue.arch.model.ir.ConfidenceLevel.LOW;
        }
        ConfidenceLevel level = trace.confidence();
        return switch (level) {
            case HIGH -> io.hexaglue.arch.model.ir.ConfidenceLevel.HIGH;
            case MEDIUM -> io.hexaglue.arch.model.ir.ConfidenceLevel.MEDIUM;
            case LOW -> io.hexaglue.arch.model.ir.ConfidenceLevel.LOW;
        };
    }

    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
