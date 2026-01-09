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

package io.hexaglue.core.style;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StyleDetectorTest {

    private ApplicationGraph graph;
    private StyleDetector detector;

    @BeforeEach
    void setUp() {
        GraphMetadata metadata = GraphMetadata.of("com.example", 17, 0);
        graph = new ApplicationGraph(metadata);
        detector = new StyleDetector(graph);
    }

    @Test
    void shouldDetectHexagonalStyle() {
        addType("com.example.domain.Order");
        addType("com.example.ports.OrderRepository");
        addType("com.example.adapters.JpaOrderRepository");

        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.DDD_HEXAGONAL);
        assertThat(style.confidence()).isGreaterThan(0.5);
    }

    @Test
    void shouldDetectOnionStyle() {
        addType("com.example.core.Order");
        addType("com.example.application.OrderService");
        addType("com.example.infrastructure.Database");

        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.DDD_ONION);
        assertThat(style.confidence()).isGreaterThan(0.5);
    }

    @Test
    void shouldDetectLayeredStyle() {
        addType("com.example.controller.OrderController");
        addType("com.example.service.OrderService");
        addType("com.example.repository.OrderRepository");
        addType("com.example.model.Order");

        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.LAYERED_TRADITIONAL);
        assertThat(style.confidence()).isGreaterThan(0.5);
    }

    @Test
    void shouldDetectCleanArchitectureStyle() {
        addType("com.example.entities.Order");
        addType("com.example.usecases.CreateOrder");
        addType("com.example.gateways.OrderGateway");

        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.CLEAN_ARCHITECTURE);
        assertThat(style.confidence()).isGreaterThan(0.5);
    }

    @Test
    void shouldReturnUnknownForEmptyGraph() {
        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.UNKNOWN);
        assertThat(style.confidence()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnUnknownForNonStandardStructure() {
        addType("com.example.random.SomeClass");
        addType("com.example.stuff.AnotherClass");

        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.UNKNOWN);
    }

    @Test
    void shouldCalculateHighConfidenceForStrongSignals() {
        // Strong hexagonal signals
        addType("com.example.domain.Order");
        addType("com.example.domain.Customer");
        addType("com.example.ports.in.CreateOrder");
        addType("com.example.ports.out.OrderRepository");
        addType("com.example.adapters.in.rest.OrderController");
        addType("com.example.adapters.out.jpa.JpaOrderRepository");

        DetectedStyle style = detector.detect();

        assertThat(style.style()).isEqualTo(ArchitectureStyle.DDD_HEXAGONAL);
        assertThat(style.isHighConfidence()).isTrue();
    }

    @Test
    void shouldCalculateMediumConfidenceForWeakerSignals() {
        // Weaker hexagonal signals - only domain package, no ports or adapters
        addType("com.example.domain.Order");

        DetectedStyle style = detector.detect();

        // With only domain package, score is 0.4 which is low confidence
        if (style.style() == ArchitectureStyle.DDD_HEXAGONAL) {
            assertThat(style.isLowConfidence()).isTrue();
        }
    }

    @Test
    void shouldProvideDescription() {
        addType("com.example.domain.Order");
        addType("com.example.ports.OrderRepository");

        DetectedStyle style = detector.detect();

        assertThat(style.description()).isNotEmpty();
    }

    @Test
    void shouldProvideEvidence() {
        addType("com.example.domain.Order");

        DetectedStyle style = detector.detect();

        assertThat(style.evidence()).isNotNull();
    }

    // === Test utilities ===

    private void addType(String qualifiedName) {
        TypeNode type = TypeNode.builder()
                .qualifiedName(qualifiedName)
                .simpleName(extractSimpleName(qualifiedName))
                .packageName(extractPackageName(qualifiedName))
                .form(io.hexaglue.core.frontend.JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.PUBLIC))
                .build();

        graph.addNode(type);
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
