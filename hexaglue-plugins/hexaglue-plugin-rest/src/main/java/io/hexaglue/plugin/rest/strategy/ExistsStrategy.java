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
 * Matches existence check queries: {@code existsById(AccountId) → GET /{id}/exists}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code QUERY}</li>
 *   <li>Method name starts with {@code exists}, {@code is}, or {@code has}</li>
 *   <li>Return type is {@code boolean} or {@code Boolean}</li>
 * </ul>
 *
 * <p>Result when exactly one parameter matches the aggregate identity:
 * {@code GET /{id}/exists} with HTTP 200 and a single identity path variable.
 *
 * <p>Result otherwise: {@code GET /exists} with HTTP 200. Any parameters become
 * required {@link QueryParamSpec} instances.
 *
 * @since 3.1.0
 */
public final class ExistsStrategy implements HttpVerbStrategy {

    private static final String EXISTS_PATH = "/exists";
    private static final String ID_EXISTS_PATH = "/{id}/exists";

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.QUERY) {
            return Optional.empty();
        }
        if (!StrategyHelper.isBooleanReturn(useCase)) {
            return Optional.empty();
        }
        String name = useCase.name();
        if (!name.startsWith("exists") && !name.startsWith("is") && !name.startsWith("has")) {
            return Optional.empty();
        }

        // Single identity parameter → /{id}/exists with a path variable
        if (useCase.method().parameters().size() == 1 && StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.of(new HttpMapping(
                    HttpMethod.GET,
                    ID_EXISTS_PATH,
                    200,
                    List.of(StrategyHelper.identityPathVariable(aggregate)),
                    List.of()));
        }

        // Fallback → /exists with query params for any remaining parameters
        List<QueryParamSpec> queryParams = useCase.method().parameters().stream()
                .map(p -> new QueryParamSpec(p.name(), p.name(), StrategyHelper.toTypeName(p.type()), true, null))
                .toList();
        return Optional.of(new HttpMapping(HttpMethod.GET, EXISTS_PATH, 200, List.of(), queryParams));
    }

    @Override
    public int order() {
        return 600;
    }
}
