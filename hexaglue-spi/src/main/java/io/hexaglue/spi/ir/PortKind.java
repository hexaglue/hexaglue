package io.hexaglue.spi.ir;

/**
 * Classification of port interfaces.
 */
public enum PortKind {

    /**
     * Repository - persistence abstraction for aggregates.
     */
    REPOSITORY,

    /**
     * Gateway - abstraction for external system integration.
     */
    GATEWAY,

    /**
     * Use case - application service defining a business operation.
     */
    USE_CASE,

    /**
     * Command handler - handles a specific command.
     */
    COMMAND,

    /**
     * Query handler - handles a specific query.
     */
    QUERY,

    /**
     * Event publisher - publishes domain events.
     */
    EVENT_PUBLISHER,

    /**
     * Generic port - could not be classified more specifically.
     */
    GENERIC
}
