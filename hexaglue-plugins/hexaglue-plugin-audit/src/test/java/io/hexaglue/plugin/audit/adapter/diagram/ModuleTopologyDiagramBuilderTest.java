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

package io.hexaglue.plugin.audit.adapter.diagram;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.report.ModuleTopology;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModuleTopologyDiagramBuilder}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleTopologyDiagramBuilder")
class ModuleTopologyDiagramBuilderTest {

    private final ModuleTopologyDiagramBuilder builder = new ModuleTopologyDiagramBuilder();

    @Nested
    @DisplayName("build()")
    class Build {

        @Test
        @DisplayName("should return empty when topology is null")
        void shouldReturnEmptyWhenTopologyIsNull() {
            Optional<String> result = builder.build(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when topology has no modules")
        void shouldReturnEmptyWhenNoModules() {
            ModuleTopology topology = new ModuleTopology(List.of(), "0 modules");

            Optional<String> result = builder.build(topology);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should generate diagram with module nodes")
        void shouldGenerateDiagramWithModuleNodes() {
            var modules = List.of(
                    new ModuleTopology.ModuleInfo("banking-core", "DOMAIN", 10, List.of("com.example.domain")),
                    new ModuleTopology.ModuleInfo(
                            "banking-persistence", "INFRASTRUCTURE", 5, List.of("com.example.infra")));
            ModuleTopology topology = new ModuleTopology(modules, "2 modules detected");

            Optional<String> result = builder.build(topology);

            assertThat(result).isPresent();
            String diagram = result.get();
            assertThat(diagram).startsWith("graph TB");
            assertThat(diagram).contains("banking-core");
            assertThat(diagram).contains("banking-persistence");
            assertThat(diagram).contains("DOMAIN");
            assertThat(diagram).contains("INFRASTRUCTURE");
            assertThat(diagram).contains("10 types");
            assertThat(diagram).contains("5 types");
        }

        @Test
        @DisplayName("should apply role-based colors")
        void shouldApplyRoleBasedColors() {
            var modules = List.of(
                    new ModuleTopology.ModuleInfo("core", "DOMAIN", 3, List.of()),
                    new ModuleTopology.ModuleInfo("infra", "INFRASTRUCTURE", 2, List.of()));
            ModuleTopology topology = new ModuleTopology(modules, "summary");

            String diagram = builder.build(topology).orElseThrow();

            // DOMAIN = green (#2d6a4f), INFRASTRUCTURE = blue (#1d3557)
            assertThat(diagram).contains("fill:#2d6a4f");
            assertThat(diagram).contains("fill:#1d3557");
        }

        @Test
        @DisplayName("should sanitize module IDs for Mermaid node identifiers")
        void shouldSanitizeModuleIds() {
            var modules = List.of(new ModuleTopology.ModuleInfo("my-module.v2", "DOMAIN", 1, List.of()));
            ModuleTopology topology = new ModuleTopology(modules, "summary");

            String diagram = builder.build(topology).orElseThrow();

            // Hyphens and dots should be replaced with underscores
            assertThat(diagram).contains("my_module_v2");
        }
    }
}
