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
 * Tests for {@link CommandHandler}.
 *
 * @since 4.1.0
 */
@DisplayName("CommandHandler")
class CommandHandlerTest {

    private static final TypeId ID = TypeId.of("com.example.PlaceOrderHandler");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.CLASS).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.INBOUND_ONLY, "explicit-command-handler", "Has CommandHandler annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            CommandHandler handler = CommandHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler.id()).isEqualTo(ID);
            assertThat(handler.structure()).isEqualTo(STRUCTURE);
            assertThat(handler.classification()).isEqualTo(TRACE);
        }

        @Test
        @DisplayName("should return COMMAND_HANDLER kind")
        void shouldReturnCommandHandlerKind() {
            // when
            CommandHandler handler = CommandHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler.kind()).isEqualTo(ArchKind.COMMAND_HANDLER);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> CommandHandler.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> CommandHandler.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> CommandHandler.of(ID, STRUCTURE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement ApplicationType interface")
        void shouldImplementApplicationType() {
            // given
            CommandHandler handler = CommandHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler).isInstanceOf(ApplicationType.class);
            assertThat(handler).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            CommandHandler handler = CommandHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler.qualifiedName()).isEqualTo("com.example.PlaceOrderHandler");
            assertThat(handler.simpleName()).isEqualTo("PlaceOrderHandler");
            assertThat(handler.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            CommandHandler h1 = CommandHandler.of(ID, STRUCTURE, TRACE);
            CommandHandler h2 = CommandHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(h1).isEqualTo(h2);
            assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.CancelOrderHandler");
            CommandHandler h1 = CommandHandler.of(ID, STRUCTURE, TRACE);
            CommandHandler h2 = CommandHandler.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(h1).isNotEqualTo(h2);
        }
    }
}
