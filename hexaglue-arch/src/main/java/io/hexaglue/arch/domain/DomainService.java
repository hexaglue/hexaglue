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

package io.hexaglue.arch.domain;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * A Domain Service in Domain-Driven Design.
 *
 * <p>Domain services encapsulate domain logic that doesn't naturally fit
 * within an entity or value object. They are stateless and operate on
 * domain objects.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Stateless - no internal state</li>
 *   <li>Operations involve multiple domain objects</li>
 *   <li>Named after domain concepts (PricingService, not PriceCalculator)</li>
 *   <li>Pure domain logic - no infrastructure concerns</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In domain model:
 * public class ShippingCostCalculator {
 *     public Money calculate(Order order, ShippingMethod method) { ... }
 * }
 *
 * // As ArchElement:
 * DomainService service = new DomainService(
 *     ElementId.of("com.example.ShippingCostCalculator"),
 *     List.of("calculate"),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param operations the names of service operations (methods)
 * @param syntax the syntax information from source analysis (nullable for synthetic types)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DomainService(
        ElementId id, List<String> operations, TypeSyntax syntax, ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DomainService instance.
     *
     * @param id the identifier, must not be null
     * @param operations the operation names, must not be null
     * @param syntax the syntax (can be null for synthetic types)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if id, operations, or classificationTrace is null
     */
    public DomainService {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(operations, "operations must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        operations = List.copyOf(operations);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.DOMAIN_SERVICE;
    }

    /**
     * Creates a simple DomainService for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DomainService
     */
    public static DomainService of(String qualifiedName, ClassificationTrace trace) {
        return new DomainService(ElementId.of(qualifiedName), List.of(), null, trace);
    }
}
