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

package io.hexaglue.plugin.livingdoc.model;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel.DocPort;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel.DocType;
import java.util.List;
import java.util.Objects;

/**
 * Factory for creating {@link DocumentationModel} from an {@link ArchitecturalModel}.
 *
 * @since 4.0.0
 */
public final class DocumentationModelFactory {

    private DocumentationModelFactory() {
        // Utility class
    }

    /**
     * Creates a DocumentationModel from an ArchitecturalModel (v4 API).
     *
     * @param model the architectural model
     * @return the documentation model
     * @throws NullPointerException if model is null
     */
    public static DocumentationModel fromArchModel(ArchitecturalModel model) {
        Objects.requireNonNull(model, "model must not be null");

        List<DocType> aggregateRoots = model.domainEntities()
                .filter(DomainEntity::isAggregateRoot)
                .map(DocumentationModelFactory::toDocType)
                .toList();

        List<DocType> entities = model.domainEntities()
                .filter(e -> !e.isAggregateRoot())
                .map(DocumentationModelFactory::toDocType)
                .toList();

        List<DocType> valueObjects =
                model.valueObjects().map(DocumentationModelFactory::toDocType).toList();

        List<DocPort> drivingPorts =
                model.drivingPorts().map(DocumentationModelFactory::toDocPort).toList();

        List<DocPort> drivenPorts =
                model.drivenPorts().map(DocumentationModelFactory::toDocPort).toList();

        return new DocumentationModel(aggregateRoots, entities, valueObjects, drivingPorts, drivenPorts);
    }

    // === Converters ===

    private static DocType toDocType(DomainEntity entity) {
        String kind = entity.isAggregateRoot() ? "Aggregate Root" : "Entity";
        int fieldCount = entity.syntax() != null ? entity.syntax().fields().size() : 0;
        String construct = entity.syntax() != null && entity.syntax().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                entity.id().simpleName(),
                entity.id().packageName(),
                entity.id().qualifiedName(),
                kind,
                construct,
                fieldCount,
                entity.classificationTrace().explain());
    }

    private static DocType toDocType(ValueObject vo) {
        int fieldCount = vo.syntax() != null ? vo.syntax().fields().size() : 0;
        String construct = vo.syntax() != null && vo.syntax().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                vo.id().simpleName(),
                vo.id().packageName(),
                vo.id().qualifiedName(),
                "Value Object",
                construct,
                fieldCount,
                vo.classificationTrace().explain());
    }

    private static DocPort toDocPort(DrivingPort port) {
        int methodCount = port.syntax() != null
                ? port.syntax().methods().size()
                : port.operations().size();

        return new DocPort(
                port.id().simpleName(),
                port.id().packageName(),
                port.id().qualifiedName(),
                port.classification().toString(),
                "DRIVING",
                methodCount,
                port.classificationTrace().explain());
    }

    private static DocPort toDocPort(DrivenPort port) {
        int methodCount = port.syntax() != null
                ? port.syntax().methods().size()
                : port.operations().size();

        return new DocPort(
                port.id().simpleName(),
                port.id().packageName(),
                port.id().qualifiedName(),
                port.classification().toString(),
                "DRIVEN",
                methodCount,
                port.classificationTrace().explain());
    }
}
