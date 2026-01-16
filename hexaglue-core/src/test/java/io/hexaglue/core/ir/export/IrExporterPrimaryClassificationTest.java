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

package io.hexaglue.core.ir.export;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.spi.classification.CertaintyLevel;
import io.hexaglue.spi.classification.ClassificationStrategy;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for primary classification result conversion in IrExporter.
 *
 * <p>Verifies that core ClassificationResult instances are correctly
 * converted to SPI PrimaryClassificationResult for enrichment plugins.
 */
@DisplayName("IrExporter - Primary Classification Conversion")
class IrExporterPrimaryClassificationTest {

    private final IrExporter exporter = new IrExporter();

    @Nested
    @DisplayName("Successful Classifications")
    class SuccessfulClassifications {

        @Test
        @DisplayName("should convert classified aggregate root with annotation evidence")
        void convertAggregateRootWithAnnotation() {
            // Given
            NodeId nodeId = NodeId.type("com.example.Order");
            Evidence annotationEvidence = Evidence.fromAnnotation("AggregateRoot", nodeId);

            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    ConfidenceLevel.EXPLICIT,
                    "HexaGlueAnnotationCriterion",
                    100,
                    "Annotated with @AggregateRoot",
                    List.of(annotationEvidence),
                    List.of());

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.typeName()).isEqualTo("com.example.Order");
            assertThat(result.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(result.certainty()).isEqualTo(CertaintyLevel.EXPLICIT);
            assertThat(result.strategy()).isEqualTo(ClassificationStrategy.ANNOTATION);
            assertThat(result.reasoning()).isEqualTo("Annotated with @AggregateRoot");
            assertThat(result.isClassified()).isTrue();
            assertThat(result.isReliable()).isTrue();
        }

        @Test
        @DisplayName("should convert entity with high confidence and structure evidence")
        void convertEntityWithStructureEvidence() {
            // Given
            NodeId nodeId = NodeId.type("com.example.OrderLine");
            Evidence structureEvidence = Evidence.fromStructure("Has identity field", List.of(nodeId));

            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "ENTITY",
                    ConfidenceLevel.HIGH,
                    "RepositoryManagedCriterion",
                    85,
                    "Type is managed by repository",
                    List.of(structureEvidence),
                    List.of());

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.typeName()).isEqualTo("com.example.OrderLine");
            assertThat(result.kind()).isEqualTo(ElementKind.ENTITY);
            assertThat(result.certainty()).isEqualTo(CertaintyLevel.CERTAIN_BY_STRUCTURE);
            assertThat(result.strategy()).isEqualTo(ClassificationStrategy.REPOSITORY);
            assertThat(result.isReliable()).isTrue();
        }

        @Test
        @DisplayName("should convert value object with medium confidence")
        void convertValueObjectWithMediumConfidence() {
            // Given
            NodeId nodeId = NodeId.type("com.example.Money");
            Evidence namingEvidence = Evidence.fromNaming("*Value", "Money");

            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "VALUE_OBJECT",
                    ConfidenceLevel.MEDIUM,
                    "RecordCriterion",
                    75,
                    "Type is a Java record",
                    List.of(namingEvidence),
                    List.of());

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.kind()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(result.certainty()).isEqualTo(CertaintyLevel.INFERRED);
            assertThat(result.strategy()).isEqualTo(ClassificationStrategy.RECORD);
            assertThat(result.isReliable()).isFalse();
            assertThat(result.needsReview()).isFalse();
        }

        @Test
        @DisplayName("should convert domain event with low confidence")
        void convertDomainEventWithLowConfidence() {
            // Given
            NodeId nodeId = NodeId.type("com.example.OrderCreated");
            Evidence packageEvidence = Evidence.fromPackage("com.example.events", "suggests domain event");

            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "DOMAIN_EVENT",
                    ConfidenceLevel.LOW,
                    "NamingPatternCriterion",
                    50,
                    "Name matches *Event pattern",
                    List.of(packageEvidence),
                    List.of());

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.kind()).isEqualTo(ElementKind.DOMAIN_EVENT);
            assertThat(result.certainty()).isEqualTo(CertaintyLevel.UNCERTAIN);
            assertThat(result.strategy()).isEqualTo(ClassificationStrategy.WEIGHTED);
            assertThat(result.isReliable()).isFalse();
            assertThat(result.needsReview()).isTrue();
        }
    }

    @Nested
    @DisplayName("Unclassified Types")
    class UnclassifiedTypes {

        @Test
        @DisplayName("should convert unclassified type")
        void convertUnclassifiedType() {
            // Given
            NodeId nodeId = NodeId.type("com.example.DTO");
            ClassificationResult coreResult = ClassificationResult.unclassified(nodeId);

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.typeName()).isEqualTo("com.example.DTO");
            assertThat(result.kind()).isNull();
            assertThat(result.certainty()).isEqualTo(CertaintyLevel.NONE);
            assertThat(result.strategy()).isEqualTo(ClassificationStrategy.UNCLASSIFIED);
            assertThat(result.isClassified()).isFalse();
            assertThat(result.needsReview()).isTrue();
        }
    }

    @Nested
    @DisplayName("Conflict Cases")
    class ConflictCases {

        @Test
        @DisplayName("should convert conflicting classification")
        void convertConflictingClassification() {
            // Given
            NodeId nodeId = NodeId.type("com.example.AmbiguousType");
            ClassificationResult coreResult = ClassificationResult.conflictDomain(
                    nodeId, List.of() // conflicts not needed for this test
                    );

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.typeName()).isEqualTo("com.example.AmbiguousType");
            assertThat(result.kind()).isNull();
            assertThat(result.certainty()).isEqualTo(CertaintyLevel.UNCERTAIN);
            assertThat(result.strategy()).isEqualTo(ClassificationStrategy.WEIGHTED);
            assertThat(result.isClassified()).isFalse();
            assertThat(result.needsReview()).isTrue();
        }
    }

    @Nested
    @DisplayName("Evidence Conversion")
    class EvidenceConversion {

        @Test
        @DisplayName("should convert all evidence types with correct weights")
        void convertAllEvidenceTypes() {
            // Given
            NodeId nodeId = NodeId.type("com.example.TestType");
            List<Evidence> evidences = List.of(
                    new Evidence(EvidenceType.ANNOTATION, "Has @Entity", List.of()),
                    new Evidence(EvidenceType.STRUCTURE, "Has ID field", List.of()),
                    new Evidence(EvidenceType.RELATIONSHIP, "Referenced by Order", List.of()),
                    new Evidence(EvidenceType.NAMING, "Name ends with Entity", List.of()),
                    new Evidence(EvidenceType.PACKAGE, "In domain package", List.of()));

            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "ENTITY",
                    ConfidenceLevel.EXPLICIT,
                    "TestCriterion",
                    100,
                    "Test classification",
                    evidences,
                    List.of());

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            PrimaryClassificationResult result = results.get(0);

            assertThat(result.evidences()).hasSize(5);
            assertThat(result.evidences())
                    .extracting("signal")
                    .containsExactly("ANNOTATION", "STRUCTURE", "RELATIONSHIP", "NAMING", "PACKAGE");
            assertThat(result.evidences()).extracting("weight").containsExactly(100, 80, 70, 50, 40);
        }

        @Test
        @DisplayName("should handle empty evidence list")
        void handleEmptyEvidenceList() {
            // Given
            NodeId nodeId = NodeId.type("com.example.SimpleType");
            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "VALUE_OBJECT",
                    ConfidenceLevel.HIGH,
                    "SimpleCriterion",
                    80,
                    "Simple classification",
                    List.of(), // no evidence
                    List.of());

            // When
            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).evidences()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Strategy Derivation")
    class StrategyDerivation {

        @Test
        @DisplayName("should derive ANNOTATION strategy from criteria name")
        void deriveAnnotationStrategy() {
            assertStrategyDerivation("HexaGlueAnnotationCriterion", ClassificationStrategy.ANNOTATION);
            assertStrategyDerivation("@AggregateRootCriterion", ClassificationStrategy.ANNOTATION);
        }

        @Test
        @DisplayName("should derive REPOSITORY strategy from criteria name")
        void deriveRepositoryStrategy() {
            assertStrategyDerivation("RepositoryManagedCriterion", ClassificationStrategy.REPOSITORY);
            assertStrategyDerivation("RepositoryPatternCriterion", ClassificationStrategy.REPOSITORY);
        }

        @Test
        @DisplayName("should derive RECORD strategy from criteria name")
        void deriveRecordStrategy() {
            assertStrategyDerivation("RecordCriterion", ClassificationStrategy.RECORD);
            assertStrategyDerivation("JavaRecordHeuristic", ClassificationStrategy.RECORD);
        }

        @Test
        @DisplayName("should derive COMPOSITION strategy from criteria name")
        void deriveCompositionStrategy() {
            assertStrategyDerivation("CompositionCriterion", ClassificationStrategy.COMPOSITION);
            assertStrategyDerivation("RelationshipCriterion", ClassificationStrategy.COMPOSITION);
            assertStrategyDerivation("EmbeddedTypeCriterion", ClassificationStrategy.COMPOSITION);
        }

        @Test
        @DisplayName("should derive WEIGHTED strategy for unknown criteria")
        void deriveWeightedStrategyForUnknown() {
            assertStrategyDerivation("SomeOtherCriterion", ClassificationStrategy.WEIGHTED);
            assertStrategyDerivation("CustomHeuristic", ClassificationStrategy.WEIGHTED);
        }

        private void assertStrategyDerivation(String criteriaName, ClassificationStrategy expectedStrategy) {
            NodeId nodeId = NodeId.type("com.example.TestType");
            ClassificationResult coreResult = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "ENTITY",
                    ConfidenceLevel.HIGH,
                    criteriaName,
                    85,
                    "Test",
                    List.of(),
                    List.of());

            List<PrimaryClassificationResult> results = exporter.exportPrimaryClassifications(List.of(coreResult));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).strategy()).isEqualTo(expectedStrategy);
        }
    }

    @Nested
    @DisplayName("Filtering and Sorting")
    class FilteringAndSorting {

        @Test
        @DisplayName("should filter out port classifications")
        void filterOutPortClassifications() {
            // Given
            ClassificationResult domainResult = ClassificationResult.classified(
                    NodeId.type("com.example.Order"),
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    ConfidenceLevel.EXPLICIT,
                    "TestCriterion",
                    100,
                    "Test",
                    List.of(),
                    List.of());

            ClassificationResult portResult = ClassificationResult.classifiedPort(
                    NodeId.type("com.example.OrderRepository"),
                    "REPOSITORY",
                    ConfidenceLevel.HIGH,
                    "TestCriterion",
                    85,
                    "Test",
                    List.of(),
                    List.of(),
                    io.hexaglue.core.classification.port.PortDirection.DRIVEN);

            // When
            List<PrimaryClassificationResult> results =
                    exporter.exportPrimaryClassifications(List.of(domainResult, portResult));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).typeName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("should sort results by type name")
        void sortResultsByTypeName() {
            // Given
            ClassificationResult result1 = createDomainResult("com.example.ZType");
            ClassificationResult result2 = createDomainResult("com.example.AType");
            ClassificationResult result3 = createDomainResult("com.example.MType");

            // When
            List<PrimaryClassificationResult> results =
                    exporter.exportPrimaryClassifications(List.of(result1, result2, result3));

            // Then
            assertThat(results).hasSize(3);
            assertThat(results)
                    .extracting(PrimaryClassificationResult::typeName)
                    .containsExactly("com.example.AType", "com.example.MType", "com.example.ZType");
        }

        private ClassificationResult createDomainResult(String typeName) {
            return ClassificationResult.classified(
                    NodeId.type(typeName),
                    ClassificationTarget.DOMAIN,
                    "VALUE_OBJECT",
                    ConfidenceLevel.HIGH,
                    "TestCriterion",
                    80,
                    "Test",
                    List.of(),
                    List.of());
        }
    }
}
