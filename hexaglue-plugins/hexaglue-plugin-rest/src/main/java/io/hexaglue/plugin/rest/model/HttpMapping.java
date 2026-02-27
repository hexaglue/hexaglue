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
 * Result of HTTP verb derivation.
 *
 * @param httpMethod     HTTP method (GET, POST, PUT, DELETE, PATCH)
 * @param path           path relative to basePath (e.g., "", "/{id}", "/{id}/deposit")
 * @param responseStatus HTTP response status code
 * @param pathVariables  path variables extracted from the method signature
 * @param queryParams    query parameters extracted from the method signature
 * @since 3.1.0
 */
public record HttpMapping(
        HttpMethod httpMethod,
        String path,
        int responseStatus,
        List<PathVariableSpec> pathVariables,
        List<QueryParamSpec> queryParams) {

    /**
     * Creates a new HttpMapping with defensive copies.
     */
    public HttpMapping {
        pathVariables = List.copyOf(pathVariables);
        queryParams = List.copyOf(queryParams);
    }
}
