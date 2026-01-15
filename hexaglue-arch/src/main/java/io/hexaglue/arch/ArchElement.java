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

package io.hexaglue.arch;

/**
 * Base interface for all architectural elements.
 *
 * <p>This sealed interface defines the contract for domain objects, ports, and adapters
 * in a Hexagonal Architecture application.</p>
 *
 * <h2>Element Categories</h2>
 * <ul>
 *   <li><strong>Domain</strong>: Aggregate, DomainEntity, ValueObject, Identifier, DomainEvent, DomainService</li>
 *   <li><strong>Ports</strong>: DrivingPort, DrivenPort, ApplicationService</li>
 *   <li><strong>Adapters</strong>: DrivingAdapter, DrivenAdapter</li>
 *   <li><strong>Fallback</strong>: UnclassifiedType</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ArchElement element = registry.get(id).orElseThrow();
 * if (element instanceof Aggregate agg) {
 *     // Process aggregate
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
public sealed interface ArchElement permits ArchElement.Marker {

    /**
     * Returns the unique identifier of this element.
     *
     * @return the element identifier
     */
    ElementId id();

    /**
     * Returns the fully qualified name of this element.
     *
     * <p>Shortcut for {@code id().qualifiedName()}.</p>
     *
     * @return the qualified name
     */
    default String qualifiedName() {
        return id().qualifiedName();
    }

    /**
     * Returns the simple name of this element (without package).
     *
     * <p>Shortcut for {@code id().simpleName()}.</p>
     *
     * @return the simple name
     */
    default String simpleName() {
        return id().simpleName();
    }

    /**
     * Returns the package name of this element.
     *
     * <p>Shortcut for {@code id().packageName()}.</p>
     *
     * @return the package name
     */
    default String packageName() {
        return id().packageName();
    }

    /**
     * Returns the classification kind of this element.
     *
     * @return the element kind
     */
    ElementKind kind();

    /**
     * Returns the classification trace explaining why this element was classified.
     *
     * @return the classification trace
     */
    ClassificationTrace classificationTrace();

    /**
     * Marker interface to allow incremental addition of permitted subtypes.
     *
     * <p>This is a temporary workaround until all concrete element types are implemented.
     * Concrete implementations (Aggregate, DomainEntity, etc.) will extend this marker
     * as a non-sealed interface.</p>
     */
    non-sealed interface Marker extends ArchElement {}
}
