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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;
import java.util.Optional;

/**
 * Details about a query handler component.
 *
 * <p>A query handler processes queries that read state without modifying it.
 * In CQRS architecture, query handlers are responsible for handling read operations.
 *
 * @param name simple name of the query handler
 * @param packageName fully qualified package name
 * @param handledQuery name of the query type handled (if detected)
 * @param returnType return type of the query (if detected)
 * @since 5.0.0
 */
public record QueryHandlerComponent(String name, String packageName, String handledQuery, String returnType) {

    /**
     * Creates a query handler component with validation.
     */
    public QueryHandlerComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
    }

    /**
     * Returns the handled query as optional.
     *
     * @return optional handled query name
     */
    public Optional<String> handledQueryOpt() {
        return Optional.ofNullable(handledQuery);
    }

    /**
     * Returns the return type as optional.
     *
     * @return optional return type name
     */
    public Optional<String> returnTypeOpt() {
        return Optional.ofNullable(returnType);
    }

    /**
     * Creates a query handler component with all fields.
     *
     * @param name simple name
     * @param packageName package name
     * @param handledQuery query type handled (may be null)
     * @param returnType return type (may be null)
     * @return the query handler component
     */
    public static QueryHandlerComponent of(String name, String packageName, String handledQuery, String returnType) {
        return new QueryHandlerComponent(name, packageName, handledQuery, returnType);
    }

    /**
     * Creates a query handler component without query/return type info.
     *
     * @param name simple name
     * @param packageName package name
     * @return the query handler component
     */
    public static QueryHandlerComponent of(String name, String packageName) {
        return new QueryHandlerComponent(name, packageName, null, null);
    }
}
