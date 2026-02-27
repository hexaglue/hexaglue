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

import java.util.List;

/**
 * Specification for a global exception handler.
 *
 * @param className   handler class name (e.g., "GlobalExceptionHandler")
 * @param packageName target package (e.g., "com.acme.api.exception")
 * @param mappings    exception-to-HTTP-status mappings
 * @since 3.1.0
 */
public record ExceptionHandlerSpec(String className, String packageName, List<ExceptionMappingSpec> mappings) {

    /**
     * Creates a new ExceptionHandlerSpec with defensive copy.
     */
    public ExceptionHandlerSpec {
        mappings = List.copyOf(mappings);
    }
}
