package io.hexaglue.spi.ir;

import java.time.Instant;
import java.util.List;

/**
 * Immutable snapshot of the analyzed application.
 *
 * <p>This is the primary output of the HexaGlue analysis phase and the main
 * input for plugins. It contains the complete domain model and port model
 * extracted from the application source code.
 *
 * @param domain the domain model (entities, value objects, aggregates, etc.)
 * @param ports the port model (repositories, gateways, use cases, etc.)
 * @param metadata analysis metadata (timestamp, version, etc.)
 */
public record IrSnapshot(DomainModel domain, PortModel ports, IrMetadata metadata) {

    /**
     * Creates an empty snapshot (for error cases).
     *
     * @param basePackage the base package
     * @return an empty snapshot
     */
    public static IrSnapshot empty(String basePackage) {
        return new IrSnapshot(
                new DomainModel(List.of()),
                new PortModel(List.of()),
                new IrMetadata(basePackage, Instant.now(), "2.0.0-SNAPSHOT", 0, 0));
    }

    /**
     * Returns true if this snapshot has no domain types or ports.
     */
    public boolean isEmpty() {
        return domain.types().isEmpty() && ports.ports().isEmpty();
    }
}
