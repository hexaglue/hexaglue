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
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.ir.PortMethod;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects port content from IrSnapshot or ArchitecturalModel and converts it to documentation models.
 *
 * <p>Supports both legacy SPI (IrSnapshot) and v4 model (ArchitecturalModel).
 *
 * @since 3.0.0
 */
public final class PortContentSelector {

    private final IrSnapshot ir;
    private final ArchitecturalModel model;

    /**
     * Creates a selector using legacy IrSnapshot.
     *
     * @param ir the IR snapshot
     * @deprecated Use {@link #PortContentSelector(ArchitecturalModel)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public PortContentSelector(IrSnapshot ir) {
        this.ir = ir;
        this.model = null;
    }

    /**
     * Creates a selector using v4 ArchitecturalModel.
     *
     * @param model the architectural model
     * @since 4.0.0
     */
    public PortContentSelector(ArchitecturalModel model) {
        this.ir = null;
        this.model = model;
    }

    public List<PortDoc> selectDrivingPorts() {
        if (model != null) {
            return model.drivingPorts().map(this::toDocV4).toList();
        }
        return ir.ports().drivingPorts().stream().map(this::toDoc).toList();
    }

    public List<PortDoc> selectDrivenPorts() {
        if (model != null) {
            return model.drivenPorts().map(this::toDocV4).toList();
        }
        return ir.ports().drivenPorts().stream().map(this::toDoc).toList();
    }

    private PortDoc toDoc(Port port) {
        return new PortDoc(
                port.simpleName(),
                port.packageName(),
                port.kind(),
                port.direction(),
                port.confidence(),
                port.managedTypes(),
                port.methods().stream().map(this::toMethodDoc).toList(),
                toDebugInfo(port));
    }

    private MethodDoc toMethodDoc(PortMethod method) {
        String returnType = method.returnType().qualifiedName();
        List<String> parameters =
                method.parameters().stream().map(p -> p.type().qualifiedName()).toList();
        return new MethodDoc(method.name(), returnType, parameters);
    }

    private DebugInfo toDebugInfo(Port port) {
        String sourceFile = null;
        int lineStart = 0;
        int lineEnd = 0;

        if (port.sourceRef() != null && port.sourceRef().isReal()) {
            sourceFile = port.sourceRef().filePath();
            lineStart = port.sourceRef().lineStart();
            lineEnd = port.sourceRef().lineEnd();
        }

        return new DebugInfo(port.qualifiedName(), port.annotations(), sourceFile, lineStart, lineEnd);
    }

    // ===== v4 Model Conversion Methods =====

    private PortDoc toDocV4(DrivingPort port) {
        String packageName = extractPackageName(port.id().qualifiedName());
        String simpleName = port.id().simpleName();

        return new PortDoc(
                simpleName,
                packageName,
                toPortKind(port.classification()),
                PortDirection.DRIVING,
                toConfidenceLevel(port.classificationTrace()),
                List.of(), // managedTypes - driving ports don't manage types
                port.operations().stream().map(this::toMethodDocV4).collect(Collectors.toList()),
                toDebugInfoV4(port.id().qualifiedName(), port.syntax()));
    }

    private PortDoc toDocV4(DrivenPort port) {
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
                port.operations().stream().map(this::toMethodDocV4).collect(Collectors.toList()),
                toDebugInfoV4(port.id().qualifiedName(), port.syntax()));
    }

    private MethodDoc toMethodDocV4(PortOperation operation) {
        String returnType =
                operation.returnType() != null ? operation.returnType().qualifiedName() : "void";
        List<String> parameters = operation.parameterTypes().stream()
                .map(io.hexaglue.syntax.TypeRef::qualifiedName)
                .collect(Collectors.toList());
        return new MethodDoc(operation.name(), returnType, parameters);
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
