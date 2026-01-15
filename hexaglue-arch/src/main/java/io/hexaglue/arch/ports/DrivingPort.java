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

package io.hexaglue.arch.ports;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * A Driving Port (Primary/Inbound) in Hexagonal Architecture.
 *
 * <p>Driving ports define the API that the application exposes to the outside world.
 * They are implemented by application services and called by driving adapters
 * (REST controllers, CLI, etc.).</p>
 *
 * <h2>Classifications</h2>
 * <ul>
 *   <li>{@link PortClassification#USE_CASE} - Application use case</li>
 *   <li>{@link PortClassification#COMMAND_HANDLER} - CQRS command</li>
 *   <li>{@link PortClassification#QUERY_HANDLER} - CQRS query</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In application:
 * public interface PlaceOrderUseCase {
 *     OrderId execute(PlaceOrderCommand command);
 * }
 *
 * // As ArchElement:
 * DrivingPort port = new DrivingPort(
 *     ElementId.of("com.example.PlaceOrderUseCase"),
 *     PortClassification.USE_CASE,
 *     List.of(executeOperation),
 *     List.of(orderServiceRef),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param classification the specific port classification
 * @param operations the operations defined by this port
 * @param implementedBy references to services that implement this port
 * @param syntax the syntax information (nullable)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DrivingPort(
        ElementId id,
        PortClassification classification,
        List<PortOperation> operations,
        List<ElementRef<ApplicationService>> implementedBy,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DrivingPort instance.
     *
     * @param id the identifier, must not be null
     * @param classification the classification, must not be null
     * @param operations the operations, must not be null
     * @param implementedBy the implementing services, must not be null
     * @param syntax the syntax (can be null)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if required fields are null
     */
    public DrivingPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(operations, "operations must not be null");
        Objects.requireNonNull(implementedBy, "implementedBy must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        operations = List.copyOf(operations);
        implementedBy = List.copyOf(implementedBy);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.DRIVING_PORT;
    }

    /**
     * Returns whether this is a use case port.
     *
     * @return true if USE_CASE classification
     */
    public boolean isUseCase() {
        return classification == PortClassification.USE_CASE;
    }

    /**
     * Returns whether this is a command handler port.
     *
     * @return true if COMMAND_HANDLER classification
     */
    public boolean isCommandHandler() {
        return classification == PortClassification.COMMAND_HANDLER;
    }

    /**
     * Returns whether this is a query handler port.
     *
     * @return true if QUERY_HANDLER classification
     */
    public boolean isQueryHandler() {
        return classification == PortClassification.QUERY_HANDLER;
    }

    /**
     * Creates a simple DrivingPort for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DrivingPort
     */
    public static DrivingPort of(String qualifiedName, ClassificationTrace trace) {
        return new DrivingPort(
                ElementId.of(qualifiedName), PortClassification.USE_CASE, List.of(), List.of(), null, trace);
    }
}
