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
import java.util.List;
import java.util.Optional;

/**
 * Matches multi-criteria search queries: {@code searchAccounts(status, customerId) → GET /search?...}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code QUERY}</li>
 *   <li>Method name starts with {@code search}, {@code query}, or {@code find}</li>
 *   <li>Two or more parameters</li>
 *   <li>Return type is a collection ({@code List}, {@code Set}, or {@code Collection})</li>
 * </ul>
 *
 * <p>Result: {@code GET /search} with HTTP 200. All parameters become required
 * {@link QueryParamSpec} instances, preserving the original declaration order.
 *
 * @since 3.1.0
 */
public final class SearchStrategy implements HttpVerbStrategy {

    private static final String SEARCH_PATH = "/search";

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.QUERY) {
            return Optional.empty();
        }
        String name = useCase.name();
        if (!name.startsWith("search") && !name.startsWith("query") && !name.startsWith("find")) {
            return Optional.empty();
        }
        List<Parameter> params = useCase.method().parameters();
        if (params.size() < 2) {
            return Optional.empty();
        }
        if (!StrategyHelper.isCollectionReturn(useCase)) {
            return Optional.empty();
        }
        List<QueryParamSpec> queryParams =
                params.stream().map(SearchStrategy::toQueryParam).toList();
        return Optional.of(new HttpMapping(HttpMethod.GET, SEARCH_PATH, 200, List.of(), queryParams));
    }

    @Override
    public int order() {
        return 400;
    }

    private static QueryParamSpec toQueryParam(Parameter param) {
        return new QueryParamSpec(param.name(), param.name(), StrategyHelper.toTypeName(param.type()), true, null);
    }
}
