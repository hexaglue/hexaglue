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
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.List;
import java.util.Optional;

/**
 * Matches sub-resource actions: {@code deposit(AccountId, Money) → POST /{id}/deposit}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code COMMAND} or {@code COMMAND_QUERY}</li>
 *   <li>Aggregate must not be {@code null}</li>
 *   <li>First parameter is the aggregate identity</li>
 *   <li>Method name does NOT match create, update, or delete prefixes</li>
 * </ul>
 *
 * <p>The action segment of the path is the full method name converted to kebab-case:
 * {@code deposit} &rarr; {@code deposit}, {@code executeTransfer} &rarr; {@code execute-transfer},
 * {@code blockCard} &rarr; {@code block-card}.
 *
 * <p>Result: {@code POST /{id}/{action-kebab}} with HTTP 200 for non-void return types or
 * HTTP 204 for void methods, one identity path variable, no query params.
 *
 * @since 3.1.0
 */
public final class SubResourceActionStrategy implements HttpVerbStrategy {

    // Combined exclusion set: all prefixes from CreateStrategy, UpdateStrategy, DeleteStrategy.
    // Excluded so that those strategies take priority when their conditions are met.
    private static final String[] EXCLUDED_PREFIXES = {
        "create",
        "open",
        "add",
        "register",
        "initiate",
        "issue",
        "new",
        "save",
        "update",
        "modify",
        "change",
        "edit",
        "rename",
        "set",
        "delete",
        "remove",
        "close",
        "archive",
        "disable",
        "deactivate"
    };

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (!useCase.type().isCommand()) {
            return Optional.empty();
        }
        if (aggregate == null) {
            return Optional.empty();
        }
        if (!StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.empty();
        }
        if (matchesExcludedPrefix(useCase.name())) {
            return Optional.empty();
        }
        String actionPath = NamingConventions.toKebabCase(useCase.name());
        int status = StrategyHelper.isVoidReturn(useCase) ? 204 : 200;
        return Optional.of(new HttpMapping(
                HttpMethod.POST,
                "/{id}/" + actionPath,
                status,
                List.of(StrategyHelper.identityPathVariable(aggregate)),
                List.of()));
    }

    private static boolean matchesExcludedPrefix(String name) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int order() {
        return 1000;
    }
}
