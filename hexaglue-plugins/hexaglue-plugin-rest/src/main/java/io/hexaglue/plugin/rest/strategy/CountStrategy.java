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
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import io.hexaglue.plugin.rest.model.QueryParamSpec;
import java.util.List;
import java.util.Optional;

/**
 * Matches count queries: {@code countAccounts() → GET /count}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code QUERY}</li>
 *   <li>Method name starts with {@code count}</li>
 *   <li>Return type is numeric ({@code int}, {@code long}, {@code Integer}, or {@code Long})</li>
 * </ul>
 *
 * <p>Result: {@code GET /count} with HTTP 200 and no path variables. Any parameters become
 * required {@link QueryParamSpec} instances, preserving the original declaration order.
 *
 * @since 3.1.0
 */
public final class CountStrategy implements HttpVerbStrategy {

    private static final String COUNT_PATH = "/count";

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.QUERY) {
            return Optional.empty();
        }
        if (!useCase.name().startsWith("count")) {
            return Optional.empty();
        }
        if (!StrategyHelper.isNumericReturn(useCase)) {
            return Optional.empty();
        }

        List<QueryParamSpec> queryParams = useCase.method().parameters().stream()
                .map(p -> new QueryParamSpec(p.name(), p.name(), StrategyHelper.toTypeName(p.type()), true, null))
                .toList();

        return Optional.of(new HttpMapping(HttpMethod.GET, COUNT_PATH, 200, List.of(), queryParams));
    }

    @Override
    public int order() {
        return 500;
    }
}
