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

package io.hexaglue.plugin.jpa.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BidirectionalDetector}.
 *
 * <p>BUG-003: This class validates automatic detection of bidirectional relationships.
 *
 * @since 5.0.0
 */
@DisplayName("BidirectionalDetector")
class BidirectionalDetectorTest {

    @Nested
    @DisplayName("getMappedBy helper")
    class GetMappedByTests {

        @Test
        @DisplayName("Should return mappedBy value when mapping exists")
        void shouldReturnMappedByWhenExists() {
            // Given
            Map<String, String> mappings = Map.of("com.example.Order#lines", "order");

            // When/Then
            assertThat(BidirectionalDetector.getMappedBy(mappings, "com.example.Order", "lines"))
                    .isEqualTo("order");
        }

        @Test
        @DisplayName("Should return null when mapping does not exist")
        void shouldReturnNullWhenNotExists() {
            // Given
            Map<String, String> mappings = Map.of("com.example.Order#lines", "order");

            // When/Then
            assertThat(BidirectionalDetector.getMappedBy(mappings, "com.example.Other", "field"))
                    .isNull();
        }

        @Test
        @DisplayName("Should handle empty mappings")
        void shouldHandleEmptyMappings() {
            // Given
            Map<String, String> mappings = Map.of();

            // When/Then
            assertThat(BidirectionalDetector.getMappedBy(mappings, "com.example.Order", "lines"))
                    .isNull();
        }
    }
}
