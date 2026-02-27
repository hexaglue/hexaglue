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

/**
 * Specification for mapping a domain exception to an HTTP status.
 *
 * @param exceptionType JavaPoet ClassName of the exception
 * @param httpStatus    HTTP status code (400, 404, 409, 422)
 * @param errorCode     error code string (e.g., "NOT_FOUND", "INSUFFICIENT_FUNDS")
 * @param handlerMethod Java method name (e.g., "handleAccountNotFound")
 * @since 3.1.0
 */
public record ExceptionMappingSpec(ClassName exceptionType, int httpStatus, String errorCode, String handlerMethod) {}
