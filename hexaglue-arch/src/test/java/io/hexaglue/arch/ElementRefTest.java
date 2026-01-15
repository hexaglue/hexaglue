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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ElementRef}.
 *
 * @since 4.0.0
 */
@DisplayName("ElementRef")
class ElementRefTest {

    private static final String ORDER_QN = "com.example.Order";
    private static final ElementId ORDER_ID = ElementId.of(ORDER_QN);

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create reference with id and type")
        void shouldCreateWithIdAndType() {
            // when
            ElementRef<ArchElement> ref = ElementRef.of(ORDER_ID, ArchElement.class);

            // then
            assertThat(ref.id()).isEqualTo(ORDER_ID);
            assertThat(ref.expectedType()).isEqualTo(ArchElement.class);
        }

        @Test
        @DisplayName("should create reference from qualified name")
        void shouldCreateFromQualifiedName() {
            // when
            ElementRef<ArchElement> ref = ElementRef.of(ORDER_QN, ArchElement.class);

            // then
            assertThat(ref.qualifiedName()).isEqualTo(ORDER_QN);
            assertThat(ref.simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> ElementRef.of((ElementId) null, ArchElement.class))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            assertThatThrownBy(() -> ElementRef.of(ORDER_ID, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("expectedType");
        }
    }

    @Nested
    @DisplayName("Resolution")
    class Resolution {

        private ElementRegistry registry;
        private TestArchElement orderElement;

        @BeforeEach
        void setUp() {
            orderElement = TestArchElement.aggregate(ORDER_QN);
            registry = ElementRegistry.builder().add(orderElement).build();
        }

        @Test
        @DisplayName("resolve() should return Resolved when element exists and type matches")
        void resolveShouldReturnResolvedWhenFound() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of(ORDER_ID, ArchElement.class);

            // when
            ResolutionResult<ArchElement> result = ref.resolve(registry);

            // then
            assertThat(result.isResolved()).isTrue();
            assertThat(result.value()).contains(orderElement);
            assertThat(result.error()).isEmpty();
        }

        @Test
        @DisplayName("resolve() should return NotFound when element does not exist")
        void resolveShouldReturnNotFoundWhenMissing() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of("com.example.Unknown", ArchElement.class);

            // when
            ResolutionResult<ArchElement> result = ref.resolve(registry);

            // then
            assertThat(result.isResolved()).isFalse();
            assertThat(result.value()).isEmpty();
            assertThat(result.error()).isPresent();
            assertThat(result.error().get()).isInstanceOf(ResolutionError.NotFound.class);
            assertThat(result.error().get().message()).contains("not found");
        }

        @Test
        @DisplayName("resolve() should return TypeMismatch when type does not match")
        void resolveShouldReturnTypeMismatchWhenWrongType() {
            // given - TestArchElement is not a TestSpecificElement
            ElementRef<TestSpecificElement> ref = ElementRef.of(ORDER_ID, TestSpecificElement.class);

            // when
            ResolutionResult<TestSpecificElement> result = ref.resolve(registry);

            // then
            assertThat(result.isResolved()).isFalse();
            assertThat(result.error()).isPresent();
            assertThat(result.error().get()).isInstanceOf(ResolutionError.TypeMismatch.class);
            ResolutionError.TypeMismatch mismatch =
                    (ResolutionError.TypeMismatch) result.error().get();
            assertThat(mismatch.expectedType()).isEqualTo(TestSpecificElement.class);
            assertThat(mismatch.actualType()).isEqualTo(TestArchElement.class);
        }

        @Test
        @DisplayName("resolveOpt() should return Optional with element when found")
        void resolveOptShouldReturnOptionalWhenFound() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of(ORDER_ID, ArchElement.class);

            // when/then
            assertThat(ref.resolveOpt(registry)).contains(orderElement);
        }

        @Test
        @DisplayName("resolveOpt() should return empty when not found")
        void resolveOptShouldReturnEmptyWhenNotFound() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of("com.example.Unknown", ArchElement.class);

            // when/then
            assertThat(ref.resolveOpt(registry)).isEmpty();
        }

        @Test
        @DisplayName("resolveOrThrow() should return element when found")
        void resolveOrThrowShouldReturnWhenFound() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of(ORDER_ID, ArchElement.class);

            // when/then
            assertThat(ref.resolveOrThrow(registry)).isEqualTo(orderElement);
        }

        @Test
        @DisplayName("resolveOrThrow() should throw when not found")
        void resolveOrThrowShouldThrowWhenNotFound() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of("com.example.Unknown", ArchElement.class);

            // when/then
            assertThatThrownBy(() -> ref.resolveOrThrow(registry))
                    .isInstanceOf(UnresolvedReferenceException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("resolveOrThrow() should throw with correct ref in exception")
        void resolveOrThrowShouldIncludeRefInException() {
            // given
            ElementRef<ArchElement> ref = ElementRef.of("com.example.Unknown", ArchElement.class);

            // when/then
            try {
                ref.resolveOrThrow(registry);
            } catch (UnresolvedReferenceException e) {
                assertThat(e.getRef()).isEqualTo(ref);
                assertThat(e.getError()).isInstanceOf(ResolutionError.NotFound.class);
            }
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when id and type match")
        void shouldBeEqualWhenIdAndTypeMatch() {
            // given
            ElementRef<ArchElement> ref1 = ElementRef.of(ORDER_ID, ArchElement.class);
            ElementRef<ArchElement> ref2 = ElementRef.of(ORDER_ID, ArchElement.class);

            // then
            assertThat(ref1).isEqualTo(ref2);
            assertThat(ref1.hashCode()).isEqualTo(ref2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            ElementRef<ArchElement> ref1 = ElementRef.of("com.example.A", ArchElement.class);
            ElementRef<ArchElement> ref2 = ElementRef.of("com.example.B", ArchElement.class);

            // then
            assertThat(ref1).isNotEqualTo(ref2);
        }
    }

    /**
     * Test-specific subtype to verify type mismatch detection.
     */
    interface TestSpecificElement extends ArchElement.Marker {}
}
