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
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.SourceReference;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.ir.ConfidenceLevel;
import io.hexaglue.arch.model.ir.PortDirection;
import io.hexaglue.arch.model.ir.PortKind;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.plugin.livingdoc.util.PortKindMapper;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Selects port content from ArchitecturalModel and converts it to documentation models.
 *
 * @since 4.0.0
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 * @since 5.0.0 - Migrated to v5 ArchType API with PortIndex
 */
public final class PortContentSelector {

    private final PortIndex portIndex;

    /**
     * Creates a selector using v5 ArchitecturalModel with PortIndex.
     *
     * <p>The {@link PortIndex} is cached at construction time to avoid
     * repeated lookups on every selection method call.</p>
     *
     * @param model the architectural model
     * @throws IllegalStateException if the model does not contain a PortIndex
     * @since 4.0.0
     * @since 5.0.0 - Migrated to v5 API, cached PortIndex
     */
    public PortContentSelector(ArchitecturalModel model) {
        this.portIndex =
                model.portIndex().orElseThrow(() -> new IllegalStateException("PortIndex required for documentation"));
    }

    public List<PortDoc> selectDrivingPorts() {
        return portIndex.drivingPorts().map(this::toDoc).toList();
    }

    public List<PortDoc> selectDrivenPorts() {
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
                toDebugInfo(port.id().qualifiedName(), port.structure()),
                port.structure().documentation().orElse(null));
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
                PortKindMapper.from(port.portType()),
                PortDirection.DRIVEN,
                toConfidenceLevel(port.classification()),
                managedTypes,
                port.structure().methods().stream().map(this::toMethodDoc).collect(Collectors.toList()),
                toDebugInfo(port.id().qualifiedName(), port.structure()),
                port.structure().documentation().orElse(null));
    }

    private MethodDoc toMethodDoc(Method method) {
        String returnType = method.returnType() != null ? formatTypeWithArguments(method.returnType()) : "void";
        List<String> parameters = method.parameters().stream()
                .map(p -> formatTypeWithArguments(p.type()))
                .collect(Collectors.toList());
        return new MethodDoc(
                method.name(), returnType, parameters, method.documentation().orElse(null));
    }

    /**
     * Formats a TypeRef with its type arguments for display.
     *
     * <p>For example, converts `java.util.List` with argument `Order` to `List<Order>`.</p>
     *
     * @param typeRef the type reference
     * @return the formatted type string with arguments
     */
    private String formatTypeWithArguments(TypeRef typeRef) {
        String simpleName = typeRef.simpleName();

        if (typeRef.typeArguments().isEmpty()) {
            return simpleName;
        }

        String args = typeRef.typeArguments().stream().map(TypeRef::simpleName).collect(Collectors.joining(", "));

        return simpleName + "<" + args + ">";
    }

    private DebugInfo toDebugInfo(String qualifiedName, TypeStructure structure) {
        // Use actual source location from TypeStructure when available, fall back to derived path
        Optional<SourceReference> srcLoc = structure.sourceLocation();
        String sourceFile = srcLoc.map(SourceReference::filePath).orElseGet(() -> deriveSourceFilePath(qualifiedName));
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
     * @param qualifiedName the fully qualified class name
     * @return the expected source file path
     * @since 5.0.0
     */
    private String deriveSourceFilePath(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return null;
        }
        return qualifiedName.replace('.', '/') + ".java";
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
