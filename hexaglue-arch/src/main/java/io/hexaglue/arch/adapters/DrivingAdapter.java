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

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * A Driving Adapter (Primary/Inbound) in Hexagonal Architecture.
 *
 * <p>Driving adapters translate external requests into calls to the application.
 * They call driving ports (use cases) to execute application logic.</p>
 *
 * <h2>Types</h2>
 * <ul>
 *   <li>{@link AdapterType#REST_CONTROLLER} - HTTP REST endpoints</li>
 *   <li>{@link AdapterType#GRAPHQL_CONTROLLER} - GraphQL endpoints</li>
 *   <li>{@link AdapterType#MESSAGE_LISTENER} - Message consumers</li>
 *   <li>{@link AdapterType#CLI} - Command-line interface</li>
 *   <li>{@link AdapterType#SCHEDULED_TASK} - Scheduled jobs</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In infrastructure:
 * @RestController
 * public class OrderController {
 *     private final PlaceOrderUseCase placeOrderUseCase;
 *
 *     @PostMapping("/orders")
 *     public ResponseEntity<OrderDto> create(OrderRequest req) { ... }
 * }
 *
 * // As ArchElement:
 * DrivingAdapter adapter = new DrivingAdapter(
 *     ElementId.of("com.example.OrderController"),
 *     AdapterType.REST_CONTROLLER,
 *     List.of(placeOrderUseCaseRef),
 *     List.of("/orders"),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param adapterType the specific adapter type
 * @param calledPorts references to driving ports called by this adapter
 * @param endpoints the endpoints exposed (for REST/GraphQL adapters)
 * @param syntax the syntax information (nullable)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DrivingAdapter(
        ElementId id,
        AdapterType adapterType,
        List<ElementRef<DrivingPort>> calledPorts,
        List<String> endpoints,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DrivingAdapter instance.
     *
     * @param id the identifier, must not be null
     * @param adapterType the adapter type, must not be null
     * @param calledPorts the called driving ports, must not be null
     * @param endpoints the exposed endpoints, must not be null
     * @param syntax the syntax (can be null)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if required fields are null
     */
    public DrivingAdapter {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(adapterType, "adapterType must not be null");
        Objects.requireNonNull(calledPorts, "calledPorts must not be null");
        Objects.requireNonNull(endpoints, "endpoints must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        calledPorts = List.copyOf(calledPorts);
        endpoints = List.copyOf(endpoints);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.DRIVING_ADAPTER;
    }

    /**
     * Returns whether this is a REST controller.
     *
     * @return true if REST_CONTROLLER type
     */
    public boolean isRestController() {
        return adapterType == AdapterType.REST_CONTROLLER;
    }

    /**
     * Returns whether this is a GraphQL controller.
     *
     * @return true if GRAPHQL_CONTROLLER type
     */
    public boolean isGraphQLController() {
        return adapterType == AdapterType.GRAPHQL_CONTROLLER;
    }

    /**
     * Returns whether this is a message listener.
     *
     * @return true if MESSAGE_LISTENER type
     */
    public boolean isMessageListener() {
        return adapterType == AdapterType.MESSAGE_LISTENER;
    }

    /**
     * Returns whether this adapter exposes endpoints.
     *
     * @return true if endpoints are exposed
     */
    public boolean hasEndpoints() {
        return !endpoints.isEmpty();
    }

    /**
     * Creates a simple DrivingAdapter for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DrivingAdapter
     */
    public static DrivingAdapter of(String qualifiedName, ClassificationTrace trace) {
        return new DrivingAdapter(
                ElementId.of(qualifiedName), AdapterType.REST_CONTROLLER, List.of(), List.of(), null, trace);
    }

    /**
     * Creates a REST controller adapter for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param endpoints the endpoints
     * @param trace the classification trace
     * @return a new DrivingAdapter
     */
    public static DrivingAdapter restController(
            String qualifiedName, List<String> endpoints, ClassificationTrace trace) {
        return new DrivingAdapter(
                ElementId.of(qualifiedName), AdapterType.REST_CONTROLLER, List.of(), endpoints, null, trace);
    }
}
