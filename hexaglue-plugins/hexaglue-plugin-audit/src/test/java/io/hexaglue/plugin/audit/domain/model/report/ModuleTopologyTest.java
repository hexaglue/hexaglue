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

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModuleTopology}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleTopology")
class ModuleTopologyTest {

    @Nested
    @DisplayName("Record validation")
    class Validation {

        @Test
        @DisplayName("should create valid ModuleTopology")
        void shouldCreateValidModuleTopology() {
            var moduleInfo = new ModuleTopology.ModuleInfo("banking-core", "DOMAIN", 5, List.of("com.example.domain"));
            var topology = new ModuleTopology(List.of(moduleInfo), "1 module detected");

            assertThat(topology.modules()).hasSize(1);
            assertThat(topology.summary()).isEqualTo("1 module detected");
        }

        @Test
        @DisplayName("should reject null modules")
        void shouldRejectNullModules() {
            assertThatThrownBy(() -> new ModuleTopology(null, "summary")).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null summary")
        void shouldRejectNullSummary() {
            assertThatThrownBy(() -> new ModuleTopology(List.of(), null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should create immutable module list")
        void shouldCreateImmutableModuleList() {
            var moduleInfo = new ModuleTopology.ModuleInfo("banking-core", "DOMAIN", 3, List.of("com.example"));
            var topology = new ModuleTopology(List.of(moduleInfo), "summary");

            assertThat(topology.modules()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("ModuleInfo validation")
    class ModuleInfoValidation {

        @Test
        @DisplayName("should create valid ModuleInfo")
        void shouldCreateValidModuleInfo() {
            var info = new ModuleTopology.ModuleInfo(
                    "banking-core", "DOMAIN", 5, List.of("com.example.domain", "com.example.domain.model"));

            assertThat(info.moduleId()).isEqualTo("banking-core");
            assertThat(info.role()).isEqualTo("DOMAIN");
            assertThat(info.typeCount()).isEqualTo(5);
            assertThat(info.packages()).hasSize(2);
        }

        @Test
        @DisplayName("should reject null moduleId")
        void shouldRejectNullModuleId() {
            assertThatThrownBy(() -> new ModuleTopology.ModuleInfo(null, "DOMAIN", 0, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null role")
        void shouldRejectNullRole() {
            assertThatThrownBy(() -> new ModuleTopology.ModuleInfo("id", null, 0, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should handle null packages as empty list")
        void shouldHandleNullPackagesAsEmptyList() {
            var info = new ModuleTopology.ModuleInfo("id", "DOMAIN", 0, null);

            assertThat(info.packages()).isEmpty();
        }

        @Test
        @DisplayName("should create immutable packages list")
        void shouldCreateImmutablePackagesList() {
            var info = new ModuleTopology.ModuleInfo("id", "DOMAIN", 1, List.of("com.example"));

            assertThat(info.packages()).isUnmodifiable();
        }
    }
}
