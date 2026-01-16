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

package io.hexaglue.core.classification.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.classification.port.PortDirection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Contribution}.
 */
@DisplayName("Contribution")
class ContributionTest {

    // =========================================================================
    // Factory Methods
    // =========================================================================

    @Nested
    @DisplayName("Factory method of()")
    class FactoryMethodTest {

        @Test
        @DisplayName("should create contribution with minimal parameters")
        void shouldCreateWithMinimalParameters() {
            Contribution<ElementKind> contribution = Contribution.of(
                    ElementKind.ENTITY, "test-criteria", 80, ConfidenceLevel.HIGH, "Test justification");

            assertThat(contribution.kind()).isEqualTo(ElementKind.ENTITY);
            assertThat(contribution.criteriaName()).isEqualTo("test-criteria");
            assertThat(contribution.priority()).isEqualTo(80);
            assertThat(contribution.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(contribution.justification()).isEqualTo("Test justification");
            assertThat(contribution.evidence()).isEmpty();
            assertThat(contribution.metadata()).isEmpty();
        }

        @Test
        @DisplayName("should create contribution with evidence")
        void shouldCreateWithEvidence() {
            Evidence evidence = new Evidence(EvidenceType.ANNOTATION, "Has @Entity", List.of());
            Contribution<ElementKind> contribution = Contribution.of(
                    ElementKind.ENTITY, "test-criteria", 100, ConfidenceLevel.EXPLICIT, "Annotated", List.of(evidence));

            assertThat(contribution.evidence()).hasSize(1);
            assertThat(contribution.evidence().get(0).description()).isEqualTo("Has @Entity");
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Nested
    @DisplayName("Validation")
    class ValidationTest {

        @Test
        @DisplayName("should reject null kind")
        void shouldRejectNullKind() {
            assertThatThrownBy(() -> Contribution.of(null, "criteria", 50, ConfidenceLevel.MEDIUM, "test"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("kind");
        }

        @Test
        @DisplayName("should reject null criteria name")
        void shouldRejectNullCriteriaName() {
            assertThatThrownBy(() -> Contribution.of(ElementKind.ENTITY, null, 50, ConfidenceLevel.MEDIUM, "test"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("criteriaName");
        }

        @Test
        @DisplayName("should reject null confidence")
        void shouldRejectNullConfidence() {
            assertThatThrownBy(() -> Contribution.of(ElementKind.ENTITY, "criteria", 50, null, "test"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("confidence");
        }

        @Test
        @DisplayName("should accept null evidence and convert to empty list")
        void shouldAcceptNullEvidence() {
            Contribution<ElementKind> contribution =
                    new Contribution<>(ElementKind.ENTITY, "test", 50, ConfidenceLevel.LOW, "test", null, Map.of());

            assertThat(contribution.evidence()).isEmpty();
        }

        @Test
        @DisplayName("should accept null metadata and convert to empty map")
        void shouldAcceptNullMetadata() {
            Contribution<ElementKind> contribution =
                    new Contribution<>(ElementKind.ENTITY, "test", 50, ConfidenceLevel.LOW, "test", List.of(), null);

            assertThat(contribution.metadata()).isEmpty();
        }
    }

    // =========================================================================
    // Immutability
    // =========================================================================

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTest {

        @Test
        @DisplayName("evidence list should be immutable")
        void evidenceListShouldBeImmutable() {
            Evidence evidence = new Evidence(EvidenceType.ANNOTATION, "test", List.of());
            Contribution<ElementKind> contribution = Contribution.of(
                    ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test", List.of(evidence));

            assertThatThrownBy(() -> contribution.evidence().add(evidence))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("metadata map should be immutable")
        void metadataMapShouldBeImmutable() {
            Contribution<ElementKind> contribution = Contribution.of(
                            ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test")
                    .withMetadata("key", "value");

            assertThatThrownBy(() -> contribution.metadata().put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("withMetadata should return new instance")
        void withMetadataShouldReturnNewInstance() {
            Contribution<ElementKind> original =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            Contribution<ElementKind> withMeta = original.withMetadata("direction", PortDirection.DRIVEN);

            assertThat(withMeta).isNotSameAs(original);
            assertThat(original.metadata()).isEmpty();
            assertThat(withMeta.metadata()).containsEntry("direction", PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("withEvidence should return new instance")
        void withEvidenceShouldReturnNewInstance() {
            Contribution<ElementKind> original =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            Evidence newEvidence = new Evidence(EvidenceType.STRUCTURE, "Has id field", List.of());
            Contribution<ElementKind> withEvidence = original.withEvidence(List.of(newEvidence));

            assertThat(withEvidence).isNotSameAs(original);
            assertThat(original.evidence()).isEmpty();
            assertThat(withEvidence.evidence()).hasSize(1);
        }
    }

    // =========================================================================
    // Metadata Operations
    // =========================================================================

    @Nested
    @DisplayName("Metadata operations")
    class MetadataOperationsTest {

        @Test
        @DisplayName("withMetadata should preserve existing metadata")
        void withMetadataShouldPreserveExisting() {
            Contribution<ElementKind> contribution = Contribution.of(
                            ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test")
                    .withMetadata("key1", "value1")
                    .withMetadata("key2", "value2");

            assertThat(contribution.metadata()).containsEntry("key1", "value1").containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("withMetadata should reject null key")
        void withMetadataShouldRejectNullKey() {
            Contribution<ElementKind> contribution =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            assertThatThrownBy(() -> contribution.withMetadata(null, "value"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("key");
        }

        @Test
        @DisplayName("withMetadata should reject null value")
        void withMetadataShouldRejectNullValue() {
            Contribution<ElementKind> contribution =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            assertThatThrownBy(() -> contribution.withMetadata("key", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("value");
        }

        @Test
        @DisplayName("metadata(key, type) should return typed value")
        void metadataShouldReturnTypedValue() {
            Contribution<ElementKind> contribution = Contribution.of(
                            ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test")
                    .withMetadata("direction", PortDirection.DRIVEN);

            assertThat(contribution.metadata("direction", PortDirection.class))
                    .isPresent()
                    .contains(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("metadata(key, type) should return empty for wrong type")
        void metadataShouldReturnEmptyForWrongType() {
            Contribution<ElementKind> contribution = Contribution.of(
                            ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test")
                    .withMetadata("direction", "DRIVEN");

            assertThat(contribution.metadata("direction", PortDirection.class)).isEmpty();
        }

        @Test
        @DisplayName("metadata(key, type) should return empty for missing key")
        void metadataShouldReturnEmptyForMissingKey() {
            Contribution<ElementKind> contribution =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            assertThat(contribution.metadata("missing", String.class)).isEmpty();
        }

        @Test
        @DisplayName("hasMetadata should return true for existing key")
        void hasMetadataShouldReturnTrueForExistingKey() {
            Contribution<ElementKind> contribution = Contribution.of(
                            ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test")
                    .withMetadata("direction", PortDirection.DRIVING);

            assertThat(contribution.hasMetadata("direction")).isTrue();
        }

        @Test
        @DisplayName("hasMetadata should return false for missing key")
        void hasMetadataShouldReturnFalseForMissingKey() {
            Contribution<ElementKind> contribution =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            assertThat(contribution.hasMetadata("direction")).isFalse();
        }
    }

    // =========================================================================
    // Evidence Operations
    // =========================================================================

    @Nested
    @DisplayName("Evidence operations")
    class EvidenceOperationsTest {

        @Test
        @DisplayName("withEvidence should add to existing evidence")
        void withEvidenceShouldAddToExisting() {
            Evidence evidence1 = new Evidence(EvidenceType.ANNOTATION, "Has @Entity", List.of());
            Contribution<ElementKind> contribution = Contribution.of(
                    ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test", List.of(evidence1));

            Evidence evidence2 = new Evidence(EvidenceType.STRUCTURE, "Has id field", List.of());
            Contribution<ElementKind> withMore = contribution.withEvidence(List.of(evidence2));

            assertThat(withMore.evidence()).hasSize(2);
        }

        @Test
        @DisplayName("withEvidence should return same instance for null list")
        void withEvidenceShouldReturnSameForNull() {
            Contribution<ElementKind> contribution =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            Contribution<ElementKind> result = contribution.withEvidence(null);

            assertThat(result).isSameAs(contribution);
        }

        @Test
        @DisplayName("withEvidence should return same instance for empty list")
        void withEvidenceShouldReturnSameForEmpty() {
            Contribution<ElementKind> contribution =
                    Contribution.of(ElementKind.ENTITY, "criteria", 80, ConfidenceLevel.HIGH, "test");

            Contribution<ElementKind> result = contribution.withEvidence(List.of());

            assertThat(result).isSameAs(contribution);
        }
    }

    // =========================================================================
    // Generic Type Usage
    // =========================================================================

    @Nested
    @DisplayName("Generic type usage")
    class GenericTypeUsageTest {

        @Test
        @DisplayName("should work with string enum-like kinds")
        void shouldWorkWithStringKinds() {
            // Test that Contribution<K> works with any type, not just enums
            Contribution<String> contribution =
                    Contribution.of("CUSTOM_KIND", "test-criteria", 50, ConfidenceLevel.MEDIUM, "Custom kind test");

            assertThat(contribution.kind()).isEqualTo("CUSTOM_KIND");
        }

        @Test
        @DisplayName("should work with ElementKind")
        void shouldWorkWithElementKind() {
            Contribution<ElementKind> contribution = Contribution.of(
                    ElementKind.AGGREGATE_ROOT, "explicit-ar", 100, ConfidenceLevel.EXPLICIT, "Has @AggregateRoot");

            assertThat(contribution.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
        }
    }
}
