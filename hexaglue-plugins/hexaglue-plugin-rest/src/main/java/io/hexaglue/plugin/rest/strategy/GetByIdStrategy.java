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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Matches get-by-id queries: {@code getAccount(AccountId) → GET /{id}}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code QUERY}</li>
 *   <li>Method name starts with {@code get}, {@code find}, {@code load}, or {@code fetch}</li>
 *   <li>Exactly one parameter whose type matches the aggregate identity field type</li>
 * </ul>
 *
 * <p>Result: {@code GET /{id}} with HTTP 200 and a single identity path variable.
 *
 * @since 3.1.0
 */
public final class GetByIdStrategy implements HttpVerbStrategy {

    private static final Pattern NAME_PATTERN = Pattern.compile("^(get|find|load|fetch).+");

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.QUERY) {
            return Optional.empty();
        }
        if (aggregate == null) {
            return Optional.empty();
        }
        if (useCase.method().parameters().size() != 1) {
            return Optional.empty();
        }
        if (!NAME_PATTERN.matcher(useCase.name()).matches()) {
            return Optional.empty();
        }
        if (!StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.empty();
        }
        return Optional.of(new HttpMapping(
                HttpMethod.GET, "/{id}", 200, List.of(StrategyHelper.identityPathVariable(aggregate)), List.of()));
    }

    @Override
    public int order() {
        return 100;
    }
}
