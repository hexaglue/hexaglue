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
import io.hexaglue.core.engine.OverwritePolicy;
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

    @Nested
    @DisplayName("plugin output override")
    class PluginOutputOverride {

        private Path coreSrcMain;
        private Path infraSrcMain;

        @BeforeEach
        void setUp() throws IOException {
            coreSrcMain = Files.createDirectories(tempDir.resolve("banking-core/src/main/java"));
            infraSrcMain = Files.createDirectories(tempDir.resolve("banking-persistence/src/main/java"));
        }

        @Nested
        @DisplayName("with relative override path")
        class RelativeOverride {

            @Test
            @DisplayName("should resolve relative path against each module's baseDir")
            void shouldResolveRelativePathAgainstModuleBaseDir() throws IOException {
                Path relativeOverride = Path.of("src/main/java");
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, relativeOverride);

                writer.writeJavaSource("banking-core", "com.example.domain", "Order", "class Order {}");
                writer.writeJavaSource(
                        "banking-persistence", "com.example.infra", "OrderEntity", "class OrderEntity {}");

                // Should write to module.baseDir().resolve("src/main/java") instead of module.outputDirectory()
                Path expectedCoreFile = coreSrcMain.resolve("com/example/domain/Order.java");
                Path expectedInfraFile = infraSrcMain.resolve("com/example/infra/OrderEntity.java");

                assertThat(expectedCoreFile).exists();
                assertThat(Files.readString(expectedCoreFile)).isEqualTo("class Order {}");

                assertThat(expectedInfraFile).exists();
                assertThat(Files.readString(expectedInfraFile)).isEqualTo("class OrderEntity {}");
            }

            @Test
            @DisplayName("should resolve different relative paths for each module")
            void shouldResolveDifferentRelativePathsForEachModule() throws IOException {
                Path customDir = Files.createDirectories(tempDir.resolve("banking-core/custom/output"));
                Path relativeOverride = Path.of("custom/output");

                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir, relativeOverride);

                writer.writeJavaSource("banking-core", "com.example", "Test", "class Test {}");

                Path expectedFile = customDir.resolve("com/example/Test.java");
                assertThat(expectedFile).exists();
            }

            @Test
            @DisplayName("should support nested relative paths")
            void shouldSupportNestedRelativePaths() throws IOException {
                Path nestedDir = Files.createDirectories(tempDir.resolve("banking-core/generated/sources/hexaglue"));
                Path relativeOverride = Path.of("generated/sources/hexaglue");

                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir, relativeOverride);

                writer.writeJavaSource("banking-core", "com.example", "Generated", "class Generated {}");

                Path expectedFile = nestedDir.resolve("com/example/Generated.java");
                assertThat(expectedFile).exists();
            }

            @Test
            @DisplayName("should handle relative path with multiple modules")
            void shouldHandleRelativePathWithMultipleModules() throws IOException {
                Path relativeOverride = Path.of("src/main/java");

                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, relativeOverride);

                writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
                writer.writeJavaSource("banking-persistence", "com.example", "B", "class B {}");

                assertThat(coreSrcMain.resolve("com/example/A.java")).exists();
                assertThat(infraSrcMain.resolve("com/example/B.java")).exists();
                // Should not write to original outputDirectory
                assertThat(coreOutputDir.resolve("com/example/A.java")).doesNotExist();
                assertThat(infraOutputDir.resolve("com/example/B.java")).doesNotExist();
            }

            @Test
            @DisplayName("getOutputDirectory(moduleId) should return overridden path")
            void getOutputDirectoryShouldReturnOverriddenPath() throws IOException {
                Path relativeOverride = Path.of("src/main/java");
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, relativeOverride);

                assertThat(writer.getOutputDirectory("banking-core")).isEqualTo(coreSrcMain);
                assertThat(writer.getOutputDirectory("banking-persistence")).isEqualTo(infraSrcMain);
            }
        }

        @Nested
        @DisplayName("with absolute override path")
        class AbsoluteOverride {

            @Test
            @DisplayName("should use absolute path directly for all modules")
            void shouldUseAbsolutePathDirectlyForAllModules() throws IOException {
                Path absoluteOverride = Files.createDirectories(tempDir.resolve("shared-output"));
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, absoluteOverride);

                writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
                writer.writeJavaSource("banking-persistence", "com.example", "B", "class B {}");

                // Both should write to the same absolute directory
                assertThat(absoluteOverride.resolve("com/example/A.java")).exists();
                assertThat(absoluteOverride.resolve("com/example/B.java")).exists();

                // Should not write to module-specific directories
                assertThat(coreOutputDir.resolve("com/example/A.java")).doesNotExist();
                assertThat(infraOutputDir.resolve("com/example/B.java")).doesNotExist();
            }

            @Test
            @DisplayName("getOutputDirectory(moduleId) should return same absolute path for all modules")
            void getOutputDirectoryShouldReturnSameAbsolutePathForAllModules() throws IOException {
                Path absoluteOverride = Files.createDirectories(tempDir.resolve("shared-output"));
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, absoluteOverride);

                assertThat(writer.getOutputDirectory("banking-core")).isEqualTo(absoluteOverride);
                assertThat(writer.getOutputDirectory("banking-persistence")).isEqualTo(absoluteOverride);
            }

            @Test
            @DisplayName("should create parent directories if absolute path does not exist")
            void shouldCreateParentDirectoriesIfAbsolutePathDoesNotExist() throws IOException {
                Path absoluteOverride = tempDir.resolve("non-existent/deep/path");
                // Don't pre-create this directory

                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir, absoluteOverride);

                writer.writeJavaSource("banking-core", "com.example", "Test", "class Test {}");

                Path expectedFile = absoluteOverride.resolve("com/example/Test.java");
                assertThat(expectedFile).exists();
            }
        }

        @Nested
        @DisplayName("with null override (default behavior)")
        class NullOverride {

            @Test
            @DisplayName("should use module outputDirectory when override is null")
            void shouldUseModuleOutputDirectoryWhenOverrideIsNull() throws IOException {
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, null);

                writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
                writer.writeJavaSource("banking-persistence", "com.example", "B", "class B {}");

                // Should use the original outputDirectory from ModuleSourceSet
                assertThat(coreOutputDir.resolve("com/example/A.java")).exists();
                assertThat(infraOutputDir.resolve("com/example/B.java")).exists();
            }

            @Test
            @DisplayName("getOutputDirectory(moduleId) should return module outputDirectory")
            void getOutputDirectoryShouldReturnModuleOutputDirectory() {
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, null);

                assertThat(writer.getOutputDirectory("banking-core")).isEqualTo(coreOutputDir);
                assertThat(writer.getOutputDirectory("banking-persistence")).isEqualTo(infraOutputDir);
            }

            @Test
            @DisplayName("should behave identically to original constructor")
            void shouldBehaveIdenticallyToOriginalConstructor() throws IOException {
                MultiModuleCodeWriter writerWithNull =
                        new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir, null);
                MultiModuleCodeWriter writerOriginal = new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir);

                writerWithNull.writeJavaSource("banking-core", "com.example", "A", "class A {}");
                writerOriginal.writeJavaSource("banking-core", "com.example", "B", "class B {}");

                assertThat(writerWithNull.getOutputDirectory("banking-core"))
                        .isEqualTo(writerOriginal.getOutputDirectory("banking-core"));
                assertThat(coreOutputDir.resolve("com/example/A.java")).exists();
                assertThat(coreOutputDir.resolve("com/example/B.java")).exists();
            }
        }

        @Nested
        @DisplayName("edge cases")
        class EdgeCases {

            @Test
            @DisplayName("should handle empty string relative path as current directory")
            void shouldHandleEmptyStringRelativePathAsCurrentDirectory() throws IOException {
                Path emptyRelative = Path.of("");
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir, emptyRelative);

                writer.writeJavaSource("banking-core", "com.example", "Test", "class Test {}");

                // Should resolve against baseDir (same as baseDir itself)
                Path expectedFile = coreModule.baseDir().resolve("com/example/Test.java");
                assertThat(expectedFile).exists();
            }

            @Test
            @DisplayName("should handle single dot relative path as current directory")
            void shouldHandleSingleDotRelativePathAsCurrentDirectory() throws IOException {
                Path dotRelative = Path.of(".");
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule), defaultOutputDir, dotRelative);

                writer.writeJavaSource("banking-core", "com.example", "Test", "class Test {}");

                Path expectedFile = coreModule.baseDir().resolve("com/example/Test.java");
                assertThat(expectedFile).exists();
            }

            @Test
            @DisplayName("should reject null modules with override")
            void shouldRejectNullModulesWithOverride() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new MultiModuleCodeWriter(null, defaultOutputDir, Path.of("src/main/java")))
                        .withMessageContaining("modules");
            }

            @Test
            @DisplayName("should reject null defaultOutputDirectory with override")
            void shouldRejectNullDefaultOutputDirectoryWithOverride() {
                assertThatNullPointerException()
                        .isThrownBy(
                                () -> new MultiModuleCodeWriter(List.of(coreModule), null, Path.of("src/main/java")))
                        .withMessageContaining("defaultOutputDirectory");
            }

            @Test
            @DisplayName("should reject empty modules list with override")
            void shouldRejectEmptyModulesListWithOverride() {
                assertThatThrownBy(
                                () -> new MultiModuleCodeWriter(List.of(), defaultOutputDir, Path.of("src/main/java")))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("modules must not be empty");
            }
        }

        @Nested
        @DisplayName("aggregation with override")
        class AggregationWithOverride {

            @Test
            @DisplayName("getGeneratedFiles should aggregate from overridden paths")
            void getGeneratedFilesShouldAggregateFromOverriddenPaths() throws IOException {
                Path relativeOverride = Path.of("src/main/java");
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, relativeOverride);

                writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
                writer.writeJavaSource("banking-persistence", "com.example", "B", "class B {}");
                writer.writeJavaSource("com.example", "C", "class C {}"); // default writer

                assertThat(writer.getGeneratedFiles()).hasSize(3);
            }

            @Test
            @DisplayName("getGeneratedFiles should work with absolute override")
            void getGeneratedFilesShouldWorkWithAbsoluteOverride() throws IOException {
                Path absoluteOverride = Files.createDirectories(tempDir.resolve("shared-output"));
                MultiModuleCodeWriter writer =
                        new MultiModuleCodeWriter(List.of(coreModule, infraModule), defaultOutputDir, absoluteOverride);

                writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
                writer.writeJavaSource("banking-persistence", "com.example", "B", "class B {}");

                assertThat(writer.getGeneratedFiles()).hasSize(2);
            }
        }
    }

    @Nested
    @DisplayName("overwrite policy propagation")
    class OverwritePolicyPropagation {

        @Test
        @DisplayName("should skip existing file in module with NEVER policy")
        void shouldSkipExistingFileInModuleWithNeverPolicy() throws IOException {
            // Pre-create a file in the infra module output
            Path existingFile = infraOutputDir.resolve("com/example/OrderEntity.java");
            Files.createDirectories(existingFile.getParent());
            Files.writeString(existingFile, "class OrderEntity { /* original */ }");

            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule, infraModule), defaultOutputDir, null, OverwritePolicy.NEVER, Map.of());

            writer.writeJavaSource(
                    "banking-persistence", "com.example", "OrderEntity", "class OrderEntity { /* new */ }");

            // Should NOT overwrite: content should remain "original"
            assertThat(Files.readString(existingFile)).contains("original");
        }

        @Test
        @DisplayName("should write new file in module regardless of NEVER policy")
        void shouldWriteNewFileInModuleRegardlessOfNeverPolicy() throws IOException {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule, infraModule), defaultOutputDir, null, OverwritePolicy.NEVER, Map.of());

            writer.writeJavaSource("banking-core", "com.example", "NewClass", "class NewClass {}");

            Path expectedFile = coreOutputDir.resolve("com/example/NewClass.java");
            assertThat(expectedFile).exists();
            assertThat(Files.readString(expectedFile)).isEqualTo("class NewClass {}");
        }

        @Test
        @DisplayName("should overwrite in module with ALWAYS policy")
        void shouldOverwriteInModuleWithAlwaysPolicy() throws IOException {
            // Pre-create a file
            Path existingFile = coreOutputDir.resolve("com/example/Order.java");
            Files.createDirectories(existingFile.getParent());
            Files.writeString(existingFile, "class Order { /* v1 */ }");

            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule), defaultOutputDir, null, OverwritePolicy.ALWAYS, Map.of());

            writer.writeJavaSource("banking-core", "com.example", "Order", "class Order { /* v2 */ }");

            assertThat(Files.readString(existingFile)).contains("v2");
        }

        @Test
        @DisplayName("should skip manually modified file in module with IF_UNCHANGED policy")
        void shouldSkipManuallyModifiedFileInModuleWithIfUnchangedPolicy() throws IOException {
            String generatedContent = "class OrderEntity { /* generated */ }";
            String generatedChecksum = FileSystemCodeWriter.computeChecksum(generatedContent);

            Path existingFile = infraOutputDir.resolve("com/example/OrderEntity.java");
            Files.createDirectories(existingFile.getParent());
            Files.writeString(existingFile, "class OrderEntity { /* manually edited */ }");

            Map<Path, String> previousChecksums = Map.of(existingFile, generatedChecksum);
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule, infraModule),
                    defaultOutputDir,
                    null,
                    OverwritePolicy.IF_UNCHANGED,
                    previousChecksums);

            writer.writeJavaSource(
                    "banking-persistence", "com.example", "OrderEntity", "class OrderEntity { /* v2 */ }");

            // Should NOT overwrite: file was manually modified
            assertThat(Files.readString(existingFile)).contains("manually edited");
        }

        @Test
        @DisplayName("should overwrite unchanged file in module with IF_UNCHANGED policy")
        void shouldOverwriteUnchangedFileInModuleWithIfUnchangedPolicy() throws IOException {
            String generatedContent = "class OrderEntity { /* generated */ }";
            String generatedChecksum = FileSystemCodeWriter.computeChecksum(generatedContent);

            Path existingFile = infraOutputDir.resolve("com/example/OrderEntity.java");
            Files.createDirectories(existingFile.getParent());
            Files.writeString(existingFile, generatedContent); // Same content as generated

            Map<Path, String> previousChecksums = Map.of(existingFile, generatedChecksum);
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule, infraModule),
                    defaultOutputDir,
                    null,
                    OverwritePolicy.IF_UNCHANGED,
                    previousChecksums);

            writer.writeJavaSource(
                    "banking-persistence", "com.example", "OrderEntity", "class OrderEntity { /* v2 */ }");

            // Should overwrite: file was unchanged since last generation
            assertThat(Files.readString(existingFile)).contains("v2");
        }
    }

    @Nested
    @DisplayName("checksum aggregation")
    class ChecksumAggregation {

        @Test
        @DisplayName("should aggregate checksums from all module writers")
        void shouldAggregateChecksumsFromAllModuleWriters() throws IOException {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule, infraModule), defaultOutputDir, null, OverwritePolicy.ALWAYS, Map.of());

            writer.writeJavaSource("banking-core", "com.example", "Order", "class Order {}");
            writer.writeJavaSource("banking-persistence", "com.example", "OrderEntity", "class OrderEntity {}");

            Map<Path, String> checksums = writer.getGeneratedFileChecksums();
            assertThat(checksums).hasSize(2);
            assertThat(checksums.values()).allSatisfy(v -> assertThat(v).startsWith("sha256:"));
        }

        @Test
        @DisplayName("should include default writer checksums in aggregation")
        void shouldIncludeDefaultWriterChecksumsInAggregation() throws IOException {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule), defaultOutputDir, null, OverwritePolicy.ALWAYS, Map.of());

            writer.writeJavaSource("banking-core", "com.example", "A", "class A {}");
            writer.writeDoc("report.md", "# Report");

            Map<Path, String> checksums = writer.getGeneratedFileChecksums();
            assertThat(checksums).hasSize(2);
        }

        @Test
        @DisplayName("should return empty checksums when nothing generated")
        void shouldReturnEmptyChecksumsWhenNothingGenerated() {
            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule), defaultOutputDir, null, OverwritePolicy.ALWAYS, Map.of());

            assertThat(writer.getGeneratedFileChecksums()).isEmpty();
        }

        @Test
        @DisplayName("should track checksums for skipped files")
        void shouldTrackChecksumsForSkippedFiles() throws IOException {
            // Pre-create a file
            Path existingFile = coreOutputDir.resolve("com/example/Order.java");
            Files.createDirectories(existingFile.getParent());
            Files.writeString(existingFile, "class Order { /* original */ }");

            MultiModuleCodeWriter writer = new MultiModuleCodeWriter(
                    List.of(coreModule), defaultOutputDir, null, OverwritePolicy.NEVER, Map.of());

            writer.writeJavaSource("banking-core", "com.example", "Order", "class Order { /* new */ }");

            // Even though the write was skipped, checksums should be tracked
            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);
            assertThat(writer.getGeneratedFiles()).hasSize(1);
        }
    }
}
