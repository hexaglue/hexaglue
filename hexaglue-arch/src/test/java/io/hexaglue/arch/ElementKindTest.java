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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ElementKind}.
 *
 * @since 4.0.0
 */
@DisplayName("ElementKind")
class ElementKindTest {

    @Nested
    @DisplayName("isPort()")
    class IsPort {

        @Test
        @DisplayName("should return true for DRIVING_PORT")
        void shouldReturnTrueForDrivingPort() {
            assertThat(ElementKind.DRIVING_PORT.isPort()).isTrue();
        }

        @Test
        @DisplayName("should return true for DRIVEN_PORT")
        void shouldReturnTrueForDrivenPort() {
            assertThat(ElementKind.DRIVEN_PORT.isPort()).isTrue();
        }

        @Test
        @DisplayName("should return false for APPLICATION_SERVICE (now separate from ports)")
        void shouldReturnFalseForApplicationService() {
            assertThat(ElementKind.APPLICATION_SERVICE.isPort()).isFalse();
        }

        @Test
        @DisplayName("should return false for application service variants")
        void shouldReturnFalseForApplicationServiceVariants() {
            assertThat(ElementKind.INBOUND_ONLY.isPort()).isFalse();
            assertThat(ElementKind.OUTBOUND_ONLY.isPort()).isFalse();
            assertThat(ElementKind.SAGA.isPort()).isFalse();
        }

        @Test
        @DisplayName("should return false for domain types")
        void shouldReturnFalseForDomainTypes() {
            assertThat(ElementKind.AGGREGATE.isPort()).isFalse();
            assertThat(ElementKind.AGGREGATE_ROOT.isPort()).isFalse();
            assertThat(ElementKind.ENTITY.isPort()).isFalse();
            assertThat(ElementKind.VALUE_OBJECT.isPort()).isFalse();
        }
    }

    @Nested
    @DisplayName("isApplicationService()")
    class IsApplicationService {

        @Test
        @DisplayName("should return true for APPLICATION_SERVICE")
        void shouldReturnTrueForApplicationService() {
            assertThat(ElementKind.APPLICATION_SERVICE.isApplicationService()).isTrue();
        }

        @Test
        @DisplayName("should return true for INBOUND_ONLY")
        void shouldReturnTrueForInboundOnly() {
            assertThat(ElementKind.INBOUND_ONLY.isApplicationService()).isTrue();
        }

        @Test
        @DisplayName("should return true for OUTBOUND_ONLY")
        void shouldReturnTrueForOutboundOnly() {
            assertThat(ElementKind.OUTBOUND_ONLY.isApplicationService()).isTrue();
        }

        @Test
        @DisplayName("should return true for SAGA")
        void shouldReturnTrueForSaga() {
            assertThat(ElementKind.SAGA.isApplicationService()).isTrue();
        }

        @Test
        @DisplayName("should return false for ports")
        void shouldReturnFalseForPorts() {
            assertThat(ElementKind.DRIVING_PORT.isApplicationService()).isFalse();
            assertThat(ElementKind.DRIVEN_PORT.isApplicationService()).isFalse();
        }

        @Test
        @DisplayName("should return false for domain types")
        void shouldReturnFalseForDomainTypes() {
            assertThat(ElementKind.AGGREGATE.isApplicationService()).isFalse();
            assertThat(ElementKind.DOMAIN_SERVICE.isApplicationService()).isFalse();
        }
    }

    @Nested
    @DisplayName("isAdapter()")
    class IsAdapter {

        @Test
        @DisplayName("should return true for DRIVING_ADAPTER")
        void shouldReturnTrueForDrivingAdapter() {
            assertThat(ElementKind.DRIVING_ADAPTER.isAdapter()).isTrue();
        }

        @Test
        @DisplayName("should return true for DRIVEN_ADAPTER")
        void shouldReturnTrueForDrivenAdapter() {
            assertThat(ElementKind.DRIVEN_ADAPTER.isAdapter()).isTrue();
        }

        @Test
        @DisplayName("should return false for ports")
        void shouldReturnFalseForPorts() {
            assertThat(ElementKind.DRIVING_PORT.isAdapter()).isFalse();
            assertThat(ElementKind.DRIVEN_PORT.isAdapter()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDomain()")
    class IsDomain {

        @Test
        @DisplayName("should return true for AGGREGATE")
        void shouldReturnTrueForAggregate() {
            assertThat(ElementKind.AGGREGATE.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return true for AGGREGATE_ROOT")
        void shouldReturnTrueForAggregateRoot() {
            assertThat(ElementKind.AGGREGATE_ROOT.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return true for ENTITY")
        void shouldReturnTrueForEntity() {
            assertThat(ElementKind.ENTITY.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return true for VALUE_OBJECT")
        void shouldReturnTrueForValueObject() {
            assertThat(ElementKind.VALUE_OBJECT.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return true for IDENTIFIER")
        void shouldReturnTrueForIdentifier() {
            assertThat(ElementKind.IDENTIFIER.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return true for DOMAIN_EVENT")
        void shouldReturnTrueForDomainEvent() {
            assertThat(ElementKind.DOMAIN_EVENT.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return true for DOMAIN_SERVICE")
        void shouldReturnTrueForDomainService() {
            assertThat(ElementKind.DOMAIN_SERVICE.isDomain()).isTrue();
        }

        @Test
        @DisplayName("should return false for ports")
        void shouldReturnFalseForPorts() {
            assertThat(ElementKind.DRIVING_PORT.isDomain()).isFalse();
            assertThat(ElementKind.DRIVEN_PORT.isDomain()).isFalse();
        }

        @Test
        @DisplayName("should return false for adapters")
        void shouldReturnFalseForAdapters() {
            assertThat(ElementKind.DRIVING_ADAPTER.isDomain()).isFalse();
            assertThat(ElementKind.DRIVEN_ADAPTER.isDomain()).isFalse();
        }

        @Test
        @DisplayName("should return false for UNCLASSIFIED")
        void shouldReturnFalseForUnclassified() {
            assertThat(ElementKind.UNCLASSIFIED.isDomain()).isFalse();
        }

        @Test
        @DisplayName("should return false for application services")
        void shouldReturnFalseForApplicationServices() {
            assertThat(ElementKind.APPLICATION_SERVICE.isDomain()).isFalse();
            assertThat(ElementKind.INBOUND_ONLY.isDomain()).isFalse();
            assertThat(ElementKind.OUTBOUND_ONLY.isDomain()).isFalse();
            assertThat(ElementKind.SAGA.isDomain()).isFalse();
        }
    }
}
