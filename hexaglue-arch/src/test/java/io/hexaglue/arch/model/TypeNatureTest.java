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

import io.hexaglue.syntax.TypeForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeNature}.
 *
 * @since 4.1.0
 */
@DisplayName("TypeNature")
class TypeNatureTest {

    @Nested
    @DisplayName("TypeForm Conversion")
    class TypeFormConversion {

        @Test
        @DisplayName("should convert CLASS from TypeForm")
        void shouldConvertClass() {
            assertThat(TypeNature.fromTypeForm(TypeForm.CLASS)).isEqualTo(TypeNature.CLASS);
        }

        @Test
        @DisplayName("should convert INTERFACE from TypeForm")
        void shouldConvertInterface() {
            assertThat(TypeNature.fromTypeForm(TypeForm.INTERFACE)).isEqualTo(TypeNature.INTERFACE);
        }

        @Test
        @DisplayName("should convert RECORD from TypeForm")
        void shouldConvertRecord() {
            assertThat(TypeNature.fromTypeForm(TypeForm.RECORD)).isEqualTo(TypeNature.RECORD);
        }

        @Test
        @DisplayName("should convert ENUM from TypeForm")
        void shouldConvertEnum() {
            assertThat(TypeNature.fromTypeForm(TypeForm.ENUM)).isEqualTo(TypeNature.ENUM);
        }

        @Test
        @DisplayName("should convert ANNOTATION from TypeForm")
        void shouldConvertAnnotation() {
            assertThat(TypeNature.fromTypeForm(TypeForm.ANNOTATION)).isEqualTo(TypeNature.ANNOTATION);
        }
    }

    @Nested
    @DisplayName("Completeness")
    class Completeness {

        @Test
        @DisplayName("should have exactly 5 values")
        void shouldHaveExactNumberOfValues() {
            assertThat(TypeNature.values()).hasSize(5);
        }

        @Test
        @DisplayName("should have same number of values as TypeForm")
        void shouldHaveSameValuesAsTypeForm() {
            assertThat(TypeNature.values()).hasSameSizeAs(TypeForm.values());
        }
    }

    @Nested
    @DisplayName("Bidirectional Mapping")
    class BidirectionalMapping {

        @Test
        @DisplayName("should convert all TypeForm values")
        void shouldConvertAllTypeFormValues() {
            for (TypeForm form : TypeForm.values()) {
                TypeNature nature = TypeNature.fromTypeForm(form);
                assertThat(nature).isNotNull();
                assertThat(nature.name()).isEqualTo(form.name());
            }
        }
    }
}
