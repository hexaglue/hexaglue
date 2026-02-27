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

package io.hexaglue.plugin.rest.model;

import com.palantir.javapoet.TypeName;

/**
 * Specification for a path variable parameter.
 *
 * @param name         variable name as it appears in the URL (e.g., "id")
 * @param javaName     Java parameter name (e.g., "id")
 * @param javaType     JavaPoet TypeName (e.g., Long)
 * @param isIdentifier true if this is the aggregate root identity
 * @since 3.1.0
 */
public record PathVariableSpec(String name, String javaName, TypeName javaType, boolean isIdentifier) {}
