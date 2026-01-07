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

import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import java.util.List;

/**
 * Documentation model for ports.
 *
 * @param name the simple name
 * @param packageName the package name
 * @param kind the port kind
 * @param direction the port direction (DRIVING, DRIVEN)
 * @param confidence the classification confidence level
 * @param managedTypes list of managed domain type qualified names
 * @param methods list of methods
 * @param debug debug information
 */
public record PortDoc(
        String name,
        String packageName,
        PortKind kind,
        PortDirection direction,
        ConfidenceLevel confidence,
        List<String> managedTypes,
        List<MethodDoc> methods,
        DebugInfo debug) {}
