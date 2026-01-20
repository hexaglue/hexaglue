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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValueObject}.
 *
 * @since 4.1.0
 */
@DisplayName("ValueObject")
class ValueObjectTest {

    private static final TypeId ID = TypeId.of("com.example.Money");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.RECORD).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.VALUE_OBJECT, "explicit-value-object", "Has ValueObject annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            ValueObject vo = ValueObject.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(vo.id()).isEqualTo(ID);
            assertThat(vo.structure()).isEqualTo(STRUCTURE);
            assertThat(vo.classification()).isEqualTo(TRACE);
        }

        @Test
        @DisplayName("should return VALUE_OBJECT kind")
        void shouldReturnValueObjectKind() {
            // when
            ValueObject vo = ValueObject.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(vo.kind()).isEqualTo(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> ValueObject.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> ValueObject.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> ValueObject.of(ID, STRUCTURE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement DomainType interface")
        void shouldImplementDomainType() {
            // given
            ValueObject vo = ValueObject.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(vo).isInstanceOf(DomainType.class);
            assertThat(vo).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            ValueObject vo = ValueObject.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(vo.qualifiedName()).isEqualTo("com.example.Money");
            assertThat(vo.simpleName()).isEqualTo("Money");
            assertThat(vo.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            ValueObject vo1 = ValueObject.of(ID, STRUCTURE, TRACE);
            ValueObject vo2 = ValueObject.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(vo1).isEqualTo(vo2);
            assertThat(vo1.hashCode()).isEqualTo(vo2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.Address");
            ValueObject vo1 = ValueObject.of(ID, STRUCTURE, TRACE);
            ValueObject vo2 = ValueObject.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(vo1).isNotEqualTo(vo2);
        }
    }
}
