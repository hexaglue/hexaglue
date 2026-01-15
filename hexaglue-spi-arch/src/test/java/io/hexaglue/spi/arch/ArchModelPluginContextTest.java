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

package io.hexaglue.spi.arch;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.spi.plugin.PluginContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArchModelPluginContext}.
 */
@DisplayName("ArchModelPluginContext")
class ArchModelPluginContextTest {

    @Nested
    @DisplayName("Interface Contract")
    class InterfaceContractTest {

        @Test
        @DisplayName("should extend PluginContext")
        void shouldExtendPluginContext() {
            assertThat(PluginContext.class).isAssignableFrom(ArchModelPluginContext.class);
        }

        @Test
        @DisplayName("should declare model() method")
        void shouldDeclareModelMethod() throws NoSuchMethodException {
            var method = ArchModelPluginContext.class.getMethod("model");
            assertThat(method.getReturnType()).isEqualTo(ArchitecturalModel.class);
        }
    }
}
