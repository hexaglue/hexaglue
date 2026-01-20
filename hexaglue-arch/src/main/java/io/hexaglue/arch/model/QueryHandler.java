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

package io.hexaglue.arch.model;

import io.hexaglue.arch.ClassificationTrace;
import java.util.Objects;

/**
 * Represents a query handler in the application layer.
 *
 * <p>A query handler processes queries that read state without modifying it.
 * In CQRS (Command Query Responsibility Segregation) architecture, query handlers
 * are responsible for handling read operations.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Read operations - does not modify state</li>
 *   <li>Single responsibility - handles one query type</li>
 *   <li>No side effects - purely returns data</li>
 * </ul>
 *
 * <h2>Typical Pattern</h2>
 * <pre>{@code
 * public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderView> {
 *     public OrderView handle(GetOrderQuery query) {
 *         // Query read model
 *         // Transform to view model
 *         // Return result
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * QueryHandler handler = QueryHandler.of(
 *     TypeId.of("com.example.GetOrderHandler"),
 *     structure,
 *     trace
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @since 4.1.0
 */
public record QueryHandler(TypeId id, TypeStructure structure, ClassificationTrace classification)
        implements ApplicationType {

    /**
     * Creates a new QueryHandler.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public QueryHandler {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.QUERY_HANDLER;
    }

    /**
     * Creates a QueryHandler with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new QueryHandler
     * @throws NullPointerException if any argument is null
     */
    public static QueryHandler of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new QueryHandler(id, structure, classification);
    }
}
