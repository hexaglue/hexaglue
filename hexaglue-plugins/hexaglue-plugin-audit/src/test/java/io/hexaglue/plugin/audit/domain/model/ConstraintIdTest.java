/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConstraintId}.
 *
 * <p>Validates the value object behavior: creation, validation, equality, and factory methods.
 */
class ConstraintIdTest {

    @Test
    @DisplayName("Should create valid constraint ID")
    void shouldCreateValidConstraintId() {
        // When
        ConstraintId id = new ConstraintId("ddd:entity-identity");

        // Then
        assertThat(id.value()).isEqualTo("ddd:entity-identity");
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        // When/Then
        assertThatThrownBy(() -> new ConstraintId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("constraint id required");
    }

    @Test
    @DisplayName("Should reject blank value")
    void shouldRejectBlankValue() {
        // When/Then
        assertThatThrownBy(() -> new ConstraintId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("constraint id cannot be blank");

        assertThatThrownBy(() -> new ConstraintId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("constraint id cannot be blank");
    }

    @Test
    @DisplayName("Should be equal by value")
    void shouldBeEqualByValue() {
        // Given
        ConstraintId id1 = new ConstraintId("ddd:aggregate-cycle");
        ConstraintId id2 = new ConstraintId("ddd:aggregate-cycle");
        ConstraintId id3 = new ConstraintId("ddd:entity-identity");

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).hasSameHashCodeAs(id2);
        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    @DisplayName("Should create using factory method")
    void shouldCreateUsingFactoryMethod() {
        // When
        ConstraintId id = ConstraintId.of("hexagonal:dependency-direction");

        // Then
        assertThat(id.value()).isEqualTo("hexagonal:dependency-direction");
    }

    @Test
    @DisplayName("Should extract category from hierarchical ID")
    void shouldExtractCategoryFromHierarchicalId() {
        // Given
        ConstraintId id = new ConstraintId("ddd:aggregate-repository");

        // When
        String category = id.category();

        // Then
        assertThat(category).isEqualTo("ddd");
    }

    @Test
    @DisplayName("Should extract name from hierarchical ID")
    void shouldExtractNameFromHierarchicalId() {
        // Given
        ConstraintId id = new ConstraintId("ddd:aggregate-repository");

        // When
        String name = id.name();

        // Then
        assertThat(name).isEqualTo("aggregate-repository");
    }

    @Test
    @DisplayName("Should return full value when no colon present")
    void shouldReturnFullValueWhenNoColonPresent() {
        // Given
        ConstraintId id = new ConstraintId("simple-constraint");

        // When/Then
        assertThat(id.category()).isEqualTo("simple-constraint");
        assertThat(id.name()).isEqualTo("simple-constraint");
    }

    @Test
    @DisplayName("Should handle multiple colons correctly")
    void shouldHandleMultipleColonsCorrectly() {
        // Given
        ConstraintId id = new ConstraintId("category:sub:constraint-name");

        // When/Then
        assertThat(id.category()).isEqualTo("category");
        assertThat(id.name()).isEqualTo("sub:constraint-name");
    }
}
