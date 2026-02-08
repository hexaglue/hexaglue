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

package io.hexaglue.plugin.livingdoc.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.index.ModuleDescriptor;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.ModuleRole;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModuleDocGenerator}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleDocGenerator")
class ModuleDocGeneratorTest {

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("should generate module documentation with all modules")
        void shouldGenerateModuleDocWithAllModules() {
            ModuleDescriptor coreModule = ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, Path.of("/tmp/core"));
            ModuleDescriptor infraModule =
                    ModuleDescriptor.of("banking-persistence", ModuleRole.INFRASTRUCTURE, Path.of("/tmp/infra"));

            ModuleIndex moduleIndex = ModuleIndex.builder()
                    .addModule(coreModule)
                    .addModule(infraModule)
                    .assignType(TypeId.of("com.example.domain.Order"), coreModule)
                    .assignType(TypeId.of("com.example.domain.Customer"), coreModule)
                    .assignType(TypeId.of("com.example.infra.OrderEntity"), infraModule)
                    .build();

            ModuleDocGenerator generator = new ModuleDocGenerator(moduleIndex);
            String output = generator.generate();

            // Title
            assertThat(output).contains("# Module Topology");

            // Summary table
            assertThat(output).contains("| Module | Role | Types | Base Package |");
            assertThat(output).contains("banking-core");
            assertThat(output).contains("banking-persistence");
            assertThat(output).contains("DOMAIN");
            assertThat(output).contains("INFRASTRUCTURE");

            // Detailed sections
            assertThat(output).contains("## banking-core");
            assertThat(output).contains("## banking-persistence");
            assertThat(output).contains("`Order`");
            assertThat(output).contains("`Customer`");
            assertThat(output).contains("`OrderEntity`");
        }

        @Test
        @DisplayName("should handle empty ModuleIndex")
        void shouldHandleEmptyModuleIndex() {
            ModuleIndex moduleIndex = ModuleIndex.builder().build();

            ModuleDocGenerator generator = new ModuleDocGenerator(moduleIndex);
            String output = generator.generate();

            assertThat(output).contains("# Module Topology");
            // Should not crash, should produce a valid markdown
            assertThat(output).isNotEmpty();
        }

        @Test
        @DisplayName("should show empty message for module with no types")
        void shouldShowEmptyMessageForModuleWithNoTypes() {
            ModuleDescriptor emptyModule =
                    ModuleDescriptor.of("empty-module", ModuleRole.SHARED, Path.of("/tmp/empty"));

            ModuleIndex moduleIndex =
                    ModuleIndex.builder().addModule(emptyModule).build();

            ModuleDocGenerator generator = new ModuleDocGenerator(moduleIndex);
            String output = generator.generate();

            assertThat(output).contains("empty-module");
            assertThat(output).contains("No classified types in this module");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null moduleIndex")
        void shouldRejectNullModuleIndex() {
            assertThatThrownBy(() -> new ModuleDocGenerator(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
