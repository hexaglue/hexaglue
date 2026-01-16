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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.spi.ir.ConfidenceLevel;
import java.util.List;

/**
 * Documentation model for domain types.
 *
 * @param name the simple name
 * @param packageName the package name
 * @param kind the domain kind
 * @param confidence the classification confidence level
 * @param construct the language construct (class, record, interface, etc.)
 * @param isRecord whether this is a record type
 * @param identity identity information, or null if not applicable
 * @param properties list of properties
 * @param relations list of explicit relations
 * @param debug debug information
 */
public record DomainTypeDoc(
        String name,
        String packageName,
        ElementKind kind,
        ConfidenceLevel confidence,
        String construct,
        boolean isRecord,
        IdentityDoc identity,
        List<PropertyDoc> properties,
        List<RelationDoc> relations,
        DebugInfo debug) {}
