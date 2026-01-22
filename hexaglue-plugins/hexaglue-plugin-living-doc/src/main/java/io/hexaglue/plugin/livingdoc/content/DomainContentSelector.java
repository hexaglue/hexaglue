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

    /**
     * Creates a selector using v5 ArchitecturalModel with DomainIndex.
     *
     * @param model the architectural model
     * @since 4.0.0
     * @since 5.0.0 - Migrated to v5 API
     */
    public DomainContentSelector(ArchitecturalModel model) {
        this.model = model;
    }

    public List<DomainTypeDoc> selectAggregateRoots() {
        DomainIndex domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
        return domainIndex.aggregateRoots().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectEntities() {
        DomainIndex domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
        return domainIndex.entities().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectValueObjects() {
        DomainIndex domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
        return domainIndex.valueObjects().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectIdentifiers() {
        DomainIndex domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
        return domainIndex.identifiers().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectDomainEvents() {
        DomainIndex domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
        return domainIndex.domainEvents().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectDomainServices() {
        DomainIndex domainIndex = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
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
                toDebugInfo(agg.id().qualifiedName(), structure));
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
                toDebugInfo(entity.id().qualifiedName(), structure));
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
                toDebugInfo(vo.id().qualifiedName(), structure));
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
                toDebugInfo(id.id().qualifiedName(), structure));
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
                toDebugInfo(event.id().qualifiedName(), structure));
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
                toDebugInfo(service.id().qualifiedName(), structure));
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
                toDebugInfo(service.id().qualifiedName(), structure));
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
                "SINGLE", // Cardinality detection simplified
                "NULLABLE", // Nullability detection simplified
                false, // isIdentity
                false, // isEmbedded
                true, // isSimple
                !typeArguments.isEmpty(),
                typeArguments,
                null);
    }

    private DebugInfo toDebugInfo(String qualifiedName, TypeStructure structure) {
        // TypeStructure doesn't track source location - provide defaults
        String sourceFile = null;
        int lineStart = 0;
        int lineEnd = 0;

        List<String> annotations =
                structure.annotations().stream().map(a -> "@" + a.simpleName()).collect(Collectors.toList());

        return new DebugInfo(qualifiedName, annotations, sourceFile, lineStart, lineEnd);
    }

    private io.hexaglue.spi.ir.ConfidenceLevel toSpiConfidenceLevel(ClassificationTrace trace) {
        if (trace == null) {
            return io.hexaglue.spi.ir.ConfidenceLevel.LOW;
        }
        ConfidenceLevel level = trace.confidence();
        return switch (level) {
            case HIGH -> io.hexaglue.spi.ir.ConfidenceLevel.HIGH;
            case MEDIUM -> io.hexaglue.spi.ir.ConfidenceLevel.MEDIUM;
            case LOW -> io.hexaglue.spi.ir.ConfidenceLevel.LOW;
        };
    }

    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
