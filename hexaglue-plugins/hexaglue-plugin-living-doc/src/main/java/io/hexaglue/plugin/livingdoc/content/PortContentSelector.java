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
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects port content from ArchitecturalModel and converts it to documentation models.
 *
 * @since 4.0.0
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 * @since 5.0.0 - Migrated to v5 ArchType API with PortIndex
 */
public final class PortContentSelector {

    private final ArchitecturalModel model;

    /**
     * Creates a selector using v5 ArchitecturalModel with PortIndex.
     *
     * @param model the architectural model
     * @since 4.0.0
     * @since 5.0.0 - Migrated to v5 API
     */
    public PortContentSelector(ArchitecturalModel model) {
        this.model = model;
    }

    public List<PortDoc> selectDrivingPorts() {
        PortIndex portIndex =
                model.portIndex().orElseThrow(() -> new IllegalStateException("PortIndex required for documentation"));
        return portIndex.drivingPorts().map(this::toDoc).toList();
    }

    public List<PortDoc> selectDrivenPorts() {
        PortIndex portIndex =
                model.portIndex().orElseThrow(() -> new IllegalStateException("PortIndex required for documentation"));
        return portIndex.drivenPorts().map(this::toDoc).toList();
    }

    private PortDoc toDoc(DrivingPort port) {
        String packageName = extractPackageName(port.id().qualifiedName());
        String simpleName = port.id().simpleName();

        return new PortDoc(
                simpleName,
                packageName,
                PortKind.USE_CASE, // Default kind for driving ports in v5
                PortDirection.DRIVING,
                toConfidenceLevel(port.classification()),
                List.of(), // managedTypes - driving ports don't manage types
                port.structure().methods().stream().map(this::toMethodDoc).collect(Collectors.toList()),
                toDebugInfo(port.id().qualifiedName(), port.structure()));
    }

    private PortDoc toDoc(DrivenPort port) {
        String packageName = extractPackageName(port.id().qualifiedName());
        String simpleName = port.id().simpleName();

        // Extract managed types
        List<String> managedTypes =
                port.managedAggregate().map(ref -> List.of(ref.qualifiedName())).orElse(List.of());

        return new PortDoc(
                simpleName,
                packageName,
                toPortKind(port.portType()),
                PortDirection.DRIVEN,
                toConfidenceLevel(port.classification()),
                managedTypes,
                port.structure().methods().stream().map(this::toMethodDoc).collect(Collectors.toList()),
                toDebugInfo(port.id().qualifiedName(), port.structure()));
    }

    private MethodDoc toMethodDoc(Method method) {
        String returnType = method.returnType() != null ? method.returnType().qualifiedName() : "void";
        List<String> parameters =
                method.parameters().stream().map(p -> p.type().qualifiedName()).collect(Collectors.toList());
        return new MethodDoc(method.name(), returnType, parameters);
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

    private PortKind toPortKind(DrivenPortType portType) {
        return switch (portType) {
            case REPOSITORY -> PortKind.REPOSITORY;
            case GATEWAY -> PortKind.GATEWAY;
            case EVENT_PUBLISHER -> PortKind.EVENT_PUBLISHER;
            case NOTIFICATION -> PortKind.GENERIC;
            case OTHER -> PortKind.GENERIC;
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
