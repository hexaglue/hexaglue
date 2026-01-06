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
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matches types that are used as the dominant type in persistence port signatures
 * and have an identity field.
 *
 * <p>This is a strong heuristic for AGGREGATE_ROOT:
 * <ul>
 *   <li>Type is used in a persistence port interface signature (return or parameter)</li>
 *   <li>Type has a field named "id" or ending with "Id"</li>
 * </ul>
 *
 * <p>Persistence ports include:
 * <ul>
 *   <li>*Repository, *Repositories - standard DDD naming</li>
 *   <li>*Fetcher, *Loader - read operations</li>
 *   <li>*Saver, *Persister, *Writer - write operations</li>
 *   <li>*Store, *Storage - storage operations</li>
 *   <li>*Gateway - data gateway pattern</li>
 * </ul>
 *
 * <p>Priority: 80 (strong heuristic)
 * <p>Confidence: HIGH
 */
public final class RepositoryDominantCriteria implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.repositoryDominant";
    }

    @Override
    public String name() {
        return "repository-dominant";
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.AGGREGATE_ROOT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Step 1: Find repositories that use this type in their signature
        Set<TypeNode> repositories = findRepositoriesUsing(node, query);
        if (repositories.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Step 2: Check if this type has an identity field
        Optional<FieldNode> identityField = findIdentityField(node, query);
        if (identityField.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Match!
        String repoNames = repositories.stream().map(TypeNode::simpleName).collect(Collectors.joining(", "));

        List<Evidence> evidences = new ArrayList<>();
        evidences.add(Evidence.fromRelationship(
                "Used in repository signature: " + repoNames,
                repositories.stream().map(TypeNode::id).toList()));
        evidences.add(Evidence.fromStructure(
                "Has identity field '" + identityField.get().simpleName() + "'",
                List.of(identityField.get().id())));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Dominant type in repository [%s], has identity field '%s'"
                        .formatted(repoNames, identityField.get().simpleName()),
                evidences);
    }

    private Set<TypeNode> findRepositoriesUsing(TypeNode node, GraphQuery query) {
        // Find interfaces that use this type in their signature (via USES_IN_SIGNATURE edge)
        return query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                .filter(iface -> isRepository(iface, node))
                .collect(Collectors.toSet());
    }

    private boolean isRepository(TypeNode type, TypeNode dominantType) {
        String name = type.simpleName();
        // Match persistence port patterns
        if (isPersistencePort(name)) {
            return true;
        }
        if (hasRepositoryAnnotation(type)) {
            return true;
        }
        // Check if interface is the plural of the dominant type (e.g., Orders for Order)
        return isPluralOf(name, dominantType.simpleName());
    }

    private boolean isPersistencePort(String name) {
        // Standard DDD naming
        if (name.endsWith("Repository") || name.endsWith("Repositories")) {
            return true;
        }
        // Read operations
        if (name.endsWith("Fetcher") || name.endsWith("Loader") || name.endsWith("Reader")) {
            return true;
        }
        // Write operations
        if (name.endsWith("Saver") || name.endsWith("Persister") || name.endsWith("Writer")) {
            return true;
        }
        // Storage patterns
        if (name.endsWith("Store") || name.endsWith("Storage")) {
            return true;
        }
        // Gateway pattern
        if (name.endsWith("Gateway")) {
            return true;
        }
        // DAO pattern (legacy but still used)
        if (name.endsWith("Dao") || name.endsWith("DAO")) {
            return true;
        }
        return false;
    }

    private boolean isPluralOf(String potentialPlural, String singular) {
        // Simple pluralization rules
        // Orders -> Order, Customers -> Customer, etc.
        if (potentialPlural.equals(singular + "s")) {
            return true;
        }
        // Entries -> Entry (y -> ies)
        if (potentialPlural.endsWith("ies") && singular.endsWith("y")) {
            String singularBase = singular.substring(0, singular.length() - 1);
            return potentialPlural.equals(singularBase + "ies");
        }
        return false;
    }

    private boolean hasRepositoryAnnotation(TypeNode type) {
        return type.annotations().stream()
                .anyMatch(a -> a.simpleName().equals("Repository")
                        || a.qualifiedName().equals("org.jmolecules.ddd.annotation.Repository"));
    }

    private Optional<FieldNode> findIdentityField(TypeNode node, GraphQuery query) {
        List<FieldNode> fields = query.fieldsOf(node);

        // Priority 1: Look for explicit @Identity annotation
        Optional<FieldNode> annotatedId =
                fields.stream().filter(this::hasIdentityAnnotation).findFirst();
        if (annotatedId.isPresent()) {
            return annotatedId;
        }

        // Priority 2: Look for field named exactly "id"
        Optional<FieldNode> exactIdField =
                fields.stream().filter(f -> f.simpleName().equals("id")).findFirst();
        if (exactIdField.isPresent()) {
            return exactIdField;
        }

        // Priority 3: Look for field ending with "Id"
        return fields.stream().filter(f -> f.simpleName().endsWith("Id")).findFirst();
    }

    private boolean hasIdentityAnnotation(FieldNode field) {
        return field.annotations().stream()
                .anyMatch(a -> a.qualifiedName().equals("org.jmolecules.ddd.annotation.Identity")
                        || a.simpleName().equals("Identity")
                        || a.simpleName().equals("Id"));
    }
}
