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
 * Specification for a request DTO record.
 *
 * @param className        DTO class name (e.g., "OpenAccountRequest")
 * @param packageName      target package (e.g., "com.acme.api.dto")
 * @param fields           ordered list of DTO fields
 * @param sourceUseCaseName name of the use case this DTO serves
 * @since 3.1.0
 */
public record RequestDtoSpec(
        String className, String packageName, List<DtoFieldSpec> fields, String sourceUseCaseName) {

    /**
     * Creates a new RequestDtoSpec with defensive copy.
     */
    public RequestDtoSpec {
        fields = List.copyOf(fields);
    }
}
