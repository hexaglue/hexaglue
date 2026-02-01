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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link SpringDataQuerySupport}.
 *
 * <p>Tests validate detection of Spring Data embedded condition suffixes
 * in query method names (e.g., {@code findByActiveTrue}, {@code existsByDeletedFalse}).
 *
 * @since 5.0.0
 */
@DisplayName("SpringDataQuerySupport")
class SpringDataQuerySupportTest {

    @Nested
    @DisplayName("hasEmbeddedConditionSuffix()")
    class HasEmbeddedConditionSuffix {

        @ParameterizedTest
        @DisplayName("should return true for methods ending with embedded condition suffixes")
        @ValueSource(
                strings = {
                    "findByActiveTrue",
                    "findByActiveFalse",
                    "findByActiveIsTrue",
                    "findByActiveIsFalse",
                    "findByNameNull",
                    "findByNameIsNull",
                    "findByNameNotNull",
                    "findByNameIsNotNull",
                    "findAllByActiveTrue",
                    "existsByActiveTrue",
                    "existsByDeletedFalse"
                })
        void shouldReturnTrueForEmbeddedConditionSuffixes(String methodName) {
            assertThat(SpringDataQuerySupport.hasEmbeddedConditionSuffix(methodName))
                    .as("Method '%s' should be recognized as having an embedded condition suffix", methodName)
                    .isTrue();
        }

        @ParameterizedTest
        @DisplayName("should return false for methods without embedded condition suffixes")
        @ValueSource(
                strings = {
                    "findByEmail",
                    "findByStatus",
                    "findByFirstNameAndLastName",
                    "findAllByStatus",
                    "existsByEmail",
                    "save",
                    "findById",
                    "deleteById"
                })
        void shouldReturnFalseForMethodsWithoutEmbeddedConditions(String methodName) {
            assertThat(SpringDataQuerySupport.hasEmbeddedConditionSuffix(methodName))
                    .as("Method '%s' should NOT be recognized as having an embedded condition suffix", methodName)
                    .isFalse();
        }

        @Test
        @DisplayName("should return false for null method name")
        void shouldReturnFalseForNull() {
            assertThat(SpringDataQuerySupport.hasEmbeddedConditionSuffix(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty method name")
        void shouldReturnFalseForEmpty() {
            assertThat(SpringDataQuerySupport.hasEmbeddedConditionSuffix("")).isFalse();
        }
    }
}
