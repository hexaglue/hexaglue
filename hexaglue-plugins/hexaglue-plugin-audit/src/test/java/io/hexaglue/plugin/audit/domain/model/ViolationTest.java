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

package io.hexaglue.plugin.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.audit.SourceLocation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Violation}.
 *
 * <p>Validates the builder pattern, required fields, optional fields, and immutability.
 */
class ViolationTest {

    private static final ConstraintId TEST_CONSTRAINT = ConstraintId.of("test:constraint");

    @Test
    @DisplayName("Should build violation with all fields")
    void shouldBuildViolationWithAllFields() {
        // Given
        Evidence evidence = StructuralEvidence.of("Test evidence", "com.example.Order");

        // When
        Violation violation = Violation.builder(TEST_CONSTRAINT)
                .severity(Severity.CRITICAL)
                .message("Test violation message")
                .affectedType("com.example.Order")
                .location(SourceLocation.of("Order.java", 10, 5))
                .evidence(evidence)
                .build();

        // Then
        assertThat(violation.constraintId()).isEqualTo(TEST_CONSTRAINT);
        assertThat(violation.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violation.message()).isEqualTo("Test violation message");
        assertThat(violation.affectedTypes()).containsExactly("com.example.Order");
        assertThat(violation.location().filePath()).isEqualTo("Order.java");
        assertThat(violation.evidence()).containsExactly(evidence);
    }

    @Test
    @DisplayName("Should build violation with minimal required fields")
    void shouldBuildViolationWithMinimalRequiredFields() {
        // When
        Violation violation =
                Violation.builder(TEST_CONSTRAINT).message("Required message").build();

        // Then
        assertThat(violation.constraintId()).isEqualTo(TEST_CONSTRAINT);
        assertThat(violation.message()).isEqualTo("Required message");
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR); // Default
        assertThat(violation.affectedTypes()).isEmpty();
        assertThat(violation.evidence()).isEmpty();
    }

    @Test
    @DisplayName("Should require constraint ID")
    void shouldRequireConstraintId() {
        // When/Then
        assertThatThrownBy(() -> Violation.builder(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("constraintId required");
    }

    @Test
    @DisplayName("Should require message")
    void shouldRequireMessage() {
        // When/Then
        assertThatThrownBy(() -> Violation.builder(TEST_CONSTRAINT).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("message must be set");
    }

    @Test
    @DisplayName("Should reject blank message")
    void shouldRejectBlankMessage() {
        // When/Then
        assertThatThrownBy(
                        () -> Violation.builder(TEST_CONSTRAINT).message("   ").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("message must be set");
    }

    @Test
    @DisplayName("Should add multiple affected types")
    void shouldAddMultipleAffectedTypes() {
        // When
        Violation violation = Violation.builder(TEST_CONSTRAINT)
                .message("Test message")
                .affectedType("com.example.Order")
                .affectedType("com.example.Customer")
                .affectedTypes(List.of("com.example.Product", "com.example.Invoice"))
                .build();

        // Then
        assertThat(violation.affectedTypes())
                .containsExactly(
                        "com.example.Order", "com.example.Customer", "com.example.Product", "com.example.Invoice");
    }

    @Test
    @DisplayName("Should add multiple evidence items")
    void shouldAddMultipleEvidenceItems() {
        // Given
        Evidence evidence1 = StructuralEvidence.of("Evidence 1", "com.example.Type1");
        Evidence evidence2 = BehavioralEvidence.of("Evidence 2", "com.example.Type2", "method");
        Evidence evidence3 = RelationshipEvidence.of("Evidence 3", List.of("Type3", "Type4"));

        // When
        Violation violation = Violation.builder(TEST_CONSTRAINT)
                .message("Test message")
                .evidence(evidence1)
                .evidence(List.of(evidence2, evidence3))
                .build();

        // Then
        assertThat(violation.evidence()).containsExactly(evidence1, evidence2, evidence3);
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        // Given
        Violation violation = Violation.builder(TEST_CONSTRAINT)
                .message("Test message")
                .affectedType("com.example.Order")
                .build();

        // When/Then: Lists should be immutable
        assertThatThrownBy(() -> violation.affectedTypes().add("NewType"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> violation.evidence().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should set default severity to MAJOR")
    void shouldSetDefaultSeverityToMajor() {
        // When
        Violation violation =
                Violation.builder(TEST_CONSTRAINT).message("Test message").build();

        // Then
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    @DisplayName("Should set default location when not specified")
    void shouldSetDefaultLocationWhenNotSpecified() {
        // When
        Violation violation =
                Violation.builder(TEST_CONSTRAINT).message("Test message").build();

        // Then
        assertThat(violation.location()).isNotNull();
        assertThat(violation.location().filePath()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should override default severity")
    void shouldOverrideDefaultSeverity() {
        // When
        Violation violation = Violation.builder(TEST_CONSTRAINT)
                .severity(Severity.BLOCKER)
                .message("Test message")
                .build();

        // Then
        assertThat(violation.severity()).isEqualTo(Severity.BLOCKER);
    }

    @Test
    @DisplayName("Should create immutable copies of collections")
    void shouldCreateImmutableCopiesOfCollections() {
        // Given
        List<String> affectedTypes = new java.util.ArrayList<>();
        affectedTypes.add("com.example.Order");

        List<Evidence> evidence = new java.util.ArrayList<>();
        evidence.add(StructuralEvidence.of("Test", "Type"));

        // When
        Violation violation = Violation.builder(TEST_CONSTRAINT)
                .message("Test message")
                .affectedTypes(affectedTypes)
                .evidence(evidence)
                .build();

        // Modify original lists
        affectedTypes.add("com.example.NewType");
        evidence.add(StructuralEvidence.of("New", "Type"));

        // Then: Violation should not be affected
        assertThat(violation.affectedTypes()).hasSize(1);
        assertThat(violation.evidence()).hasSize(1);
    }
}
