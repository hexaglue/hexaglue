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
 * An Application Service in Hexagonal Architecture.
 *
 * <p>Application services orchestrate use cases by coordinating domain objects
 * and driven ports. They implement driving ports and depend on driven ports.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Implement driving port interfaces</li>
 *   <li>Coordinate domain objects</li>
 *   <li>Use driven ports for infrastructure</li>
 *   <li>Transaction management</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In application:
 * public class OrderApplicationService implements PlaceOrderUseCase {
 *     private final OrderRepository orderRepository;
 *     private final PaymentGateway paymentGateway;
 *
 *     public OrderId execute(PlaceOrderCommand cmd) { ... }
 * }
 *
 * // As ArchElement:
 * ApplicationService service = new ApplicationService(
 *     ElementId.of("com.example.OrderApplicationService"),
 *     List.of(placeOrderUseCaseRef),
 *     List.of(orderRepositoryRef, paymentGatewayRef),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param implementedPorts references to driving ports implemented
 * @param drivenPortDependencies references to driven ports used
 * @param syntax the syntax information (nullable)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record ApplicationService(
        ElementId id,
        List<ElementRef<DrivingPort>> implementedPorts,
        List<ElementRef<DrivenPort>> drivenPortDependencies,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new ApplicationService instance.
     *
     * @param id the identifier, must not be null
     * @param implementedPorts the implemented driving ports, must not be null
     * @param drivenPortDependencies the driven port dependencies, must not be null
     * @param syntax the syntax (can be null)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if required fields are null
     */
    public ApplicationService {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(implementedPorts, "implementedPorts must not be null");
        Objects.requireNonNull(drivenPortDependencies, "drivenPortDependencies must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        implementedPorts = List.copyOf(implementedPorts);
        drivenPortDependencies = List.copyOf(drivenPortDependencies);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.APPLICATION_SERVICE;
    }

    /**
     * Returns the number of driven port dependencies.
     *
     * <p>A high number might indicate a "God Service" anti-pattern.</p>
     *
     * @return the dependency count
     */
    public int dependencyCount() {
        return drivenPortDependencies.size();
    }

    /**
     * Returns whether this service implements any driving ports.
     *
     * @return true if at least one driving port is implemented
     */
    public boolean implementsDrivingPorts() {
        return !implementedPorts.isEmpty();
    }

    /**
     * Creates a simple ApplicationService for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new ApplicationService
     */
    public static ApplicationService of(String qualifiedName, ClassificationTrace trace) {
        return new ApplicationService(ElementId.of(qualifiedName), List.of(), List.of(), null, trace);
    }
}
