package io.hexaglue.core.classification.port;

/**
 * Direction of a port in hexagonal architecture.
 */
public enum PortDirection {

    /**
     * Driving port (inbound/primary) - the outside world drives the application.
     * Examples: REST controllers call use cases, CLI commands.
     */
    DRIVING,

    /**
     * Driven port (outbound/secondary) - the application drives external services.
     * Examples: Repositories, gateways, notification services.
     */
    DRIVEN
}
