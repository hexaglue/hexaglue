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
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * A Driven Adapter (Secondary/Outbound) in Hexagonal Architecture.
 *
 * <p>Driven adapters implement driven port interfaces, providing the actual
 * infrastructure integration (database, external APIs, messaging, etc.).</p>
 *
 * <h2>Types</h2>
 * <ul>
 *   <li>{@link AdapterType#JPA_REPOSITORY} - JPA/Hibernate persistence</li>
 *   <li>{@link AdapterType#JDBC_REPOSITORY} - Direct JDBC</li>
 *   <li>{@link AdapterType#HTTP_CLIENT} - HTTP client for APIs</li>
 *   <li>{@link AdapterType#MESSAGE_PRODUCER} - Message producer</li>
 *   <li>{@link AdapterType#FILE_STORAGE} - File system</li>
 *   <li>{@link AdapterType#CACHE} - Caching</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In infrastructure:
 * @Repository
 * public class JpaOrderRepository implements OrderRepository {
 *     private final EntityManager em;
 *
 *     @Override
 *     public Optional<Order> findById(OrderId id) { ... }
 * }
 *
 * // As ArchElement:
 * DrivenAdapter adapter = new DrivenAdapter(
 *     ElementId.of("com.example.JpaOrderRepository"),
 *     AdapterType.JPA_REPOSITORY,
 *     List.of(orderRepositoryRef),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param adapterType the specific adapter type
 * @param implementedPorts references to driven ports implemented
 * @param syntax the syntax information (nullable)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DrivenAdapter(
        ElementId id,
        AdapterType adapterType,
        List<ElementRef<DrivenPort>> implementedPorts,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DrivenAdapter instance.
     *
     * @param id the identifier, must not be null
     * @param adapterType the adapter type, must not be null
     * @param implementedPorts the implemented driven ports, must not be null
     * @param syntax the syntax (can be null)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if required fields are null
     */
    public DrivenAdapter {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(adapterType, "adapterType must not be null");
        Objects.requireNonNull(implementedPorts, "implementedPorts must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        implementedPorts = List.copyOf(implementedPorts);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.DRIVEN_ADAPTER;
    }

    /**
     * Returns whether this is a JPA repository.
     *
     * @return true if JPA_REPOSITORY type
     */
    public boolean isJpaRepository() {
        return adapterType == AdapterType.JPA_REPOSITORY;
    }

    /**
     * Returns whether this is a JDBC repository.
     *
     * @return true if JDBC_REPOSITORY type
     */
    public boolean isJdbcRepository() {
        return adapterType == AdapterType.JDBC_REPOSITORY;
    }

    /**
     * Returns whether this is an HTTP client.
     *
     * @return true if HTTP_CLIENT type
     */
    public boolean isHttpClient() {
        return adapterType == AdapterType.HTTP_CLIENT;
    }

    /**
     * Returns whether this is a message producer.
     *
     * @return true if MESSAGE_PRODUCER type
     */
    public boolean isMessageProducer() {
        return adapterType == AdapterType.MESSAGE_PRODUCER;
    }

    /**
     * Returns whether this adapter implements any driven ports.
     *
     * @return true if at least one driven port is implemented
     */
    public boolean implementsDrivenPorts() {
        return !implementedPorts.isEmpty();
    }

    /**
     * Creates a simple DrivenAdapter for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DrivenAdapter
     */
    public static DrivenAdapter of(String qualifiedName, ClassificationTrace trace) {
        return new DrivenAdapter(ElementId.of(qualifiedName), AdapterType.JPA_REPOSITORY, List.of(), null, trace);
    }

    /**
     * Creates a JPA repository adapter for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param portRef reference to the implemented port
     * @param trace the classification trace
     * @return a new DrivenAdapter
     */
    public static DrivenAdapter jpaRepository(
            String qualifiedName, ElementRef<DrivenPort> portRef, ClassificationTrace trace) {
        return new DrivenAdapter(
                ElementId.of(qualifiedName), AdapterType.JPA_REPOSITORY, List.of(portRef), null, trace);
    }
}
