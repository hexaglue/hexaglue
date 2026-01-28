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
 * Tests for {@link MethodDetail}.
 *
 * @since 5.0.0
 */
@DisplayName("MethodDetail")
class MethodDetailTest {

    @Nested
    @DisplayName("toMermaid()")
    class ToMermaid {

        @Test
        @DisplayName("should format public method with return type")
        void shouldFormatPublicMethod() {
            MethodDetail method = MethodDetail.of("place", "(CustomerId, List~OrderLine~): Order", "+");
            assertThat(method.toMermaid()).isEqualTo("+place(CustomerId, List~OrderLine~): Order");
        }

        @Test
        @DisplayName("should format void method correctly")
        void shouldFormatVoidMethod() {
            MethodDetail method = MethodDetail.of("cancel", "(): void", "+");
            assertThat(method.toMermaid()).isEqualTo("+cancel(): void");
        }

        @Test
        @DisplayName("should format method with no parameters")
        void shouldFormatNoParamsMethod() {
            MethodDetail method = MethodDetail.of("calculateTotal", "(): Money", "+");
            assertThat(method.toMermaid()).isEqualTo("+calculateTotal(): Money");
        }

        @Test
        @DisplayName("should format private method")
        void shouldFormatPrivateMethod() {
            MethodDetail method = MethodDetail.of("validate", "(): void", "-");
            assertThat(method.toMermaid()).isEqualTo("-validate(): void");
        }

        @Test
        @DisplayName("should format protected method")
        void shouldFormatProtectedMethod() {
            MethodDetail method = MethodDetail.of("onInit", "(): void", "#");
            assertThat(method.toMermaid()).isEqualTo("#onInit(): void");
        }

        @Test
        @DisplayName("should format package-private method")
        void shouldFormatPackagePrivateMethod() {
            MethodDetail method = MethodDetail.of("process", "(Data): Result", "~");
            assertThat(method.toMermaid()).isEqualTo("~process(Data): Result");
        }

        @Test
        @DisplayName("should format static method with $ suffix")
        void shouldFormatStaticMethod() {
            MethodDetail method = MethodDetail.of("getInstance", "(): Singleton", "+", true);
            assertThat(method.toMermaid()).isEqualTo("+getInstance$(): Singleton");
        }

        @Test
        @DisplayName("should format static factory method")
        void shouldFormatStaticFactoryMethod() {
            MethodDetail method = MethodDetail.of("of", "(String): OrderId", "+", true);
            assertThat(method.toMermaid()).isEqualTo("+of$(String): OrderId");
        }

        @Test
        @DisplayName("should format non-static method without $ suffix")
        void shouldFormatNonStaticMethod() {
            MethodDetail method = MethodDetail.of("addLine", "(OrderLine): void", "+", false);
            assertThat(method.toMermaid()).isEqualTo("+addLine(OrderLine): void");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> MethodDetail.of(null, "(): void", "+"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject null signatureMermaid")
        void shouldRejectNullSignature() {
            assertThatThrownBy(() -> MethodDetail.of("method", null, "+"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("signatureMermaid");
        }

        @Test
        @DisplayName("should reject null visibility")
        void shouldRejectNullVisibility() {
            assertThatThrownBy(() -> MethodDetail.of("method", "(): void", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("visibility");
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of(name, signature, visibility) should create non-static method")
        void shouldCreateNonStaticMethod() {
            MethodDetail method = MethodDetail.of("process", "(Input): Output", "+");
            assertThat(method.isStatic()).isFalse();
        }

        @Test
        @DisplayName("of(name, signature, visibility, isStatic) should create method with static flag")
        void shouldCreateMethodWithStaticFlag() {
            MethodDetail method = MethodDetail.of("valueOf", "(String): MyEnum", "+", true);
            assertThat(method.isStatic()).isTrue();
        }
    }
}
