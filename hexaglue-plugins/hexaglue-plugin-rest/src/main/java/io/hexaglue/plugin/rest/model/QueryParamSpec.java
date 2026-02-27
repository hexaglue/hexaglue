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
 * Specification for a query parameter.
 *
 * @param name         parameter name in the URL (e.g., "customerId")
 * @param javaName     Java parameter name
 * @param javaType     JavaPoet TypeName
 * @param required     whether the parameter is required
 * @param defaultValue default value (null if none)
 * @since 3.1.0
 */
public record QueryParamSpec(String name, String javaName, TypeName javaType, boolean required, String defaultValue) {}
