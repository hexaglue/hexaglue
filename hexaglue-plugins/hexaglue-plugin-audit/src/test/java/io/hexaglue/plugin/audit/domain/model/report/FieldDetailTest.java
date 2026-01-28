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

package io.hexaglue.plugin.audit.domain.model.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FieldDetail}.
 *
 * @since 5.0.0
 */
@DisplayName("FieldDetail")
class FieldDetailTest {

    @Nested
    @DisplayName("toMermaid()")
    class ToMermaid {

        @Test
        @DisplayName("should format private field correctly")
        void shouldFormatPrivateField() {
            FieldDetail field = FieldDetail.of("id", "OrderId", "-");
            assertThat(field.toMermaid()).isEqualTo("-id: OrderId");
        }

        @Test
        @DisplayName("should format public field correctly")
        void shouldFormatPublicField() {
            FieldDetail field = FieldDetail.of("name", "String", "+");
            assertThat(field.toMermaid()).isEqualTo("+name: String");
        }

        @Test
        @DisplayName("should format protected field correctly")
        void shouldFormatProtectedField() {
            FieldDetail field = FieldDetail.of("value", "int", "#");
            assertThat(field.toMermaid()).isEqualTo("#value: int");
        }

        @Test
        @DisplayName("should format package-private field correctly")
        void shouldFormatPackagePrivateField() {
            FieldDetail field = FieldDetail.of("data", "byte[]", "~");
            assertThat(field.toMermaid()).isEqualTo("~data: byte[]");
        }

        @Test
        @DisplayName("should format generic type field correctly")
        void shouldFormatGenericTypeField() {
            FieldDetail field = FieldDetail.of("items", "List~OrderLine~", "-");
            assertThat(field.toMermaid()).isEqualTo("-items: List~OrderLine~");
        }

        @Test
        @DisplayName("should format static field with $ suffix")
        void shouldFormatStaticField() {
            FieldDetail field = FieldDetail.of("INSTANCE", "Singleton", "+", true);
            assertThat(field.toMermaid()).isEqualTo("+INSTANCE$: Singleton");
        }

        @Test
        @DisplayName("should format non-static field without $ suffix")
        void shouldFormatNonStaticField() {
            FieldDetail field = FieldDetail.of("name", "String", "-", false);
            assertThat(field.toMermaid()).isEqualTo("-name: String");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> FieldDetail.of(null, "String", "-"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject null typeMermaid")
        void shouldRejectNullType() {
            assertThatThrownBy(() -> FieldDetail.of("name", null, "-"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeMermaid");
        }

        @Test
        @DisplayName("should reject null visibility")
        void shouldRejectNullVisibility() {
            assertThatThrownBy(() -> FieldDetail.of("name", "String", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("visibility");
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of(name, type, visibility) should create non-static field")
        void shouldCreateNonStaticField() {
            FieldDetail field = FieldDetail.of("name", "String", "-");
            assertThat(field.isStatic()).isFalse();
        }

        @Test
        @DisplayName("of(name, type, visibility, isStatic) should create field with static flag")
        void shouldCreateFieldWithStaticFlag() {
            FieldDetail field = FieldDetail.of("CONSTANT", "int", "+", true);
            assertThat(field.isStatic()).isTrue();
        }
    }
}
