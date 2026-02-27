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
import io.hexaglue.plugin.rest.model.PathVariableSpec;
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.List;
import java.util.Optional;

/**
 * Matches get-by-property queries: {@code getAccountByNumber(String) → GET /by-number/{accountNumber}}.
 *
 * <p>Conditions:
 * <ul>
 *   <li>Use case type is {@code QUERY}</li>
 *   <li>Method name contains {@code By} (e.g., {@code getAccountByNumber}, {@code findByEmail})</li>
 *   <li>Exactly one parameter whose type does NOT match the aggregate identity field type</li>
 * </ul>
 *
 * <p>The property segment is extracted from the method name after {@code By} and converted to
 * kebab-case. The path variable uses the actual Java parameter name.
 *
 * <p>Result: {@code GET /by-{property}/{paramName}} with HTTP 200.
 *
 * @since 3.1.0
 */
public final class GetByPropertyStrategy implements HttpVerbStrategy {

    /** Delimiter used to split the method name on the "By" boundary. */
    private static final String BY_DELIMITER = "By";

    @Override
    public Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath) {
        if (useCase.type() != UseCaseType.QUERY) {
            return Optional.empty();
        }
        if (useCase.method().parameters().size() != 1) {
            return Optional.empty();
        }
        int byIndex = useCase.name().indexOf(BY_DELIMITER);
        if (byIndex < 0) {
            return Optional.empty();
        }
        // The property segment must have at least one character after "By"
        String propertySegment = useCase.name().substring(byIndex + BY_DELIMITER.length());
        if (propertySegment.isEmpty()) {
            return Optional.empty();
        }
        // The single parameter must NOT be the aggregate identity
        if (StrategyHelper.isFirstParamIdentity(useCase, aggregate)) {
            return Optional.empty();
        }
        Parameter param = useCase.method().parameters().get(0);
        String propertyKebab = NamingConventions.toKebabCase(propertySegment);
        String path = "/by-" + propertyKebab + "/{" + param.name() + "}";
        PathVariableSpec pathVariable =
                new PathVariableSpec(param.name(), param.name(), StrategyHelper.toTypeName(param.type()), false);
        return Optional.of(new HttpMapping(HttpMethod.GET, path, 200, List.of(pathVariable), List.of()));
    }

    @Override
    public int order() {
        return 200;
    }
}
