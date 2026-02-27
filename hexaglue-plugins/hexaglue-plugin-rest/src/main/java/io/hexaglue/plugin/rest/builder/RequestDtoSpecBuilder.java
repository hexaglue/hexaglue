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

package io.hexaglue.plugin.rest.builder;

import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.PathVariableSpec;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a {@link RequestDtoSpec} from a use case's parameters.
 *
 * <p>Only COMMAND and COMMAND_QUERY use cases produce request DTOs.
 * Parameters already handled as path variables are excluded.
 *
 * @since 3.1.0
 */
public final class RequestDtoSpecBuilder {

    private RequestDtoSpecBuilder() {
        /* utility class */
    }

    /**
     * Builds a request DTO spec from a use case's parameters.
     *
     * @param useCase     the use case (only COMMAND/COMMAND_QUERY produce DTOs)
     * @param httpMapping the HTTP mapping (pathVariables determine excluded params)
     * @param domainIndex domain type index for identifier/VO detection
     * @param config      plugin configuration
     * @param dtoPackage  target package for DTOs
     * @return the request DTO spec, or empty if no DTO is needed
     */
    public static Optional<RequestDtoSpec> build(
            UseCase useCase, HttpMapping httpMapping, DomainIndex domainIndex, RestConfig config, String dtoPackage) {

        // Only COMMAND and COMMAND_QUERY get request DTOs
        if (useCase.type() == UseCaseType.QUERY) {
            return Optional.empty();
        }

        List<Parameter> params = useCase.method().parameters();
        if (params.isEmpty()) {
            return Optional.empty();
        }

        // Exclude parameters already handled as path variables
        Set<String> pathVarNames = httpMapping.pathVariables().stream()
                .map(PathVariableSpec::javaName)
                .collect(Collectors.toSet());
        List<Parameter> dtoParams =
                params.stream().filter(p -> !pathVarNames.contains(p.name())).toList();

        if (dtoParams.isEmpty()) {
            return Optional.empty();
        }

        // Derive DTO class name
        String className = NamingConventions.capitalize(useCase.name()) + config.requestDtoSuffix();

        // Map parameters to DTO fields
        List<DtoFieldSpec> fields = dtoParams.stream()
                .flatMap(p -> DtoFieldMapper.mapForRequest(p, domainIndex, config).stream())
                .toList();

        return Optional.of(new RequestDtoSpec(className, dtoPackage, fields, useCase.name()));
    }
}
