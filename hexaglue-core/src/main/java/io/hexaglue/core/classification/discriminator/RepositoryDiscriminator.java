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

package io.hexaglue.core.classification.discriminator;

import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.spi.classification.CertaintyLevel;
import io.hexaglue.spi.classification.ClassificationEvidence;
import io.hexaglue.spi.classification.ClassificationStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtTypeReference;

/**
 * Discriminator for detecting aggregate roots via Repository pattern.
 *
 * <p>This discriminator identifies aggregate roots by analyzing repository interfaces.
 * A repository interface manages the lifecycle of an aggregate root through standard
 * persistence operations.
 *
 * <p>Detection rules for a repository interface:
 * <ol>
 *   <li>Name ends with "Repository"</li>
 *   <li>Has a save(T) or persist(T) method</li>
 *   <li>Has a findById(...) method returning Optional&lt;T&gt; or T</li>
 *   <li>The managed type T is consistent across methods</li>
 * </ol>
 *
 * <p>When a repository is detected, the managed type T is classified as AGGREGATE_ROOT with:
 * <ul>
 *   <li>Certainty: {@link CertaintyLevel#CERTAIN_BY_STRUCTURE}</li>
 *   <li>Strategy: {@link ClassificationStrategy#REPOSITORY}</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class RepositoryDiscriminator {

    /**
     * Method names indicating save operations.
     */
    private static final Set<String> SAVE_METHOD_NAMES =
            Set.of("save", "persist", "store", "insert", "update", "upsert");

    /**
     * Method names indicating find-by-id operations.
     */
    private static final Set<String> FIND_BY_ID_NAMES = Set.of("findById", "getById", "findByIdentifier", "loadById");

    /**
     * Attempts to detect a repository interface and extract the managed aggregate root type.
     *
     * @param iface the interface to analyze
     * @param graph the application graph (for additional context)
     * @return optional containing classification if this is a repository
     */
    public Optional<AggregateRootClassification> detect(CtInterface<?> iface, ApplicationGraph graph) {
        Objects.requireNonNull(iface, "iface required");
        Objects.requireNonNull(graph, "graph required");

        List<ClassificationEvidence> evidences = new ArrayList<>();

        // Check naming convention
        String interfaceName = iface.getSimpleName();
        if (!interfaceName.endsWith("Repository")) {
            return Optional.empty();
        }

        evidences.add(ClassificationEvidence.positive(
                "REPOSITORY_NAMING",
                40,
                String.format("Interface '%s' follows Repository naming convention", interfaceName)));

        // Find save method and extract managed type
        Optional<String> saveType = findSaveMethodType(iface);
        if (saveType.isEmpty()) {
            evidences.add(ClassificationEvidence.negative("NO_SAVE_METHOD", -50, "No save/persist method found"));
            return Optional.empty();
        }

        evidences.add(ClassificationEvidence.positive(
                "HAS_SAVE_METHOD", 50, String.format("Has save method for type %s", simplifyTypeName(saveType.get()))));

        String managedType = saveType.get();

        // Check for findById method
        Optional<String> findByIdType = findFindByIdMethodType(iface);
        if (findByIdType.isPresent()) {
            evidences.add(ClassificationEvidence.positive(
                    "HAS_FIND_BY_ID",
                    50,
                    String.format("Has findById method returning %s", simplifyTypeName(findByIdType.get()))));

            // Verify consistency
            if (!findByIdType.get().equals(managedType)) {
                evidences.add(ClassificationEvidence.negative(
                        "INCONSISTENT_TYPES",
                        -30,
                        String.format(
                                "save() and findById() manage different types: %s vs %s",
                                simplifyTypeName(managedType), simplifyTypeName(findByIdType.get()))));
                // Still proceed - use the save type as authoritative
            }
        }

        String reasoning = String.format(
                "Type '%s' is managed by repository '%s' - classified as AGGREGATE_ROOT",
                simplifyTypeName(managedType), interfaceName);

        return Optional.of(new AggregateRootClassification(
                managedType,
                CertaintyLevel.CERTAIN_BY_STRUCTURE,
                ClassificationStrategy.REPOSITORY,
                reasoning,
                evidences));
    }

    /**
     * Finds the save method and returns the type it manages.
     *
     * @param iface the repository interface
     * @return optional containing the managed type qualified name
     */
    private Optional<String> findSaveMethodType(CtInterface<?> iface) {
        for (CtMethod<?> method : iface.getMethods()) {
            if (SAVE_METHOD_NAMES.contains(method.getSimpleName())) {
                // Save methods should have one parameter of the managed type
                List<CtParameter<?>> parameters = method.getParameters();
                if (parameters.size() == 1) {
                    CtTypeReference<?> paramType = parameters.get(0).getType();
                    return Optional.of(paramType.getQualifiedName());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the findById method and returns the type it returns.
     *
     * @param iface the repository interface
     * @return optional containing the managed type qualified name
     */
    private Optional<String> findFindByIdMethodType(CtInterface<?> iface) {
        for (CtMethod<?> method : iface.getMethods()) {
            if (FIND_BY_ID_NAMES.contains(method.getSimpleName())) {
                CtTypeReference<?> returnType = method.getType();

                // Handle Optional<T> return type
                if (isOptional(returnType)) {
                    List<CtTypeReference<?>> typeArgs = returnType.getActualTypeArguments();
                    if (!typeArgs.isEmpty()) {
                        return Optional.of(typeArgs.get(0).getQualifiedName());
                    }
                } else {
                    // Direct type return
                    return Optional.of(returnType.getQualifiedName());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a type reference is Optional.
     *
     * @param typeRef the type reference
     * @return true if the type is java.util.Optional
     */
    private boolean isOptional(CtTypeReference<?> typeRef) {
        String qualifiedName = typeRef.getQualifiedName();
        return qualifiedName.equals("java.util.Optional");
    }

    /**
     * Simplifies a qualified type name for display.
     *
     * @param qualifiedName the fully qualified name
     * @return the simple name
     */
    private String simplifyTypeName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Classification result for an aggregate root detected via repository.
     *
     * @param typeName   the fully qualified type name of the aggregate root
     * @param certainty  the certainty level of the classification
     * @param strategy   the classification strategy used
     * @param reasoning  human-readable explanation
     * @param evidences  list of supporting evidence
     */
    public record AggregateRootClassification(
            String typeName,
            CertaintyLevel certainty,
            ClassificationStrategy strategy,
            String reasoning,
            List<ClassificationEvidence> evidences) {

        public AggregateRootClassification {
            Objects.requireNonNull(typeName, "typeName required");
            Objects.requireNonNull(certainty, "certainty required");
            Objects.requireNonNull(strategy, "strategy required");
            Objects.requireNonNull(reasoning, "reasoning required");
            evidences = evidences != null ? List.copyOf(evidences) : List.of();
        }
    }
}
