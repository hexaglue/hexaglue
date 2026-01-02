package io.hexaglue.spi.ir;

/**
 * Direction of a port in hexagonal architecture.
 */
public enum PortDirection {

    /**
     * Driving port (primary/inbound).
     *
     * <p>These ports are called BY external actors (UI, REST controllers, etc.)
     * to drive the application. Examples: use cases, command handlers.
     */
    DRIVING,

    /**
     * Driven port (secondary/outbound).
     *
     * <p>These ports are called BY the application to interact with
     * external systems. Examples: repositories, gateways, event publishers.
     */
    DRIVEN
}
