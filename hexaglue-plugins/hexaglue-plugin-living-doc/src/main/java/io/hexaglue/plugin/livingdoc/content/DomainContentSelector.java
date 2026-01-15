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
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.plugin.livingdoc.model.RelationInfoDoc;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.RelationInfo;
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects domain content from IrSnapshot or ArchitecturalModel and converts it to documentation models.
 *
 * <p>Supports both legacy SPI (IrSnapshot) and v4 model (ArchitecturalModel).
 *
 * @since 3.0.0
 */
public final class DomainContentSelector {

    private final IrSnapshot ir;
    private final ArchitecturalModel model;

    /**
     * Creates a selector using legacy IrSnapshot.
     *
     * @param ir the IR snapshot
     * @deprecated Use {@link #DomainContentSelector(ArchitecturalModel)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public DomainContentSelector(IrSnapshot ir) {
        this.ir = ir;
        this.model = null;
    }

    /**
     * Creates a selector using v4 ArchitecturalModel.
     *
     * @param model the architectural model
     * @since 4.0.0
     */
    public DomainContentSelector(ArchitecturalModel model) {
        this.ir = null;
        this.model = model;
    }

    public List<DomainTypeDoc> selectAggregateRoots() {
        if (model != null) {
            return model.domainEntities()
                    .filter(DomainEntity::isAggregateRoot)
                    .map(this::toDocV4)
                    .toList();
        }
        return ir.domain().aggregateRoots().stream().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectEntities() {
        if (model != null) {
            return model.domainEntities()
                    .filter(e -> !e.isAggregateRoot())
                    .map(this::toDocV4)
                    .toList();
        }
        return ir.domain().typesOfKind(DomainKind.ENTITY).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectValueObjects() {
        if (model != null) {
            return model.valueObjects().map(this::toDocV4).toList();
        }
        return ir.domain().valueObjects().stream().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectIdentifiers() {
        if (model != null) {
            return model.identifiers().map(this::toDocV4).toList();
        }
        return ir.domain().typesOfKind(DomainKind.IDENTIFIER).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectDomainEvents() {
        if (model != null) {
            return model.domainEvents().map(this::toDocV4).toList();
        }
        return ir.domain().typesOfKind(DomainKind.DOMAIN_EVENT).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectDomainServices() {
        if (model != null) {
            return model.domainServices().map(this::toDocV4).toList();
        }
        return ir.domain().typesOfKind(DomainKind.DOMAIN_SERVICE).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectApplicationServices() {
        if (model != null) {
            return model.applicationServices().map(this::toDocV4).toList();
        }
        return ir.domain().typesOfKind(DomainKind.APPLICATION_SERVICE).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectAllTypes() {
        if (model != null) {
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
        return ir.domain().types().stream().map(this::toDoc).toList();
    }

    private DomainTypeDoc toDoc(DomainType type) {
        return new DomainTypeDoc(
                type.simpleName(),
                type.packageName(),
                type.kind(),
                type.confidence(),
                type.construct().toString(),
                type.isRecord(),
                type.hasIdentity() ? toIdentityDoc(type.identity().get()) : null,
                type.properties().stream().map(this::toPropertyDoc).toList(),
                type.relations().stream().map(this::toRelationDoc).toList(),
                toDebugInfo(type));
    }

    private IdentityDoc toIdentityDoc(Identity id) {
        boolean requiresGeneratedValue = id.strategy().requiresGeneratedValue();
        String jpaGenerationType = requiresGeneratedValue ? id.strategy().toJpaGenerationType() : null;

        return new IdentityDoc(
                id.fieldName(),
                id.type().simpleName(),
                id.unwrappedType().simpleName(),
                id.strategy().toString(),
                id.wrapperKind().toString(),
                id.isWrapped(),
                requiresGeneratedValue,
                jpaGenerationType);
    }

    private PropertyDoc toPropertyDoc(DomainProperty prop) {
        List<String> typeArguments = prop.type().isParameterized()
                ? prop.type().typeArguments().stream()
                        .map(t -> t.qualifiedName())
                        .collect(Collectors.toList())
                : List.of();

        RelationInfoDoc relationInfo =
                prop.relationInfoOpt().map(this::toRelationInfoDoc).orElse(null);

        return new PropertyDoc(
                prop.name(),
                prop.type().qualifiedName(),
                prop.cardinality().toString(),
                prop.nullability().toString(),
                prop.isIdentity(),
                prop.isEmbedded(),
                prop.isSimple(),
                prop.type().isParameterized(),
                typeArguments,
                relationInfo);
    }

    private RelationInfoDoc toRelationInfoDoc(RelationInfo info) {
        return new RelationInfoDoc(
                info.kind().toString(),
                info.targetType(),
                info.owning(),
                info.mappedBy(),
                info.isBidirectional(),
                info.isEmbedded());
    }

    private RelationDoc toRelationDoc(DomainRelation rel) {
        return new RelationDoc(
                rel.propertyName(),
                rel.targetSimpleName(),
                rel.targetKind().toString(),
                rel.kind().toString(),
                rel.isOwning(),
                rel.isBidirectional(),
                rel.mappedBy(),
                rel.cascade().toString(),
                rel.fetch().toString(),
                rel.orphanRemoval());
    }

    private DebugInfo toDebugInfo(DomainType type) {
        String sourceFile = null;
        int lineStart = 0;
        int lineEnd = 0;

        if (type.sourceRef() != null && type.sourceRef().isReal()) {
            sourceFile = type.sourceRef().filePath();
            lineStart = type.sourceRef().lineStart();
            lineEnd = type.sourceRef().lineEnd();
        }

        return new DebugInfo(type.qualifiedName(), type.annotations(), sourceFile, lineStart, lineEnd);
    }

    // ===== v4 Model Conversion Methods =====

    private DomainTypeDoc toDocV4(DomainEntity entity) {
        TypeSyntax syntax = entity.syntax();
        String packageName = extractPackageName(entity.id().qualifiedName());
        String simpleName = entity.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        IdentityDoc identityDoc = null;
        if (entity.hasIdentity()) {
            identityDoc = toIdentityDocV4(entity);
        }

        return new DomainTypeDoc(
                simpleName,
                packageName,
                entity.isAggregateRoot() ? DomainKind.AGGREGATE_ROOT : DomainKind.ENTITY,
                toConfidenceLevel(entity.classificationTrace()),
                construct,
                isRecord,
                identityDoc,
                extractPropertiesV4(syntax),
                List.of(), // Relations not tracked in v4 yet
                toDebugInfoV4(entity.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDocV4(ValueObject vo) {
        TypeSyntax syntax = vo.syntax();
        String packageName = extractPackageName(vo.id().qualifiedName());
        String simpleName = vo.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                DomainKind.VALUE_OBJECT,
                toConfidenceLevel(vo.classificationTrace()),
                construct,
                isRecord,
                null,
                extractPropertiesV4(syntax),
                List.of(),
                toDebugInfoV4(vo.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDocV4(Identifier id) {
        TypeSyntax syntax = id.syntax();
        String packageName = extractPackageName(id.id().qualifiedName());
        String simpleName = id.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                DomainKind.IDENTIFIER,
                toConfidenceLevel(id.classificationTrace()),
                construct,
                isRecord,
                null,
                extractPropertiesV4(syntax),
                List.of(),
                toDebugInfoV4(id.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDocV4(DomainEvent event) {
        TypeSyntax syntax = event.syntax();
        String packageName = extractPackageName(event.id().qualifiedName());
        String simpleName = event.id().simpleName();
        boolean isRecord = syntax != null && syntax.form() == TypeForm.RECORD;
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                DomainKind.DOMAIN_EVENT,
                toConfidenceLevel(event.classificationTrace()),
                construct,
                isRecord,
                null,
                extractPropertiesV4(syntax),
                List.of(),
                toDebugInfoV4(event.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDocV4(DomainService service) {
        TypeSyntax syntax = service.syntax();
        String packageName = extractPackageName(service.id().qualifiedName());
        String simpleName = service.id().simpleName();
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                DomainKind.DOMAIN_SERVICE,
                toConfidenceLevel(service.classificationTrace()),
                construct,
                false,
                null,
                extractPropertiesV4(syntax),
                List.of(),
                toDebugInfoV4(service.id().qualifiedName(), syntax));
    }

    private DomainTypeDoc toDocV4(ApplicationService service) {
        TypeSyntax syntax = service.syntax();
        String packageName = extractPackageName(service.id().qualifiedName());
        String simpleName = service.id().simpleName();
        String construct = syntax != null ? syntax.form().name().toLowerCase() : "class";

        return new DomainTypeDoc(
                simpleName,
                packageName,
                DomainKind.APPLICATION_SERVICE,
                toConfidenceLevel(service.classificationTrace()),
                construct,
                false,
                null,
                extractPropertiesV4(syntax),
                List.of(),
                toDebugInfoV4(service.id().qualifiedName(), syntax));
    }

    private IdentityDoc toIdentityDocV4(DomainEntity entity) {
        String fieldName = entity.identityField();
        io.hexaglue.syntax.TypeRef idType = entity.identityType();

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

    private List<PropertyDoc> extractPropertiesV4(TypeSyntax syntax) {
        if (syntax == null || syntax.fields() == null) {
            return List.of();
        }

        return syntax.fields().stream().map(this::toPropertyDocV4).collect(Collectors.toList());
    }

    private PropertyDoc toPropertyDocV4(FieldSyntax field) {
        io.hexaglue.syntax.TypeRef type = field.type();
        String typeName = type != null ? type.qualifiedName() : "Object";
        List<String> typeArguments = type != null && !type.typeArguments().isEmpty()
                ? type.typeArguments().stream()
                        .map(io.hexaglue.syntax.TypeRef::qualifiedName)
                        .collect(Collectors.toList())
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

    private DebugInfo toDebugInfoV4(String qualifiedName, TypeSyntax syntax) {
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

    private ConfidenceLevel toConfidenceLevel(io.hexaglue.arch.ClassificationTrace trace) {
        if (trace == null) {
            return ConfidenceLevel.LOW;
        }
        // Map v4 ConfidenceLevel to legacy SPI ConfidenceLevel
        io.hexaglue.arch.ConfidenceLevel level = trace.confidence();
        return switch (level) {
            case HIGH -> ConfidenceLevel.HIGH;
            case MEDIUM -> ConfidenceLevel.MEDIUM;
            case LOW -> ConfidenceLevel.LOW;
        };
    }

    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
