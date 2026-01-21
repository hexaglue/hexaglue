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
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DrivenPortBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("DrivenPortBuilder")
class DrivenPortBuilderTest {

    private DrivenPortBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        MethodRoleDetector methodRoleDetector = new MethodRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        builder = new DrivenPortBuilder(typeStructureBuilder, traceConverter);
    }

    @Nested
    @DisplayName("Build DrivenPort")
    class BuildDrivenPort {

        @Test
        @DisplayName("should build DrivenPort with correct id")
        void shouldBuildDrivenPortWithCorrectId() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderRepository")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "REPOSITORY");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.id().qualifiedName()).isEqualTo("com.example.OrderRepository");
        }

        @Test
        @DisplayName("should build DrivenPort with correct kind")
        void shouldBuildDrivenPortWithCorrectKind() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.PaymentGateway")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "GATEWAY");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.kind()).isEqualTo(ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("should build DrivenPort with structure")
        void shouldBuildDrivenPortWithStructure() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.NotificationService")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "GATEWAY");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.structure()).isNotNull();
            assertThat(drivenPort.structure().isInterface()).isTrue();
        }
    }

    @Nested
    @DisplayName("Port Type Detection")
    class PortTypeDetection {

        @Test
        @DisplayName("should detect REPOSITORY port type")
        void shouldDetectRepositoryPortType() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.CustomerRepository")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "REPOSITORY");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.portType()).isEqualTo(DrivenPortType.REPOSITORY);
            assertThat(drivenPort.isRepository()).isTrue();
        }

        @Test
        @DisplayName("should detect GATEWAY port type")
        void shouldDetectGatewayPortType() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.PaymentGateway")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "GATEWAY");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.portType()).isEqualTo(DrivenPortType.GATEWAY);
            assertThat(drivenPort.isGateway()).isTrue();
        }

        @Test
        @DisplayName("should detect EVENT_PUBLISHER port type")
        void shouldDetectEventPublisherPortType() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.DomainEventPublisher")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "EVENT_PUBLISHER");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.portType()).isEqualTo(DrivenPortType.EVENT_PUBLISHER);
            assertThat(drivenPort.isEventPublisher()).isTrue();
        }

        @Test
        @DisplayName("should default to OTHER port type for generic driven port")
        void shouldDefaultToOtherPortTypeForGenericDrivenPort() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.ExternalServicePort")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "GENERIC");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.portType()).isEqualTo(DrivenPortType.OTHER);
        }
    }

    @Nested
    @DisplayName("Managed Aggregate Detection")
    class ManagedAggregateDetection {

        @Test
        @DisplayName("should detect managed aggregate from method return type")
        void shouldDetectManagedAggregateFromMethodReturnType() {
            MethodNode findByIdMethod = MethodNode.builder()
                    .declaringTypeName("com.example.OrderRepository")
                    .simpleName("findById")
                    .returnType(TypeRef.parameterized("java.util.Optional", TypeRef.of("com.example.Order")))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderRepository")
                    .form(JavaForm.INTERFACE)
                    .build();

            // Add Order as aggregate in the classification
            TypeNode orderNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();
            ClassificationResult orderClassification = createDomainClassification(orderNode, "AGGREGATE_ROOT");
            ClassificationResult repoClassification = createClassification(typeNode, "REPOSITORY");

            TestGraphQueryWithMethods graphQuery = new TestGraphQueryWithMethods(List.of(findByIdMethod));
            BuilderContext context = createContextWithMultiple(
                    List.of(typeNode, orderNode), List.of(repoClassification, orderClassification), graphQuery);

            DrivenPort drivenPort = builder.build(typeNode, repoClassification, context);

            assertThat(drivenPort.hasAggregate()).isTrue();
            assertThat(drivenPort.managedAggregate()).isPresent();
            assertThat(drivenPort.managedAggregate().get().qualifiedName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("should return empty aggregate when none found")
        void shouldReturnEmptyAggregateWhenNoneFound() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.GenericGateway")
                    .form(JavaForm.INTERFACE)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "GATEWAY");
            BuilderContext context = createContext(typeNode, classification);

            DrivenPort drivenPort = builder.build(typeNode, classification, context);

            assertThat(drivenPort.hasAggregate()).isFalse();
            assertThat(drivenPort.managedAggregate()).isEmpty();
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

    private ClassificationResult createDomainClassification(TypeNode typeNode, String kind) {
        return ClassificationResult.classified(
                typeNode.id(),
                ClassificationTarget.DOMAIN,
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
                "REPOSITORY",
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

    private BuilderContext createContextWithMultiple(
            List<TypeNode> typeNodes,
            List<ClassificationResult> classifications,
            TestGraphQueryWithMethods graphQuery) {
        Map<NodeId, ClassificationResult> resultMap = new java.util.HashMap<>();
        for (int i = 0; i < typeNodes.size(); i++) {
            resultMap.put(typeNodes.get(i).id(), classifications.get(i));
        }
        ClassificationResults results = new ClassificationResults(resultMap);
        return BuilderContext.of(graphQuery, results);
    }

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }

    /**
     * Test helper class that provides methods for testing.
     */
    private static class TestGraphQueryWithMethods extends TestGraphQuery {
        private final List<MethodNode> methods;

        TestGraphQueryWithMethods(List<MethodNode> methods) {
            this.methods = methods;
        }

        @Override
        public List<MethodNode> methodsOf(TypeNode typeNode) {
            return methods;
        }
    }
}
