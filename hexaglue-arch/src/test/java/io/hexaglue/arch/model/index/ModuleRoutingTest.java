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

package io.hexaglue.arch.model.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleRouting}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleRouting")
class ModuleRoutingTest {

    private static final ModuleDescriptor CORE_MODULE =
            ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, Path.of("/projects/banking-core"));
    private static final ModuleDescriptor INFRA_MODULE = ModuleDescriptor.of(
            "banking-persistence", ModuleRole.INFRASTRUCTURE, Path.of("/projects/banking-persistence"));
    private static final ModuleDescriptor INFRA_MODULE_2 =
            ModuleDescriptor.of("banking-jpa", ModuleRole.INFRASTRUCTURE, Path.of("/projects/banking-jpa"));
    private static final ModuleDescriptor APP_MODULE =
            ModuleDescriptor.of("banking-service", ModuleRole.APPLICATION, Path.of("/projects/banking-service"));

    @Nested
    @DisplayName("resolveUniqueModuleByRole()")
    class ResolveUniqueModuleByRole {

        @Test
        @DisplayName("should return moduleId when exactly one module has the role")
        void shouldReturnModuleIdWhenExactlyOneModuleHasRole() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .addModule(APP_MODULE)
                    .build();

            Optional<String> result = ModuleRouting.resolveUniqueModuleByRole(index, ModuleRole.INFRASTRUCTURE);

            assertThat(result).contains("banking-persistence");
        }

        @Test
        @DisplayName("should return empty when no module has the role")
        void shouldReturnEmptyWhenNoModuleHasRole() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(APP_MODULE)
                    .build();

            Optional<String> result = ModuleRouting.resolveUniqueModuleByRole(index, ModuleRole.INFRASTRUCTURE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when multiple modules have the role")
        void shouldReturnEmptyWhenMultipleModulesHaveRole() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .addModule(INFRA_MODULE_2)
                    .build();

            Optional<String> result = ModuleRouting.resolveUniqueModuleByRole(index, ModuleRole.INFRASTRUCTURE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reject null moduleIndex")
        void shouldRejectNullModuleIndex() {
            assertThatNullPointerException()
                    .isThrownBy(() -> ModuleRouting.resolveUniqueModuleByRole(null, ModuleRole.INFRASTRUCTURE))
                    .withMessageContaining("moduleIndex");
        }

        @Test
        @DisplayName("should reject null role")
        void shouldRejectNullRole() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThatNullPointerException()
                    .isThrownBy(() -> ModuleRouting.resolveUniqueModuleByRole(index, null))
                    .withMessageContaining("role");
        }
    }
}
