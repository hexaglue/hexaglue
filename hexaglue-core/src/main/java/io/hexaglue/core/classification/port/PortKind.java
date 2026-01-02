package io.hexaglue.core.classification.port;

/**
 * Classification kinds for port interfaces in hexagonal architecture.
 */
public enum PortKind {

    /**
     * Repository port - persistence abstraction for aggregates.
     * Direction: DRIVEN (outbound/secondary).
     */
    REPOSITORY,

    /**
     * Use case port - application service / command handler.
     * Direction: DRIVING (inbound/primary).
     */
    USE_CASE,

    /**
     * Gateway port - external service abstraction.
     * Direction: DRIVEN (outbound/secondary).
     */
    GATEWAY,

    /**
     * Query port - read-only data access.
     * Direction: DRIVING (inbound/primary).
     */
    QUERY,

    /**
     * Command port - write operations / state changes.
     * Direction: DRIVING (inbound/primary).
     */
    COMMAND
}
