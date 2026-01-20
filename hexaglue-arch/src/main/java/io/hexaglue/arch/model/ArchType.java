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

/**
 * Root sealed interface for all architectural types in the new type hierarchy.
 *
 * <p>This interface represents any type that has been classified by the HexaGlue
 * classification engine. Every ArchType has:</p>
 * <ul>
 *   <li>A stable {@link TypeId} that doesn't change when classification evolves</li>
 *   <li>An {@link ArchKind} indicating its architectural classification</li>
 *   <li>A {@link TypeStructure} describing its structural elements</li>
 *   <li>A {@link ClassificationTrace} explaining why it was classified this way</li>
 * </ul>
 *
 * <h2>Type Hierarchy</h2>
 * <pre>
 * ArchType (sealed interface)
 * ├── DomainType (sealed interface)
 * │   ├── AggregateRoot (record)
 * │   ├── Entity (record)
 * │   ├── ValueObject (record)
 * │   ├── Identifier (record)
 * │   ├── DomainEvent (record)
 * │   └── DomainService (record)
 * ├── PortType (sealed interface)
 * │   ├── DrivingPort (record)
 * │   └── DrivenPort (record)
 * ├── ApplicationType (sealed interface)
 * │   ├── ApplicationService (record)
 * │   ├── CommandHandler (record)
 * │   └── QueryHandler (record)
 * └── UnclassifiedType (record)
 * </pre>
 *
 * <h2>Pattern Matching</h2>
 * <p>Because this is a sealed interface, you can use exhaustive pattern matching:</p>
 * <pre>{@code
 * switch (archType) {
 *     case DomainType domain -> handleDomain(domain);
 *     case PortType port -> handlePort(port);
 *     case ApplicationType app -> handleApplication(app);
 *     case UnclassifiedType unclassified -> handleUnclassified(unclassified);
 * }
 * }</pre>
 *
 * @since 4.1.0
 */
public sealed interface ArchType permits DomainType, PortType, ApplicationType, UnclassifiedType {

    /**
     * Returns the stable identifier for this type.
     *
     * <p>The id does not change when classification evolves. If heuristics change
     * and a type is reclassified, its id remains stable.</p>
     *
     * @return the type id
     */
    TypeId id();

    /**
     * Returns the architectural kind of this type.
     *
     * @return the arch kind
     */
    ArchKind kind();

    /**
     * Returns the structural description of this type.
     *
     * <p>The structure contains all syntactic information about the type:
     * fields, methods, constructors, annotations, etc.</p>
     *
     * @return the type structure
     */
    TypeStructure structure();

    /**
     * Returns the classification trace explaining why this type was classified this way.
     *
     * <p>The trace provides full transparency into the classification decision,
     * including the winning criterion, all evaluated criteria, and any conflicts.</p>
     *
     * @return the classification trace
     */
    ClassificationTrace classification();

    /**
     * Returns the fully qualified name of this type.
     *
     * <p>Convenience method equivalent to {@code id().qualifiedName()}.</p>
     *
     * @return the qualified name
     */
    default String qualifiedName() {
        return id().qualifiedName();
    }

    /**
     * Returns the simple name of this type.
     *
     * <p>Convenience method equivalent to {@code id().simpleName()}.</p>
     *
     * @return the simple name
     */
    default String simpleName() {
        return id().simpleName();
    }

    /**
     * Returns the package name of this type.
     *
     * <p>Convenience method equivalent to {@code id().packageName()}.</p>
     *
     * @return the package name
     */
    default String packageName() {
        return id().packageName();
    }
}
