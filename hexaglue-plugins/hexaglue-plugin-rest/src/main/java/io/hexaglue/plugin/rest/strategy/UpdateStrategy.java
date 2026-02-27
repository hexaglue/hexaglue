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
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Matches update commands: {@code updateCustomer(CustomerId, ...) → PUT /{id}}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code COMMAND} or {@code COMMAND_QUERY}</li>
 *   <li>Method name starts with an update prefix: {@code update}, {@code modify}, {@code change},
 *       {@code edit}, {@code rename}, or {@code set}</li>
 *   <li>First parameter IS the aggregate identity (distinguishes from {@link CreateStrategy})</li>
 *   <li>Aggregate must not be {@code null}</li>
 * </ul>
 *
 * <p>Result: {@code PUT /{id}} with HTTP 200, one identity path variable, no query params.
 *
 * @since 3.1.0
 */
public final class UpdateStrategy implements HttpVerbStrategy {

    private static final Set<String> UPDATE_PREFIXES = Set.of("update", "modify", "change", "edit", "rename", "set");

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (!useCase.type().isCommand()) {
            return Optional.empty();
        }
        if (aggregate == null) {
            return Optional.empty();
        }
        if (!matchesUpdatePrefix(useCase.name())) {
            return Optional.empty();
        }
        if (!StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.empty();
        }
        return Optional.of(new HttpMapping(
                HttpMethod.PUT, "/{id}", 200, List.of(StrategyHelper.identityPathVariable(aggregate)), List.of()));
    }

    private static boolean matchesUpdatePrefix(String name) {
        for (String prefix : UPDATE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int order() {
        return 800;
    }
}
