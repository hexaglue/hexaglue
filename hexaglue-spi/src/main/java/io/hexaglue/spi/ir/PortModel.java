package io.hexaglue.spi.ir;

import java.util.List;
import java.util.Optional;

/**
 * The port model extracted from the application.
 *
 * @param ports all detected ports (repositories, gateways, use cases, etc.)
 */
public record PortModel(List<Port> ports) {

    /**
     * Finds a port by its qualified name.
     */
    public Optional<Port> findByQualifiedName(String qualifiedName) {
        return ports.stream()
                .filter(p -> p.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    /**
     * Returns all ports of a specific kind.
     */
    public List<Port> portsOfKind(PortKind kind) {
        return ports.stream().filter(p -> p.kind() == kind).toList();
    }

    /**
     * Returns all driving ports (primary/inbound).
     */
    public List<Port> drivingPorts() {
        return ports.stream()
                .filter(p -> p.direction() == PortDirection.DRIVING)
                .toList();
    }

    /**
     * Returns all driven ports (secondary/outbound).
     */
    public List<Port> drivenPorts() {
        return ports.stream().filter(p -> p.direction() == PortDirection.DRIVEN).toList();
    }

    /**
     * Returns all repositories.
     */
    public List<Port> repositories() {
        return portsOfKind(PortKind.REPOSITORY);
    }
}
