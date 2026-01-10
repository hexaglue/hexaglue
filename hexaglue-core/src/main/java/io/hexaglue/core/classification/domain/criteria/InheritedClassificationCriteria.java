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
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classifies types based on their parent type's classification.
 *
 * <p>In DDD, a subclass of an AggregateRoot is also an AggregateRoot (polymorphism).
 * Similarly, a subclass of an Entity is also an Entity.
 *
 * <p>This criteria checks if the parent type has jMolecules annotations and
 * propagates the appropriate classification to the child.
 *
 * <p>Priority: 75 (between strong heuristics 80 and medium heuristics 70)
 * <p>Confidence: HIGH (inherited from explicit parent classification)
 */
public final class InheritedClassificationCriteria implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.inheritedClassification";
    }

    private static final Map<String, DomainKind> ANNOTATION_TO_KIND = Map.of(
            "org.jmolecules.ddd.annotation.AggregateRoot", DomainKind.AGGREGATE_ROOT,
            "org.jmolecules.ddd.annotation.Entity", DomainKind.ENTITY,
            "org.jmolecules.ddd.annotation.ValueObject", DomainKind.VALUE_OBJECT);

    private static final Map<String, DomainKind> INTERFACE_TO_KIND = Map.of(
            "org.jmolecules.ddd.types.AggregateRoot", DomainKind.AGGREGATE_ROOT,
            "org.jmolecules.ddd.types.Entity", DomainKind.ENTITY,
            "org.jmolecules.ddd.types.ValueObject", DomainKind.VALUE_OBJECT);

    // Track the matched kind for proper targetKind() return
    private DomainKind matchedKind = DomainKind.AGGREGATE_ROOT;

    @Override
    public String name() {
        return "inherited-classification";
    }

    @Override
    public int priority() {
        return 75; // Between strong (80) and medium (70) heuristics
    }

    @Override
    public DomainKind targetKind() {
        return matchedKind;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Don't apply to types that have their own explicit annotations
        if (hasOwnDomainAnnotation(node)) {
            return MatchResult.noMatch();
        }

        // Check supertype chain for classified parents
        MatchResult superResult = checkSupertypeClassification(node, query);
        if (superResult.matched()) {
            return superResult;
        }

        // Check implemented interfaces for classified parents
        return checkInterfaceClassification(node, query);
    }

    private MatchResult checkInterfaceClassification(TypeNode node, GraphQuery query) {
        for (TypeRef interfaceRef : node.interfaces()) {
            String interfaceName = interfaceRef.rawQualifiedName();

            // Skip jMolecules type interfaces (handled by ImplementsJMoleculesInterfaceCriteria)
            if (INTERFACE_TO_KIND.containsKey(interfaceName)) {
                continue;
            }

            // Try to get the interface type from the graph
            Optional<TypeNode> interfaceNode = query.type(interfaceName);
            if (interfaceNode.isEmpty()) {
                continue;
            }

            TypeNode iface = interfaceNode.get();

            // Check if interface has jMolecules annotation
            for (AnnotationRef annotation : iface.annotations()) {
                DomainKind kind = ANNOTATION_TO_KIND.get(annotation.qualifiedName());
                if (kind != null) {
                    matchedKind = kind;
                    return createInheritedMatch(kind, iface, "annotation @" + annotation.simpleName());
                }
            }
        }
        return MatchResult.noMatch();
    }

    private boolean hasOwnDomainAnnotation(TypeNode node) {
        for (AnnotationRef annotation : node.annotations()) {
            if (ANNOTATION_TO_KIND.containsKey(annotation.qualifiedName())) {
                return true;
            }
        }
        return false;
    }

    private MatchResult checkSupertypeClassification(TypeNode node, GraphQuery query) {
        Optional<TypeRef> superTypeRef = node.superType();
        if (superTypeRef.isEmpty()) {
            return MatchResult.noMatch();
        }

        String superTypeName = superTypeRef.get().rawQualifiedName();

        // Skip java.lang.Object and java.lang.Record
        if (superTypeName.startsWith("java.lang.")) {
            return MatchResult.noMatch();
        }

        // Try to get the parent type from the graph
        Optional<TypeNode> superTypeNode = query.type(superTypeName);
        if (superTypeNode.isEmpty()) {
            return MatchResult.noMatch();
        }

        TypeNode parent = superTypeNode.get();

        // Check if parent has jMolecules annotation
        for (AnnotationRef annotation : parent.annotations()) {
            DomainKind kind = ANNOTATION_TO_KIND.get(annotation.qualifiedName());
            if (kind != null) {
                matchedKind = kind;
                return createInheritedMatch(kind, parent, "annotation @" + annotation.simpleName());
            }
        }

        // Check if parent implements jMolecules interface
        for (TypeRef interfaceRef : parent.interfaces()) {
            DomainKind kind = INTERFACE_TO_KIND.get(interfaceRef.rawQualifiedName());
            if (kind != null) {
                matchedKind = kind;
                return createInheritedMatch(kind, parent, "interface " + interfaceRef.simpleName());
            }
        }

        // Check if parent would be classified as AGGREGATE_ROOT via repository heuristic
        MatchResult repositoryMatch = checkParentViaRepository(parent, query);
        if (repositoryMatch.matched()) {
            return repositoryMatch;
        }

        // Recurse to check grandparent
        return checkSupertypeClassification(parent, query);
    }

    /**
     * Checks if a parent type would be classified as AGGREGATE_ROOT via repository heuristic.
     * This mirrors the logic from RepositoryDominantCriteria.
     *
     * @param parent the parent type to check
     * @param query the graph query
     * @return match result if parent is repository-managed with identity, no match otherwise
     */
    private MatchResult checkParentViaRepository(TypeNode parent, GraphQuery query) {
        // Check if parent is used by a repository (USES_IN_SIGNATURE edge pointing to parent)
        Set<TypeNode> repositories = query.graph().edgesTo(parent.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                .filter(this::isRepository)
                .collect(Collectors.toSet());

        if (repositories.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Check if parent has an identity field
        Optional<FieldNode> identityField = findIdentityField(parent, query);
        if (identityField.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Parent is repository-managed and has identity -> AGGREGATE_ROOT
        matchedKind = DomainKind.AGGREGATE_ROOT;
        String repoNames = repositories.stream().map(TypeNode::simpleName).collect(Collectors.joining(", "));
        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Inherits AGGREGATE_ROOT from parent " + parent.simpleName() + " (repository-managed)",
                new Evidence(
                        EvidenceType.RELATIONSHIP,
                        "Parent type " + parent.qualifiedName() + " is used by repository [" + repoNames
                                + "] and has identity field",
                        List.of(parent.id())));
    }

    private boolean isRepository(TypeNode type) {
        String name = type.simpleName();
        // Match persistence port patterns
        if (name.endsWith("Repository") || name.endsWith("Repositories")) {
            return true;
        }
        if (name.endsWith("Fetcher") || name.endsWith("Loader") || name.endsWith("Reader")) {
            return true;
        }
        if (name.endsWith("Saver") || name.endsWith("Persister") || name.endsWith("Writer")) {
            return true;
        }
        if (name.endsWith("Store") || name.endsWith("Storage")) {
            return true;
        }
        if (name.endsWith("Gateway")) {
            return true;
        }
        if (name.endsWith("Dao") || name.endsWith("DAO")) {
            return true;
        }
        return type.annotations().stream()
                .anyMatch(a -> a.simpleName().equals("Repository")
                        || a.qualifiedName().equals("org.jmolecules.ddd.annotation.Repository"));
    }

    private Optional<FieldNode> findIdentityField(TypeNode node, GraphQuery query) {
        List<FieldNode> fields = query.fieldsOf(node);

        // Priority 1: Look for explicit @Identity annotation
        Optional<FieldNode> annotatedId = fields.stream()
                .filter(f -> f.annotations().stream()
                        .anyMatch(a -> a.qualifiedName().equals("org.jmolecules.ddd.annotation.Identity")
                                || a.simpleName().equals("Identity")
                                || a.simpleName().equals("Id")))
                .findFirst();
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

    private MatchResult createInheritedMatch(DomainKind kind, TypeNode parent, String reason) {
        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Inherits " + kind.name() + " classification from parent " + parent.simpleName(),
                new Evidence(
                        EvidenceType.RELATIONSHIP,
                        "Parent type " + parent.qualifiedName() + " has " + reason,
                        List.of(parent.id())));
    }
}
