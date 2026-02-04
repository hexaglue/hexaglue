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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel.DocPort;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel.DocType;
import java.util.List;
import java.util.Objects;

/**
 * Factory for creating {@link DocumentationModel} from an {@link ArchitecturalModel}.
 *
 * @since 4.0.0
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 * @since 5.0.0 - Migrated to v5 ArchType API with DomainIndex and PortIndex
 */
public final class DocumentationModelFactory {

    private DocumentationModelFactory() {
        // Utility class
    }

    /**
     * Creates a DocumentationModel from an ArchitecturalModel (v5 API).
     *
     * @param model the architectural model
     * @return the documentation model
     * @throws NullPointerException if model is null
     * @throws IllegalStateException if DomainIndex or PortIndex are not present
     * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
     * @since 5.0.0 - Migrated to v5 API with DomainIndex and PortIndex
     */
    public static DocumentationModel fromArchModel(ArchitecturalModel model) {
        Objects.requireNonNull(model, "model must not be null");

        DomainIndex domain = model.domainIndex()
                .orElseThrow(() -> new IllegalStateException("DomainIndex required for documentation"));
        PortIndex ports =
                model.portIndex().orElseThrow(() -> new IllegalStateException("PortIndex required for documentation"));

        List<DocType> aggregateRoots = domain.aggregateRoots()
                .map(DocumentationModelFactory::toDocType)
                .toList();

        List<DocType> entities =
                domain.entities().map(DocumentationModelFactory::toDocType).toList();

        List<DocType> valueObjects =
                domain.valueObjects().map(DocumentationModelFactory::toDocType).toList();

        List<DocType> identifiers =
                domain.identifiers().map(DocumentationModelFactory::toDocType).toList();

        List<DocType> applicationServices = model.typeRegistry()
                .map(r -> r.all(ApplicationService.class)
                        .map(DocumentationModelFactory::toDocType)
                        .toList())
                .orElse(List.of());

        List<DocPort> drivingPorts =
                ports.drivingPorts().map(DocumentationModelFactory::toDocPort).toList();

        List<DocPort> drivenPorts =
                ports.drivenPorts().map(DocumentationModelFactory::toDocPort).toList();

        return new DocumentationModel(
                aggregateRoots, entities, valueObjects, identifiers, applicationServices, drivingPorts, drivenPorts);
    }

    // === Converters ===

    private static DocType toDocType(AggregateRoot agg) {
        int fieldCount = agg.structure().fields().size();
        String construct = agg.structure().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                agg.id().simpleName(),
                agg.id().packageName(),
                agg.id().qualifiedName(),
                "Aggregate Root",
                construct,
                fieldCount,
                agg.classification().explain());
    }

    private static DocType toDocType(Entity entity) {
        int fieldCount = entity.structure().fields().size();
        String construct = entity.structure().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                entity.id().simpleName(),
                entity.id().packageName(),
                entity.id().qualifiedName(),
                "Entity",
                construct,
                fieldCount,
                entity.classification().explain());
    }

    private static DocType toDocType(ValueObject vo) {
        int fieldCount = vo.structure().fields().size();
        String construct = vo.structure().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                vo.id().simpleName(),
                vo.id().packageName(),
                vo.id().qualifiedName(),
                "Value Object",
                construct,
                fieldCount,
                vo.classification().explain());
    }

    private static DocType toDocType(Identifier id) {
        int fieldCount = id.structure().fields().size();
        String construct = id.structure().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                id.id().simpleName(),
                id.id().packageName(),
                id.id().qualifiedName(),
                "Identifier",
                construct,
                fieldCount,
                id.classification().explain());
    }

    /**
     * Converts an ApplicationService to a DocType for documentation.
     *
     * @param svc the application service
     * @return the documentation type
     * @since 5.0.0
     */
    private static DocType toDocType(ApplicationService svc) {
        int methodCount = svc.structure().methods().size();
        String construct = svc.structure().isRecord() ? "RECORD" : "CLASS";

        return new DocType(
                svc.id().simpleName(),
                svc.id().packageName(),
                svc.id().qualifiedName(),
                "Application Service",
                construct,
                methodCount,
                svc.classification().explain());
    }

    private static DocPort toDocPort(DrivingPort port) {
        int methodCount = port.structure().methods().size();

        return new DocPort(
                port.id().simpleName(),
                port.id().packageName(),
                port.id().qualifiedName(),
                "USE_CASE", // Default classification for driving ports in v5
                "DRIVING",
                methodCount,
                port.classification().explain());
    }

    private static DocPort toDocPort(DrivenPort port) {
        int methodCount = port.structure().methods().size();

        return new DocPort(
                port.id().simpleName(),
                port.id().packageName(),
                port.id().qualifiedName(),
                port.portType().toString(),
                "DRIVEN",
                methodCount,
                port.classification().explain());
    }
}
