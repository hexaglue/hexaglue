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
 * Specification for a REST controller generated from a driving port.
 *
 * @param className          controller class name (e.g., "AccountController")
 * @param packageName        target package (e.g., "com.acme.api.controller")
 * @param basePath           base URL path (e.g., "/api/accounts")
 * @param drivingPortType    JavaPoet ClassName of the driving port interface
 * @param aggregateType      JavaPoet ClassName of the associated aggregate (nullable)
 * @param tagName            OpenAPI @Tag name (e.g., "Accounts")
 * @param tagDescription     OpenAPI @Tag description
 * @param endpoints          ordered list of endpoint specifications
 * @param requestDtos        request DTO specs to generate
 * @param responseDtos       response DTO specs to generate
 * @param exceptionMappings  exception-to-HTTP-status mappings
 * @since 3.1.0
 */
public record ControllerSpec(
        String className,
        String packageName,
        String basePath,
        ClassName drivingPortType,
        ClassName aggregateType,
        String tagName,
        String tagDescription,
        List<EndpointSpec> endpoints,
        List<RequestDtoSpec> requestDtos,
        List<ResponseDtoSpec> responseDtos,
        List<ExceptionMappingSpec> exceptionMappings) {

    /**
     * Creates a new ControllerSpec with defensive copies.
     */
    public ControllerSpec {
        endpoints = List.copyOf(endpoints);
        requestDtos = List.copyOf(requestDtos);
        responseDtos = List.copyOf(responseDtos);
        exceptionMappings = List.copyOf(exceptionMappings);
    }

    /**
     * Returns the fully qualified class name.
     *
     * @return package + class name
     */
    public String fullyQualifiedClassName() {
        return packageName + "." + className;
    }

    /**
     * Returns whether this controller has an associated aggregate.
     *
     * @return true if aggregateType is not null
     */
    public boolean hasAggregate() {
        return aggregateType != null;
    }
}
