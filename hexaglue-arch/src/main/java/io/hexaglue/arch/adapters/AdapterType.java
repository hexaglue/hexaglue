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

package io.hexaglue.arch.adapters;

/**
 * Classification of adapter types in Hexagonal Architecture.
 *
 * <h2>Driving Adapters (Primary/Inbound)</h2>
 * <ul>
 *   <li>{@link #REST_CONTROLLER} - HTTP REST endpoints</li>
 *   <li>{@link #GRAPHQL_CONTROLLER} - GraphQL endpoints</li>
 *   <li>{@link #MESSAGE_LISTENER} - Message queue consumers</li>
 *   <li>{@link #CLI} - Command-line interface</li>
 *   <li>{@link #SCHEDULED_TASK} - Scheduled/cron jobs</li>
 * </ul>
 *
 * <h2>Driven Adapters (Secondary/Outbound)</h2>
 * <ul>
 *   <li>{@link #JPA_REPOSITORY} - JPA/Hibernate persistence</li>
 *   <li>{@link #JDBC_REPOSITORY} - Direct JDBC access</li>
 *   <li>{@link #HTTP_CLIENT} - HTTP client for external APIs</li>
 *   <li>{@link #MESSAGE_PRODUCER} - Message queue producer</li>
 *   <li>{@link #FILE_STORAGE} - File system access</li>
 *   <li>{@link #CACHE} - Caching infrastructure</li>
 * </ul>
 *
 * @since 4.0.0
 */
public enum AdapterType {

    // === Driving Adapters ===

    /**
     * REST controller handling HTTP requests.
     */
    REST_CONTROLLER,

    /**
     * GraphQL resolver/controller.
     */
    GRAPHQL_CONTROLLER,

    /**
     * Message queue listener/consumer.
     */
    MESSAGE_LISTENER,

    /**
     * Command-line interface adapter.
     */
    CLI,

    /**
     * Scheduled task/cron job.
     */
    SCHEDULED_TASK,

    // === Driven Adapters ===

    /**
     * JPA/Hibernate repository implementation.
     */
    JPA_REPOSITORY,

    /**
     * Direct JDBC repository implementation.
     */
    JDBC_REPOSITORY,

    /**
     * HTTP client for external API calls.
     */
    HTTP_CLIENT,

    /**
     * Message queue producer.
     */
    MESSAGE_PRODUCER,

    /**
     * File storage implementation.
     */
    FILE_STORAGE,

    /**
     * Caching adapter.
     */
    CACHE,

    /**
     * Unclassified adapter type.
     */
    UNKNOWN;

    /**
     * Returns whether this is a driving (inbound) adapter type.
     *
     * @return true if driving adapter
     */
    public boolean isDriving() {
        return this == REST_CONTROLLER
                || this == GRAPHQL_CONTROLLER
                || this == MESSAGE_LISTENER
                || this == CLI
                || this == SCHEDULED_TASK;
    }

    /**
     * Returns whether this is a driven (outbound) adapter type.
     *
     * @return true if driven adapter
     */
    public boolean isDriven() {
        return this == JPA_REPOSITORY
                || this == JDBC_REPOSITORY
                || this == HTTP_CLIENT
                || this == MESSAGE_PRODUCER
                || this == FILE_STORAGE
                || this == CACHE;
    }
}
