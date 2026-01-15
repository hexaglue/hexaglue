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

package io.hexaglue.arch.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.Evidence;
import io.hexaglue.arch.EvidenceType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CriterionMatch")
class CriterionMatchTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTest {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllFields() {
            // given
            Evidence evidence = Evidence.of(EvidenceType.ANNOTATION, "@AggregateRoot annotation found");

            // when
            CriterionMatch match =
                    new CriterionMatch("Type has @AggregateRoot annotation", ConfidenceLevel.HIGH, List.of(evidence));

            // then
            assertThat(match.justification()).isEqualTo("Type has @AggregateRoot annotation");
            assertThat(match.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(match.evidence()).hasSize(1);
            assertThat(match.evidence().get(0)).isEqualTo(evidence);
        }

        @Test
        @DisplayName("should make evidence list immutable")
        void shouldMakeEvidenceImmutable() {
            // given
            Evidence evidence = Evidence.of(EvidenceType.ANNOTATION, "Test");
            CriterionMatch match = new CriterionMatch("Justification", ConfidenceLevel.HIGH, List.of(evidence));

            // then
            assertThatThrownBy(() -> match.evidence().add(evidence)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should reject null justification")
        void shouldRejectNullJustification() {
            assertThatThrownBy(() -> new CriterionMatch(null, ConfidenceLevel.HIGH, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("justification");
        }

        @Test
        @DisplayName("should reject blank justification")
        void shouldRejectBlankJustification() {
            assertThatThrownBy(() -> new CriterionMatch("  ", ConfidenceLevel.HIGH, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("justification");
        }

        @Test
        @DisplayName("should reject null confidence")
        void shouldRejectNullConfidence() {
            assertThatThrownBy(() -> new CriterionMatch("Test", null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("confidence");
        }

        @Test
        @DisplayName("should reject null evidence")
        void shouldRejectNullEvidence() {
            assertThatThrownBy(() -> new CriterionMatch("Test", ConfidenceLevel.HIGH, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("evidence");
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("should create with single evidence using of()")
        void shouldCreateWithSingleEvidence() {
            // given
            Evidence evidence = Evidence.of(EvidenceType.ANNOTATION, "Test evidence");

            // when
            CriterionMatch match = CriterionMatch.of("Test justification", ConfidenceLevel.MEDIUM, evidence);

            // then
            assertThat(match.justification()).isEqualTo("Test justification");
            assertThat(match.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(match.evidence()).hasSize(1);
        }

        @Test
        @DisplayName("should create with multiple evidence using of()")
        void shouldCreateWithMultipleEvidence() {
            // given
            Evidence e1 = Evidence.of(EvidenceType.ANNOTATION, "Evidence 1");
            Evidence e2 = Evidence.of(EvidenceType.NAMING, "Evidence 2");

            // when
            CriterionMatch match = CriterionMatch.of("Test justification", ConfidenceLevel.HIGH, List.of(e1, e2));

            // then
            assertThat(match.evidence()).hasSize(2);
        }

        @Test
        @DisplayName("should create HIGH confidence match")
        void shouldCreateHighConfidenceMatch() {
            // given
            Evidence evidence = Evidence.of(EvidenceType.ANNOTATION, "Test");

            // when
            CriterionMatch match = CriterionMatch.high("High confidence reason", evidence);

            // then
            assertThat(match.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(match.justification()).isEqualTo("High confidence reason");
        }

        @Test
        @DisplayName("should create MEDIUM confidence match")
        void shouldCreateMediumConfidenceMatch() {
            // given
            Evidence evidence = Evidence.of(EvidenceType.STRUCTURE, "Test");

            // when
            CriterionMatch match = CriterionMatch.medium("Medium confidence reason", evidence);

            // then
            assertThat(match.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        @DisplayName("should create LOW confidence match")
        void shouldCreateLowConfidenceMatch() {
            // given
            Evidence evidence = Evidence.of(EvidenceType.NAMING, "Test");

            // when
            CriterionMatch match = CriterionMatch.low("Low confidence reason", evidence);

            // then
            assertThat(match.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }
    }
}
