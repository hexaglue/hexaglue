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

package io.hexaglue.arch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemberId}.
 *
 * @since 4.0.0
 */
@DisplayName("MemberId")
class MemberIdTest {

    private static final ElementId ORDER = ElementId.of("com.example.Order");

    @Nested
    @DisplayName("Field factory")
    class FieldFactory {

        @Test
        @DisplayName("should create MemberId for field")
        void shouldCreateForField() {
            // when
            MemberId id = MemberId.field(ORDER, "status");

            // then
            assertThat(id.owner()).isEqualTo(ORDER);
            assertThat(id.memberName()).isEqualTo("status");
            assertThat(id.signature()).isEmpty();
        }

        @Test
        @DisplayName("should reject null owner for field")
        void shouldRejectNullOwner() {
            assertThatThrownBy(() -> MemberId.field(null, "status"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("owner");
        }

        @Test
        @DisplayName("should reject null field name")
        void shouldRejectNullFieldName() {
            assertThatThrownBy(() -> MemberId.field(ORDER, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("memberName");
        }
    }

    @Nested
    @DisplayName("Method factory")
    class MethodFactory {

        @Test
        @DisplayName("should create MemberId for method without parameters")
        void shouldCreateForMethodWithoutParams() {
            // when
            MemberId id = MemberId.method(ORDER, "getStatus", "()");

            // then
            assertThat(id.owner()).isEqualTo(ORDER);
            assertThat(id.memberName()).isEqualTo("getStatus");
            assertThat(id.signature()).isEqualTo("()");
        }

        @Test
        @DisplayName("should create MemberId for method with parameters")
        void shouldCreateForMethodWithParams() {
            // when
            MemberId id = MemberId.method(ORDER, "setStatus", "(String)");

            // then
            assertThat(id.owner()).isEqualTo(ORDER);
            assertThat(id.memberName()).isEqualTo("setStatus");
            assertThat(id.signature()).isEqualTo("(String)");
        }

        @Test
        @DisplayName("should create MemberId for method with multiple parameters")
        void shouldCreateForMethodWithMultipleParams() {
            // when
            MemberId id = MemberId.method(ORDER, "findBy", "(String,int)");

            // then
            assertThat(id.signature()).isEqualTo("(String,int)");
        }

        @Test
        @DisplayName("should reject null signature for method")
        void shouldRejectNullSignature() {
            assertThatThrownBy(() -> MemberId.method(ORDER, "getStatus", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("signature");
        }
    }

    @Nested
    @DisplayName("Constructor factory")
    class ConstructorFactory {

        @Test
        @DisplayName("should create MemberId for no-arg constructor")
        void shouldCreateForNoArgConstructor() {
            // when
            MemberId id = MemberId.constructor(ORDER, "()");

            // then
            assertThat(id.owner()).isEqualTo(ORDER);
            assertThat(id.memberName()).isEqualTo("<init>");
            assertThat(id.signature()).isEqualTo("()");
        }

        @Test
        @DisplayName("should create MemberId for constructor with parameters")
        void shouldCreateForConstructorWithParams() {
            // when
            MemberId id = MemberId.constructor(ORDER, "(String,List)");

            // then
            assertThat(id.memberName()).isEqualTo("<init>");
            assertThat(id.signature()).isEqualTo("(String,List)");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when owner, name and signature match")
        void shouldBeEqualWhenAllMatch() {
            // given
            MemberId id1 = MemberId.method(ORDER, "getStatus", "()");
            MemberId id2 = MemberId.method(ORDER, "getStatus", "()");

            // then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when signatures differ (overloading)")
        void shouldNotBeEqualWhenSignaturesDiffer() {
            // given
            MemberId id1 = MemberId.method(ORDER, "find", "()");
            MemberId id2 = MemberId.method(ORDER, "find", "(String)");

            // then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal when owners differ")
        void shouldNotBeEqualWhenOwnersDiffer() {
            // given
            ElementId customer = ElementId.of("com.example.Customer");
            MemberId id1 = MemberId.method(ORDER, "getStatus", "()");
            MemberId id2 = MemberId.method(customer, "getStatus", "()");

            // then
            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("Comparison")
    class Comparison {

        @Test
        @DisplayName("should compare by owner first")
        void shouldCompareByOwnerFirst() {
            // given
            ElementId aClass = ElementId.of("com.a.A");
            ElementId bClass = ElementId.of("com.b.B");
            MemberId a = MemberId.method(aClass, "zMethod", "()");
            MemberId b = MemberId.method(bClass, "aMethod", "()");

            // then
            assertThat(a).isLessThan(b);
        }

        @Test
        @DisplayName("should compare by member name when owners equal")
        void shouldCompareByNameWhenOwnersEqual() {
            // given
            MemberId a = MemberId.method(ORDER, "aMethod", "()");
            MemberId b = MemberId.method(ORDER, "bMethod", "()");

            // then
            assertThat(a).isLessThan(b);
        }

        @Test
        @DisplayName("should compare by signature when owner and name equal")
        void shouldCompareBySignatureWhenOwnerAndNameEqual() {
            // given
            MemberId a = MemberId.method(ORDER, "find", "()");
            MemberId b = MemberId.method(ORDER, "find", "(String)");

            // then
            assertThat(a).isLessThan(b);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringBehavior {

        @Test
        @DisplayName("should format as owner#memberName+signature")
        void shouldFormatCorrectly() {
            // given
            MemberId id = MemberId.method(ORDER, "findById", "(String)");

            // then
            assertThat(id.toString()).isEqualTo("com.example.Order#findById(String)");
        }

        @Test
        @DisplayName("should format field without signature")
        void shouldFormatFieldWithoutSignature() {
            // given
            MemberId id = MemberId.field(ORDER, "status");

            // then
            assertThat(id.toString()).isEqualTo("com.example.Order#status");
        }
    }
}
