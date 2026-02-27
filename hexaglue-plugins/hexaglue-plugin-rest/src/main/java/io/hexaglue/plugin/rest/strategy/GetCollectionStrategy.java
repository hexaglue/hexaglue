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
import java.util.regex.Pattern;

/**
 * Matches collection-returning queries: {@code getAllAccounts() → GET ""}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code QUERY}</li>
 *   <li>Return type is a collection ({@code List}, {@code Set}, or {@code Collection})</li>
 *   <li>Method name matches {@code getAll*}, {@code findAll*}, {@code list*}, or exactly
 *       {@code getAll} / {@code findAll}</li>
 *   <li>Zero or one parameters</li>
 * </ul>
 *
 * <p>Result:
 * <ul>
 *   <li>Zero parameters: {@code GET ""} with HTTP 200, no path variables, no query params</li>
 *   <li>One parameter (filter): {@code GET ""} with HTTP 200, no path variables,
 *       one required {@link QueryParamSpec} derived from the parameter</li>
 * </ul>
 *
 * @since 3.1.0
 */
public final class GetCollectionStrategy implements HttpVerbStrategy {

    /**
     * Matches {@code getAll}, {@code findAll}, {@code getAll*}, {@code findAll*}, or {@code list*}.
     *
     * <p>The alternation order ensures the bare keywords are tried after the suffixed forms,
     * while a single {@code matches()} call covers both variants via the trailing anchor absence.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^(getAll|findAll|list).+|getAll|findAll");

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.QUERY) {
            return Optional.empty();
        }
        if (!StrategyHelper.isCollectionReturn(useCase)) {
            return Optional.empty();
        }
        if (!NAME_PATTERN.matcher(useCase.name()).matches()) {
            return Optional.empty();
        }
        List<Parameter> params = useCase.method().parameters();
        if (params.size() > 1) {
            return Optional.empty();
        }
        List<QueryParamSpec> queryParams = params.isEmpty() ? List.of() : List.of(toQueryParam(params.get(0)));
        return Optional.of(new HttpMapping(HttpMethod.GET, "", 200, List.of(), queryParams));
    }

    @Override
    public int order() {
        return 300;
    }

    private static QueryParamSpec toQueryParam(Parameter param) {
        return new QueryParamSpec(param.name(), param.name(), StrategyHelper.toTypeName(param.type()), true, null);
    }
}
