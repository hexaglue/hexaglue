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

import io.hexaglue.core.classification.anchor.AnchorContext;
import io.hexaglue.core.classification.anchor.AnchorDetector;
import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.domain.criteria.FlexibleInboundOnlyCriteria;
import io.hexaglue.core.classification.domain.criteria.FlexibleOutboundOnlyCriteria;
import io.hexaglue.core.classification.domain.criteria.FlexibleSagaCriteria;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.classification.port.PortKindClassifier;
import io.hexaglue.core.classification.port.criteria.SemanticDrivenPortCriteria;
import io.hexaglue.core.classification.port.criteria.SemanticDrivingPortCriteria;
import io.hexaglue.core.classification.semantic.CoreAppClassDetector;
import io.hexaglue.core.classification.semantic.CoreAppClassIndex;
import io.hexaglue.core.classification.semantic.InterfaceFacts;
import io.hexaglue.core.classification.semantic.InterfaceFactsIndex;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.spi.core.ClassificationConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Single-pass classifier that uses CoreAppClass as the pivot for classification.
 *
 * <p>This classifier replaces the two-pass approach with a unified pass that:
 * <ol>
 *   <li><b>Pre-classification:</b> Build semantic indexes (Anchors, CoreAppClass, InterfaceFacts)</li>
 *   <li><b>Ports first:</b> Classify interfaces as DRIVING/DRIVEN based on CoreAppClass relationships</li>
 *   <li><b>Actors:</b> Classify CoreAppClasses as APPLICATION_SERVICE, INBOUND_ONLY, OUTBOUND_ONLY, SAGA</li>
 *   <li><b>Domain:</b> Classify types with port context available</li>
 * </ol>
 *
 * <p><b>Key principle:</b> Classification is derived from RELATIONSHIPS in the graph, not from NAMES.
 * The pivot (CoreAppClass) allows distinguishing DRIVING (what it implements) from DRIVEN (what it depends on).
 *
 * <p><b>Port Classification:</b>
 * <ul>
 *   <li>DRIVING: Interface implemented by at least one CoreAppClass</li>
 *   <li>DRIVEN: Interface used by CoreAppClass + (MissingImpl OR InternalImplOnly) + has port annotation</li>
 * </ul>
 *
 * <p><b>Actor Classification:</b>
 * <ul>
 *   <li>APPLICATION_SERVICE: CoreAppClass that implements DRIVING + depends on DRIVEN</li>
 *   <li>INBOUND_ONLY: CoreAppClass that implements DRIVING but no DRIVEN dependencies</li>
 *   <li>OUTBOUND_ONLY: CoreAppClass that depends on DRIVEN but doesn't implement DRIVING</li>
 *   <li>SAGA: OUTBOUND_ONLY with 2+ DRIVEN dependencies + stateful fields</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * SinglePassClassifier classifier = new SinglePassClassifier();
 * ClassificationResults results = classifier.classify(graph);
 *
 * results.portClassifications().forEach(System.out::println);
 * results.domainClassifications().forEach(System.out::println);
 * }</pre>
 */
public final class SinglePassClassifier {

    private DomainClassifier domainClassifier;
    private PortClassifier portClassifier;

    /**
     * Creates a classifier with default domain and port classifiers.
     */
    public SinglePassClassifier() {
        this.domainClassifier = null;
        this.portClassifier = null;
    }

    /**
     * Creates a classifier with custom classifiers (for testing).
     *
     * @param domainClassifier the domain classifier to use
     * @param portClassifier the port classifier to use
     */
    public SinglePassClassifier(DomainClassifier domainClassifier, PortClassifier portClassifier) {
        this.domainClassifier = domainClassifier;
        this.portClassifier = portClassifier;
    }

    /**
     * Classifies all types in the graph using single-pass classification.
     *
     * <p>Uses default classification configuration.
     *
     * @param graph the application graph
     * @return the classification results
     */
    public ClassificationResults classify(ApplicationGraph graph) {
        return classify(graph, ClassificationConfig.defaults());
    }

    /**
     * Classifies all types in the graph using single-pass classification with configuration.
     *
     * <p>The classification order:
     * <ol>
     *   <li><b>Pre-classification:</b> Build semantic indexes</li>
     *   <li><b>Ports:</b> Classify interfaces first (enables port-aware domain classification)</li>
     *   <li><b>Domain:</b> Classify types with full port context</li>
     * </ol>
     *
     * <p>Configuration features:
     * <ul>
     *   <li><b>Exclusions:</b> Types matching exclude patterns are skipped</li>
     *   <li><b>Explicit:</b> Types with explicit classifications bypass criteria evaluation</li>
     * </ul>
     *
     * @param graph the application graph
     * @param config the classification configuration
     * @return the classification results
     * @since 3.0.0
     */
    public ClassificationResults classify(ApplicationGraph graph, ClassificationConfig config) {
        GraphQuery query = graph.query();

        // Phase 1-3: Build semantic indexes
        SemanticIndexes indexes = buildSemanticIndexes(graph, query);

        // Create classifiers with semantic criteria if not provided
        DomainClassifier effectiveDomainClassifier =
                domainClassifier != null ? domainClassifier : createDomainClassifierWithSemanticCriteria(indexes);

        PortClassifier effectivePortClassifier =
                portClassifier != null ? portClassifier : createPortClassifierWithSemanticCriteria(indexes);

        // Phase 4a: Classify PORTS FIRST
        Map<NodeId, ClassificationResult> portResults =
                classifyPorts(graph, query, indexes, effectivePortClassifier, config);

        // Extract classified port sets for domain classification context
        Set<NodeId> drivingPorts = extractPortsByDirection(portResults, PortDirection.DRIVING);
        Set<NodeId> drivenPorts = extractPortsByDirection(portResults, PortDirection.DRIVEN);

        // Create context with port classifications for domain classification
        ClassificationContext context = createPortAwareContext(portResults, drivingPorts, drivenPorts);

        // Phase 4b: Classify domain types with port context
        Map<NodeId, ClassificationResult> domainResults =
                classifyDomainTypes(graph, query, context, indexes, effectiveDomainClassifier, config);

        // Merge all results
        Map<NodeId, ClassificationResult> allResults = new HashMap<>();
        allResults.putAll(portResults);
        allResults.putAll(domainResults);

        return new ClassificationResults(allResults);
    }

    /**
     * Builds semantic indexes required for semantic classification criteria.
     */
    private SemanticIndexes buildSemanticIndexes(ApplicationGraph graph, GraphQuery query) {
        // Phase 1: Detect anchors
        AnchorContext anchorContext = AnchorDetector.analyze(graph);

        // Phase 2: Detect CoreAppClasses
        CoreAppClassIndex coreAppClassIndex = CoreAppClassDetector.analyze(graph, anchorContext);

        // Phase 3: Compute InterfaceFacts
        InterfaceFactsIndex interfaceFactsIndex = InterfaceFactsIndex.build(graph, anchorContext, coreAppClassIndex);

        return new SemanticIndexes(anchorContext, coreAppClassIndex, interfaceFactsIndex);
    }

    /**
     * Creates a DomainClassifier with semantic criteria added.
     */
    private DomainClassifier createDomainClassifierWithSemanticCriteria(SemanticIndexes indexes) {
        List<ClassificationCriteria<DomainKind>> criteria = new ArrayList<>(DomainClassifier.defaultCriteria());

        // Add flexible actor criteria (priority 68-72)
        criteria.add(new FlexibleSagaCriteria(indexes.coreAppClassIndex()));
        criteria.add(new FlexibleInboundOnlyCriteria(indexes.coreAppClassIndex()));
        criteria.add(new FlexibleOutboundOnlyCriteria(indexes.coreAppClassIndex()));

        return new DomainClassifier(criteria);
    }

    /**
     * Creates a PortClassifier with semantic criteria added.
     */
    private PortClassifier createPortClassifierWithSemanticCriteria(SemanticIndexes indexes) {
        List<PortClassificationCriteria> criteria = new ArrayList<>(PortClassifier.defaultCriteria());

        // Add semantic port criteria (priority 85)
        criteria.add(new SemanticDrivingPortCriteria(indexes.interfaceFactsIndex()));
        criteria.add(new SemanticDrivenPortCriteria(indexes.interfaceFactsIndex()));

        return new PortClassifier(criteria);
    }

    /**
     * Internal record holding semantic indexes.
     */
    private record SemanticIndexes(
            AnchorContext anchorContext,
            CoreAppClassIndex coreAppClassIndex,
            InterfaceFactsIndex interfaceFactsIndex) {}

    /**
     * Classify all interfaces as ports FIRST.
     *
     * <p>This is the key difference from TwoPassClassifier: ports are classified
     * before domain types, enabling domain classification to use port context.
     *
     * <p>Respects ClassificationConfig for exclusions and explicit classifications.
     */
    private Map<NodeId, ClassificationResult> classifyPorts(
            ApplicationGraph graph,
            GraphQuery query,
            SemanticIndexes indexes,
            PortClassifier classifier,
            ClassificationConfig config) {
        Map<NodeId, ClassificationResult> results = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            // Only classify interfaces as ports
            if (type.form() != JavaForm.INTERFACE) {
                continue;
            }

            String qualifiedName = type.qualifiedName();

            // Check if type should be excluded
            if (config.shouldExclude(qualifiedName)) {
                // Skip excluded types entirely
                continue;
            }

            // Check for explicit classification
            Optional<String> explicitKind = config.getExplicitKind(qualifiedName);
            if (explicitKind.isPresent()) {
                results.put(type.id(), createExplicitClassificationResult(type, explicitKind.get()));
                continue;
            }

            // First, check if we have semantic facts for this interface
            var facts = indexes.interfaceFactsIndex().get(type.id());

            if (facts.isPresent()) {
                InterfaceFacts f = facts.get();

                // Use semantic facts for classification
                if (f.isDrivingPortCandidate()) {
                    results.put(type.id(), classifyDrivingPort(type));
                    continue;
                }

                if (f.isDrivenPortCandidate()) {
                    results.put(type.id(), classifyDrivenPort(type, query, f, true));
                    continue;
                }

                // Fallback: Use without annotation check for broader detection
                if (f.isDrivenPortCandidateWithoutAnnotationCheck()) {
                    results.put(type.id(), classifyDrivenPort(type, query, f, false));
                    continue;
                }
            }

            // Fallback to criteria-based classification
            ClassificationResult result = classifier.classify(type, query);
            results.put(type.id(), result);
        }

        return results;
    }

    /**
     * Creates a classification result for a driving port.
     *
     * @param type the interface type node
     * @return the classification result
     */
    private ClassificationResult classifyDrivingPort(TypeNode type) {
        ReasonTrace trace =
                ReasonTrace.builder().coreAppClassRole("implementedByCore").build();

        return ClassificationResult.classifiedPort(
                type.id(),
                "DRIVING_PORT",
                ConfidenceLevel.HIGH,
                "SemanticDrivingPortCriteria",
                85,
                "Interface is implemented by a CoreAppClass",
                List.of(Evidence.fromRelationship("implementedByCore=true", List.of())),
                List.of(),
                PortDirection.DRIVING,
                trace);
    }

    /**
     * Creates a classification result for a driven port.
     *
     * @param type the interface type node
     * @param query the graph query
     * @param facts the interface facts
     * @param hasAnnotation whether port annotation was detected
     * @return the classification result
     */
    private ClassificationResult classifyDrivenPort(
            TypeNode type, GraphQuery query, InterfaceFacts facts, boolean hasAnnotation) {
        PortKind portKind = PortKindClassifier.classify(type, query);

        String role = hasAnnotation ? "usedByCore" : "usedByCore (relaxed)";
        ReasonTrace trace = ReasonTrace.builder().coreAppClassRole(role).build();

        ConfidenceLevel confidence = hasAnnotation ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM;
        String criteriaName = hasAnnotation ? "SemanticDrivenPortCriteria" : "SemanticDrivenPortCriteria (relaxed)";
        int priority = hasAnnotation ? 85 : 80;

        String justification = hasAnnotation
                ? "Interface is used by CoreAppClass with missing/internal implementation (kind=" + portKind + ")"
                : "Interface is used by CoreAppClass with missing/internal implementation (no annotation, kind="
                        + portKind + ")";

        String evidenceDetail = hasAnnotation
                ? "usedByCore=true, missingImpl=" + facts.missingImpl() + ", portKind=" + portKind
                : "usedByCore=true, missingImpl=" + facts.missingImpl() + ", hasPortAnnotation=false, portKind="
                        + portKind;

        return ClassificationResult.classifiedPort(
                type.id(),
                portKind.name(),
                confidence,
                criteriaName,
                priority,
                justification,
                List.of(Evidence.fromRelationship(evidenceDetail, List.of())),
                List.of(),
                PortDirection.DRIVEN,
                trace);
    }

    /**
     * Extracts NodeIds of ports by direction from classification results.
     */
    private Set<NodeId> extractPortsByDirection(
            Map<NodeId, ClassificationResult> portResults, PortDirection direction) {
        Set<NodeId> ports = new HashSet<>();
        for (var entry : portResults.entrySet()) {
            if (entry.getValue().portDirection() == direction) {
                ports.add(entry.getKey());
            }
        }
        return ports;
    }

    /**
     * Creates a classification context with port information.
     */
    private ClassificationContext createPortAwareContext(
            Map<NodeId, ClassificationResult> portResults, Set<NodeId> drivingPorts, Set<NodeId> drivenPorts) {
        // The context will have port classifications available
        return new ClassificationContext(portResults);
    }

    /**
     * Classify all non-interface types as domain types.
     *
     * <p>Domain classification now has access to port classifications via context.
     *
     * <p>Respects ClassificationConfig for exclusions and explicit classifications.
     */
    private Map<NodeId, ClassificationResult> classifyDomainTypes(
            ApplicationGraph graph,
            GraphQuery query,
            ClassificationContext context,
            SemanticIndexes indexes,
            DomainClassifier classifier,
            ClassificationConfig config) {
        Map<NodeId, ClassificationResult> results = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            // Skip interfaces - they are classified as ports
            if (type.form() == JavaForm.INTERFACE) {
                continue;
            }

            String qualifiedName = type.qualifiedName();

            // Check if type should be excluded
            if (config.shouldExclude(qualifiedName)) {
                // Skip excluded types entirely
                continue;
            }

            // Check for explicit classification
            Optional<String> explicitKind = config.getExplicitKind(qualifiedName);
            if (explicitKind.isPresent()) {
                results.put(type.id(), createExplicitClassificationResult(type, explicitKind.get()));
                continue;
            }

            ClassificationResult result = classifier.classify(type, query);
            results.put(type.id(), result);
        }

        return results;
    }

    /**
     * Returns the domain classifier used by this classifier.
     */
    public DomainClassifier domainClassifier() {
        return domainClassifier;
    }

    /**
     * Returns the port classifier used by this classifier.
     */
    public PortClassifier portClassifier() {
        return portClassifier;
    }

    /**
     * Creates a classification result from explicit user configuration.
     *
     * <p>Explicit classifications have the highest priority (100) and HIGH confidence,
     * as they represent deliberate user decisions that should override all other criteria.
     *
     * @param type the type to classify
     * @param explicitKind the explicit kind from configuration (e.g., "ENTITY", "VALUE_OBJECT")
     * @return the classification result
     */
    private ClassificationResult createExplicitClassificationResult(TypeNode type, String explicitKind) {
        ClassificationTarget target =
                type.form() == JavaForm.INTERFACE ? ClassificationTarget.PORT : ClassificationTarget.DOMAIN;

        ReasonTrace trace = ReasonTrace.builder()
                .addEvaluatedCriteria(ReasonTrace.EvaluatedCriteria.matched(
                        "ExplicitConfiguration",
                        100,
                        explicitKind,
                        ConfidenceLevel.HIGH,
                        "User-configured explicit classification in hexaglue.yaml"))
                .anchorKind("EXPLICIT_CONFIG")
                .build();

        return ClassificationResult.classified(
                type.id(),
                target,
                explicitKind,
                ConfidenceLevel.HIGH,
                "ExplicitConfiguration",
                100, // Highest priority - user decision overrides all
                "Explicitly configured as " + explicitKind + " in hexaglue.yaml",
                List.of(Evidence.fromStructure("explicit: " + explicitKind, List.of())),
                List.of(),
                trace);
    }
}
