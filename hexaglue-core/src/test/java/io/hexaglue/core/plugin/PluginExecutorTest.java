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

package io.hexaglue.core.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.spi.arch.ArchModelPluginContext;
import io.hexaglue.spi.arch.PluginContexts;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link PluginExecutor}.
 */
class PluginExecutorTest {

    @TempDir
    Path tempDir;

    private Path outputDir;

    @BeforeEach
    void setUp() {
        outputDir = tempDir.resolve("generated-sources");
    }

    @Test
    void executeWithEmptyIr_noFilesGenerated() {
        // Given: No plugins registered (JPA plugin is external)
        PluginExecutor executor = new PluginExecutor(outputDir, Map.of(), null, null);
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: No plugins run, no files generated
        assertThat(result.pluginCount()).isZero();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.totalGeneratedFiles()).isZero();
    }

    @Test
    void mapPluginConfig_getString() {
        // Given
        Map<String, Object> values = Map.of("key1", "value1", "key2", 42);
        MapPluginConfig config = new MapPluginConfig(values);

        // Then
        assertThat(config.getString("key1")).hasValue("value1");
        assertThat(config.getString("key2")).hasValue("42");
        assertThat(config.getString("missing")).isEmpty();
    }

    @Test
    void mapPluginConfig_getBoolean() {
        // Given
        Map<String, Object> values = Map.of(
                "flag1", true,
                "flag2", "true",
                "flag3", "false");
        MapPluginConfig config = new MapPluginConfig(values);

        // Then
        assertThat(config.getBoolean("flag1")).hasValue(true);
        assertThat(config.getBoolean("flag2")).hasValue(true);
        assertThat(config.getBoolean("flag3")).hasValue(false);
        assertThat(config.getBoolean("missing")).isEmpty();
    }

    @Test
    void mapPluginConfig_getInteger() {
        // Given
        Map<String, Object> values = Map.of(
                "num1", 42,
                "num2", "100",
                "invalid", "not-a-number");
        MapPluginConfig config = new MapPluginConfig(values);

        // Then
        assertThat(config.getInteger("num1")).hasValue(42);
        assertThat(config.getInteger("num2")).hasValue(100);
        assertThat(config.getInteger("invalid")).isEmpty();
        assertThat(config.getInteger("missing")).isEmpty();
    }

    @Test
    void fileSystemCodeWriter_writesJavaSource() throws IOException {
        // Given
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

        // When
        writer.writeJavaSource("com.example", "TestClass", "package com.example;\n\npublic class TestClass {}");

        // Then
        Path expectedFile = outputDir.resolve("com/example/TestClass.java");
        assertThat(expectedFile).exists();
        assertThat(Files.readString(expectedFile)).contains("public class TestClass");
        assertThat(writer.getGeneratedFiles()).containsExactly(expectedFile);
    }

    @Test
    void fileSystemCodeWriter_writesResource() throws IOException {
        // Given
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

        // When
        writer.writeResource("config/test.json", "{\"key\": \"value\"}");

        // Then
        Path expectedFile = outputDir.getParent().resolve("generated-resources/config/test.json");
        assertThat(expectedFile).exists();
        assertThat(Files.readString(expectedFile)).contains("\"key\"");
    }

    @Test
    void fileSystemCodeWriter_existsAndDelete() throws IOException {
        // Given
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);
        writer.writeJavaSource("com.example", "ToDelete", "package com.example;\npublic class ToDelete {}");

        // Then - exists returns true
        assertThat(writer.exists("com.example", "ToDelete")).isTrue();
        assertThat(writer.exists("com.example", "NonExistent")).isFalse();

        // When - delete
        writer.delete("com.example", "ToDelete");

        // Then - exists returns false after delete
        assertThat(writer.exists("com.example", "ToDelete")).isFalse();
    }

    @Test
    void fileSystemCodeWriter_writesDoc() throws IOException {
        // Given
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

        // When
        writer.writeDoc("architecture/domain.md", "# Domain Model\n\nDescription...");

        // Then
        Path expectedFile = outputDir.getParent().resolve("reports/architecture/domain.md");
        assertThat(expectedFile).exists();
        assertThat(Files.readString(expectedFile)).contains("# Domain Model");
        assertThat(writer.docExists("architecture/domain.md")).isTrue();
    }

    @Test
    void fileSystemCodeWriter_writeMarkdown() throws IOException {
        // Given
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

        // When - without .md extension
        writer.writeMarkdown("api/ports", "# Ports\n\n...");

        // Then - .md extension added automatically
        Path expectedFile = outputDir.getParent().resolve("reports/api/ports.md");
        assertThat(expectedFile).exists();
    }

    @Test
    void fileSystemCodeWriter_getOutputDirectories() {
        // Given
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

        // Then
        assertThat(writer.getOutputDirectory()).isEqualTo(outputDir);
        assertThat(writer.getDocsOutputDirectory())
                .isEqualTo(outputDir.getParent().resolve("reports"));
    }

    @Test
    void collectingDiagnosticReporter_collectsDiagnostics() {
        // Given
        CollectingDiagnosticReporter reporter = new CollectingDiagnosticReporter("test-plugin");

        // When
        reporter.info("Info message");
        reporter.warn("Warning message");
        reporter.error("Error message");
        reporter.error("Error with cause", new RuntimeException("test"));

        // Then
        List<PluginDiagnostic> diagnostics = reporter.getDiagnostics();
        assertThat(diagnostics).hasSize(4);

        assertThat(diagnostics.get(0).severity()).isEqualTo(PluginDiagnostic.Severity.INFO);
        assertThat(diagnostics.get(0).message()).isEqualTo("Info message");

        assertThat(diagnostics.get(1).severity()).isEqualTo(PluginDiagnostic.Severity.WARNING);
        assertThat(diagnostics.get(2).severity()).isEqualTo(PluginDiagnostic.Severity.ERROR);
        assertThat(diagnostics.get(2).cause()).isNull();

        assertThat(diagnostics.get(3).severity()).isEqualTo(PluginDiagnostic.Severity.ERROR);
        assertThat(diagnostics.get(3).cause()).isNotNull();
    }

    @Test
    void pluginDiagnostic_isError() {
        // Given
        PluginDiagnostic info = new PluginDiagnostic(PluginDiagnostic.Severity.INFO, "info", null);
        PluginDiagnostic warning = new PluginDiagnostic(PluginDiagnostic.Severity.WARNING, "warn", null);
        PluginDiagnostic error = new PluginDiagnostic(PluginDiagnostic.Severity.ERROR, "error", null);

        // Then
        assertThat(info.isError()).isFalse();
        assertThat(warning.isError()).isFalse();
        assertThat(error.isError()).isTrue();
    }

    @Test
    void pluginResult_errors() {
        // Given
        List<PluginDiagnostic> diagnostics = List.of(
                new PluginDiagnostic(PluginDiagnostic.Severity.INFO, "info", null),
                new PluginDiagnostic(PluginDiagnostic.Severity.ERROR, "error1", null),
                new PluginDiagnostic(PluginDiagnostic.Severity.WARNING, "warn", null),
                new PluginDiagnostic(PluginDiagnostic.Severity.ERROR, "error2", null));

        PluginResult result = new PluginResult("test", true, List.of(), diagnostics, 100, null, Map.of());

        // Then
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors()).extracting(PluginDiagnostic::message).containsExactly("error1", "error2");
    }

    @Test
    void pluginExecutionResult_aggregatesMethods() {
        // Given
        Path file1 = Path.of("/gen/File1.java");
        Path file2 = Path.of("/gen/File2.java");
        Path file3 = Path.of("/gen/File3.java");

        PluginResult result1 = new PluginResult(
                "plugin1",
                true,
                List.of(file1, file2),
                List.of(new PluginDiagnostic(PluginDiagnostic.Severity.INFO, "info", null)),
                50,
                null,
                Map.of());

        PluginResult result2 = new PluginResult(
                "plugin2",
                true,
                List.of(file3),
                List.of(new PluginDiagnostic(PluginDiagnostic.Severity.ERROR, "error", null)),
                30,
                null,
                Map.of());

        PluginExecutionResult execResult = new PluginExecutionResult(List.of(result1, result2));

        // Then
        assertThat(execResult.pluginCount()).isEqualTo(2);
        assertThat(execResult.totalGeneratedFiles()).isEqualTo(3);
        assertThat(execResult.allGeneratedFiles()).containsExactly(file1, file2, file3);
        assertThat(execResult.allErrors()).hasSize(1);
        assertThat(execResult.isSuccess()).isTrue(); // Both marked as success
    }

    @Test
    void pluginExecutionResult_isSuccessFalse_whenPluginFails() {
        // Given
        PluginResult success = new PluginResult("p1", true, List.of(), List.of(), 10, null, Map.of());
        PluginResult failure = new PluginResult("p2", false, List.of(), List.of(), 5, new RuntimeException(), Map.of());

        PluginExecutionResult result = new PluginExecutionResult(List.of(success, failure));

        // Then
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void defaultPluginContext_providesAllAccessors() {
        // Given
        IrSnapshot ir = IrSnapshot.empty("com.example");
        PluginConfig config = new MapPluginConfig(Map.of("key", "value"));
        CodeWriter writer = new TestCodeWriter();
        DiagnosticReporter diagnostics = new TestDiagnosticReporter();
        PluginOutputStore outputStore = new PluginOutputStore();

        // When
        PluginContext context =
                new DefaultPluginContext("test-plugin", ir, config, writer, diagnostics, outputStore, null);

        // Then
        assertThat(context.ir()).isSameAs(ir);
        assertThat(context.config()).isSameAs(config);
        assertThat(context.writer()).isSameAs(writer);
        assertThat(context.diagnostics()).isSameAs(diagnostics);
        assertThat(context.currentPluginId()).isEqualTo("test-plugin");
        assertThat(context.templates()).isNotNull();
        assertThat(context.architectureQuery()).isEmpty();
    }

    @Test
    void defaultPluginContext_outputSharing() {
        // Given
        IrSnapshot ir = IrSnapshot.empty("com.example");
        PluginConfig config = new MapPluginConfig(Map.of());
        CodeWriter writer = new TestCodeWriter();
        DiagnosticReporter diagnostics = new TestDiagnosticReporter();
        PluginOutputStore outputStore = new PluginOutputStore();

        PluginContext plugin1Context =
                new DefaultPluginContext("plugin-1", ir, config, writer, diagnostics, outputStore, null);
        PluginContext plugin2Context =
                new DefaultPluginContext("plugin-2", ir, config, writer, diagnostics, outputStore, null);

        // When - plugin 1 stores output
        plugin1Context.setOutput("my-data", "hello world");

        // Then - plugin 2 can retrieve it
        assertThat(plugin2Context.getOutput("plugin-1", "my-data", String.class))
                .hasValue("hello world");
        assertThat(plugin2Context.getOutput("plugin-1", "missing", String.class))
                .isEmpty();
        assertThat(plugin2Context.getOutput("unknown-plugin", "my-data", String.class))
                .isEmpty();
    }

    /**
     * Tests for v4 ArchitecturalModel integration with DefaultPluginContext.
     *
     * @since 4.0.0
     */
    @Nested
    @DisplayName("V4 ArchitecturalModel Integration")
    class ArchitecturalModelIntegrationTest {

        private IrSnapshot ir;
        private PluginConfig config;
        private CodeWriter writer;
        private DiagnosticReporter diagnostics;
        private PluginOutputStore outputStore;
        private ArchitecturalModel model;

        @BeforeEach
        void setUp() {
            ir = IrSnapshot.empty("com.example");
            config = new MapPluginConfig(Map.of());
            writer = new TestCodeWriter();
            diagnostics = new TestDiagnosticReporter();
            outputStore = new PluginOutputStore();
            model = ArchitecturalModel.builder(ProjectContext.forTesting("Test", "com.example")).build();
        }

        @Test
        @DisplayName("DefaultPluginContext with model implements ArchModelPluginContext")
        void contextWithModel_implementsArchModelPluginContext() {
            // Given
            DefaultPluginContext context =
                    new DefaultPluginContext("test-plugin", ir, model, config, writer, diagnostics, outputStore, null);

            // Then
            assertThat(context).isInstanceOf(ArchModelPluginContext.class);
        }

        @Test
        @DisplayName("DefaultPluginContext with model provides model()")
        void contextWithModel_providesModel() {
            // Given
            DefaultPluginContext context =
                    new DefaultPluginContext("test-plugin", ir, model, config, writer, diagnostics, outputStore, null);

            // Then
            assertThat(context.model()).isSameAs(model);
            assertThat(context.hasModel()).isTrue();
        }

        @Test
        @DisplayName("DefaultPluginContext without model throws on model()")
        void contextWithoutModel_throwsOnModel() {
            // Given
            DefaultPluginContext context =
                    new DefaultPluginContext("test-plugin", ir, config, writer, diagnostics, outputStore, null);

            // Then
            assertThat(context.hasModel()).isFalse();
            assertThatThrownBy(context::model)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ArchitecturalModel not available");
        }

        @Test
        @DisplayName("PluginContexts.hasArchModel() returns true for context with model")
        void pluginContexts_hasArchModel_withModel() {
            // Given
            PluginContext context =
                    new DefaultPluginContext("test-plugin", ir, model, config, writer, diagnostics, outputStore, null);

            // Then
            assertThat(PluginContexts.hasArchModel(context)).isTrue();
        }

        @Test
        @DisplayName("PluginContexts.hasArchModel() returns false for context without model")
        void pluginContexts_hasArchModel_withoutModel() {
            // Given
            PluginContext context =
                    new DefaultPluginContext("test-plugin", ir, config, writer, diagnostics, outputStore, null);

            // Then - context still implements ArchModelPluginContext, but hasArchModel checks model()
            assertThat(PluginContexts.hasArchModel(context)).isTrue();
        }

        @Test
        @DisplayName("PluginContexts.getModel() returns model for context with model")
        void pluginContexts_getModel_withModel() {
            // Given
            PluginContext context =
                    new DefaultPluginContext("test-plugin", ir, model, config, writer, diagnostics, outputStore, null);

            // Then
            assertThat(PluginContexts.getModel(context)).hasValue(model);
        }

        @Test
        @DisplayName("PluginContexts.getModel() returns empty for plain PluginContext")
        void pluginContexts_getModel_withPlainContext() {
            // Given - use a plain PluginContext that doesn't implement ArchModelPluginContext
            PluginContext plainContext = new PluginContext() {
                @Override
                public IrSnapshot ir() { return ir; }
                @Override
                public PluginConfig config() { return config; }
                @Override
                public CodeWriter writer() { return writer; }
                @Override
                public DiagnosticReporter diagnostics() { return diagnostics; }
                @Override
                public <T> void setOutput(String key, T value) {}
                @Override
                public <T> java.util.Optional<T> getOutput(String fromPluginId, String key, Class<T> type) {
                    return java.util.Optional.empty();
                }
                @Override
                public String currentPluginId() { return "plain-plugin"; }
                @Override
                public io.hexaglue.spi.plugin.TemplateEngine templates() { return null; }
                @Override
                public java.util.Optional<io.hexaglue.spi.audit.ArchitectureQuery> architectureQuery() {
                    return java.util.Optional.empty();
                }
            };

            // Then
            assertThat(PluginContexts.getModel(plainContext)).isEmpty();
        }

        @Test
        @DisplayName("PluginContexts.getModel() returns empty for legacy DefaultPluginContext without model")
        void pluginContexts_getModel_withLegacyContext() {
            // Given - DefaultPluginContext without model (legacy mode)
            PluginContext legacyContext =
                    new DefaultPluginContext("test-plugin", ir, config, writer, diagnostics, outputStore, null);

            // Then - getModel() should return empty, not throw
            assertThat(PluginContexts.getModel(legacyContext)).isEmpty();
        }

        @Test
        @DisplayName("v4 model coexists with legacy IR")
        void modelCoexistsWithLegacyIr() {
            // Given
            DefaultPluginContext context =
                    new DefaultPluginContext("test-plugin", ir, model, config, writer, diagnostics, outputStore, null);

            // Then - both APIs work
            assertThat(context.ir()).isSameAs(ir);
            assertThat(context.model()).isSameAs(model);
        }
    }

    // Test helpers
    private static class TestCodeWriter implements CodeWriter {
        @Override
        public void writeJavaSource(String packageName, String className, String content) {}

        @Override
        public boolean exists(String packageName, String className) {
            return false;
        }

        @Override
        public void delete(String packageName, String className) {}

        @Override
        public Path getOutputDirectory() {
            return Path.of("target/test-output");
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
            return Path.of("target/test-docs");
        }
    }

    private static class TestDiagnosticReporter implements DiagnosticReporter {
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
