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

package io.hexaglue.syntax.spoon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JavadocCleaner}.
 *
 * @since 5.0.0
 */
@DisplayName("JavadocCleaner")
class JavadocCleanerTest {

    @Nested
    @DisplayName("Null and Blank Input")
    class NullAndBlankInput {

        @Test
        @DisplayName("should return empty for null")
        void shouldReturnEmptyForNull() {
            // when
            Optional<String> result = JavadocCleaner.clean(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for blank")
        void shouldReturnEmptyForBlank() {
            // when
            Optional<String> result = JavadocCleaner.clean("   ");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty string")
        void shouldReturnEmptyForEmptyString() {
            // when
            Optional<String> result = JavadocCleaner.clean("");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Simple Description")
    class SimpleDescription {

        @Test
        @DisplayName("should clean simple one-liner")
        void shouldCleanSimpleOneLiner() {
            // given
            String raw = "A simple description.";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("A simple description.");
        }

        @Test
        @DisplayName("should clean multiline description")
        void shouldCleanMultilineDescription() {
            // given
            String raw = "First line of description.\nSecond line of description.";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("First line of description. Second line of description.");
        }
    }

    @Nested
    @DisplayName("Asterisk Removal")
    class AsteriskRemoval {

        @Test
        @DisplayName("should remove leading asterisks")
        void shouldRemoveLeadingAsterisks() {
            // given — typical Spoon getDocComment() output
            String raw = " * A documented class.\n * With two lines.";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("A documented class. With two lines.");
        }
    }

    @Nested
    @DisplayName("Tag Filtering")
    class TagFiltering {

        @Test
        @DisplayName("should filter @param tags")
        void shouldFilterParamTags() {
            // given
            String raw = "Finds an order.\n@param id the order ID";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).doesNotContain("@param");
            assertThat(result.get()).isEqualTo("Finds an order.");
        }

        @Test
        @DisplayName("should filter @return tag")
        void shouldFilterReturnTag() {
            // given
            String raw = "Returns the name.\n@return the name value";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).doesNotContain("@return");
            assertThat(result.get()).isEqualTo("Returns the name.");
        }

        @Test
        @DisplayName("should filter @throws tag")
        void shouldFilterThrowsTag() {
            // given
            String raw = "Deletes an order.\n@throws IllegalStateException if not found";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).doesNotContain("@throws");
            assertThat(result.get()).isEqualTo("Deletes an order.");
        }

        @Test
        @DisplayName("should return empty when only tags")
        void shouldReturnEmptyWhenOnlyTags() {
            // given
            String raw = "@param id the order ID\n@return the order";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle blank lines in Javadoc")
        void shouldHandleBlankLinesInJavadoc() {
            // given
            String raw = "First paragraph.\n\nSecond paragraph.";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("First paragraph. Second paragraph.");
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            // given
            String raw = "   A description with extra whitespace.   ";

            // when
            Optional<String> result = JavadocCleaner.clean(raw);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("A description with extra whitespace.");
        }
    }
}
