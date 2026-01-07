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

import java.util.List;

/**
 * Debug information for documentation elements.
 *
 * @param qualifiedName the fully qualified name
 * @param annotations list of annotation qualified names
 * @param sourceFile the source file path, or null if synthetic
 * @param lineStart starting line number, or 0 if synthetic
 * @param lineEnd ending line number, or 0 if synthetic
 */
public record DebugInfo(
        String qualifiedName, List<String> annotations, String sourceFile, int lineStart, int lineEnd) {}
