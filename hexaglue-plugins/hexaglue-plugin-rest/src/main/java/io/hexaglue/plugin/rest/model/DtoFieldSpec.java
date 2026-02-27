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
 * Specification for a single field in a DTO.
 *
 * @param fieldName       Java field name (e.g., "balanceAmount")
 * @param javaType        JavaPoet TypeName (e.g., BigDecimal)
 * @param sourceFieldName original field name in the domain type (e.g., "balance")
 * @param accessorChain   accessor expression for response mapping (e.g., "getBalance().amount()")
 * @param validation      Bean Validation annotation to apply (null if none)
 * @param projectionKind  how this field was derived from the domain model
 * @since 3.1.0
 */
public record DtoFieldSpec(
        String fieldName,
        TypeName javaType,
        String sourceFieldName,
        String accessorChain,
        ValidationKind validation,
        ProjectionKind projectionKind) {}
