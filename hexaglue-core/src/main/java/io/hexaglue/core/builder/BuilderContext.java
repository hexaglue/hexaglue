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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.ArchType;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared context for ArchType builders.
 *
 * <p>Provides access to the graph query interface, classification results,
 * and already built ArchType instances. This context is passed to all builders
 * during the transformation pipeline.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * BuilderContext context = BuilderContext.of(graphQuery, classificationResults);
 *
 * // Lookup classifications
 * context.getClassification("com.example.Order")
 *     .ifPresent(c -> System.out.println(c.kind()));
 *
 * // Lookup already built types
 * context.getBuiltType("com.example.Money")
 *     .ifPresent(t -> System.out.println(t.kind()));
 *
 * // Create new context with additional built type
 * BuilderContext updated = context.withBuiltType("com.example.Order", orderType);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class BuilderContext {

    private final GraphQuery graphQuery;
    private final ClassificationResults classificationResults;
    private final Map<String, ArchType> builtTypes;

    private BuilderContext(
            GraphQuery graphQuery, ClassificationResults classificationResults, Map<String, ArchType> builtTypes) {
        this.graphQuery = Objects.requireNonNull(graphQuery, "graphQuery must not be null");
        this.classificationResults =
                Objects.requireNonNull(classificationResults, "classificationResults must not be null");
        this.builtTypes = builtTypes != null ? Map.copyOf(builtTypes) : Map.of();
    }

    /**
     * Creates a new BuilderContext with the given graph query and classification results.
     *
     * @param graphQuery the graph query interface
     * @param classificationResults the classification results
     * @return a new BuilderContext
     * @throws NullPointerException if graphQuery or classificationResults is null
     */
    public static BuilderContext of(GraphQuery graphQuery, ClassificationResults classificationResults) {
        return new BuilderContext(graphQuery, classificationResults, null);
    }

    /**
     * Creates a new BuilderContext with the given parameters.
     *
     * @param graphQuery the graph query interface
     * @param classificationResults the classification results
     * @param builtTypes the already built ArchType instances (may be null)
     * @return a new BuilderContext
     * @throws NullPointerException if graphQuery or classificationResults is null
     */
    public static BuilderContext of(
            GraphQuery graphQuery, ClassificationResults classificationResults, Map<String, ArchType> builtTypes) {
        return new BuilderContext(graphQuery, classificationResults, builtTypes);
    }

    /**
     * Returns the graph query interface.
     *
     * @return the graph query
     */
    public GraphQuery graphQuery() {
        return graphQuery;
    }

    /**
     * Returns the classification results.
     *
     * @return the classification results
     */
    public ClassificationResults classificationResults() {
        return classificationResults;
    }

    /**
     * Returns an unmodifiable view of the built types.
     *
     * @return the built types map
     */
    public Map<String, ArchType> builtTypes() {
        return builtTypes;
    }

    /**
     * Returns the classification for the given qualified name.
     *
     * @param qualifiedName the fully qualified type name
     * @return the classification, or empty if not found
     */
    public Optional<ClassificationResult> getClassification(String qualifiedName) {
        NodeId nodeId = NodeId.type(qualifiedName);
        return classificationResults.get(nodeId);
    }

    /**
     * Returns the built ArchType for the given qualified name.
     *
     * @param qualifiedName the fully qualified type name
     * @return the built type, or empty if not found
     */
    public Optional<ArchType> getBuiltType(String qualifiedName) {
        return Optional.ofNullable(builtTypes.get(qualifiedName));
    }

    /**
     * Checks if a type is classified as a specific kind.
     *
     * @param qualifiedName the fully qualified type name
     * @param kind the expected classification kind (e.g., "AGGREGATE_ROOT")
     * @return true if the type is classified as the given kind
     */
    public boolean isClassifiedAs(String qualifiedName, String kind) {
        return getClassification(qualifiedName).map(c -> kind.equals(c.kind())).orElse(false);
    }

    /**
     * Creates a new BuilderContext with an additional built type.
     *
     * <p>This method does not modify the current context; it returns a new instance.</p>
     *
     * @param qualifiedName the fully qualified type name
     * @param type the built ArchType
     * @return a new BuilderContext with the additional type
     */
    public BuilderContext withBuiltType(String qualifiedName, ArchType type) {
        Map<String, ArchType> newBuiltTypes = new HashMap<>(builtTypes);
        newBuiltTypes.put(qualifiedName, type);
        return new BuilderContext(graphQuery, classificationResults, newBuiltTypes);
    }

    /**
     * Creates a new BuilderContext with additional built types.
     *
     * <p>This method does not modify the current context; it returns a new instance.</p>
     *
     * @param types the built ArchTypes to add
     * @return a new BuilderContext with the additional types
     */
    public BuilderContext withBuiltTypes(Map<String, ArchType> types) {
        Map<String, ArchType> newBuiltTypes = new HashMap<>(builtTypes);
        newBuiltTypes.putAll(types);
        return new BuilderContext(graphQuery, classificationResults, newBuiltTypes);
    }
}
