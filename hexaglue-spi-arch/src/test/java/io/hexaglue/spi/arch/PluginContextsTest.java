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
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import io.hexaglue.spi.plugin.TemplateEngine;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PluginContexts}.
 */
@DisplayName("PluginContexts")
class PluginContextsTest {

    @Nested
    @DisplayName("hasArchModel()")
    class HasArchModelTest {

        @Test
        @DisplayName("should return true for ArchModelPluginContext")
        void shouldReturnTrueForArchModelContext() {
            PluginContext context = createArchModelContext();
            assertThat(PluginContexts.hasArchModel(context)).isTrue();
        }

        @Test
        @DisplayName("should return false for plain PluginContext")
        void shouldReturnFalseForPlainContext() {
            PluginContext context = createPlainContext();
            assertThat(PluginContexts.hasArchModel(context)).isFalse();
        }
    }

    @Nested
    @DisplayName("getModel()")
    class GetModelTest {

        @Test
        @DisplayName("should return model for ArchModelPluginContext")
        void shouldReturnModelForArchModelContext() {
            PluginContext context = createArchModelContext();
            Optional<ArchitecturalModel> model = PluginContexts.getModel(context);
            assertThat(model).isPresent();
        }

        @Test
        @DisplayName("should return empty for plain PluginContext")
        void shouldReturnEmptyForPlainContext() {
            PluginContext context = createPlainContext();
            Optional<ArchitecturalModel> model = PluginContexts.getModel(context);
            assertThat(model).isEmpty();
        }
    }

    @Nested
    @DisplayName("asArchModelContext()")
    class AsArchModelContextTest {

        @Test
        @DisplayName("should return Optional with context for ArchModelPluginContext")
        void shouldReturnContextForArchModelContext() {
            PluginContext context = createArchModelContext();
            Optional<ArchModelPluginContext> archContext = PluginContexts.asArchModelContext(context);
            assertThat(archContext).isPresent();
        }

        @Test
        @DisplayName("should return empty for plain PluginContext")
        void shouldReturnEmptyForPlainContext() {
            PluginContext context = createPlainContext();
            Optional<ArchModelPluginContext> archContext = PluginContexts.asArchModelContext(context);
            assertThat(archContext).isEmpty();
        }
    }

    // ===== Helper methods =====

    private PluginContext createPlainContext() {
        return new StubPluginContext();
    }

    private ArchModelPluginContext createArchModelContext() {
        return new StubArchModelPluginContext();
    }

    /**
     * Stub implementation of PluginContext for testing.
     */
    private static class StubPluginContext implements PluginContext {
        @Override
        public IrSnapshot ir() {
            return IrSnapshot.empty("com.example");
        }

        @Override
        public PluginConfig config() {
            return new PluginConfig() {
                @Override
                public Optional<String> getString(String key) {
                    return Optional.empty();
                }

                @Override
                public Optional<Boolean> getBoolean(String key) {
                    return Optional.empty();
                }

                @Override
                public Optional<Integer> getInteger(String key) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public CodeWriter writer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DiagnosticReporter diagnostics() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> void setOutput(String key, T value) {}

        @Override
        public <T> Optional<T> getOutput(String pluginId, String key, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public String currentPluginId() {
            return "test.plugin";
        }

        @Override
        public TemplateEngine templates() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ArchitectureQuery> architectureQuery() {
            return Optional.empty();
        }
    }

    /**
     * Stub implementation of ArchModelPluginContext for testing.
     */
    private static class StubArchModelPluginContext extends StubPluginContext implements ArchModelPluginContext {
        @Override
        public ArchitecturalModel model() {
            // Return a minimal model for testing
            return ArchitecturalModel.builder(io.hexaglue.arch.ProjectContext.forTesting("Test", "com.example"))
                    .build();
        }
    }
}
