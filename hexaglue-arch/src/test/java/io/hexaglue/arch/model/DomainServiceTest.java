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
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainService}.
 *
 * @since 4.1.0
 */
@DisplayName("DomainService")
class DomainServiceTest {

    private static final TypeId ID = TypeId.of("com.example.PricingService");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.INTERFACE).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.DOMAIN_SERVICE, "explicit-domain-service", "Has Service annotation");
    private static final TypeRef INVENTORY_PORT =
            new TypeRef("com.example.InventoryPort", "InventoryPort", List.of(), false, false, 0);
    private static final TypeRef PRICING_PORT =
            new TypeRef("com.example.PricingPort", "PricingPort", List.of(), false, false, 0);
    private static final Method CALCULATE_PRICE_METHOD = Method.of(
            "calculatePrice",
            new TypeRef("java.math.BigDecimal", "BigDecimal", List.of(), false, false, 0),
            Set.of(MethodRole.BUSINESS));

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.id()).isEqualTo(ID);
            assertThat(service.structure()).isEqualTo(STRUCTURE);
            assertThat(service.classification()).isEqualTo(TRACE);
            assertThat(service.injectedPorts()).isEmpty();
            assertThat(service.operations()).isEmpty();
        }

        @Test
        @DisplayName("should create with injected ports and operations")
        void shouldCreateWithInjectedPortsAndOperations() {
            // when
            DomainService service = DomainService.of(
                    ID, STRUCTURE, TRACE, List.of(INVENTORY_PORT, PRICING_PORT), List.of(CALCULATE_PRICE_METHOD));

            // then
            assertThat(service.id()).isEqualTo(ID);
            assertThat(service.injectedPorts()).containsExactly(INVENTORY_PORT, PRICING_PORT);
            assertThat(service.operations()).containsExactly(CALCULATE_PRICE_METHOD);
        }

        @Test
        @DisplayName("should return DOMAIN_SERVICE kind")
        void shouldReturnDomainServiceKind() {
            // when
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.kind()).isEqualTo(ArchKind.DOMAIN_SERVICE);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> DomainService.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> DomainService.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> DomainService.of(ID, STRUCTURE, null))
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
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service).isInstanceOf(DomainType.class);
            assertThat(service).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.qualifiedName()).isEqualTo("com.example.PricingService");
            assertThat(service.simpleName()).isEqualTo("PricingService");
            assertThat(service.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Injected Ports and Operations")
    class InjectedPortsAndOperations {

        @Test
        @DisplayName("hasInjectedPorts should return true when ports are present")
        void hasInjectedPortsShouldReturnTrueWhenPresent() {
            // given
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE, List.of(INVENTORY_PORT), List.of());

            // then
            assertThat(service.hasInjectedPorts()).isTrue();
        }

        @Test
        @DisplayName("hasInjectedPorts should return false when no ports")
        void hasInjectedPortsShouldReturnFalseWhenAbsent() {
            // given
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.hasInjectedPorts()).isFalse();
        }

        @Test
        @DisplayName("hasOperations should return true when operations are present")
        void hasOperationsShouldReturnTrueWhenPresent() {
            // given
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE, List.of(), List.of(CALCULATE_PRICE_METHOD));

            // then
            assertThat(service.hasOperations()).isTrue();
        }

        @Test
        @DisplayName("hasOperations should return false when no operations")
        void hasOperationsShouldReturnFalseWhenAbsent() {
            // given
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.hasOperations()).isFalse();
        }

        @Test
        @DisplayName("should make defensive copies of lists")
        void shouldMakeDefensiveCopies() {
            // given
            List<TypeRef> mutablePorts = new java.util.ArrayList<>();
            mutablePorts.add(INVENTORY_PORT);
            List<Method> mutableOps = new java.util.ArrayList<>();
            mutableOps.add(CALCULATE_PRICE_METHOD);

            // when
            DomainService service = DomainService.of(ID, STRUCTURE, TRACE, mutablePorts, mutableOps);
            mutablePorts.add(PRICING_PORT);

            // then
            assertThat(service.injectedPorts()).containsExactly(INVENTORY_PORT);
            assertThat(service.operations()).containsExactly(CALCULATE_PRICE_METHOD);
        }

        @Test
        @DisplayName("should reject null injectedPorts")
        void shouldRejectNullInjectedPorts() {
            assertThatThrownBy(() -> DomainService.of(ID, STRUCTURE, TRACE, null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("injectedPorts");
        }

        @Test
        @DisplayName("should reject null operations")
        void shouldRejectNullOperations() {
            assertThatThrownBy(() -> DomainService.of(ID, STRUCTURE, TRACE, List.of(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("operations");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            DomainService s1 = DomainService.of(ID, STRUCTURE, TRACE);
            DomainService s2 = DomainService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(s1).isEqualTo(s2);
            assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.InventoryService");
            DomainService s1 = DomainService.of(ID, STRUCTURE, TRACE);
            DomainService s2 = DomainService.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(s1).isNotEqualTo(s2);
        }
    }
}
