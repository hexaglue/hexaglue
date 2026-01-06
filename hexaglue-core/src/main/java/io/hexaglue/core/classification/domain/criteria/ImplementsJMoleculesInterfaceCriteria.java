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

package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Map;

/**
 * Matches types implementing jMolecules DDD type interfaces.
 *
 * <p>jMolecules provides both annotations and interfaces for DDD concepts.
 * This criteria handles the interface-based approach:
 * <ul>
 *   <li>{@code org.jmolecules.ddd.types.AggregateRoot} → AGGREGATE_ROOT</li>
 *   <li>{@code org.jmolecules.ddd.types.Entity} → ENTITY</li>
 *   <li>{@code org.jmolecules.ddd.types.ValueObject} → VALUE_OBJECT</li>
 *   <li>{@code org.jmolecules.ddd.types.Identifier} → IDENTIFIER</li>
 * </ul>
 *
 * <p>Priority: 100 (same as explicit annotations - implementing an interface is equally explicit)
 * <p>Confidence: EXPLICIT
 */
public final class ImplementsJMoleculesInterfaceCriteria
        implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {

    private static final String JMOLECULES_TYPES_PACKAGE = "org.jmolecules.ddd.types.";

    private static final Map<String, DomainKind> INTERFACE_TO_KIND = Map.of(
            JMOLECULES_TYPES_PACKAGE + "AggregateRoot", DomainKind.AGGREGATE_ROOT,
            JMOLECULES_TYPES_PACKAGE + "Entity", DomainKind.ENTITY,
            JMOLECULES_TYPES_PACKAGE + "ValueObject", DomainKind.VALUE_OBJECT,
            JMOLECULES_TYPES_PACKAGE + "Identifier", DomainKind.IDENTIFIER);

    // The kind this specific instance targets (set after matching)
    private final DomainKind fixedKind;

    /**
     * Creates an instance that matches any jMolecules interface.
     */
    public ImplementsJMoleculesInterfaceCriteria() {
        this(null);
    }

    /**
     * Creates an instance that targets a specific kind (used internally for consistent results).
     */
    private ImplementsJMoleculesInterfaceCriteria(DomainKind fixedKind) {
        this.fixedKind = fixedKind;
    }

    @Override
    public String id() {
        return "domain.explicit.jmoleculesInterface";
    }

    @Override
    public String name() {
        if (fixedKind != null) {
            return "implements-jmolecules-" + fixedKind.name().toLowerCase().replace('_', '-');
        }
        return "implements-jmolecules-interface";
    }

    @Override
    public int priority() {
        return 100; // Same as explicit annotations
    }

    @Override
    public DomainKind targetKind() {
        // Return the fixed kind if set, otherwise default to AGGREGATE_ROOT
        // (this is only used for tie-breaking when multiple criteria match)
        return fixedKind != null ? fixedKind : DomainKind.AGGREGATE_ROOT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Check direct interfaces
        for (TypeRef interfaceRef : node.interfaces()) {
            DomainKind kind = INTERFACE_TO_KIND.get(interfaceRef.rawQualifiedName());
            if (kind != null) {
                return createMatch(kind, interfaceRef.simpleName(), node);
            }
        }

        // Check interfaces of supertype (transitively)
        return checkSupertypeInterfaces(node, query);
    }

    private MatchResult checkSupertypeInterfaces(TypeNode node, GraphQuery query) {
        return node.superType()
                .flatMap(superRef -> query.type(superRef.rawQualifiedName()))
                .map(superNode -> {
                    // Check if supertype implements a jMolecules interface
                    for (TypeRef interfaceRef : superNode.interfaces()) {
                        DomainKind kind = INTERFACE_TO_KIND.get(interfaceRef.rawQualifiedName());
                        if (kind != null) {
                            return createMatch(kind, interfaceRef.simpleName(), node);
                        }
                    }
                    // Recurse to check grandparent
                    return checkSupertypeInterfaces(superNode, query);
                })
                .orElse(MatchResult.noMatch());
    }

    private MatchResult createMatch(DomainKind kind, String interfaceName, TypeNode node) {
        return MatchResult.match(
                ConfidenceLevel.EXPLICIT,
                "Implements jMolecules " + interfaceName + " interface",
                new Evidence(
                        EvidenceType.STRUCTURE,
                        "Type implements " + JMOLECULES_TYPES_PACKAGE + interfaceName,
                        List.of(node.id())));
    }
}
