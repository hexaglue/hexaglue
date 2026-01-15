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
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortMethod;
import java.util.List;

/**
 * Selects port content from IrSnapshot and converts it to documentation models.
 */
public final class PortContentSelector {

    private final IrSnapshot ir;

    public PortContentSelector(IrSnapshot ir) {
        this.ir = ir;
    }

    public List<PortDoc> selectDrivingPorts() {
        return ir.ports().drivingPorts().stream().map(this::toDoc).toList();
    }

    public List<PortDoc> selectDrivenPorts() {
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
}
