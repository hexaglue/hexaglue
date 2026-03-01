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

package io.hexaglue.plugin.rest.strategy;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import io.hexaglue.plugin.rest.model.QueryParamSpec;
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.List;
import java.util.Optional;

/**
 * Fallback strategy: QUERY to GET, COMMAND/COMMAND_QUERY to POST.
 *
 * <p>This strategy always matches and serves as the last resort in the chain.
 * The path is derived from the use case name in kebab-case.
 *
 * @since 3.1.0
 */
public final class FallbackStrategy implements HttpVerbStrategy {

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        HttpMethod method = useCase.type() == UseCaseType.QUERY ? HttpMethod.GET : HttpMethod.POST;
        String path = "/" + NamingConventions.toKebabCase(useCase.name());

        List<QueryParamSpec> queryParams = List.of();
        if (method == HttpMethod.GET) {
            queryParams = useCase.method().parameters().stream()
                    .map(FallbackStrategy::toQueryParam)
                    .toList();
        }

        return Optional.of(new HttpMapping(method, path, 200, List.of(), queryParams));
    }

    private static QueryParamSpec toQueryParam(Parameter param) {
        return new QueryParamSpec(param.name(), param.name(), StrategyHelper.toTypeName(param.type()), true, null);
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
