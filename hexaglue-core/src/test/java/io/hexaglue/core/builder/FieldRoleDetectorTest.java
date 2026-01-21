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

import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.NodeId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FieldRoleDetector}.
 *
 * @since 4.1.0
 */
@DisplayName("FieldRoleDetector")
class FieldRoleDetectorTest {

    private FieldRoleDetector detector;
    private BuilderContext context;

    @BeforeEach
    void setUp() {
        detector = new FieldRoleDetector();
        context = BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }

    @Nested
    @DisplayName("Identity Detection")
    class IdentityDetection {

        @Test
        @DisplayName("should detect @Id annotation (javax)")
        void shouldDetectJavaxIdAnnotation() {
            FieldNode field = createField("id", "java.lang.Long", List.of(AnnotationRef.of("javax.persistence.Id")));

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should detect @Id annotation (jakarta)")
        void shouldDetectJakartaIdAnnotation() {
            FieldNode field = createField("id", "java.lang.Long", List.of(AnnotationRef.of("jakarta.persistence.Id")));

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should detect @Identity annotation (jMolecules)")
        void shouldDetectJmoleculesIdentityAnnotation() {
            FieldNode field = createField(
                    "id", "java.util.UUID", List.of(AnnotationRef.of("org.jmolecules.ddd.annotation.Identity")));

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should detect field named 'id'")
        void shouldDetectFieldNamedId() {
            FieldNode field = createField("id", "java.util.UUID", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should detect field ending with 'Id' matching declaring type")
        void shouldDetectFieldEndingWithId() {
            // orderId in Order class should be detected as identity
            FieldNode field = createFieldInType("orderId", "com.example.OrderId", List.of(), "com.example.Order");

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should not detect field ending with 'Id' when it's a foreign key reference")
        void shouldNotDetectForeignKeyAsIdentity() {
            // productId in OrderLine is a foreign key reference, not an identity
            FieldNode field = createFieldInType("productId", "com.example.ProductId", List.of(), "com.example.OrderLine");

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).doesNotContain(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should not detect random field as identity")
        void shouldNotDetectRandomFieldAsIdentity() {
            FieldNode field = createField("name", "java.lang.String", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).doesNotContain(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should not detect collection with Id suffix as identity")
        void shouldNotDetectCollectionWithIdSuffixAsIdentity() {
            FieldNode field = createCollectionField("linesByProductId", "java.util.Map", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).doesNotContain(FieldRole.IDENTITY);
        }
    }

    @Nested
    @DisplayName("Collection Detection")
    class CollectionDetection {

        @Test
        @DisplayName("should detect List field")
        void shouldDetectListField() {
            FieldNode field = createCollectionField("items", "java.util.List", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.COLLECTION);
        }

        @Test
        @DisplayName("should detect Set field")
        void shouldDetectSetField() {
            FieldNode field = createCollectionField("tags", "java.util.Set", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.COLLECTION);
        }

        @Test
        @DisplayName("should detect Collection field")
        void shouldDetectCollectionField() {
            FieldNode field = createCollectionField("elements", "java.util.Collection", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.COLLECTION);
        }
    }

    @Nested
    @DisplayName("Aggregate Reference Detection")
    class AggregateReferenceDetection {

        @Test
        @DisplayName("should detect reference to AggregateRoot")
        void shouldDetectReferenceToAggregateRoot() {
            // Setup context with a classified AGGREGATE_ROOT
            NodeId aggregateNodeId = NodeId.type("com.example.Order");
            ClassificationResult aggregateResult = ClassificationResult.classified(
                    aggregateNodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test",
                    List.of(),
                    List.of());

            context = BuilderContext.of(
                    new TestGraphQuery(), new ClassificationResults(Map.of(aggregateNodeId, aggregateResult)));

            FieldNode field = createField("order", "com.example.Order", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.AGGREGATE_REFERENCE);
        }
    }

    @Nested
    @DisplayName("Embedded Detection")
    class EmbeddedDetection {

        @Test
        @DisplayName("should detect reference to ValueObject")
        void shouldDetectReferenceToValueObject() {
            NodeId voNodeId = NodeId.type("com.example.Money");
            ClassificationResult voResult = ClassificationResult.classified(
                    voNodeId,
                    ClassificationTarget.DOMAIN,
                    "VALUE_OBJECT",
                    ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test",
                    List.of(),
                    List.of());

            context = BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of(voNodeId, voResult)));

            FieldNode field = createField("amount", "com.example.Money", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.EMBEDDED);
        }

        @Test
        @DisplayName("should detect reference to Identifier")
        void shouldDetectReferenceToIdentifier() {
            NodeId idNodeId = NodeId.type("com.example.OrderId");
            ClassificationResult idResult = ClassificationResult.classified(
                    idNodeId,
                    ClassificationTarget.DOMAIN,
                    "IDENTIFIER",
                    ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test",
                    List.of(),
                    List.of());

            context = BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of(idNodeId, idResult)));

            FieldNode field = createField("orderId", "com.example.OrderId", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.EMBEDDED);
        }
    }

    @Nested
    @DisplayName("Audit Detection")
    class AuditDetection {

        @Test
        @DisplayName("should detect createdAt field")
        void shouldDetectCreatedAtField() {
            FieldNode field = createField("createdAt", "java.time.Instant", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.AUDIT);
        }

        @Test
        @DisplayName("should detect updatedBy field")
        void shouldDetectUpdatedByField() {
            FieldNode field = createField("updatedBy", "java.lang.String", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.AUDIT);
        }

        @Test
        @DisplayName("should detect createdOn field")
        void shouldDetectCreatedOnField() {
            FieldNode field = createField("createdOn", "java.time.LocalDateTime", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.AUDIT);
        }
    }

    @Nested
    @DisplayName("Technical Detection")
    class TechnicalDetection {

        @Test
        @DisplayName("should detect version field")
        void shouldDetectVersionField() {
            FieldNode field = createField("version", "java.lang.Long", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.TECHNICAL);
        }

        @Test
        @DisplayName("should detect @Version annotation")
        void shouldDetectVersionAnnotation() {
            FieldNode field =
                    createField("versionNumber", "int", List.of(AnnotationRef.of("javax.persistence.Version")));

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.TECHNICAL);
        }

        @Test
        @DisplayName("should detect serialVersionUID field")
        void shouldDetectSerialVersionUIDField() {
            FieldNode field = createField("serialVersionUID", "long", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.TECHNICAL);
        }

        @Test
        @DisplayName("should detect tenantId field")
        void shouldDetectTenantIdField() {
            FieldNode field = createField("tenantId", "java.lang.String", List.of());

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.TECHNICAL);
        }
    }

    @Nested
    @DisplayName("Multiple Roles")
    class MultipleRoles {

        @Test
        @DisplayName("should detect multiple roles for a field")
        void shouldDetectMultipleRoles() {
            // Field that is both IDENTITY and EMBEDDED (orderId in Order referencing an Identifier type)
            NodeId idNodeId = NodeId.type("com.example.OrderId");
            ClassificationResult idResult = ClassificationResult.classified(
                    idNodeId,
                    ClassificationTarget.DOMAIN,
                    "IDENTIFIER",
                    ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test",
                    List.of(),
                    List.of());

            context = BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of(idNodeId, idResult)));

            // orderId in Order class is both IDENTITY and EMBEDDED
            FieldNode field = createFieldInType("orderId", "com.example.OrderId", List.of(), "com.example.Order");

            Set<FieldRole> roles = detector.detect(field, context);

            assertThat(roles).contains(FieldRole.IDENTITY, FieldRole.EMBEDDED);
        }
    }

    // Helper methods

    private FieldNode createField(String name, String typeName, List<AnnotationRef> annotations) {
        return FieldNode.builder()
                .declaringTypeName("com.example.TestClass")
                .simpleName(name)
                .type(TypeRef.of(typeName))
                .annotations(annotations)
                .build();
    }

    private FieldNode createCollectionField(String name, String typeName, List<AnnotationRef> annotations) {
        // Create a collection type reference
        TypeRef collectionType = TypeRef.parameterized(typeName, TypeRef.of("java.lang.Object"));
        return FieldNode.builder()
                .declaringTypeName("com.example.TestClass")
                .simpleName(name)
                .type(collectionType)
                .annotations(annotations)
                .build();
    }

    private FieldNode createFieldInType(
            String name, String typeName, List<AnnotationRef> annotations, String declaringTypeName) {
        return FieldNode.builder()
                .declaringTypeName(declaringTypeName)
                .simpleName(name)
                .type(TypeRef.of(typeName))
                .annotations(annotations)
                .build();
    }
}
