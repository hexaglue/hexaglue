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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DrivingPortBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("DrivingPortBuilder")
class DrivingPortBuilderTest {

    private DrivingPortBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        builder = new DrivingPortBuilder(typeStructureBuilder, traceConverter);
    }

    @Nested
    @DisplayName("Build DrivingPort")
    class BuildDrivingPort {

        @Test
        @DisplayName("should build DrivingPort with correct id")
        void shouldBuildDrivingPortWithCorrectId() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderService")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "DRIVING_PORT");
            BuilderContext context = createContext(typeNode, classification);

            DrivingPort drivingPort = builder.build(typeNode, classification, context);

            assertThat(drivingPort.id().qualifiedName()).isEqualTo("com.example.OrderService");
        }

        @Test
        @DisplayName("should build DrivingPort with correct kind")
        void shouldBuildDrivingPortWithCorrectKind() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.PlaceOrderUseCase")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "DRIVING_PORT");
            BuilderContext context = createContext(typeNode, classification);

            DrivingPort drivingPort = builder.build(typeNode, classification, context);

            assertThat(drivingPort.kind()).isEqualTo(ArchKind.DRIVING_PORT);
        }

        @Test
        @DisplayName("should build DrivingPort with structure")
        void shouldBuildDrivingPortWithStructure() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.QueryOrderUseCase")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "DRIVING_PORT");
            BuilderContext context = createContext(typeNode, classification);

            DrivingPort drivingPort = builder.build(typeNode, classification, context);

            assertThat(drivingPort.structure()).isNotNull();
            assertThat(drivingPort.structure().isInterface()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when typeNode is null")
        void shouldThrowWhenTypeNodeIsNull() {
            ClassificationResult classification = createClassificationForNullCheck();
            BuilderContext context = createEmptyContext();

            assertThatThrownBy(() -> builder.build(null, classification, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeNode");
        }

        @Test
        @DisplayName("should throw when classification is null")
        void shouldThrowWhenClassificationIsNull() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.INTERFACE)
                    .build();
            BuilderContext context = createEmptyContext();

            assertThatThrownBy(() -> builder.build(typeNode, null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should throw when context is null")
        void shouldThrowWhenContextIsNull() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.INTERFACE)
                    .build();
            ClassificationResult classification = createClassificationForNullCheck();

            assertThatThrownBy(() -> builder.build(typeNode, classification, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }

    // Helper methods

    private ClassificationResult createClassification(TypeNode typeNode, String kind) {
        return ClassificationResult.classified(
                typeNode.id(),
                ClassificationTarget.PORT,
                kind,
                ConfidenceLevel.HIGH,
                "test-criterion",
                80,
                "Test justification",
                List.of(),
                List.of());
    }

    private ClassificationResult createClassificationForNullCheck() {
        NodeId nodeId = NodeId.type("com.example.Test");
        return ClassificationResult.classified(
                nodeId,
                ClassificationTarget.PORT,
                "DRIVING_PORT",
                ConfidenceLevel.HIGH,
                "test-criterion",
                80,
                "Test justification",
                List.of(),
                List.of());
    }

    private BuilderContext createContext(TypeNode typeNode, ClassificationResult classification) {
        ClassificationResults results = new ClassificationResults(Map.of(typeNode.id(), classification));
        return BuilderContext.of(new TestGraphQuery(), results);
    }

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }
}
