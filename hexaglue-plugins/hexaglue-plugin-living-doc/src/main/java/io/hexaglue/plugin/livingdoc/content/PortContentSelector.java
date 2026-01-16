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
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.ports.PortClassification;
import io.hexaglue.arch.ports.PortOperation;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects port content from ArchitecturalModel and converts it to documentation models.
 *
 * @since 4.0.0
 */
public final class PortContentSelector {

    private final ArchitecturalModel model;

    /**
     * Creates a selector using v4 ArchitecturalModel.
     *
     * @param model the architectural model
     * @since 4.0.0
     */
    public PortContentSelector(ArchitecturalModel model) {
        this.model = model;
    }

    public List<PortDoc> selectDrivingPorts() {
        return model.drivingPorts().map(this::toDoc).toList();
    }

    public List<PortDoc> selectDrivenPorts() {
        return model.drivenPorts().map(this::toDoc).toList();
    }

    private PortDoc toDoc(DrivingPort port) {
        String packageName = extractPackageName(port.id().qualifiedName());
        String simpleName = port.id().simpleName();

        return new PortDoc(
                simpleName,
                packageName,
                toPortKind(port.classification()),
                PortDirection.DRIVING,
                toConfidenceLevel(port.classificationTrace()),
                List.of(), // managedTypes - driving ports don't manage types
                port.operations().stream().map(this::toMethodDoc).collect(Collectors.toList()),
                toDebugInfo(port.id().qualifiedName(), port.syntax()));
    }

    private PortDoc toDoc(DrivenPort port) {
        String packageName = extractPackageName(port.id().qualifiedName());
        String simpleName = port.id().simpleName();

        // Extract managed types
        List<String> managedTypes = port.primaryManagedType()
                .map(ref -> List.of(ref.id().qualifiedName()))
                .orElse(List.of());

        return new PortDoc(
                simpleName,
                packageName,
                toPortKind(port.classification()),
                PortDirection.DRIVEN,
                toConfidenceLevel(port.classificationTrace()),
                managedTypes,
                port.operations().stream().map(this::toMethodDoc).collect(Collectors.toList()),
                toDebugInfo(port.id().qualifiedName(), port.syntax()));
    }

    private MethodDoc toMethodDoc(PortOperation operation) {
        String returnType =
                operation.returnType() != null ? operation.returnType().qualifiedName() : "void";
        List<String> parameters = operation.parameterTypes().stream()
                .map(io.hexaglue.syntax.TypeRef::qualifiedName)
                .collect(Collectors.toList());
        return new MethodDoc(operation.name(), returnType, parameters);
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

    private PortKind toPortKind(PortClassification classification) {
        return switch (classification) {
            case REPOSITORY -> PortKind.REPOSITORY;
            case GATEWAY -> PortKind.GATEWAY;
            case USE_CASE -> PortKind.USE_CASE;
            case COMMAND_HANDLER -> PortKind.COMMAND;
            case QUERY_HANDLER -> PortKind.QUERY;
            case EVENT_PUBLISHER -> PortKind.EVENT_PUBLISHER;
            case NOTIFICATION, UNKNOWN -> PortKind.GENERIC;
        };
    }

    private ConfidenceLevel toConfidenceLevel(io.hexaglue.arch.ClassificationTrace trace) {
        if (trace == null) {
            return ConfidenceLevel.LOW;
        }
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
