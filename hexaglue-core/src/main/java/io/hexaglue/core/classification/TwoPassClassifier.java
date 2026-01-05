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

package io.hexaglue.core.classification;

import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates two-pass classification of domain types and ports.
 *
 * <p>The two-pass approach enables port classification to use knowledge
 * of domain classifications:
 * <ol>
 *   <li><b>Pass 1 - Domain Classification:</b> Classify all non-interface types
 *       as domain types (entities, value objects, aggregates, services, etc.)</li>
 *   <li><b>Pass 2 - Port Classification:</b> Classify interfaces as ports,
 *       with access to domain classification context</li>
 * </ol>
 *
 * <p>This enables more accurate classification because:
 * <ul>
 *   <li>A port that manipulates aggregate roots is likely a repository (DRIVEN)</li>
 *   <li>A port that uses only value objects might be a query service</li>
 *   <li>Classes with port dependencies are APPLICATION_SERVICE</li>
 *   <li>Classes without port dependencies but with domain logic are DOMAIN_SERVICE</li>
 * </ul>
 *
 * <p><b>Relationship-based criteria:</b>
 * <ul>
 *   <li>{@code InjectedAsDependencyCriteria}: Interface injected as field → DRIVEN port</li>
 *   <li>{@code HasPortDependenciesCriteria}: Class with interface fields → APPLICATION_SERVICE</li>
 *   <li>{@code StatelessNoDependenciesCriteria}: Class without port deps → DOMAIN_SERVICE</li>
 *   <li>{@code RepositoryDominantCriteria}: Type dominant in persistence port → AGGREGATE_ROOT</li>
 * </ul>
 *
 * <p><b>Note on pass order:</b> The current implementation works because criteria
 * use graph structure (edges, naming) rather than classification results. Future
 * enhancements may introduce a three-pass approach where port direction is determined
 * first, then used in domain classification.
 *
 * <p>Example:
 * <pre>{@code
 * TwoPassClassifier classifier = new TwoPassClassifier();
 * ClassificationResults results = classifier.classify(graph);
 *
 * results.domainClassifications().forEach(System.out::println);
 * results.portClassifications().forEach(System.out::println);
 * }</pre>
 */
public final class TwoPassClassifier {

    private final DomainClassifier domainClassifier;
    private final PortClassifier portClassifier;

    /**
     * Creates a classifier with default domain and port classifiers.
     */
    public TwoPassClassifier() {
        this(new DomainClassifier(), new PortClassifier());
    }

    /**
     * Creates a classifier with custom classifiers (for testing).
     */
    public TwoPassClassifier(DomainClassifier domainClassifier, PortClassifier portClassifier) {
        this.domainClassifier = domainClassifier;
        this.portClassifier = portClassifier;
    }

    /**
     * Classifies all types in the graph using two-pass classification.
     *
     * @param graph the application graph
     * @return the classification results
     */
    public ClassificationResults classify(ApplicationGraph graph) {
        GraphQuery query = graph.query();

        // Pass 1: Classify domain types (non-interfaces)
        Map<NodeId, ClassificationResult> domainResults = classifyDomainTypes(graph, query);

        // Create context with domain classifications for port classification
        ClassificationContext context = new ClassificationContext(domainResults);

        // Pass 2: Classify ports (interfaces) with domain context
        Map<NodeId, ClassificationResult> portResults = classifyPorts(graph, query, context);

        // Merge all results
        Map<NodeId, ClassificationResult> allResults = new HashMap<>();
        allResults.putAll(domainResults);
        allResults.putAll(portResults);

        return new ClassificationResults(allResults);
    }

    /**
     * Pass 1: Classify all non-interface types as domain types.
     */
    private Map<NodeId, ClassificationResult> classifyDomainTypes(ApplicationGraph graph, GraphQuery query) {
        Map<NodeId, ClassificationResult> results = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            // Skip interfaces - they are classified as ports in pass 2
            if (type.form() == JavaForm.INTERFACE) {
                continue;
            }

            ClassificationResult result = domainClassifier.classify(type, query);
            results.put(type.id(), result);
        }

        return results;
    }

    /**
     * Pass 2: Classify all interfaces as ports.
     *
     * <p>The context provides access to domain classifications from Pass 1,
     * enabling criteria to make more informed decisions.
     */
    private Map<NodeId, ClassificationResult> classifyPorts(
            ApplicationGraph graph, GraphQuery query, ClassificationContext context) {
        Map<NodeId, ClassificationResult> results = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            // Only classify interfaces as ports
            if (type.form() != JavaForm.INTERFACE) {
                continue;
            }

            // TODO: Pass context to port classifier when criteria support it
            // For now, use the standard classify method
            ClassificationResult result = portClassifier.classify(type, query);
            results.put(type.id(), result);
        }

        return results;
    }

    /**
     * Returns the domain classifier used by this two-pass classifier.
     */
    public DomainClassifier domainClassifier() {
        return domainClassifier;
    }

    /**
     * Returns the port classifier used by this two-pass classifier.
     */
    public PortClassifier portClassifier() {
        return portClassifier;
    }
}
