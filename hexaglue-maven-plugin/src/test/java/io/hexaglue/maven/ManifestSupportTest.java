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

package io.hexaglue.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.core.engine.EngineMetrics;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.GenerationManifest;
import io.hexaglue.core.engine.StaleFilePolicy;
import io.hexaglue.core.plugin.PluginExecutionResult;
import io.hexaglue.core.plugin.PluginResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ManifestSupport}.
 *
 * @since 5.0.0
 */
@DisplayName("ManifestSupport")
class ManifestSupportTest {

    private final SystemStreamLog log = new SystemStreamLog();

    @TempDir
    Path tempDir;

    private Path projectRoot() {
        return tempDir.resolve("project");
    }

    private Path outputDirectory() {
        return projectRoot().resolve("target/hexaglue/generated-sources");
    }

    private Path manifestPath() {
        return outputDirectory().getParent().resolve(ManifestSupport.MANIFEST_FILENAME);
    }

    @Nested
    @DisplayName("Build manifest from plugin results")
    class BuildManifest {

        @Test
        @DisplayName("should build manifest from plugin results and save it")
        void shouldBuildManifestFromPluginResults() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path generatedFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Foo.java");
            Files.createDirectories(generatedFile.getParent());
            Files.writeString(generatedFile, "class Foo {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(generatedFile))));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            assertThat(manifestPath()).exists();
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
            assertThat(saved.allFiles()).anyMatch(f -> f.contains("Foo.java"));
        }

        @Test
        @DisplayName("should relativize absolute paths against project root")
        void shouldRelativizeAbsolutePathsAgainstProjectRoot() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path generatedFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Bar.java");
            Files.createDirectories(generatedFile.getParent());
            Files.writeString(generatedFile, "class Bar {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(generatedFile))));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            GenerationManifest saved = GenerationManifest.load(manifestPath());
            // Paths should be relative (not start with /)
            for (String file : saved.allFiles()) {
                assertThat(file).doesNotStartWith("/");
            }
        }

        @Test
        @DisplayName("should record files from multiple plugins")
        void shouldRecordFilesFromMultiplePlugins() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path file1 = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Jpa.java");
            Path file2 = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Doc.md");
            Files.createDirectories(file1.getParent());
            Files.writeString(file1, "class Jpa {}");
            Files.writeString(file2, "# Doc");

            EngineResult result = engineResultWith(List.of(
                    pluginResult("jpa-plugin", List.of(file1)), pluginResult("living-doc-plugin", List.of(file2))));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Stale file detection")
    class StaleFileDetection {

        @Test
        @DisplayName("should detect stale files from previous manifest")
        void shouldDetectStaleFilesFromPreviousManifest() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Create a previous manifest with a src/ file that will become stale
            Path prevManifestPath = manifestPath();
            Files.createDirectories(prevManifestPath.getParent());
            GenerationManifest previous = new GenerationManifest(prevManifestPath);
            previous.recordFile("jpa-plugin", Path.of("src/main/java/com/example/OldEntity.java"));
            previous.recordFile("jpa-plugin", Path.of("target/hexaglue/generated-sources/com/example/Kept.java"));
            previous.save();

            // Current build only produces the target/ file
            Path keptFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Kept.java");
            Files.createDirectories(keptFile.getParent());
            Files.writeString(keptFile, "class Kept {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(keptFile))));

            // Should not throw with WARN policy
            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // The saved manifest should only contain the current file
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw MojoFailureException on FAIL policy with stale src/ files")
        void shouldThrowMojoFailureExceptionOnFailPolicy() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Create a previous manifest with a src/ file that will become stale
            Path prevManifestPath = manifestPath();
            Files.createDirectories(prevManifestPath.getParent());
            GenerationManifest previous = new GenerationManifest(prevManifestPath);
            previous.recordFile("jpa-plugin", Path.of("src/main/java/com/example/Stale.java"));
            previous.save();

            // Current build produces a different file
            Path newFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/New.java");
            Files.createDirectories(newFile.getParent());
            Files.writeString(newFile, "class New {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(newFile))));

            assertThatThrownBy(() -> ManifestSupport.processManifest(
                            result, projectRoot, outputDirectory(), StaleFilePolicy.FAIL, log))
                    .isInstanceOf(MojoFailureException.class)
                    .hasMessageContaining("stale");
        }
    }

    @Nested
    @DisplayName("No-op scenarios")
    class NoOpScenarios {

        @Test
        @DisplayName("should no-op when pluginResult is null")
        void shouldNoOpWhenPluginResultIsNull() throws Exception {
            EngineResult result = EngineResult.withoutPlugins(null, List.of(), emptyMetrics(), List.of());

            // Should not throw and should not create any files
            ManifestSupport.processManifest(result, projectRoot(), outputDirectory(), StaleFilePolicy.WARN, log);

            assertThat(manifestPath()).doesNotExist();
        }

        @Test
        @DisplayName("should no-op when plugin results list is empty")
        void shouldNoOpWhenPluginResultsListIsEmpty() throws Exception {
            EngineResult result = engineResultWith(List.of());

            ManifestSupport.processManifest(result, projectRoot(), outputDirectory(), StaleFilePolicy.WARN, log);

            assertThat(manifestPath()).doesNotExist();
        }

        @Test
        @DisplayName("should not save manifest when no files were generated")
        void shouldNotSaveManifestWhenNoFilesGenerated() throws Exception {
            Files.createDirectories(outputDirectory());

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of())));

            ManifestSupport.processManifest(result, projectRoot(), outputDirectory(), StaleFilePolicy.WARN, log);

            assertThat(manifestPath()).doesNotExist();
        }
    }

    @Nested
    @DisplayName("Manifest file location")
    class ManifestLocation {

        @Test
        @DisplayName("should store manifest in outputDirectory parent")
        void shouldStoreManifestInOutputDirectoryParent() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path generatedFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Foo.java");
            Files.createDirectories(generatedFile.getParent());
            Files.writeString(generatedFile, "class Foo {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(generatedFile))));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // Manifest should be at target/hexaglue/manifest.txt (parent of generated-sources)
            assertThat(manifestPath()).exists();
            assertThat(manifestPath().getParent()).isEqualTo(outputDirectory().getParent());
        }
    }

    @Nested
    @DisplayName("Checksums in manifest")
    class ChecksumsInManifest {

        @Test
        @DisplayName("should include checksums in manifest from plugin result")
        void shouldIncludeChecksumsInManifest() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path generatedFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Foo.java");
            Files.createDirectories(generatedFile.getParent());
            Files.writeString(generatedFile, "class Foo {}");

            // Create a PluginResult with checksums
            Map<String, String> checksums = Map.of(generatedFile.toString(), "sha256:abc123def456");
            PluginResult pr = pluginResultWithChecksums("jpa-plugin", List.of(generatedFile), checksums);
            EngineResult result = engineResultWith(List.of(pr));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // Verify the saved manifest contains checksums
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
            String relativePath = saved.allFiles().iterator().next();
            assertThat(saved.checksumFor(relativePath)).hasValue("sha256:abc123def456");
        }

        @Test
        @DisplayName("should build manifest with checksums round-trip via PluginResult")
        void shouldBuildManifestWithChecksumsRoundTrip() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path file1 = projectRoot.resolve("target/hexaglue/generated-sources/com/example/A.java");
            Path file2 = projectRoot.resolve("target/hexaglue/generated-sources/com/example/B.java");
            Files.createDirectories(file1.getParent());
            Files.writeString(file1, "class A {}");
            Files.writeString(file2, "class B {}");

            Map<String, String> checksums = Map.of(
                    file1.toString(), "sha256:aaa",
                    file2.toString(), "sha256:bbb");
            PluginResult pr = pluginResultWithChecksums("jpa-plugin", List.of(file1, file2), checksums);
            EngineResult result = engineResultWith(List.of(pr));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // Load and verify
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(2);
            // All files should have checksums
            for (String filePath : saved.allFiles()) {
                assertThat(saved.checksumFor(filePath)).isPresent();
            }
        }
    }

    // --- Helpers ---

    private static PluginResult pluginResult(String pluginId, List<Path> generatedFiles) {
        return new PluginResult(pluginId, true, generatedFiles, List.of(), 10L, null, Map.of());
    }

    private static PluginResult pluginResultWithChecksums(
            String pluginId, List<Path> generatedFiles, Map<String, String> checksums) {
        return new PluginResult(
                pluginId, true, generatedFiles, List.of(), 10L, null, Map.of(), java.util.Set.of(), checksums);
    }

    private static EngineResult engineResultWith(List<PluginResult> pluginResults) {
        PluginExecutionResult pluginExecResult = new PluginExecutionResult(pluginResults);
        return new EngineResult(null, List.of(), emptyMetrics(), pluginExecResult, List.of());
    }

    private static EngineMetrics emptyMetrics() {
        return new EngineMetrics(0, 0, 0, Duration.ZERO);
    }
}
