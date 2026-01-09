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

package io.hexaglue.core.audit;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.spi.audit.*;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultArchitectureQueryTest {

    private ApplicationGraph graph;
    private DefaultArchitectureQuery query;

    @BeforeEach
    void setUp() {
        GraphMetadata metadata = GraphMetadata.of("com.example", 17, 0);
        graph = new ApplicationGraph(metadata);
        query = new DefaultArchitectureQuery(graph);
    }

    // === Cycle detection ===

    @Test
    void shouldDetectDirectCycle() {
        TypeNode typeA = createType("com.example.A");
        TypeNode typeB = createType("com.example.B");

        graph.addNode(typeA);
        graph.addNode(typeB);

        // Create cycle: A → B → A
        graph.addEdge(Edge.raw(typeA.id(), typeB.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(typeB.id(), typeA.id(), EdgeKind.REFERENCES));

        var cycles = query.findDependencyCycles();

        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0).kind()).isEqualTo(CycleKind.TYPE_LEVEL);
        assertThat(cycles.get(0).isDirect()).isTrue();
    }

    @Test
    void shouldDetectTransitiveCycle() {
        TypeNode typeA = createType("com.example.A");
        TypeNode typeB = createType("com.example.B");
        TypeNode typeC = createType("com.example.C");

        graph.addNode(typeA);
        graph.addNode(typeB);
        graph.addNode(typeC);

        // Create cycle: A → B → C → A
        graph.addEdge(Edge.raw(typeA.id(), typeB.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(typeB.id(), typeC.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(typeC.id(), typeA.id(), EdgeKind.REFERENCES));

        var cycles = query.findDependencyCycles();

        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0).kind()).isEqualTo(CycleKind.TYPE_LEVEL);
        assertThat(cycles.get(0).length()).isEqualTo(3);
    }

    @Test
    void shouldDetectPackageCycle() {
        TypeNode typeA1 = createType("com.example.pkg1.A1");
        TypeNode typeB1 = createType("com.example.pkg2.B1");

        graph.addNode(typeA1);
        graph.addNode(typeB1);

        // Create package cycle: pkg1 → pkg2 → pkg1
        graph.addEdge(Edge.raw(typeA1.id(), typeB1.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(typeB1.id(), typeA1.id(), EdgeKind.REFERENCES));

        var cycles = query.findPackageCycles();

        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0).kind()).isEqualTo(CycleKind.PACKAGE_LEVEL);
    }

    // === Lakos metrics ===

    @Test
    void shouldCalculateDependsOnScore() {
        TypeNode typeA = createType("com.example.A");
        TypeNode typeB = createType("com.example.B");
        TypeNode typeC = createType("com.example.C");

        graph.addNode(typeA);
        graph.addNode(typeB);
        graph.addNode(typeC);

        // A depends on B, B depends on C
        graph.addEdge(Edge.raw(typeA.id(), typeB.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(typeB.id(), typeC.id(), EdgeKind.REFERENCES));

        int score = query.calculateDependsOnScore("com.example.A");

        // A transitively depends on B and C
        assertThat(score).isEqualTo(2);
    }

    @Test
    void shouldCalculateCCD() {
        TypeNode typeA = createType("com.example.pkg.A");
        TypeNode typeB = createType("com.example.pkg.B");
        TypeNode typeC = createType("com.example.other.C");

        graph.addNode(typeA);
        graph.addNode(typeB);
        graph.addNode(typeC);

        graph.addEdge(Edge.raw(typeA.id(), typeC.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(typeB.id(), typeC.id(), EdgeKind.REFERENCES));

        int ccd = query.calculateCCD("com.example.pkg");

        assertThat(ccd).isGreaterThan(0);
    }

    @Test
    void shouldCalculateNCCD() {
        TypeNode typeA = createType("com.example.pkg.A");
        TypeNode typeB = createType("com.example.pkg.B");

        graph.addNode(typeA);
        graph.addNode(typeB);

        double nccd = query.calculateNCCD("com.example.pkg");

        assertThat(nccd).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
    }

    // === Coupling metrics ===

    @Test
    void shouldAnalyzePackageCoupling() {
        TypeNode typeA = createType("com.example.pkg1.A");
        TypeNode typeB = createType("com.example.pkg2.B");

        graph.addNode(typeA);
        graph.addNode(typeB);

        // pkg1 depends on pkg2
        graph.addEdge(Edge.raw(typeA.id(), typeB.id(), EdgeKind.REFERENCES));

        Optional<CouplingMetrics> metrics = query.analyzePackageCoupling("com.example.pkg1");

        assertThat(metrics).isPresent();
        assertThat(metrics.get().packageName()).isEqualTo("com.example.pkg1");
        assertThat(metrics.get().efferentCoupling()).isGreaterThan(0);
    }

    @Test
    void shouldCalculateInstability() {
        TypeNode typeA = createType("com.example.pkg1.A");
        TypeNode typeB = createType("com.example.pkg2.B");

        graph.addNode(typeA);
        graph.addNode(typeB);

        // pkg1 depends on pkg2 (efferent coupling)
        graph.addEdge(Edge.raw(typeA.id(), typeB.id(), EdgeKind.REFERENCES));

        Optional<CouplingMetrics> metrics = query.analyzePackageCoupling("com.example.pkg1");

        assertThat(metrics).isPresent();
        double instability = metrics.get().instability();
        assertThat(instability).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
    }

    @Test
    void shouldCalculateDistanceFromMainSequence() {
        TypeNode typeA = createType("com.example.pkg.A");
        graph.addNode(typeA);

        Optional<CouplingMetrics> metrics = query.analyzePackageCoupling("com.example.pkg");

        assertThat(metrics).isPresent();
        double distance = metrics.get().distanceFromMainSequence();
        assertThat(distance).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
    }

    // === Layer violations ===

    @Test
    void shouldDetectLayerViolation() {
        TypeNode domainType = createType("com.example.domain.Order");
        TypeNode infraType = createType("com.example.infrastructure.Database");

        graph.addNode(domainType);
        graph.addNode(infraType);

        // Domain depending on infrastructure is a violation
        graph.addEdge(Edge.raw(domainType.id(), infraType.id(), EdgeKind.REFERENCES));

        var violations = query.findLayerViolations();

        assertThat(violations)
                .anyMatch(v -> v.fromLayer().equals("domain") && v.toLayer().equals("infrastructure"));
    }

    // === Stability violations ===

    @Test
    void shouldDetectStabilityViolation() {
        TypeNode unstableType = createType("com.example.UnstableType");
        TypeNode stableType = createType("com.example.StableType");
        TypeNode dependencyOfStable = createType("com.example.Dependency");

        graph.addNode(unstableType);
        graph.addNode(stableType);
        graph.addNode(dependencyOfStable);

        // Make stableType more stable by giving it fewer dependencies
        graph.addEdge(Edge.raw(stableType.id(), dependencyOfStable.id(), EdgeKind.REFERENCES));

        // Unstable depends on stable (violation)
        graph.addEdge(Edge.raw(unstableType.id(), stableType.id(), EdgeKind.REFERENCES));
        graph.addEdge(Edge.raw(unstableType.id(), dependencyOfStable.id(), EdgeKind.REFERENCES));

        var violations = query.findStabilityViolations();

        // Should have violations where unstable depends on more stable
        assertThat(violations).isNotEmpty();
    }

    // === Test utilities ===

    private TypeNode createType(String qualifiedName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .simpleName(extractSimpleName(qualifiedName))
                .packageName(extractPackageName(qualifiedName))
                .form(io.hexaglue.core.frontend.JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.PUBLIC))
                .build();
    }

    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
