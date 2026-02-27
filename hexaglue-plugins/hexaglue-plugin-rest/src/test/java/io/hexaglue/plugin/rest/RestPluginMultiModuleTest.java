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

package io.hexaglue.plugin.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.index.ModuleDescriptor;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.ModuleRole;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for multi-module routing in {@link RestPlugin}.
 *
 * <p>Validates that generated artifacts are routed to the correct module
 * when the project is multi-module, following the same pattern as JPA plugin.
 *
 * @since 3.1.0
 */
@DisplayName("RestPlugin multi-module routing")
class RestPluginMultiModuleTest {

    private static final String BASE_PACKAGE = "com.acme.core";

    @Nested
    @DisplayName("Explicit target module")
    class ExplicitTargetModule {

        @Test
        @DisplayName("should route to explicit target module")
        void should_route_to_explicit_target_module() throws Exception {
            RecordingWriter writer = new RecordingWriter(true);
            GeneratorContext context = buildContext(writer, Optional.of("api-module"), null);

            new RestPlugin().generate(context);

            assertThat(writer.routedWrites).isNotEmpty();
            assertThat(writer.routedWrites)
                    .allSatisfy(write -> assertThat(write.moduleId).isEqualTo("api-module"));
            assertThat(writer.defaultWrites).isEmpty();
        }
    }

    @Nested
    @DisplayName("Auto-routing")
    class AutoRouting {

        @Test
        @DisplayName("should auto-route to unique API module")
        void should_auto_route_to_unique_api_module() throws Exception {
            ModuleIndex moduleIndex = ModuleIndex.builder()
                    .addModule(ModuleDescriptor.of("my-api", ModuleRole.API, Path.of("/project/my-api")))
                    .addModule(ModuleDescriptor.of("my-domain", ModuleRole.DOMAIN, Path.of("/project/my-domain")))
                    .build();

            RecordingWriter writer = new RecordingWriter(true);
            GeneratorContext context = buildContext(writer, Optional.empty(), moduleIndex);

            new RestPlugin().generate(context);

            assertThat(writer.routedWrites).isNotEmpty();
            assertThat(writer.routedWrites)
                    .allSatisfy(write -> assertThat(write.moduleId).isEqualTo("my-api"));
            assertThat(writer.defaultWrites).isEmpty();
        }
    }

    @Nested
    @DisplayName("Mono-module")
    class MonoModule {

        @Test
        @DisplayName("should use default output when mono-module")
        void should_use_default_output_when_mono_module() throws Exception {
            RecordingWriter writer = new RecordingWriter(false);
            GeneratorContext context = buildContext(writer, Optional.empty(), null);

            new RestPlugin().generate(context);

            assertThat(writer.defaultWrites).isNotEmpty();
            assertThat(writer.routedWrites).isEmpty();
        }
    }

    // === Test infrastructure ===

    /**
     * Builds a GeneratorContext with a minimal model containing one driving port with one QUERY use case.
     */
    private static GeneratorContext buildContext(
            RecordingWriter writer, Optional<String> targetModule, ModuleIndex moduleIndex) {
        UseCase query = TestUseCaseFactory.query("getAccount");
        DrivingPort port = TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(query));

        TypeRegistry registry = TypeRegistry.builder().add(port).build();
        PortIndex portIndex = PortIndex.from(registry);

        ArchitecturalModel.Builder modelBuilder =
                ArchitecturalModel.builder(ProjectContext.forTesting("test-project", BASE_PACKAGE));
        modelBuilder.typeRegistry(registry);
        modelBuilder.portIndex(portIndex);
        if (moduleIndex != null) {
            modelBuilder.moduleIndex(moduleIndex);
        }

        ArchitecturalModel model = modelBuilder.build();

        PluginConfig pluginConfig = new StubPluginConfig(targetModule.orElse(null));
        DiagnosticReporter diagnostics = new NoOpDiagnostics();

        return GeneratorContext.of(ArtifactWriter.of(writer), diagnostics, pluginConfig, new StubPluginContext(model));
    }

    /** Records writeJavaSource calls, distinguishing routed vs default writes. */
    private static final class RecordingWriter implements io.hexaglue.spi.plugin.CodeWriter {
        final List<RoutedWrite> routedWrites = new ArrayList<>();
        final List<DefaultWrite> defaultWrites = new ArrayList<>();
        private final boolean multiModule;

        RecordingWriter(boolean multiModule) {
            this.multiModule = multiModule;
        }

        @Override
        public void writeJavaSource(String packageName, String className, String content) {
            defaultWrites.add(new DefaultWrite(packageName, className));
        }

        @Override
        public void writeJavaSource(String moduleId, String packageName, String className, String content) {
            routedWrites.add(new RoutedWrite(moduleId, packageName, className));
        }

        @Override
        public boolean isMultiModule() {
            return multiModule;
        }

        @Override
        public boolean exists(String packageName, String className) {
            return false;
        }

        @Override
        public void delete(String packageName, String className) {}

        @Override
        public Path getOutputDirectory() {
            return Path.of("target/generated-sources");
        }

        @Override
        public void writeResource(String path, String content) {}

        @Override
        public boolean resourceExists(String path) {
            return false;
        }

        @Override
        public void deleteResource(String path) {}

        @Override
        public void writeDoc(String path, String content) {}

        @Override
        public boolean docExists(String path) {
            return false;
        }

        @Override
        public void deleteDoc(String path) {}

        @Override
        public Path getDocsOutputDirectory() {
            return Path.of("target/generated-docs");
        }
    }

    record RoutedWrite(String moduleId, String packageName, String className) {}

    record DefaultWrite(String packageName, String className) {}

    /** Minimal PluginConfig that returns targetModule if configured. */
    private static final class StubPluginConfig implements PluginConfig {
        private final String targetModule;

        StubPluginConfig(String targetModule) {
            this.targetModule = targetModule;
        }

        @Override
        public Optional<String> getString(String key) {
            if ("targetModule".equals(key) && targetModule != null) {
                return Optional.of(targetModule);
            }
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
    }

    /** Minimal PluginContext that exposes the model. */
    private static final class StubPluginContext implements io.hexaglue.spi.plugin.PluginContext {
        private final ArchitecturalModel model;

        StubPluginContext(ArchitecturalModel model) {
            this.model = model;
        }

        @Override
        public ArchitecturalModel model() {
            return model;
        }

        @Override
        public PluginConfig config() {
            return new StubPluginConfig(null);
        }

        @Override
        public io.hexaglue.spi.plugin.CodeWriter writer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DiagnosticReporter diagnostics() {
            return new NoOpDiagnostics();
        }

        @Override
        public <T> void setOutput(String key, T value) {}

        @Override
        public <T> Optional<T> getOutput(String pluginId, String key, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public String currentPluginId() {
            return RestPlugin.PLUGIN_ID;
        }

        @Override
        public io.hexaglue.spi.plugin.TemplateEngine templates() {
            throw new UnsupportedOperationException();
        }
    }

    /** No-op diagnostics for tests. */
    private static final class NoOpDiagnostics implements DiagnosticReporter {
        @Override
        public void info(String message) {}

        @Override
        public void warn(String message) {}

        @Override
        public void error(String message) {}

        @Override
        public void error(String message, Throwable cause) {}
    }
}
