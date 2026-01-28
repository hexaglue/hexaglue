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

import io.hexaglue.plugin.audit.domain.model.report.TypeViolation.ViolationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link TypeViolation} and {@link ViolationType}.
 *
 * @since 5.0.0
 */
class TypeViolationTest {

    @Nested
    @DisplayName("ViolationType enum")
    class ViolationTypeTests {

        @Test
        @DisplayName("should have exactly 11 violation types")
        void shouldHaveExactlyElevenViolationTypes() {
            // The enum should have 11 values covering all DDD and hexagonal violations
            assertThat(ViolationType.values()).hasSize(11);
        }

        @Test
        @DisplayName("should contain all DDD violation types")
        void shouldContainAllDddViolationTypes() {
            assertThat(ViolationType.values())
                    .contains(
                            ViolationType.CYCLE,
                            ViolationType.MUTABLE_VALUE_OBJECT,
                            ViolationType.IMPURE_DOMAIN,
                            ViolationType.BOUNDARY_VIOLATION,
                            ViolationType.MISSING_IDENTITY,
                            ViolationType.MISSING_REPOSITORY,
                            ViolationType.EVENT_NAMING);
        }

        @Test
        @DisplayName("should contain all hexagonal violation types")
        void shouldContainAllHexagonalViolationTypes() {
            assertThat(ViolationType.values())
                    .contains(
                            ViolationType.PORT_UNCOVERED,
                            ViolationType.DEPENDENCY_INVERSION,
                            ViolationType.LAYER_VIOLATION,
                            ViolationType.PORT_NOT_INTERFACE);
        }
    }

    @Nested
    @DisplayName("TypeViolation record")
    class TypeViolationRecordTests {

        @Test
        @DisplayName("should reject null typeName")
        void shouldRejectNullTypeName() {
            assertThatThrownBy(() -> new TypeViolation(null, ViolationType.CYCLE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeName");
        }

        @Test
        @DisplayName("should reject null violationType")
        void shouldRejectNullViolationType() {
            assertThatThrownBy(() -> new TypeViolation("Order", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("violationType");
        }

        @ParameterizedTest
        @EnumSource(ViolationType.class)
        @DisplayName("should accept all violation types")
        void shouldAcceptAllViolationTypes(ViolationType type) {
            var violation = new TypeViolation("TestType", type);
            assertThat(violation.typeName()).isEqualTo("TestType");
            assertThat(violation.violationType()).isEqualTo(type);
        }
    }

    @Nested
    @DisplayName("Existing factory methods")
    class ExistingFactoryMethodsTests {

        @Test
        @DisplayName("cycle() should create CYCLE violation")
        void cycleShouldCreateCycleViolation() {
            var violation = TypeViolation.cycle("Order");

            assertThat(violation.typeName()).isEqualTo("Order");
            assertThat(violation.violationType()).isEqualTo(ViolationType.CYCLE);
        }

        @Test
        @DisplayName("mutableValueObject() should create MUTABLE_VALUE_OBJECT violation")
        void mutableValueObjectShouldCreateMutableValueObjectViolation() {
            var violation = TypeViolation.mutableValueObject("Money");

            assertThat(violation.typeName()).isEqualTo("Money");
            assertThat(violation.violationType()).isEqualTo(ViolationType.MUTABLE_VALUE_OBJECT);
        }

        @Test
        @DisplayName("impureDomain() should create IMPURE_DOMAIN violation")
        void impureDomainShouldCreateImpureDomainViolation() {
            var violation = TypeViolation.impureDomain("OrderService");

            assertThat(violation.typeName()).isEqualTo("OrderService");
            assertThat(violation.violationType()).isEqualTo(ViolationType.IMPURE_DOMAIN);
        }

        @Test
        @DisplayName("boundaryViolation() should create BOUNDARY_VIOLATION violation")
        void boundaryViolationShouldCreateBoundaryViolation() {
            var violation = TypeViolation.boundaryViolation("Order");

            assertThat(violation.typeName()).isEqualTo("Order");
            assertThat(violation.violationType()).isEqualTo(ViolationType.BOUNDARY_VIOLATION);
        }
    }

    @Nested
    @DisplayName("New DDD factory methods")
    class NewDddFactoryMethodsTests {

        @Test
        @DisplayName("missingIdentity() should create MISSING_IDENTITY violation")
        void missingIdentityShouldCreateMissingIdentityViolation() {
            var violation = TypeViolation.missingIdentity("Customer");

            assertThat(violation.typeName()).isEqualTo("Customer");
            assertThat(violation.violationType()).isEqualTo(ViolationType.MISSING_IDENTITY);
        }

        @Test
        @DisplayName("missingRepository() should create MISSING_REPOSITORY violation")
        void missingRepositoryShouldCreateMissingRepositoryViolation() {
            var violation = TypeViolation.missingRepository("Product");

            assertThat(violation.typeName()).isEqualTo("Product");
            assertThat(violation.violationType()).isEqualTo(ViolationType.MISSING_REPOSITORY);
        }

        @Test
        @DisplayName("eventNaming() should create EVENT_NAMING violation")
        void eventNamingShouldCreateEventNamingViolation() {
            var violation = TypeViolation.eventNaming("OrderCreated");

            assertThat(violation.typeName()).isEqualTo("OrderCreated");
            assertThat(violation.violationType()).isEqualTo(ViolationType.EVENT_NAMING);
        }
    }

    @Nested
    @DisplayName("New hexagonal factory methods")
    class NewHexagonalFactoryMethodsTests {

        @Test
        @DisplayName("portUncovered() should create PORT_UNCOVERED violation")
        void portUncoveredShouldCreatePortUncoveredViolation() {
            var violation = TypeViolation.portUncovered("PaymentGateway");

            assertThat(violation.typeName()).isEqualTo("PaymentGateway");
            assertThat(violation.violationType()).isEqualTo(ViolationType.PORT_UNCOVERED);
        }

        @Test
        @DisplayName("dependencyInversion() should create DEPENDENCY_INVERSION violation")
        void dependencyInversionShouldCreateDependencyInversionViolation() {
            var violation = TypeViolation.dependencyInversion("OrderService");

            assertThat(violation.typeName()).isEqualTo("OrderService");
            assertThat(violation.violationType()).isEqualTo(ViolationType.DEPENDENCY_INVERSION);
        }

        @Test
        @DisplayName("layerViolation() should create LAYER_VIOLATION violation")
        void layerViolationShouldCreateLayerViolation() {
            var violation = TypeViolation.layerViolation("OrderAdapter");

            assertThat(violation.typeName()).isEqualTo("OrderAdapter");
            assertThat(violation.violationType()).isEqualTo(ViolationType.LAYER_VIOLATION);
        }

        @Test
        @DisplayName("portNotInterface() should create PORT_NOT_INTERFACE violation")
        void portNotInterfaceShouldCreatePortNotInterfaceViolation() {
            var violation = TypeViolation.portNotInterface("OrderPort");

            assertThat(violation.typeName()).isEqualTo("OrderPort");
            assertThat(violation.violationType()).isEqualTo(ViolationType.PORT_NOT_INTERFACE);
        }
    }
}
