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

import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.plugin.livingdoc.model.RelationInfoDoc;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.RelationInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects domain content from IrSnapshot and converts it to documentation models.
 */
public final class DomainContentSelector {

    private final IrSnapshot ir;

    public DomainContentSelector(IrSnapshot ir) {
        this.ir = ir;
    }

    public List<DomainTypeDoc> selectAggregateRoots() {
        return ir.domain().aggregateRoots().stream().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectEntities() {
        return ir.domain().typesOfKind(DomainKind.ENTITY).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectValueObjects() {
        return ir.domain().valueObjects().stream().map(this::toDoc).toList();
    }

    public List<DomainTypeDoc> selectIdentifiers() {
        return ir.domain().typesOfKind(DomainKind.IDENTIFIER).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectDomainEvents() {
        return ir.domain().typesOfKind(DomainKind.DOMAIN_EVENT).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectDomainServices() {
        return ir.domain().typesOfKind(DomainKind.DOMAIN_SERVICE).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectApplicationServices() {
        return ir.domain().typesOfKind(DomainKind.APPLICATION_SERVICE).stream()
                .map(this::toDoc)
                .toList();
    }

    public List<DomainTypeDoc> selectAllTypes() {
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
}
