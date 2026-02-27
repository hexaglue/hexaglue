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
 * Matches creation commands: {@code openAccount(...) → POST → 201}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code COMMAND} or {@code COMMAND_QUERY}</li>
 *   <li>Method name starts with a creation prefix: {@code create}, {@code open}, {@code add},
 *       {@code register}, {@code initiate}, {@code issue}, {@code new}, or {@code save}</li>
 *   <li>First parameter is NOT the aggregate identity (distinguishes from {@link UpdateStrategy})</li>
 * </ul>
 *
 * <p>Result: {@code POST ""} with HTTP 201, no path variables, no query params.
 *
 * <p>When {@code aggregate} is {@code null}, {@code isFirstParamIdentity} returns false, so
 * creation still matches — this is correct, as you can create resources without a known aggregate.
 *
 * @since 3.1.0
 */
public final class CreateStrategy implements HttpVerbStrategy {

    private static final Set<String> CREATE_PREFIXES =
            Set.of("create", "open", "add", "register", "initiate", "issue", "new", "save");

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (!useCase.type().isCommand()) {
            return Optional.empty();
        }
        if (!matchesCreatePrefix(useCase.name())) {
            return Optional.empty();
        }
        if (StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.empty();
        }
        return Optional.of(new HttpMapping(HttpMethod.POST, "", 201, List.of(), List.of()));
    }

    private static boolean matchesCreatePrefix(String name) {
        for (String prefix : CREATE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int order() {
        return 700;
    }
}
