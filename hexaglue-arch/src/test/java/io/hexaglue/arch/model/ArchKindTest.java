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

import io.hexaglue.arch.ElementKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link ArchKind}.
 *
 * @since 4.1.0
 */
@DisplayName("ArchKind")
class ArchKindTest {

    @Nested
    @DisplayName("Domain Kinds")
    class DomainKinds {

        @ParameterizedTest
        @EnumSource(
                value = ArchKind.class,
                names = {"AGGREGATE_ROOT", "ENTITY", "VALUE_OBJECT", "IDENTIFIER", "DOMAIN_EVENT", "DOMAIN_SERVICE"})
        @DisplayName("should identify domain kinds correctly")
        void shouldIdentifyDomainKinds(ArchKind kind) {
            assertThat(kind.isDomain()).isTrue();
            assertThat(kind.isPort()).isFalse();
            assertThat(kind.isApplication()).isFalse();
        }
    }

    @Nested
    @DisplayName("Port Kinds")
    class PortKinds {

        @ParameterizedTest
        @EnumSource(
                value = ArchKind.class,
                names = {"DRIVING_PORT", "DRIVEN_PORT"})
        @DisplayName("should identify port kinds correctly")
        void shouldIdentifyPortKinds(ArchKind kind) {
            assertThat(kind.isPort()).isTrue();
            assertThat(kind.isDomain()).isFalse();
            assertThat(kind.isApplication()).isFalse();
        }
    }

    @Nested
    @DisplayName("Application Kinds")
    class ApplicationKinds {

        @ParameterizedTest
        @EnumSource(
                value = ArchKind.class,
                names = {"APPLICATION_SERVICE", "COMMAND_HANDLER", "QUERY_HANDLER"})
        @DisplayName("should identify application kinds correctly")
        void shouldIdentifyApplicationKinds(ArchKind kind) {
            assertThat(kind.isApplication()).isTrue();
            assertThat(kind.isDomain()).isFalse();
            assertThat(kind.isPort()).isFalse();
        }
    }

    @Nested
    @DisplayName("Unclassified Kind")
    class UnclassifiedKind {

        @Test
        @DisplayName("should identify UNCLASSIFIED as neither domain, port nor application")
        void shouldIdentifyUnclassifiedCorrectly() {
            // given
            ArchKind unclassified = ArchKind.UNCLASSIFIED;

            // then
            assertThat(unclassified.isDomain()).isFalse();
            assertThat(unclassified.isPort()).isFalse();
            assertThat(unclassified.isApplication()).isFalse();
        }
    }

    @Nested
    @DisplayName("ElementKind Conversion")
    class ElementKindConversion {

        @Test
        @DisplayName("should convert AGGREGATE_ROOT from ElementKind")
        void shouldConvertAggregateRoot() {
            assertThat(ArchKind.fromElementKind(ElementKind.AGGREGATE_ROOT)).isEqualTo(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should convert ENTITY from ElementKind")
        void shouldConvertEntity() {
            assertThat(ArchKind.fromElementKind(ElementKind.ENTITY)).isEqualTo(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("should convert VALUE_OBJECT from ElementKind")
        void shouldConvertValueObject() {
            assertThat(ArchKind.fromElementKind(ElementKind.VALUE_OBJECT)).isEqualTo(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should convert IDENTIFIER from ElementKind")
        void shouldConvertIdentifier() {
            assertThat(ArchKind.fromElementKind(ElementKind.IDENTIFIER)).isEqualTo(ArchKind.IDENTIFIER);
        }

        @Test
        @DisplayName("should convert DOMAIN_EVENT from ElementKind")
        void shouldConvertDomainEvent() {
            assertThat(ArchKind.fromElementKind(ElementKind.DOMAIN_EVENT)).isEqualTo(ArchKind.DOMAIN_EVENT);
        }

        @Test
        @DisplayName("should convert DOMAIN_SERVICE from ElementKind")
        void shouldConvertDomainService() {
            assertThat(ArchKind.fromElementKind(ElementKind.DOMAIN_SERVICE)).isEqualTo(ArchKind.DOMAIN_SERVICE);
        }

        @Test
        @DisplayName("should convert DRIVING_PORT from ElementKind")
        void shouldConvertDrivingPort() {
            assertThat(ArchKind.fromElementKind(ElementKind.DRIVING_PORT)).isEqualTo(ArchKind.DRIVING_PORT);
        }

        @Test
        @DisplayName("should convert DRIVEN_PORT from ElementKind")
        void shouldConvertDrivenPort() {
            assertThat(ArchKind.fromElementKind(ElementKind.DRIVEN_PORT)).isEqualTo(ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("should convert APPLICATION_SERVICE from ElementKind")
        void shouldConvertApplicationService() {
            assertThat(ArchKind.fromElementKind(ElementKind.APPLICATION_SERVICE))
                    .isEqualTo(ArchKind.APPLICATION_SERVICE);
        }

        @Test
        @DisplayName("should convert UNCLASSIFIED from ElementKind")
        void shouldConvertUnclassified() {
            assertThat(ArchKind.fromElementKind(ElementKind.UNCLASSIFIED)).isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("should convert unsupported ElementKind to UNCLASSIFIED")
        void shouldConvertUnsupportedToUnclassified() {
            // DRIVING_ADAPTER and DRIVEN_ADAPTER are not classified - they're generated
            assertThat(ArchKind.fromElementKind(ElementKind.DRIVING_ADAPTER)).isEqualTo(ArchKind.UNCLASSIFIED);
            assertThat(ArchKind.fromElementKind(ElementKind.DRIVEN_ADAPTER)).isEqualTo(ArchKind.UNCLASSIFIED);
        }
    }

    @Nested
    @DisplayName("Completeness")
    class Completeness {

        @Test
        @DisplayName("should have exactly 12 values")
        void shouldHaveExactNumberOfValues() {
            // Domain: 6, Ports: 2, Application: 3, Fallback: 1
            assertThat(ArchKind.values()).hasSize(12);
        }
    }
}
