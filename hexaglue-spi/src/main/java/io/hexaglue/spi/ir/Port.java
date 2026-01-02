package io.hexaglue.spi.ir;

import java.util.List;

/**
 * A port interface in the hexagonal architecture.
 *
 * @param qualifiedName the fully qualified interface name
 * @param simpleName the simple interface name
 * @param packageName the package name
 * @param kind the port classification
 * @param direction driving (inbound) or driven (outbound)
 * @param confidence how confident the classification is
 * @param managedTypes the domain types used in this port's signatures
 * @param methods the port methods
 * @param annotations the annotation qualified names present on this interface
 * @param sourceRef source location for diagnostics
 */
public record Port(
        String qualifiedName,
        String simpleName,
        String packageName,
        PortKind kind,
        PortDirection direction,
        ConfidenceLevel confidence,
        List<String> managedTypes,
        List<PortMethod> methods,
        List<String> annotations,
        SourceRef sourceRef) {

    /**
     * Returns true if this is a repository port.
     */
    public boolean isRepository() {
        return kind == PortKind.REPOSITORY;
    }

    /**
     * Returns true if this is a driving (primary/inbound) port.
     */
    public boolean isDriving() {
        return direction == PortDirection.DRIVING;
    }

    /**
     * Returns true if this is a driven (secondary/outbound) port.
     */
    public boolean isDriven() {
        return direction == PortDirection.DRIVEN;
    }
}
