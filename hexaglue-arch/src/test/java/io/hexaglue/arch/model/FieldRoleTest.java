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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link FieldRole}.
 *
 * @since 4.1.0
 */
@DisplayName("FieldRole")
class FieldRoleTest {

    @Nested
    @DisplayName("Business Relevance")
    class BusinessRelevance {

        @ParameterizedTest
        @EnumSource(
                value = FieldRole.class,
                names = {"IDENTITY", "COLLECTION", "AGGREGATE_REFERENCE", "EMBEDDED"})
        @DisplayName("should identify business-relevant roles")
        void shouldIdentifyBusinessRelevantRoles(FieldRole role) {
            assertThat(role.isBusinessRelevant()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = FieldRole.class,
                names = {"AUDIT", "TECHNICAL"})
        @DisplayName("should identify non-business roles")
        void shouldIdentifyNonBusinessRoles(FieldRole role) {
            assertThat(role.isBusinessRelevant()).isFalse();
        }
    }

    @Nested
    @DisplayName("Completeness")
    class Completeness {

        @Test
        @DisplayName("should have exactly 6 values")
        void shouldHaveExactNumberOfValues() {
            // IDENTITY, COLLECTION, AGGREGATE_REFERENCE, EMBEDDED, AUDIT, TECHNICAL
            assertThat(FieldRole.values()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("Categories")
    class Categories {

        @Test
        @DisplayName("IDENTITY should be business-relevant")
        void identityShouldBeBusinessRelevant() {
            assertThat(FieldRole.IDENTITY.isBusinessRelevant()).isTrue();
        }

        @Test
        @DisplayName("COLLECTION should be business-relevant")
        void collectionShouldBeBusinessRelevant() {
            assertThat(FieldRole.COLLECTION.isBusinessRelevant()).isTrue();
        }

        @Test
        @DisplayName("AGGREGATE_REFERENCE should be business-relevant")
        void aggregateReferenceShouldBeBusinessRelevant() {
            assertThat(FieldRole.AGGREGATE_REFERENCE.isBusinessRelevant()).isTrue();
        }

        @Test
        @DisplayName("EMBEDDED should be business-relevant")
        void embeddedShouldBeBusinessRelevant() {
            assertThat(FieldRole.EMBEDDED.isBusinessRelevant()).isTrue();
        }

        @Test
        @DisplayName("AUDIT should not be business-relevant")
        void auditShouldNotBeBusinessRelevant() {
            assertThat(FieldRole.AUDIT.isBusinessRelevant()).isFalse();
        }

        @Test
        @DisplayName("TECHNICAL should not be business-relevant")
        void technicalShouldNotBeBusinessRelevant() {
            assertThat(FieldRole.TECHNICAL.isBusinessRelevant()).isFalse();
        }
    }
}
