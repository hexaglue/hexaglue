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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleRole}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleRole")
class ModuleRoleTest {

    @Test
    @DisplayName("should have exactly 6 values")
    void shouldHaveExactlySixValues() {
        assertThat(ModuleRole.values()).hasSize(6);
    }

    @Test
    @DisplayName("should contain all expected roles")
    void shouldContainAllExpectedRoles() {
        assertThat(ModuleRole.values())
                .containsExactly(
                        ModuleRole.DOMAIN,
                        ModuleRole.INFRASTRUCTURE,
                        ModuleRole.APPLICATION,
                        ModuleRole.API,
                        ModuleRole.ASSEMBLY,
                        ModuleRole.SHARED);
    }

    @Test
    @DisplayName("should resolve from name string")
    void shouldResolveFromNameString() {
        assertThat(ModuleRole.valueOf("DOMAIN")).isEqualTo(ModuleRole.DOMAIN);
        assertThat(ModuleRole.valueOf("INFRASTRUCTURE")).isEqualTo(ModuleRole.INFRASTRUCTURE);
        assertThat(ModuleRole.valueOf("APPLICATION")).isEqualTo(ModuleRole.APPLICATION);
        assertThat(ModuleRole.valueOf("API")).isEqualTo(ModuleRole.API);
        assertThat(ModuleRole.valueOf("ASSEMBLY")).isEqualTo(ModuleRole.ASSEMBLY);
        assertThat(ModuleRole.valueOf("SHARED")).isEqualTo(ModuleRole.SHARED);
    }
}
