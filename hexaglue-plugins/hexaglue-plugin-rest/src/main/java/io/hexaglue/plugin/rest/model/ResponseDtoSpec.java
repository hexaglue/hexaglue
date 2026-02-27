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

import com.palantir.javapoet.ClassName;
import java.util.List;

/**
 * Specification for a response DTO record.
 *
 * @param className      DTO class name (e.g., "AccountResponse")
 * @param packageName    target package (e.g., "com.acme.api.dto")
 * @param fields         ordered list of projected fields
 * @param domainType     JavaPoet ClassName of the domain type
 * @param sourceTypeName simple name of the domain type (e.g., "Account")
 * @since 3.1.0
 */
public record ResponseDtoSpec(
        String className, String packageName, List<DtoFieldSpec> fields, ClassName domainType, String sourceTypeName) {

    /**
     * Creates a new ResponseDtoSpec with defensive copy.
     */
    public ResponseDtoSpec {
        fields = List.copyOf(fields);
    }
}
