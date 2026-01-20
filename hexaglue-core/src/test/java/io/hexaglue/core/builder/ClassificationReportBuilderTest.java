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

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClassificationReportBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("ClassificationReportBuilder")
class ClassificationReportBuilderTest {

    private ClassificationReportBuilder builder;
    private TypeStructure defaultStructure;
    private Field idField;

    @BeforeEach
    void setUp() {
        builder = new ClassificationReportBuilder();
        defaultStructure = TypeStructure.builder(TypeNature.CLASS).build();
        idField = Field.of("id", TypeRef.of("java.util.UUID"));
    }

    private AggregateRoot createAggregate(TypeId id) {
        ClassificationTrace trace =
                ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test-criterion", "Test");
        return AggregateRoot.builder(id, defaultStructure, trace, idField).build();
    }

    private UnclassifiedType createUnclassified(TypeId id, UnclassifiedCategory category) {
        ClassificationTrace trace = ClassificationTrace.unclassified("Test unclassified", List.of());
        return UnclassifiedType.of(id, defaultStructure, trace, category);
    }

    private ClassificationResult createClassifiedResult(NodeId nodeId, String kind, ConfidenceLevel confidence) {
        return ClassificationResult.classified(
                nodeId,
                ClassificationTarget.DOMAIN,
                kind,
                confidence,
                "test-criterion",
                100,
                "Test classification",
                List.of(),
                List.of());
    }

    private ClassificationResult createUnclassifiedResult(NodeId nodeId) {
        return ClassificationResult.unclassifiedDomain(nodeId, null);
    }

    private ClassificationResult createConflictResult(NodeId nodeId) {
        return ClassificationResult.conflictDomain(
                nodeId,
                List.of(
                        Conflict.error(
                                "AGGREGATE_ROOT", "CriterionA", ConfidenceLevel.HIGH, 100, "Conflict with ENTITY"),
                        Conflict.error(
                                "ENTITY", "CriterionB", ConfidenceLevel.HIGH, 100, "Conflict with AGGREGATE_ROOT")),
                null);
    }

    @Nested
    @DisplayName("build()")
    class BuildTests {

        @Test
        @DisplayName("should build report with stats")
        void shouldBuildReportWithStats() {
            // given
            TypeId orderId = TypeId.of("com.example.Order");
            TypeId itemId = TypeId.of("com.example.Item");

            List<ArchType> allTypes =
                    List.of(createAggregate(orderId), createUnclassified(itemId, UnclassifiedCategory.UNKNOWN));
            List<UnclassifiedType> unclassified = List.of(createUnclassified(itemId, UnclassifiedCategory.UNKNOWN));

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(orderId.qualifiedName()),
                    createClassifiedResult(
                            NodeId.type(orderId.qualifiedName()), "AGGREGATE_ROOT", ConfidenceLevel.HIGH),
                    NodeId.type(itemId.qualifiedName()),
                    createUnclassifiedResult(NodeId.type(itemId.qualifiedName()))));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.stats().totalTypes()).isEqualTo(2);
            assertThat(report.stats().classifiedTypes()).isEqualTo(1);
            assertThat(report.stats().unclassifiedTypes()).isEqualTo(1);
        }

        @Test
        @DisplayName("should include unclassified by category")
        void shouldIncludeUnclassifiedByCategory() {
            // given
            TypeId itemId = TypeId.of("com.example.Item");
            TypeId utilsId = TypeId.of("com.example.Utils");

            List<ArchType> allTypes = List.of(
                    createUnclassified(itemId, UnclassifiedCategory.UNKNOWN),
                    createUnclassified(utilsId, UnclassifiedCategory.UTILITY));
            List<UnclassifiedType> unclassified = List.of(
                    createUnclassified(itemId, UnclassifiedCategory.UNKNOWN),
                    createUnclassified(utilsId, UnclassifiedCategory.UTILITY));

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(itemId.qualifiedName()), createUnclassifiedResult(NodeId.type(itemId.qualifiedName())),
                    NodeId.type(utilsId.qualifiedName()),
                            createUnclassifiedResult(NodeId.type(utilsId.qualifiedName()))));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.unclassifiedByCategory()).containsKey(UnclassifiedCategory.UNKNOWN);
            assertThat(report.unclassifiedByCategory()).containsKey(UnclassifiedCategory.UTILITY);
            assertThat(report.unclassifiedByCategory().get(UnclassifiedCategory.UNKNOWN))
                    .hasSize(1);
            assertThat(report.unclassifiedByCategory().get(UnclassifiedCategory.UTILITY))
                    .hasSize(1);
        }

        @Test
        @DisplayName("should count conflicts")
        void shouldCountConflicts() {
            // given
            TypeId orderId = TypeId.of("com.example.Order");
            TypeId itemId = TypeId.of("com.example.Item");

            // One classified, one conflict
            List<ArchType> allTypes =
                    List.of(createAggregate(orderId), createUnclassified(itemId, UnclassifiedCategory.CONFLICTING));
            List<UnclassifiedType> unclassified = List.of(createUnclassified(itemId, UnclassifiedCategory.CONFLICTING));

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(orderId.qualifiedName()),
                    createClassifiedResult(
                            NodeId.type(orderId.qualifiedName()), "AGGREGATE_ROOT", ConfidenceLevel.HIGH),
                    NodeId.type(itemId.qualifiedName()),
                    createConflictResult(NodeId.type(itemId.qualifiedName()))));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.stats().conflictCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should extract conflicts")
        void shouldExtractConflicts() {
            // given
            TypeId itemId = TypeId.of("com.example.Item");

            List<ArchType> allTypes = List.of(createUnclassified(itemId, UnclassifiedCategory.CONFLICTING));
            List<UnclassifiedType> unclassified = List.of(createUnclassified(itemId, UnclassifiedCategory.CONFLICTING));

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(itemId.qualifiedName()), createConflictResult(NodeId.type(itemId.qualifiedName()))));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.conflicts()).hasSize(1);
            assertThat(report.conflicts().get(0).typeId()).isEqualTo(itemId);
            assertThat(report.conflicts().get(0).contributions()).hasSize(2);
        }

        @Test
        @DisplayName("should generate remediations")
        void shouldGenerateRemediations() {
            // given
            TypeId itemId = TypeId.of("com.example.Item");

            List<ArchType> allTypes = List.of(createUnclassified(itemId, UnclassifiedCategory.AMBIGUOUS));
            List<UnclassifiedType> unclassified = List.of(createUnclassified(itemId, UnclassifiedCategory.AMBIGUOUS));

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(itemId.qualifiedName()),
                    createUnclassifiedResult(NodeId.type(itemId.qualifiedName()))));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.remediations()).hasSize(1);
            assertThat(report.remediations().get(0).typeId()).isEqualTo(itemId);
            assertThat(report.remediations().get(0).category()).isEqualTo(UnclassifiedCategory.AMBIGUOUS);
        }

        @Test
        @DisplayName("should count types by kind")
        void shouldCountTypesByKind() {
            // given
            TypeId orderId = TypeId.of("com.example.Order");
            TypeId itemId = TypeId.of("com.example.Item");

            List<ArchType> allTypes = List.of(createAggregate(orderId), createAggregate(itemId));
            List<UnclassifiedType> unclassified = List.of();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(orderId.qualifiedName()),
                    createClassifiedResult(
                            NodeId.type(orderId.qualifiedName()), "AGGREGATE_ROOT", ConfidenceLevel.HIGH),
                    NodeId.type(itemId.qualifiedName()),
                    createClassifiedResult(
                            NodeId.type(itemId.qualifiedName()), "AGGREGATE_ROOT", ConfidenceLevel.HIGH)));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.stats().countByKind().get(ArchKind.AGGREGATE_ROOT))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("should set generatedAt timestamp")
        void shouldSetGeneratedAtTimestamp() {
            // given
            TypeId orderId = TypeId.of("com.example.Order");
            List<ArchType> allTypes = List.of(createAggregate(orderId));
            List<UnclassifiedType> unclassified = List.of();
            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type(orderId.qualifiedName()),
                    createClassifiedResult(
                            NodeId.type(orderId.qualifiedName()), "AGGREGATE_ROOT", ConfidenceLevel.HIGH)));

            // when
            ClassificationReport report = builder.build(allTypes, unclassified, results);

            // then
            assertThat(report.generatedAt()).isNotNull();
        }
    }
}
