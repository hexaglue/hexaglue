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
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.domain.Identifier;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeRef;
import io.hexaglue.syntax.TypeSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects domain content from ArchitecturalModel and converts it to documentation models.
 *
 * @since 4.0.0
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 */
public final class DomainContentSelector {

    private final ArchitecturalModel model;

    /**
     * Creates a selector using v4 ArchitecturalModel.
     *
     * @param model the architectural model
     * @since 4.0.0
     */
    public DomainContentSelector(ArchitecturalModel model) {
        this.model = model;
    }

    public List<DomainTypeDoc> selectAggregateRoots() {
        return model.registry()
                .all(DomainEntity.class)
                .filter(DomainEntity::isAggregateRoot)
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectEntities() {
        return model.registry()
                .all(DomainEntity.class)
                .filter(e -> !e.isAggregateRoot())
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectValueObjects() {
        return model.registry().all(ValueObject.class).map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectIdentifiers() {
        return model.registry().all(Identifier.class).map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectDomainEvents() {
        return model.registry().all(DomainEvent.class).map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectDomainServices() {
        return model.registry().all(DomainService.class).map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectApplicationServices() {
        return model.registry().all(ApplicationService.class).map(this::toDoc).toList();
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

    private DomainTypeDoc toDoc(DomainEntity entity) {
        TypeSyntax syntax = entity.syntax();
        String packageName = extractPackageName(entity.id().qualifiedName());
        String simpleName = entity.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        IdentityDoc identityDoc = null;
        if (entity.hasIdentity()) {
            identityDoc = toIdentityDoc(entity);
        }

        return new DomainTypeDoc(
                simpleName,
                packageName,
                entity.isAggregateRoot() ? ElementKind.AGGREGATE_ROOT : ElementKind.ENTITY,
                toSpiConfidenceLevel(entity.classificationTrace()),
                construct,
                isRecord,
                identityDoc,
                extractProperties(syntax),
                List.of(), // Relations not tracked in v4 yet
                toDebugInfo(entity.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDoc(ValueObject vo) {
        TypeSyntax syntax = vo.syntax();
        String packageName = extractPackageName(vo.id().qualifiedName());
        String simpleName = vo.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.VALUE_OBJECT,
                toSpiConfidenceLevel(vo.classificationTrace()),
                construct,
                isRecord,
                null,
                extractProperties(syntax),
                List.of(),
                toDebugInfo(vo.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDoc(Identifier id) {
        TypeSyntax syntax = id.syntax();
        String packageName = extractPackageName(id.id().qualifiedName());
        String simpleName = id.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.IDENTIFIER,
                toSpiConfidenceLevel(id.classificationTrace()),
                construct,
                isRecord,
                null,
                extractProperties(syntax),
                List.of(),
                toDebugInfo(id.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDoc(DomainEvent event) {
        TypeSyntax syntax = event.syntax();
        String packageName = extractPackageName(event.id().qualifiedName());
        String simpleName = event.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.DOMAIN_EVENT,
                toSpiConfidenceLevel(event.classificationTrace()),
                construct,
                isRecord,
                null,
                extractProperties(syntax),
                List.of(),
                toDebugInfo(event.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDoc(DomainService service) {
        TypeSyntax syntax = service.syntax();
        String packageName = extractPackageName(service.id().qualifiedName());
        String simpleName = service.id().simpleName();
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.DOMAIN_SERVICE,
                toSpiConfidenceLevel(service.classificationTrace()),
                construct,
                false,
                null,
                extractProperties(syntax),
                List.of(),
                toDebugInfo(service.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDoc(ApplicationService service) {
        TypeSyntax syntax = service.syntax();
        String packageName = extractPackageName(service.id().qualifiedName());
        String simpleName = service.id().simpleName();
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                ElementKind.APPLICATION_SERVICE,
                toSpiConfidenceLevel(service.classificationTrace()),
                construct,
                false,
                null,
                extractProperties(syntax),
                List.of(),
                toDebugInfo(service.id().qualifiedName(), syntax));
    }

    private IdentityDoc toIdentityDoc(DomainEntity entity) {
        String fieldName = entity.identityField();
        TypeRef idType = entity.identityType();

        String typeName = idType != null ? idType.simpleName() : "Object";
        String unwrappedTypeName = typeName; // Simplified - would need deeper analysis for wrapped IDs

        return new IdentityDoc(
                fieldName,
                typeName,
                unwrappedTypeName,
                "AUTO", // Strategy not tracked in v4 yet
                "DIRECT",
                false,
                false,
                null);
    }

    private List<PropertyDoc> extractProperties(TypeSyntax syntax) {
        if (syntax == null || syntax.fields() == null) {
            return List.of();
        }

        return syntax.fields().stream().map(this::toPropertyDoc).collect(Collectors.toList());
    }

    private PropertyDoc toPropertyDoc(FieldSyntax field) {
        TypeRef type = field.type();
        String typeName = type != null ? type.qualifiedName() : "Object";
        List<String> typeArguments = type != null && !type.typeArguments().isEmpty()
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

    private DebugInfo toDebugInfo(String qualifiedName, TypeSyntax syntax) {
        String sourceFile = null;
        int lineStart = 0;
        int lineEnd = 0;

        if (syntax != null
                && syntax.sourceLocation() != null
                && syntax.sourceLocation().isKnown()) {
            sourceFile = syntax.sourceLocation().filePath().toString();
            lineStart = syntax.sourceLocation().line();
            lineEnd = syntax.sourceLocation().endLine();
        }

        List<String> annotations = syntax != null && syntax.annotations() != null
                ? syntax.annotations().stream().map(a -> "@" + a.simpleName()).collect(Collectors.toList())
                : List.of();

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
