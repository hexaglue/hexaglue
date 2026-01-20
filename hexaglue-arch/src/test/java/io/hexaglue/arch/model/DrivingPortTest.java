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
 * Tests for {@link DrivingPort}.
 *
 * @since 4.1.0
 */
@DisplayName("DrivingPort")
class DrivingPortTest {

    private static final TypeId ID = TypeId.of("com.example.OrderService");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.INTERFACE).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.DRIVING_PORT, "explicit-driving-port", "Has port annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.id()).isEqualTo(ID);
            assertThat(port.structure()).isEqualTo(STRUCTURE);
            assertThat(port.classification()).isEqualTo(TRACE);
        }

        @Test
        @DisplayName("should return DRIVING_PORT kind")
        void shouldReturnDrivingPortKind() {
            // when
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.kind()).isEqualTo(ArchKind.DRIVING_PORT);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> DrivingPort.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> DrivingPort.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> DrivingPort.of(ID, STRUCTURE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement PortType interface")
        void shouldImplementPortType() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port).isInstanceOf(PortType.class);
            assertThat(port).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.qualifiedName()).isEqualTo("com.example.OrderService");
            assertThat(port.simpleName()).isEqualTo("OrderService");
            assertThat(port.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            DrivingPort p1 = DrivingPort.of(ID, STRUCTURE, TRACE);
            DrivingPort p2 = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.CustomerService");
            DrivingPort p1 = DrivingPort.of(ID, STRUCTURE, TRACE);
            DrivingPort p2 = DrivingPort.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(p1).isNotEqualTo(p2);
        }
    }
}
