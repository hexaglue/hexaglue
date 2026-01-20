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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Invariant}.
 *
 * @since 4.1.0
 */
@DisplayName("Invariant")
class InvariantTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with valid name and description")
        void shouldCreateWithValidNameAndDescription() {
            // given
            String name = "orderMustHaveItems";
            String description = "An order must have at least one item";

            // when
            Invariant invariant = Invariant.of(name, description);

            // then
            assertThat(invariant.name()).isEqualTo(name);
            assertThat(invariant.description()).isEqualTo(description);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> Invariant.of(null, "description"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject null description")
        void shouldRejectNullDescription() {
            assertThatThrownBy(() -> Invariant.of("name", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> Invariant.of("  ", "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject blank description")
        void shouldRejectBlankDescription() {
            assertThatThrownBy(() -> Invariant.of("name", "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should reject empty name")
        void shouldRejectEmptyName() {
            assertThatThrownBy(() -> Invariant.of("", "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject empty description")
        void shouldRejectEmptyDescription() {
            assertThatThrownBy(() -> Invariant.of("name", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when name and description match")
        void shouldBeEqualWhenNameAndDescriptionMatch() {
            // given
            Invariant inv1 = Invariant.of("name", "description");
            Invariant inv2 = Invariant.of("name", "description");

            // then
            assertThat(inv1).isEqualTo(inv2);
            assertThat(inv1.hashCode()).isEqualTo(inv2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            // given
            Invariant inv1 = Invariant.of("name1", "description");
            Invariant inv2 = Invariant.of("name2", "description");

            // then
            assertThat(inv1).isNotEqualTo(inv2);
        }

        @Test
        @DisplayName("should not be equal when descriptions differ")
        void shouldNotBeEqualWhenDescriptionsDiffer() {
            // given
            Invariant inv1 = Invariant.of("name", "description1");
            Invariant inv2 = Invariant.of("name", "description2");

            // then
            assertThat(inv1).isNotEqualTo(inv2);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should be immutable record")
        void shouldBeImmutableRecord() {
            // given
            String originalName = "invariantName";
            String originalDescription = "Original description";
            Invariant invariant = Invariant.of(originalName, originalDescription);

            // then - values cannot be modified (record is inherently immutable)
            assertThat(invariant.name()).isEqualTo(originalName);
            assertThat(invariant.description()).isEqualTo(originalDescription);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringBehavior {

        @Test
        @DisplayName("should include name and description")
        void shouldIncludeNameAndDescription() {
            // given
            Invariant invariant = Invariant.of("myInvariant", "My invariant description");

            // when
            String str = invariant.toString();

            // then
            assertThat(str).contains("myInvariant");
            assertThat(str).contains("My invariant description");
        }
    }
}
