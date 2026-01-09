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

import io.hexaglue.core.analysis.AnalysisBudget;
import io.hexaglue.core.analysis.PublicApiPrioritizer;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.GeneratedCodeFilter;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaMethod;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.JavaType;
import io.hexaglue.core.frontend.MethodBodyAnalysis;
import io.hexaglue.core.frontend.spoon.adapters.SpoonMethodAdapter;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import spoon.reflect.declaration.CtMethod;

/**
 * Progressive 3-pass classifier with budget enforcement and performance optimization.
 *
 * <p>This classifier implements a multi-pass strategy that balances classification accuracy
 * with performance constraints. It progressively refines classifications through three passes:
 *
 * <ol>
 *   <li><b>PASS 1 - Fast (100ms for 1000 types):</b> Annotations and obvious patterns
 *       <ul>
 *         <li>@Entity, @Aggregate, @ValueObject annotations → EXPLICIT classification</li>
 *         <li>Records without ID fields → VALUE_OBJECT (HIGH confidence)</li>
 *         <li>Repository interfaces → REPOSITORY pattern detection</li>
 *         <li>Generated code filtering</li>
 *       </ul>
 *   </li>
 *   <li><b>PASS 2 - Medium (500ms for 1000 types):</b> Composition graph analysis
 *       <ul>
 *         <li>Field type analysis (has ID field → ENTITY/AGGREGATE_ROOT)</li>
 *         <li>Interface implementation patterns (UseCase, Repository)</li>
 *         <li>Type relationship inference (extends Entity → likely ENTITY)</li>
 *       </ul>
 *   </li>
 *   <li><b>PASS 3 - Deep (only if uncertain):</b> Method body analysis
 *       <ul>
 *         <li>Method invocations to detect behavioral patterns</li>
 *         <li>Field access patterns for mutability analysis</li>
 *         <li>Only performed on high-priority types (public APIs)</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Budget Enforcement:</b> The classifier respects {@link AnalysisBudget} limits to prevent
 * runaway analysis. When the budget is exhausted, remaining types are classified with UNCERTAIN
 * confidence based on available information.
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Small projects (100-500 types): 100-300ms total</li>
 *   <li>Medium projects (500-2000 types): 300-800ms total</li>
 *   <li>Large projects (2000+ types): 800ms-3s total</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * AnalysisBudget budget = AnalysisBudget.mediumProject();
 * ProgressiveClassifier classifier = new ProgressiveClassifier(budget);
 *
 * ClassificationResults results = classifier.classifyProgressive(graph);
 *
 * System.out.println("Pass 1: " + classifier.pass1Duration());
 * System.out.println("Pass 2: " + classifier.pass2Duration());
 * System.out.println("Pass 3: " + classifier.pass3Duration());
 * System.out.println("Budget: " + budget.summary());
 * }</pre>
 *
 * @since 3.0.0
 */
public final class ProgressiveClassifier {

    private final AnalysisBudget budget;
    private final JavaSemanticModel semanticModel;
    private final CachedSpoonAnalyzer spoonAnalyzer;

    // Pass timing
    private Duration pass1Duration = Duration.ZERO;
    private Duration pass2Duration = Duration.ZERO;
    private Duration pass3Duration = Duration.ZERO;

    // Classification results by pass
    private final Map<TypeNode, PassResult> classificationsByPass = new HashMap<>();

    /**
     * Creates a progressive classifier with the given budget, semantic model, and analyzer.
     *
     * @param budget the analysis budget
     * @param semanticModel the Java semantic model for accessing Spoon types
     * @param spoonAnalyzer the analyzer for method body analysis
     */
    public ProgressiveClassifier(
            AnalysisBudget budget, JavaSemanticModel semanticModel, CachedSpoonAnalyzer spoonAnalyzer) {
        this.budget = Objects.requireNonNull(budget, "budget cannot be null");
        this.semanticModel = Objects.requireNonNull(semanticModel, "semanticModel cannot be null");
        this.spoonAnalyzer = Objects.requireNonNull(spoonAnalyzer, "spoonAnalyzer cannot be null");
    }

    /**
     * Creates a progressive classifier with a default medium project budget.
     *
     * @param semanticModel the Java semantic model for accessing Spoon types
     * @param spoonAnalyzer the analyzer for method body analysis
     */
    public ProgressiveClassifier(JavaSemanticModel semanticModel, CachedSpoonAnalyzer spoonAnalyzer) {
        this(AnalysisBudget.mediumProject(), semanticModel, spoonAnalyzer);
    }

    /**
     * Creates a progressive classifier with the given budget.
     *
     * @param budget the analysis budget
     * @deprecated Use {@link #ProgressiveClassifier(AnalysisBudget, JavaSemanticModel, CachedSpoonAnalyzer)}
     */
    @Deprecated
    public ProgressiveClassifier(AnalysisBudget budget) {
        this.budget = Objects.requireNonNull(budget, "budget cannot be null");
        this.semanticModel = null;
        this.spoonAnalyzer = null;
    }

    /**
     * Creates a progressive classifier with a default medium project budget.
     *
     * @deprecated Use {@link #ProgressiveClassifier(JavaSemanticModel, CachedSpoonAnalyzer)}
     */
    @Deprecated
    public ProgressiveClassifier() {
        this(AnalysisBudget.mediumProject());
    }

    /**
     * Performs progressive classification of all types in the graph.
     *
     * <p>This is the main entry point for classification. It executes the three passes
     * in sequence, respecting the budget at each stage.
     *
     * @param graph the application graph
     * @return the classification results
     */
    public ClassificationResults classifyProgressive(ApplicationGraph graph) {
        Objects.requireNonNull(graph, "graph cannot be null");

        GraphQuery query = graph.query();

        // Filter out generated code
        List<TypeNode> userTypes = GeneratedCodeFilter.filterOut(graph.typeNodes());

        // Execute the three passes
        fastPassClassification(userTypes, query);

        if (!budget.isExhausted()) {
            mediumPassClassification(userTypes, query);
        }

        if (!budget.isExhausted()) {
            deepPassClassification(userTypes, query);
        }

        // Build final results
        return buildResults(userTypes);
    }

    /**
     * PASS 1: Fast classification based on annotations and obvious patterns.
     *
     * <p>This pass analyzes:
     * <ul>
     *   <li>DDD annotations (@Entity, @Aggregate, @ValueObject)</li>
     *   <li>JPA annotations (@Entity, @Embeddable)</li>
     *   <li>Records without ID → VALUE_OBJECT</li>
     *   <li>Repository naming pattern</li>
     * </ul>
     *
     * <p>Expected time: ~100ms for 1000 types
     *
     * @param types the types to classify
     * @param query the graph query interface
     */
    void fastPassClassification(List<TypeNode> types, GraphQuery query) {
        Instant start = Instant.now();

        for (TypeNode type : types) {
            if (budget.isExhausted()) {
                break;
            }

            PassResult result = classifyFast(type, query);
            if (result.confidence() != ConfidenceLevel.LOW) {
                classificationsByPass.put(type, result);
            }
        }

        pass1Duration = Duration.between(start, Instant.now());
    }

    /**
     * PASS 2: Medium-depth classification using composition graph analysis.
     *
     * <p>This pass analyzes:
     * <ul>
     *   <li>Field types (has ID field → ENTITY/AGGREGATE_ROOT)</li>
     *   <li>Interface implementations (implements Repository → REPOSITORY)</li>
     *   <li>Inheritance patterns (extends Entity → ENTITY)</li>
     * </ul>
     *
     * <p>Expected time: ~500ms for 1000 types
     *
     * @param types the types to classify
     * @param query the graph query interface
     */
    void mediumPassClassification(List<TypeNode> types, GraphQuery query) {
        Instant start = Instant.now();

        // Only classify types not already classified in Pass 1
        List<TypeNode> unclassified = types.stream()
                .filter(t -> !classificationsByPass.containsKey(t))
                .toList();

        for (TypeNode type : unclassified) {
            if (budget.isExhausted()) {
                break;
            }

            PassResult result = classifyMedium(type, query);
            if (result.confidence() != ConfidenceLevel.LOW) {
                classificationsByPass.put(type, result);
            }

            budget.recordNodeTraversed();
        }

        pass2Duration = Duration.between(start, Instant.now());
    }

    /**
     * PASS 3: Deep classification using method body analysis (only if uncertain).
     *
     * <p>This pass is only performed on:
     * <ul>
     *   <li>Public API types (prioritized by {@link PublicApiPrioritizer})</li>
     *   <li>Types still UNCERTAIN after Pass 2</li>
     *   <li>Types within budget limits</li>
     * </ul>
     *
     * <p>Method body analysis is expensive and should be avoided when possible.
     *
     * @param types the types to classify
     * @param query the graph query interface
     */
    void deepPassClassification(List<TypeNode> types, GraphQuery query) {
        Instant start = Instant.now();

        // Only classify public types not already classified
        List<TypeNode> unclassified = types.stream()
                .filter(t -> !classificationsByPass.containsKey(t))
                .filter(TypeNode::isPublic)
                .toList();

        for (TypeNode type : unclassified) {
            if (budget.isExhausted()) {
                break;
            }

            PassResult result = classifyDeep(type, query);
            if (result.confidence() != ConfidenceLevel.LOW) {
                classificationsByPass.put(type, result);
            }

            budget.recordMethodAnalyzed();
        }

        pass3Duration = Duration.between(start, Instant.now());
    }

    /**
     * Fast classification logic - annotations and obvious patterns.
     */
    private PassResult classifyFast(TypeNode type, GraphQuery query) {
        // Check for DDD annotations
        if (type.hasAnnotation("org.springframework.data.annotation.Aggregate")
                || type.hasAnnotation("io.hexaglue.domain.Aggregate")) {
            return new PassResult(DomainKind.AGGREGATE_ROOT.name(), ConfidenceLevel.EXPLICIT, 1);
        }

        if (type.hasAnnotation("javax.persistence.Entity") || type.hasAnnotation("jakarta.persistence.Entity")) {
            // JPA Entity - could be ENTITY or AGGREGATE_ROOT, need further analysis
            return new PassResult(DomainKind.ENTITY.name(), ConfidenceLevel.HIGH, 1);
        }

        if (type.hasAnnotation("javax.persistence.Embeddable")
                || type.hasAnnotation("jakarta.persistence.Embeddable")) {
            return new PassResult(DomainKind.VALUE_OBJECT.name(), ConfidenceLevel.EXPLICIT, 1);
        }

        // Records without ID are likely VALUE_OBJECTs
        if (type.form() == JavaForm.RECORD) {
            boolean hasIdField =
                    query.fieldsOf(type).stream().anyMatch(f -> f.simpleName().equalsIgnoreCase("id"));

            if (!hasIdField) {
                return new PassResult(DomainKind.VALUE_OBJECT.name(), ConfidenceLevel.HIGH, 1);
            }
        }

        // Repository pattern
        if (type.hasRepositorySuffix() && type.form() == JavaForm.INTERFACE) {
            return new PassResult("REPOSITORY", ConfidenceLevel.HIGH, 1);
        }

        return PassResult.uncertain(1);
    }

    /**
     * Medium-depth classification logic - composition analysis.
     */
    private PassResult classifyMedium(TypeNode type, GraphQuery query) {
        // Check for ID field (ENTITY or AGGREGATE_ROOT)
        boolean hasIdField = query.fieldsOf(type).stream()
                .anyMatch(f -> f.simpleName().equalsIgnoreCase("id") || f.looksLikeIdentity());

        if (hasIdField) {
            // Has identity - check if it's an aggregate root
            List<TypeNode> interfaces = query.interfacesOf(type);
            boolean implementsRepository = interfaces.stream().anyMatch(TypeNode::hasRepositorySuffix);

            if (implementsRepository) {
                return new PassResult(DomainKind.AGGREGATE_ROOT.name(), ConfidenceLevel.HIGH, 2);
            } else {
                return new PassResult(DomainKind.ENTITY.name(), ConfidenceLevel.MEDIUM, 2);
            }
        }

        // Check for value object patterns (immutable, no ID)
        if (type.form() == JavaForm.RECORD || isImmutableClass(type, query)) {
            return new PassResult(DomainKind.VALUE_OBJECT.name(), ConfidenceLevel.MEDIUM, 2);
        }

        return PassResult.uncertain(2);
    }

    /**
     * Deep classification logic - method body analysis.
     *
     * <p>This method analyzes method bodies to detect behavioral patterns:
     * <ul>
     *   <li>Use cases: Methods that orchestrate multiple domain operations</li>
     *   <li>Repositories: Methods with persistence-related invocations (save, find, delete)</li>
     *   <li>Entities: Methods that mutate internal state (write to fields)</li>
     *   <li>Value objects: No field writes, only reads</li>
     * </ul>
     */
    private PassResult classifyDeep(TypeNode type, GraphQuery query) {
        // If analyzer not available (deprecated constructor used), skip deep analysis
        if (semanticModel == null || spoonAnalyzer == null) {
            return PassResult.uncertain(3);
        }

        // Look up the JavaType from the semantic model by qualified name
        Optional<JavaType> javaTypeOpt = findJavaType(type.qualifiedName());
        if (javaTypeOpt.isEmpty()) {
            return PassResult.uncertain(3);
        }

        JavaType javaType = javaTypeOpt.get();
        List<JavaMethod> methods = javaType.methods();

        if (methods.isEmpty()) {
            return PassResult.uncertain(3);
        }

        // Analyze all methods and aggregate results
        int totalInvocations = 0;
        int totalFieldWrites = 0;
        int totalFieldReads = 0;
        int persistenceInvocations = 0;
        int domainInvocations = 0;

        for (JavaMethod method : methods) {
            // Extract CtMethod from SpoonMethodAdapter
            CtMethod<?> ctMethod = extractCtMethod(method);
            if (ctMethod == null) {
                continue;
            }

            MethodBodyAnalysis analysis = spoonAnalyzer.analyzeMethodBody(ctMethod);

            totalInvocations += analysis.invocations().size();

            // Count persistence-related invocations (save, find, delete, etc.)
            persistenceInvocations += (int) analysis.invocations().stream()
                    .filter(inv -> isPersistenceMethod(inv.targetMethod()))
                    .count();

            // Count domain-related invocations
            domainInvocations += (int) analysis.invocations().stream()
                    .filter(inv -> isDomainMethod(inv.targetMethod()))
                    .count();

            // Count field accesses
            totalFieldWrites += (int) analysis.fieldAccesses().stream()
                    .filter(MethodBodyAnalysis.FieldAccess::isWrite)
                    .count();

            totalFieldReads += (int) analysis.fieldAccesses().stream()
                    .filter(MethodBodyAnalysis.FieldAccess::isRead)
                    .count();
        }

        // Classification heuristics based on method body analysis

        // Repository pattern: Many persistence invocations
        if (persistenceInvocations > 0 && type.form() == JavaForm.INTERFACE) {
            return new PassResult("REPOSITORY", ConfidenceLevel.MEDIUM, 3);
        }

        // Use case pattern: Orchestrates multiple domain operations
        if (domainInvocations >= 3 && totalInvocations >= 5) {
            return new PassResult("USE_CASE", ConfidenceLevel.MEDIUM, 3);
        }

        // Entity pattern: Has field writes (mutable state)
        if (totalFieldWrites > 0) {
            boolean hasIdField = query.fieldsOf(type).stream()
                    .anyMatch(f -> f.simpleName().equalsIgnoreCase("id") || f.looksLikeIdentity());

            if (hasIdField) {
                return new PassResult(DomainKind.ENTITY.name(), ConfidenceLevel.MEDIUM, 3);
            }
        }

        // Value object pattern: Only field reads, no writes (immutable behavior)
        if (totalFieldReads > 0 && totalFieldWrites == 0) {
            return new PassResult(DomainKind.VALUE_OBJECT.name(), ConfidenceLevel.MEDIUM, 3);
        }

        return PassResult.uncertain(3);
    }

    /**
     * Looks up a JavaType from the semantic model by qualified name.
     */
    private Optional<JavaType> findJavaType(String qualifiedName) {
        return semanticModel.types().stream()
                .filter(t -> t.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    /**
     * Extracts the underlying CtMethod from a JavaMethod (if it's a SpoonMethodAdapter).
     */
    private CtMethod<?> extractCtMethod(JavaMethod method) {
        if (method instanceof SpoonMethodAdapter adapter) {
            return adapter.getCtMethod();
        }
        return null;
    }

    /**
     * Returns true if the method signature suggests a persistence operation.
     */
    private boolean isPersistenceMethod(String methodSignature) {
        String lowerSig = methodSignature.toLowerCase();
        return lowerSig.contains("save")
                || lowerSig.contains("find")
                || lowerSig.contains("delete")
                || lowerSig.contains("persist")
                || lowerSig.contains("remove")
                || lowerSig.contains("update")
                || lowerSig.contains("query")
                || lowerSig.contains("repository");
    }

    /**
     * Returns true if the method signature suggests a domain operation.
     */
    private boolean isDomainMethod(String methodSignature) {
        // Domain methods typically don't involve infrastructure concerns
        return !isPersistenceMethod(methodSignature)
                && !methodSignature.contains("java.lang")
                && !methodSignature.contains("java.util");
    }

    /**
     * Checks if a class is immutable (all fields final).
     */
    private boolean isImmutableClass(TypeNode type, GraphQuery query) {
        var fields = query.fieldsOf(type);
        if (fields.isEmpty()) {
            return false;
        }

        return fields.stream().allMatch(f -> f.isFinal());
    }

    /**
     * Builds the final classification results from the pass results.
     */
    private ClassificationResults buildResults(List<TypeNode> types) {
        Map<io.hexaglue.core.graph.model.NodeId, ClassificationResult> resultMap = new HashMap<>();

        for (TypeNode type : types) {
            PassResult passResult = classificationsByPass.get(type);

            if (passResult != null) {
                ReasonTrace trace = ReasonTrace.builder().build();

                ClassificationResult result = ClassificationResult.classified(
                        type.id(),
                        ClassificationTarget.DOMAIN,
                        passResult.kind(),
                        passResult.confidence(),
                        "Progressive classification - Pass " + passResult.pass(),
                        passResult.pass(),
                        "Classified in pass " + passResult.pass(),
                        List.of(),
                        List.of(),
                        trace);

                resultMap.put(type.id(), result);
            } else {
                // Unclassified
                ReasonTrace trace = ReasonTrace.builder().build();

                ClassificationResult result = ClassificationResult.unclassified(type.id(), trace);

                resultMap.put(type.id(), result);
            }
        }

        return new ClassificationResults(resultMap);
    }

    // === Getters for timing information ===

    public Duration pass1Duration() {
        return pass1Duration;
    }

    public Duration pass2Duration() {
        return pass2Duration;
    }

    public Duration pass3Duration() {
        return pass3Duration;
    }

    public Duration totalDuration() {
        return pass1Duration.plus(pass2Duration).plus(pass3Duration);
    }

    public AnalysisBudget budget() {
        return budget;
    }

    /**
     * Statistics about the progressive classification.
     *
     * @return the statistics
     */
    public ProgressiveClassificationStatistics statistics() {
        long pass1Count = classificationsByPass.values().stream()
                .filter(r -> r.pass() == 1)
                .count();
        long pass2Count = classificationsByPass.values().stream()
                .filter(r -> r.pass() == 2)
                .count();
        long pass3Count = classificationsByPass.values().stream()
                .filter(r -> r.pass() == 3)
                .count();

        return new ProgressiveClassificationStatistics(
                pass1Count, pass2Count, pass3Count, pass1Duration, pass2Duration, pass3Duration);
    }

    /**
     * Internal record for tracking pass results.
     */
    private record PassResult(String kind, ConfidenceLevel confidence, int pass) {
        static PassResult uncertain(int pass) {
            return new PassResult("UNCLASSIFIED", ConfidenceLevel.LOW, pass);
        }
    }

    /**
     * Statistics about progressive classification performance.
     *
     * @param pass1Count number of types classified in pass 1
     * @param pass2Count number of types classified in pass 2
     * @param pass3Count number of types classified in pass 3
     * @param pass1Duration duration of pass 1
     * @param pass2Duration duration of pass 2
     * @param pass3Duration duration of pass 3
     * @since 3.0.0
     */
    public record ProgressiveClassificationStatistics(
            long pass1Count,
            long pass2Count,
            long pass3Count,
            Duration pass1Duration,
            Duration pass2Duration,
            Duration pass3Duration) {

        public long totalClassified() {
            return pass1Count + pass2Count + pass3Count;
        }

        public Duration totalDuration() {
            return pass1Duration.plus(pass2Duration).plus(pass3Duration);
        }

        public double pass1Percentage() {
            long total = totalClassified();
            return total == 0 ? 0.0 : (pass1Count * 100.0) / total;
        }

        public double pass2Percentage() {
            long total = totalClassified();
            return total == 0 ? 0.0 : (pass2Count * 100.0) / total;
        }

        public double pass3Percentage() {
            long total = totalClassified();
            return total == 0 ? 0.0 : (pass3Count * 100.0) / total;
        }

        public String summary() {
            return String.format(
                    "ProgressiveClassificationStatistics[total=%d | " + "pass1: %d (%.1f%%), %.0fms | "
                            + "pass2: %d (%.1f%%), %.0fms | " + "pass3: %d (%.1f%%), %.0fms | " + "total: %.0fms]",
                    totalClassified(),
                    pass1Count,
                    pass1Percentage(),
                    (double) pass1Duration.toMillis(),
                    pass2Count,
                    pass2Percentage(),
                    (double) pass2Duration.toMillis(),
                    pass3Count,
                    pass3Percentage(),
                    (double) pass3Duration.toMillis(),
                    (double) totalDuration().toMillis());
        }

        @Override
        public String toString() {
            return summary();
        }
    }
}
