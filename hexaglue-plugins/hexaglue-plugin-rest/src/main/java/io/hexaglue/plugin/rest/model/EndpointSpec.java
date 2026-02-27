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
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import java.util.List;

/**
 * Specification for a single REST endpoint.
 *
 * @param methodName       Java method name in the controller
 * @param httpMethod       HTTP method (GET, POST, PUT, DELETE, PATCH)
 * @param path             path relative to controller basePath ("", "/{id}", "/{id}/deposit")
 * @param operationSummary OpenAPI @Operation summary text
 * @param returnType       JavaPoet TypeName for the method return type
 * @param responseStatus   HTTP response status code (200, 201, 204)
 * @param requestDtoRef    class name of request DTO (null if no request body)
 * @param responseDtoRef   class name of response DTO (null if void return)
 * @param pathVariables    path variable specifications
 * @param queryParams      query parameter specifications
 * @param thrownExceptions   exception types that this endpoint can throw
 * @param useCaseType        use case classification (COMMAND, QUERY, COMMAND_QUERY)
 * @param parameterBindings  how port parameters are reconstructed from DTO fields
 * @since 3.1.0
 */
public record EndpointSpec(
        String methodName,
        HttpMethod httpMethod,
        String path,
        String operationSummary,
        TypeName returnType,
        int responseStatus,
        String requestDtoRef,
        String responseDtoRef,
        List<PathVariableSpec> pathVariables,
        List<QueryParamSpec> queryParams,
        List<ClassName> thrownExceptions,
        UseCaseType useCaseType,
        List<ParameterBindingSpec> parameterBindings) {

    /**
     * Creates a new EndpointSpec with defensive copies.
     */
    public EndpointSpec {
        pathVariables = List.copyOf(pathVariables);
        queryParams = List.copyOf(queryParams);
        thrownExceptions = List.copyOf(thrownExceptions);
        parameterBindings = List.copyOf(parameterBindings);
    }

    /**
     * Returns whether this endpoint expects a request body.
     *
     * @return true if a request DTO is referenced
     */
    public boolean hasRequestBody() {
        return requestDtoRef != null;
    }

    /**
     * Returns whether this endpoint produces a response body.
     *
     * @return true if a response DTO is referenced
     */
    public boolean hasResponseBody() {
        return responseDtoRef != null;
    }

    /**
     * Returns whether this endpoint returns no content.
     *
     * @return true if response status is 204
     */
    public boolean isVoid() {
        return responseStatus == 204;
    }
}
