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
 * Tests for {@link QueryHandler}.
 *
 * @since 4.1.0
 */
@DisplayName("QueryHandler")
class QueryHandlerTest {

    private static final TypeId ID = TypeId.of("com.example.GetOrderHandler");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.CLASS).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.OUTBOUND_ONLY, "explicit-query-handler", "Has QueryHandler annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            QueryHandler handler = QueryHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler.id()).isEqualTo(ID);
            assertThat(handler.structure()).isEqualTo(STRUCTURE);
            assertThat(handler.classification()).isEqualTo(TRACE);
        }

        @Test
        @DisplayName("should return QUERY_HANDLER kind")
        void shouldReturnQueryHandlerKind() {
            // when
            QueryHandler handler = QueryHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler.kind()).isEqualTo(ArchKind.QUERY_HANDLER);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> QueryHandler.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> QueryHandler.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> QueryHandler.of(ID, STRUCTURE, null))
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
            QueryHandler handler = QueryHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler).isInstanceOf(ApplicationType.class);
            assertThat(handler).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            QueryHandler handler = QueryHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(handler.qualifiedName()).isEqualTo("com.example.GetOrderHandler");
            assertThat(handler.simpleName()).isEqualTo("GetOrderHandler");
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
            QueryHandler h1 = QueryHandler.of(ID, STRUCTURE, TRACE);
            QueryHandler h2 = QueryHandler.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(h1).isEqualTo(h2);
            assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.ListOrdersHandler");
            QueryHandler h1 = QueryHandler.of(ID, STRUCTURE, TRACE);
            QueryHandler h2 = QueryHandler.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(h1).isNotEqualTo(h2);
        }
    }
}
