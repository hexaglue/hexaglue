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

package io.hexaglue.spi.ir;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Identity}.
 */
class IdentityTest {

    @Nested
    @DisplayName("isWrapped()")
    class IsWrappedTest {

        @Test
        @DisplayName("should return true for RECORD wrapper")
        void returnsTrueForRecordWrapper() {
            Identity identity = new Identity(
                    "id",
                    TypeRef.of("com.example.OrderId"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.UUID,
                    IdentityWrapperKind.RECORD);

            assertThat(identity.isWrapped()).isTrue();
        }

        @Test
        @DisplayName("should return true for CLASS wrapper")
        void returnsTrueForClassWrapper() {
            Identity identity = new Identity(
                    "id",
                    TypeRef.of("com.example.CustomerId"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.UUID,
                    IdentityWrapperKind.CLASS);

            assertThat(identity.isWrapped()).isTrue();
        }

        @Test
        @DisplayName("should return false for NONE wrapper")
        void returnsFalseForNoneWrapper() {
            Identity identity = new Identity(
                    "id",
                    TypeRef.of("java.util.UUID"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.UUID,
                    IdentityWrapperKind.NONE);

            assertThat(identity.isWrapped()).isFalse();
        }
    }

    @Nested
    @DisplayName("Deprecated factory method")
    class DeprecatedFactoryTest {

        @Test
        @DisplayName("should create Identity with correct wrapper kind inference")
        @SuppressWarnings("deprecation")
        void createWithStringParameters() {
            Identity wrapped = Identity.of("id", "com.example.OrderId", "java.util.UUID", IdentityStrategy.UUID);

            assertThat(wrapped.fieldName()).isEqualTo("id");
            assertThat(wrapped.typeName()).isEqualTo("com.example.OrderId");
            assertThat(wrapped.unwrappedTypeName()).isEqualTo("java.util.UUID");
            assertThat(wrapped.wrapperKind()).isEqualTo(IdentityWrapperKind.RECORD); // Defaults to RECORD when wrapped
            assertThat(wrapped.isWrapped()).isTrue();
        }

        @Test
        @DisplayName("should create Identity with NONE wrapper when types match")
        @SuppressWarnings("deprecation")
        void createUnwrappedIdentity() {
            Identity unwrapped = Identity.of("id", "java.util.UUID", "java.util.UUID", IdentityStrategy.UUID);

            assertThat(unwrapped.wrapperKind()).isEqualTo(IdentityWrapperKind.NONE);
            assertThat(unwrapped.isWrapped()).isFalse();
        }
    }

    @Nested
    @DisplayName("TypeRef accessors")
    class TypeRefAccessorsTest {

        @Test
        @DisplayName("should return type with full information")
        void typeHasFullInformation() {
            Identity identity = new Identity(
                    "id",
                    TypeRef.of("com.example.OrderId"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.UUID,
                    IdentityWrapperKind.RECORD);

            assertThat(identity.type().qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(identity.type().simpleName()).isEqualTo("OrderId");
        }

        @Test
        @DisplayName("should return unwrappedType with full information")
        void unwrappedTypeHasFullInformation() {
            Identity identity = new Identity(
                    "id",
                    TypeRef.of("com.example.OrderId"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.UUID,
                    IdentityWrapperKind.RECORD);

            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(identity.unwrappedType().simpleName()).isEqualTo("UUID");
        }
    }
}
