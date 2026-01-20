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
 * Tests for {@link ApplicationService}.
 *
 * @since 4.1.0
 */
@DisplayName("ApplicationService")
class ApplicationServiceTest {

    private static final TypeId ID = TypeId.of("com.example.OrderApplicationService");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.CLASS).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.APPLICATION_SERVICE, "explicit-app-service", "Has ApplicationService annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            ApplicationService service = ApplicationService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.id()).isEqualTo(ID);
            assertThat(service.structure()).isEqualTo(STRUCTURE);
            assertThat(service.classification()).isEqualTo(TRACE);
        }

        @Test
        @DisplayName("should return APPLICATION_SERVICE kind")
        void shouldReturnApplicationServiceKind() {
            // when
            ApplicationService service = ApplicationService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.kind()).isEqualTo(ArchKind.APPLICATION_SERVICE);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> ApplicationService.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> ApplicationService.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> ApplicationService.of(ID, STRUCTURE, null))
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
            ApplicationService service = ApplicationService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service).isInstanceOf(ApplicationType.class);
            assertThat(service).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            ApplicationService service = ApplicationService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(service.qualifiedName()).isEqualTo("com.example.OrderApplicationService");
            assertThat(service.simpleName()).isEqualTo("OrderApplicationService");
            assertThat(service.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            ApplicationService s1 = ApplicationService.of(ID, STRUCTURE, TRACE);
            ApplicationService s2 = ApplicationService.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(s1).isEqualTo(s2);
            assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.CustomerApplicationService");
            ApplicationService s1 = ApplicationService.of(ID, STRUCTURE, TRACE);
            ApplicationService s2 = ApplicationService.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(s1).isNotEqualTo(s2);
        }
    }
}
