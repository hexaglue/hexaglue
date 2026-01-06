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

package io.hexaglue.core.classification.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.port.PortKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CompatibilityPolicy}.
 */
@DisplayName("CompatibilityPolicy")
class CompatibilityPolicyTest {

    // =========================================================================
    // noneCompatible()
    // =========================================================================

    @Nested
    @DisplayName("noneCompatible()")
    class NoneCompatibleTest {

        @Test
        @DisplayName("identical kinds should be compatible")
        void identicalKindsShouldBeCompatible() {
            CompatibilityPolicy<DomainKind> policy = CompatibilityPolicy.noneCompatible();

            assertThat(policy.areCompatible(DomainKind.ENTITY, DomainKind.ENTITY))
                    .isTrue();
            assertThat(policy.areCompatible(DomainKind.AGGREGATE_ROOT, DomainKind.AGGREGATE_ROOT))
                    .isTrue();
            assertThat(policy.areCompatible(DomainKind.VALUE_OBJECT, DomainKind.VALUE_OBJECT))
                    .isTrue();
        }

        @Test
        @DisplayName("different kinds should not be compatible")
        void differentKindsShouldNotBeCompatible() {
            CompatibilityPolicy<DomainKind> policy = CompatibilityPolicy.noneCompatible();

            assertThat(policy.areCompatible(DomainKind.ENTITY, DomainKind.VALUE_OBJECT))
                    .isFalse();
            assertThat(policy.areCompatible(DomainKind.AGGREGATE_ROOT, DomainKind.ENTITY))
                    .isFalse();
            assertThat(policy.areCompatible(DomainKind.DOMAIN_SERVICE, DomainKind.APPLICATION_SERVICE))
                    .isFalse();
        }

        @Test
        @DisplayName("should work with any type")
        void shouldWorkWithAnyType() {
            CompatibilityPolicy<String> policy = CompatibilityPolicy.noneCompatible();

            assertThat(policy.areCompatible("A", "A")).isTrue();
            assertThat(policy.areCompatible("A", "B")).isFalse();
        }
    }

    // =========================================================================
    // domainDefault()
    // =========================================================================

    @Nested
    @DisplayName("domainDefault()")
    class DomainDefaultTest {

        private final CompatibilityPolicy<DomainKind> policy = CompatibilityPolicy.domainDefault();

        @Test
        @DisplayName("identical kinds should be compatible")
        void identicalKindsShouldBeCompatible() {
            for (DomainKind kind : DomainKind.values()) {
                assertThat(policy.areCompatible(kind, kind))
                        .as("Same kind %s should be compatible with itself", kind)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("AGGREGATE_ROOT and ENTITY should be compatible")
        void aggregateRootAndEntityShouldBeCompatible() {
            assertThat(policy.areCompatible(DomainKind.AGGREGATE_ROOT, DomainKind.ENTITY))
                    .isTrue();
            assertThat(policy.areCompatible(DomainKind.ENTITY, DomainKind.AGGREGATE_ROOT))
                    .isTrue();
        }

        @Test
        @DisplayName("VALUE_OBJECT should not be compatible with ENTITY")
        void valueObjectAndEntityShouldNotBeCompatible() {
            assertThat(policy.areCompatible(DomainKind.VALUE_OBJECT, DomainKind.ENTITY))
                    .isFalse();
            assertThat(policy.areCompatible(DomainKind.ENTITY, DomainKind.VALUE_OBJECT))
                    .isFalse();
        }

        @Test
        @DisplayName("VALUE_OBJECT should not be compatible with AGGREGATE_ROOT")
        void valueObjectAndAggregateRootShouldNotBeCompatible() {
            assertThat(policy.areCompatible(DomainKind.VALUE_OBJECT, DomainKind.AGGREGATE_ROOT))
                    .isFalse();
            assertThat(policy.areCompatible(DomainKind.AGGREGATE_ROOT, DomainKind.VALUE_OBJECT))
                    .isFalse();
        }

        @Test
        @DisplayName("DOMAIN_SERVICE and APPLICATION_SERVICE should not be compatible")
        void domainServiceAndApplicationServiceShouldNotBeCompatible() {
            assertThat(policy.areCompatible(DomainKind.DOMAIN_SERVICE, DomainKind.APPLICATION_SERVICE))
                    .isFalse();
            assertThat(policy.areCompatible(DomainKind.APPLICATION_SERVICE, DomainKind.DOMAIN_SERVICE))
                    .isFalse();
        }

        @Test
        @DisplayName("IDENTIFIER should not be compatible with VALUE_OBJECT")
        void identifierAndValueObjectShouldNotBeCompatible() {
            assertThat(policy.areCompatible(DomainKind.IDENTIFIER, DomainKind.VALUE_OBJECT))
                    .isFalse();
            assertThat(policy.areCompatible(DomainKind.VALUE_OBJECT, DomainKind.IDENTIFIER))
                    .isFalse();
        }

        @Test
        @DisplayName("DOMAIN_EVENT should not be compatible with any other kind")
        void domainEventShouldNotBeCompatibleWithOthers() {
            for (DomainKind kind : DomainKind.values()) {
                if (kind != DomainKind.DOMAIN_EVENT) {
                    assertThat(policy.areCompatible(DomainKind.DOMAIN_EVENT, kind))
                            .as("DOMAIN_EVENT should not be compatible with %s", kind)
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("compatibility should be symmetric")
        void compatibilityShouldBeSymmetric() {
            for (DomainKind a : DomainKind.values()) {
                for (DomainKind b : DomainKind.values()) {
                    assertThat(policy.areCompatible(a, b))
                            .as("Compatibility of %s and %s should be symmetric", a, b)
                            .isEqualTo(policy.areCompatible(b, a));
                }
            }
        }
    }

    // =========================================================================
    // portDefault()
    // =========================================================================

    @Nested
    @DisplayName("portDefault()")
    class PortDefaultTest {

        private final CompatibilityPolicy<PortKind> policy = CompatibilityPolicy.portDefault();

        @Test
        @DisplayName("identical kinds should be compatible")
        void identicalKindsShouldBeCompatible() {
            for (PortKind kind : PortKind.values()) {
                assertThat(policy.areCompatible(kind, kind))
                        .as("Same kind %s should be compatible with itself", kind)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("all different port kinds should be incompatible")
        void differentPortKindsShouldNotBeCompatible() {
            for (PortKind a : PortKind.values()) {
                for (PortKind b : PortKind.values()) {
                    if (a != b) {
                        assertThat(policy.areCompatible(a, b))
                                .as("%s and %s should not be compatible", a, b)
                                .isFalse();
                    }
                }
            }
        }

        @Test
        @DisplayName("REPOSITORY should not be compatible with USE_CASE")
        void repositoryAndUseCaseShouldNotBeCompatible() {
            assertThat(policy.areCompatible(PortKind.REPOSITORY, PortKind.USE_CASE))
                    .isFalse();
        }

        @Test
        @DisplayName("GATEWAY should not be compatible with REPOSITORY")
        void gatewayAndRepositoryShouldNotBeCompatible() {
            assertThat(policy.areCompatible(PortKind.GATEWAY, PortKind.REPOSITORY))
                    .isFalse();
        }
    }

    // =========================================================================
    // Custom Lambda Implementation
    // =========================================================================

    @Nested
    @DisplayName("Custom lambda implementation")
    class CustomLambdaTest {

        @Test
        @DisplayName("should support custom compatibility logic")
        void shouldSupportCustomLogic() {
            // Custom policy: all kinds starting with same letter are compatible
            CompatibilityPolicy<String> policy = (a, b) -> a.equals(b) || (a.charAt(0) == b.charAt(0));

            assertThat(policy.areCompatible("Apple", "Apple")).isTrue();
            assertThat(policy.areCompatible("Apple", "Avocado")).isTrue();
            assertThat(policy.areCompatible("Apple", "Banana")).isFalse();
        }

        @Test
        @DisplayName("custom domain policy can extend default compatibility")
        void customDomainPolicyCanExtendDefault() {
            // Extend default: also make VALUE_OBJECT compatible with IDENTIFIER
            CompatibilityPolicy<DomainKind> extended = (a, b) -> {
                if (a == b) return true;
                if (CompatibilityPolicy.domainDefault().areCompatible(a, b)) return true;
                // Custom extension
                return (a == DomainKind.VALUE_OBJECT && b == DomainKind.IDENTIFIER)
                        || (a == DomainKind.IDENTIFIER && b == DomainKind.VALUE_OBJECT);
            };

            // Original domain compatibility
            assertThat(extended.areCompatible(DomainKind.AGGREGATE_ROOT, DomainKind.ENTITY))
                    .isTrue();

            // Extended compatibility
            assertThat(extended.areCompatible(DomainKind.VALUE_OBJECT, DomainKind.IDENTIFIER))
                    .isTrue();
            assertThat(extended.areCompatible(DomainKind.IDENTIFIER, DomainKind.VALUE_OBJECT))
                    .isTrue();

            // Still incompatible
            assertThat(extended.areCompatible(DomainKind.ENTITY, DomainKind.VALUE_OBJECT))
                    .isFalse();
        }
    }
}
