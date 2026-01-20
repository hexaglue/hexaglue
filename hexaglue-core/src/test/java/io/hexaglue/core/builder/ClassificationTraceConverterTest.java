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

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.graph.model.NodeId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClassificationTraceConverter}.
 *
 * @since 4.1.0
 */
@DisplayName("ClassificationTraceConverter")
class ClassificationTraceConverterTest {

    private ClassificationTraceConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ClassificationTraceConverter();
    }

    @Nested
    @DisplayName("Convert Successful Classification")
    class ConvertSuccessfulClassification {

        @Test
        @DisplayName("should convert HIGH confidence AGGREGATE_ROOT")
        void shouldConvertHighConfidenceAggregateRoot() {
            ClassificationResult result = createClassifiedResult(
                    "AGGREGATE_ROOT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "repository-primary-type",
                    85,
                    "Type is primary managed type in repository");

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(trace.winningCriterion().name()).isEqualTo("repository-primary-type");
            assertThat(trace.winningCriterion().priority()).isEqualTo(85);
        }

        @Test
        @DisplayName("should convert EXPLICIT confidence as HIGH")
        void shouldConvertExplicitConfidenceAsHigh() {
            ClassificationResult result = createClassifiedResult(
                    "ENTITY",
                    io.hexaglue.core.classification.ConfidenceLevel.EXPLICIT,
                    "explicit-entity-annotation",
                    100,
                    "Type has @Entity annotation");

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.ENTITY);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should convert MEDIUM confidence")
        void shouldConvertMediumConfidence() {
            ClassificationResult result = createClassifiedResult(
                    "VALUE_OBJECT",
                    io.hexaglue.core.classification.ConfidenceLevel.MEDIUM,
                    "naming-pattern",
                    70,
                    "Type ends with VO suffix");

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        @DisplayName("should convert LOW confidence")
        void shouldConvertLowConfidence() {
            ClassificationResult result = createClassifiedResult(
                    "DOMAIN_SERVICE",
                    io.hexaglue.core.classification.ConfidenceLevel.LOW,
                    "weak-heuristic",
                    60,
                    "Weak match");

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DOMAIN_SERVICE);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }
    }

    @Nested
    @DisplayName("Convert Port Classifications")
    class ConvertPortClassifications {

        @Test
        @DisplayName("should convert DRIVEN_PORT")
        void shouldConvertDrivenPort() {
            ClassificationResult result = createClassifiedResult(
                    "DRIVEN_PORT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "repository-suffix",
                    80,
                    "Type ends with Repository");

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVEN_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should convert DRIVING_PORT")
        void shouldConvertDrivingPort() {
            ClassificationResult result = createClassifiedResult(
                    "DRIVING_PORT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "in-package-location",
                    75,
                    "Type is in ports.in package");

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
        }
    }

    @Nested
    @DisplayName("Convert Unclassified")
    class ConvertUnclassified {

        @Test
        @DisplayName("should convert UNCLASSIFIED result")
        void shouldConvertUnclassifiedResult() {
            NodeId nodeId = NodeId.type("com.example.SomeClass");
            ClassificationResult result = ClassificationResult.unclassifiedDomain(nodeId, null);

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }
    }

    @Nested
    @DisplayName("Convert With Evidence")
    class ConvertWithEvidence {

        @Test
        @DisplayName("should include evidence in evaluated criteria")
        void shouldIncludeEvidenceInEvaluatedCriteria() {
            NodeId nodeId = NodeId.type("com.example.Order");
            ClassificationResult result = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test justification",
                    List.of(Evidence.fromAnnotation("AggregateRoot", nodeId), Evidence.fromNaming("*Order", "Order")),
                    List.of());

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.evaluatedCriteria()).hasSizeGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Convert With Conflicts")
    class ConvertWithConflicts {

        @Test
        @DisplayName("should include conflicts in trace")
        void shouldIncludeConflictsInTrace() {
            NodeId nodeId = NodeId.type("com.example.Order");
            ClassificationResult result = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "repository-primary-type",
                    85,
                    "Test justification",
                    List.of(),
                    List.of(Conflict.warning(
                            "ENTITY",
                            "has-identity-field",
                            io.hexaglue.core.classification.ConfidenceLevel.MEDIUM,
                            75,
                            "Also matched as ENTITY")));

            ClassificationTrace trace = converter.convert(result);

            assertThat(trace.conflicts()).isNotEmpty();
            assertThat(trace.conflicts().get(0).alternativeKind()).isEqualTo(ElementKind.ENTITY);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when result is null")
        void shouldThrowWhenResultIsNull() {
            assertThatThrownBy(() -> converter.convert(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classificationResult");
        }
    }

    // Helper methods

    private ClassificationResult createClassifiedResult(
            String kind,
            io.hexaglue.core.classification.ConfidenceLevel confidence,
            String criteriaName,
            int priority,
            String justification) {
        NodeId nodeId = NodeId.type("com.example.TestType");
        return ClassificationResult.classified(
                nodeId,
                ClassificationTarget.DOMAIN,
                kind,
                confidence,
                criteriaName,
                priority,
                justification,
                List.of(),
                List.of());
    }
}
