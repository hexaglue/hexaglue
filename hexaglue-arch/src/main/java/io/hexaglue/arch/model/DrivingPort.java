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

package io.hexaglue.arch.model;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * Represents a driving (primary/inbound) port in hexagonal architecture.
 *
 * <p>A driving port is an interface that the outside world uses to interact
 * with the application. These ports are "driven" by external actors (users,
 * other systems, scheduled tasks) and define the use cases the application
 * provides.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Inbound - defines what the application can do</li>
 *   <li>Interface - typically an interface implemented by application services</li>
 *   <li>Use case oriented - methods represent business operations</li>
 * </ul>
 *
 * <h2>Use Cases (since 5.0.0)</h2>
 * <p>The {@link #useCases()} field contains all use cases exposed by this port,
 * derived from the interface methods. Each use case is categorized as COMMAND,
 * QUERY, or COMMAND_QUERY based on its signature.</p>
 *
 * <h2>Input and Output Types (since 5.0.0)</h2>
 * <p>The port tracks all types used as method parameters ({@link #inputTypes()})
 * and return types ({@link #outputTypes()}). This enables dependency analysis
 * and understanding the port's data contract.</p>
 *
 * <h2>Examples</h2>
 * <p>Common driving port patterns:</p>
 * <ul>
 *   <li>OrderService - creating and managing orders</li>
 *   <li>QueryOrderUseCase - querying order information</li>
 *   <li>PlaceOrderCommand - command handler for placing orders</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Basic driving port
 * DrivingPort port = DrivingPort.of(
 *     TypeId.of("com.example.OrderService"),
 *     structure,
 *     trace
 * );
 *
 * // Driving port with full details (since 5.0.0)
 * DrivingPort fullPort = DrivingPort.of(
 *     TypeId.of("com.example.OrderService"),
 *     structure,
 *     trace,
 *     useCases,
 *     inputTypes,
 *     outputTypes
 * );
 *
 * // Iterate use cases
 * port.useCases().stream()
 *     .filter(UseCase::isCommand)
 *     .forEach(uc -> System.out.println(uc.name()));
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param useCases the use cases exposed by this port (never null)
 * @param inputTypes the types used as method parameters (never null)
 * @param outputTypes the types returned by methods (never null)
 * @since 4.1.0
 * @since 5.0.0 added useCases, inputTypes, outputTypes
 */
public record DrivingPort(
        TypeId id,
        TypeStructure structure,
        ClassificationTrace classification,
        List<UseCase> useCases,
        List<TypeRef> inputTypes,
        List<TypeRef> outputTypes)
        implements PortType {

    /**
     * Creates a new DrivingPort.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param useCases the use cases, must not be null
     * @param inputTypes the input types, must not be null
     * @param outputTypes the output types, must not be null
     * @throws NullPointerException if any argument is null
     */
    public DrivingPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(useCases, "useCases must not be null");
        Objects.requireNonNull(inputTypes, "inputTypes must not be null");
        Objects.requireNonNull(outputTypes, "outputTypes must not be null");
        useCases = List.copyOf(useCases);
        inputTypes = List.copyOf(inputTypes);
        outputTypes = List.copyOf(outputTypes);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVING_PORT;
    }

    /**
     * Returns whether this port has any use cases.
     *
     * @return true if use cases are defined
     * @since 5.0.0
     */
    public boolean hasUseCases() {
        return !useCases.isEmpty();
    }

    /**
     * Returns whether this port has any input types.
     *
     * @return true if input types are defined
     * @since 5.0.0
     */
    public boolean hasInputTypes() {
        return !inputTypes.isEmpty();
    }

    /**
     * Returns whether this port has any output types.
     *
     * @return true if output types are defined
     * @since 5.0.0
     */
    public boolean hasOutputTypes() {
        return !outputTypes.isEmpty();
    }

    /**
     * Returns the command use cases (COMMAND or COMMAND_QUERY).
     *
     * @return list of command use cases
     * @since 5.0.0
     */
    public List<UseCase> commands() {
        return useCases.stream().filter(UseCase::isCommand).toList();
    }

    /**
     * Returns the query use cases (QUERY or COMMAND_QUERY).
     *
     * @return list of query use cases
     * @since 5.0.0
     */
    public List<UseCase> queries() {
        return useCases.stream().filter(UseCase::isQuery).toList();
    }

    /**
     * Creates a DrivingPort with the given parameters (no use cases or types).
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new DrivingPort with empty use cases and types
     * @throws NullPointerException if any argument is null
     */
    public static DrivingPort of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new DrivingPort(id, structure, classification, List.of(), List.of(), List.of());
    }

    /**
     * Creates a DrivingPort with all parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param useCases the use cases exposed by this port
     * @param inputTypes the types used as method parameters
     * @param outputTypes the types returned by methods
     * @return a new DrivingPort
     * @throws NullPointerException if any argument is null
     * @since 5.0.0
     */
    public static DrivingPort of(
            TypeId id,
            TypeStructure structure,
            ClassificationTrace classification,
            List<UseCase> useCases,
            List<TypeRef> inputTypes,
            List<TypeRef> outputTypes) {
        return new DrivingPort(id, structure, classification, useCases, inputTypes, outputTypes);
    }
}
