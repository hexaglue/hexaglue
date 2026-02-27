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
import java.util.Set;

/**
 * Matches deletion commands: {@code closeAccount(AccountId) → DELETE /{id} → 204}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is strictly {@code COMMAND} (not {@code COMMAND_QUERY})</li>
 *   <li>Method name starts with a deletion prefix: {@code delete}, {@code remove},
 *       {@code close}, {@code archive}, {@code disable}, or {@code deactivate}</li>
 *   <li>Return type is {@code void}</li>
 *   <li>Exactly one parameter that is the aggregate identity</li>
 *   <li>Aggregate must not be {@code null}</li>
 * </ul>
 *
 * <p>Result: {@code DELETE /{id}} with HTTP 204, one identity path variable, no query params.
 *
 * @since 3.1.0
 */
public final class DeleteStrategy implements HttpVerbStrategy {

    private static final Set<String> DELETE_PREFIXES =
            Set.of("delete", "remove", "close", "archive", "disable", "deactivate");

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.COMMAND) {
            return Optional.empty();
        }
        if (aggregate == null) {
            return Optional.empty();
        }
        if (!StrategyHelper.isVoidReturn(useCase)) {
            return Optional.empty();
        }
        if (!matchesDeletePrefix(useCase.name())) {
            return Optional.empty();
        }
        if (useCase.method().parameters().size() != 1) {
            return Optional.empty();
        }
        if (!StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.empty();
        }
        return Optional.of(new HttpMapping(
                HttpMethod.DELETE, "/{id}", 204, List.of(StrategyHelper.identityPathVariable(aggregate)), List.of()));
    }

    private static boolean matchesDeletePrefix(String name) {
        for (String prefix : DELETE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int order() {
        return 900;
    }
}
