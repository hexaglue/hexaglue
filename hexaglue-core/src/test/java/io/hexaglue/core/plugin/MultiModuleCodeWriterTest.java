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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.index.ModuleRole;
import io.hexaglue.core.engine.ModuleSourceSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MultiModuleCodeWriter}.
 *
 * @since 5.0.0
 */
@DisplayName("MultiModuleCodeWriter")
class MultiModuleCodeWriterTest {

    @TempDir
    Path tempDir;

    private Path coreOutputDir;
    private Path infraOutputDir;
    private Path defaultOutputDir;
    private ModuleSourceSet coreModule;
    private ModuleSourceSet infraModule;

    @BeforeEach
    void setUp() throws IOException {
        coreOutputDir = Files.createDirectories(tempDir.resolve("banking-core/target/generated-sources/hexaglue"));
        infraOutputDir =
                Files.createDirectories(tempDir.resolve("banking-persistence/target/generated-sources/hexaglue"));
        defaultOutputDir = Files.createDirectories(tempDir.resolve("default/target/generated-sources/hexaglue"));

        coreModule = new ModuleSourceSet(
                "banking-core",
                ModuleRole.DOMAIN,
                List.of(tempDir.resolve("banking-core/src/main/java")),
                List.of(),
                coreOutputDir,
                tempDir.resolve("banking-core"));

        infraModule = new ModuleSourceSet(
                "banking-persistence",
                ModuleRole.INFRASTRUCTURE,
                List.of(tempDir.resolve("banking-persistence/src/main/java")),
                List.of(),
                infraOutputDir,
                tempDir.resolve("banking-persistence"));
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should reject null modules")
        void shouldRejectNullModules() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new MultiModuleCodeWriter(null, defaultOutputDir))
                    .withMessageContaining("modules");
        }

        @Test
        @DisplayName("should reject null defaultOutputDirectory")
        void shouldRejectNullDefaultOutputDirectory() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new MultiModuleCodeWriter(List.of(coreModule), null))
                    .withMessageContaining("defaultOutputDirectory");
        }

        @Test
        @DisplayName("should reject empty modules list")
        void shouldRejectEmptyModulesList() {
            assertThatThrownBy(() -> new MultiModuleCodeWriter(List.of(), defaultOutputDir))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("modules must not be empty");
        }
    }

    @Nested
    @DisplayName("isMultiModule()")
    class IsMultiModule {

        @Test
        @DisplayName("should return true")
        void shouldReturnTrue() {
            MultiModuleCodeWriter writer =
                    new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir);

            assertThat(writer.isMultiModule()).isTrue();
        }
    }

    @Nested
    @DisplayName("writeJavaSource(moduleId, ...)")
    class WriteJavaSourceWithModule {

        @Test
        @DisplayName("should route to correct module output directory")
        void shouldRouteToCorrectModuleOutputDirectory() throws IOException {
            MultiModuleCodeWriter writer =
                    new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir);

            writer.writeJavaSource(
                    "banking-persistence", "com.example.infra", "OrderJpaEntity", "class OrderJpaEntity {}");

            Path expectedFile = infraOutputDir.resolve("com/example/infra/OrderJpaEntity.java");
            assertThat(expectedFile).exists();
            assertThat(Files.readString(expectedFile)).isEqualTo("class OrderJpaEntity {}");
        }

        @Test
        @DisplayName("should route to different modules correctly")
        void shouldRouteToDifferentModulesCorrectly() throws IOException {
            MultiModuleCodeWriter writer =
                    new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir);

            writer.writeJavaSource("banking-core", "com.example.domain", "Order", "class Order {}");
            writer.writeJavaSource("banking-persistence", "com.example.infra", "OrderEntity", "class OrderEntity {}");

            assertThat(coreOutputDir.resolve("com/example/domain/Order.java")).exists();
            assertThat(infraOutputDir.resolve("com/example/infra/OrderEntity.java"))
                    .exists();
        }

        @Test
        @DisplayName("should throw on unknown module")
        void shouldThrowOnUnknownModule() {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            assertThatThrownBy(() -> writer.writeJavaSource("unknown-module", "com.example", "Foo", "class Foo {}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown module: unknown-module");
        }

        @Test
        @DisplayName("should reject null moduleId")
        void shouldRejectNullModuleId() {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            assertThatNullPointerException()
                    .isThrownBy(() -> writer.writeJavaSource(null, "com.example", "Foo", "class Foo {}"))
                    .withMessageContaining("moduleId");
        }
    }

    @Nested
    @DisplayName("writeJavaSource(pkg, cls, content)")
    class WriteJavaSourceDefault {

        @Test
        @DisplayName("should delegate to default writer")
        void shouldDelegateToDefaultWriter() throws IOException {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            writer.writeJavaSource("com.example", "DefaultClass", "class DefaultClass {}");

            Path expectedFile = defaultOutputDir.resolve("com/example/DefaultClass.java");
            assertThat(expectedFile).exists();
        }
    }

    @Nested
    @DisplayName("getOutputDirectory(moduleId)")
    class GetOutputDirectoryWithModule {

        @Test
        @DisplayName("should return module-specific output directory")
        void shouldReturnModuleSpecificOutputDirectory() {
            MultiModuleCodeWriter writer =
                    new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir);

            assertThat(writer.getOutputDirectory("banking-core")).isEqualTo(coreOutputDir);
            assertThat(writer.getOutputDirectory("banking-persistence")).isEqualTo(infraOutputDir);
        }

        @Test
        @DisplayName("should throw on unknown module")
        void shouldThrowOnUnknownModule() {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            assertThatThrownBy(() -> writer.getOutputDirectory("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown module");
        }
    }

    @Nested
    @DisplayName("getOutputDirectory()")
    class GetOutputDirectoryDefault {

        @Test
        @DisplayName("should return default output directory")
        void shouldReturnDefaultOutputDirectory() {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            assertThat(writer.getOutputDirectory()).isEqualTo(defaultOutputDir);
        }
    }

    @Nested
    @DisplayName("getGeneratedFiles()")
    class GetGeneratedFiles {

        @Test
        @DisplayName("should aggregate files from all writers")
        void shouldAggregateFilesFromAllWriters() throws IOException {
            MultiModuleCodeWriter writer =
                    new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir);

            writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
            writer.writeJavaSource("banking-persistence", "com.example", "B", "class B {}");
            writer.writeJavaSource("com.example", "C", "class C {}");

            assertThat(writer.getGeneratedFiles()).hasSize(3);
        }

        @Test
        @DisplayName("should return empty list when nothing generated")
        void shouldReturnEmptyListWhenNothingGenerated() {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            assertThat(writer.getGeneratedFiles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("documentation delegation")
    class DocumentationDelegation {

        @Test
        @DisplayName("should write docs to default writer")
        void shouldWriteDocsToDefaultWriter() throws IOException {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

            writer.writeDoc("audit/report.html", "<html>Report</html>");

            assertThat(writer.getDocsOutputDirectory()).isNotNull();
            assertThat(writer.getGeneratedFiles()).hasSize(1);
        }
    }
}
