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

    @Nested
    @DisplayName("DELETE policy stale file cleanup")
    class DeletePolicyStaleFileCleanup {

        @Test
        @DisplayName("should physically delete stale src/ files with DELETE policy")
        void shouldDeleteStaleSrcFilesWithDeletePolicy() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Create a src/ file on disk that the previous manifest references
            Path staleFile = projectRoot.resolve("src/main/java/com/example/OldEntity.java");
            Files.createDirectories(staleFile.getParent());
            Files.writeString(staleFile, "class OldEntity {}");

            // Create a previous manifest referencing this src/ file
            Path prevManifestPath = manifestPath();
            Files.createDirectories(prevManifestPath.getParent());
            GenerationManifest previous = new GenerationManifest(prevManifestPath);
            previous.recordFile("jpa-plugin", Path.of("src/main/java/com/example/OldEntity.java"));
            previous.save();

            // Current build produces a different file (OldEntity is now stale)
            Path currentFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/NewEntity.java");
            Files.createDirectories(currentFile.getParent());
            Files.writeString(currentFile, "class NewEntity {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(currentFile))));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.DELETE, log);

            // The stale src/ file should have been physically deleted
            assertThat(staleFile).doesNotExist();

            // The manifest should only contain the current file
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
            assertThat(saved.allFiles()).anyMatch(f -> f.contains("NewEntity.java"));
        }

        @Test
        @DisplayName("should not delete stale target/ files (only src/)")
        void shouldNotDeleteStaleTargetFiles() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Previous manifest has a target/ file
            Path prevManifestPath = manifestPath();
            Files.createDirectories(prevManifestPath.getParent());
            GenerationManifest previous = new GenerationManifest(prevManifestPath);
            previous.recordFile("jpa-plugin", Path.of("target/hexaglue/generated-sources/com/example/Old.java"));
            previous.save();

            // Create the file on disk
            Path targetFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Old.java");
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, "class Old {}");

            // Current build produces a different file
            Path currentFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/New.java");
            Files.createDirectories(currentFile.getParent());
            Files.writeString(currentFile, "class New {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(currentFile))));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.DELETE, log);

            // target/ files should NOT be deleted by stale cleanup (mvn clean handles them)
            assertThat(targetFile).exists();
        }
    }

    @Nested
    @DisplayName("Multi-module paths in manifest")
    class MultiModulePaths {

        @Test
        @DisplayName("should handle multi-module paths with modules/ subdirectory")
        void shouldHandleMultiModulePathsWithModulesSubdirectory() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Simulate multi-module layout: target/generated-sources/hexaglue/modules/<moduleId>/...
            Path infraFile = projectRoot.resolve(
                    "target/generated-sources/hexaglue/modules/banking-infrastructure/com/example/AccountEntity.java");
            Path domainFile = projectRoot.resolve(
                    "target/generated-sources/hexaglue/modules/banking-domain/com/example/Order.java");
            Files.createDirectories(infraFile.getParent());
            Files.createDirectories(domainFile.getParent());
            Files.writeString(infraFile, "@Entity class AccountEntity {}");
            Files.writeString(domainFile, "class Order {}");

            Map<String, String> checksums = Map.of(
                    infraFile.toString(), "sha256:aaa111",
                    domainFile.toString(), "sha256:bbb222");

            PluginResult pr = pluginResultWithChecksums("jpa-plugin", List.of(infraFile, domainFile), checksums);
            EngineResult result = engineResultWith(List.of(pr));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // Load and verify round-trip
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(2);

            // All paths should be relative
            for (String filePath : saved.allFiles()) {
                assertThat(filePath).doesNotStartWith("/");
            }

            // Paths should contain the modules/<moduleId> structure
            assertThat(saved.allFiles())
                    .anyMatch(f -> f.contains("modules/banking-infrastructure/"))
                    .anyMatch(f -> f.contains("modules/banking-domain/"));

            // Checksums should survive round-trip
            for (String filePath : saved.allFiles()) {
                assertThat(saved.checksumFor(filePath)).isPresent();
            }
        }

        @Test
        @DisplayName("should detect stale files across multi-module paths")
        void shouldDetectStaleFilesAcrossMultiModulePaths() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Previous manifest had a src/ file in a multi-module-like structure
            Path prevManifestPath = manifestPath();
            Files.createDirectories(prevManifestPath.getParent());
            GenerationManifest previous = new GenerationManifest(prevManifestPath);
            previous.recordFile(
                    "jpa-plugin",
                    Path.of("target/generated-sources/hexaglue/modules/banking-infrastructure/com/example/Old.java"));
            previous.recordFile(
                    "jpa-plugin",
                    Path.of(
                            "target/generated-sources/hexaglue/modules/banking-infrastructure/com/example/Current.java"));
            previous.save();

            // Current build only produces Current.java (Old.java is stale but in target/, so ignored)
            Path currentFile = projectRoot.resolve(
                    "target/generated-sources/hexaglue/modules/banking-infrastructure/com/example/Current.java");
            Files.createDirectories(currentFile.getParent());
            Files.writeString(currentFile, "class Current {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(currentFile))));

            // Should not throw even with FAIL policy since stale files are in target/ (not src/)
            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.FAIL, log);

            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should record generated files from failed plugin result")
        void shouldRecordGeneratedFilesFromFailedPluginResult() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // A plugin that fails but still produced some files before failing
            Path generatedFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Partial.java");
            Files.createDirectories(generatedFile.getParent());
            Files.writeString(generatedFile, "class Partial {}");

            PluginResult failedResult = new PluginResult(
                    "jpa-plugin",
                    false,
                    List.of(generatedFile),
                    List.of(),
                    10L,
                    new RuntimeException("boom"),
                    Map.of());

            EngineResult result = engineResultWith(List.of(failedResult));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // The manifest should still contain the generated file from the failed plugin
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
            assertThat(saved.allFiles()).anyMatch(f -> f.contains("Partial.java"));
        }

        @Test
        @DisplayName("should handle corrupted previous manifest gracefully")
        void shouldHandleCorruptedPreviousManifestGracefully() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // Write a corrupted manifest (invalid content)
            Path prevManifestPath = manifestPath();
            Files.createDirectories(prevManifestPath.getParent());
            Files.writeString(prevManifestPath, "THIS IS NOT A VALID MANIFEST\n\0\0\0GARBAGE");

            // Current build produces a normal file
            Path generatedFile = projectRoot.resolve("target/hexaglue/generated-sources/com/example/Foo.java");
            Files.createDirectories(generatedFile.getParent());
            Files.writeString(generatedFile, "class Foo {}");

            EngineResult result = engineResultWith(List.of(pluginResult("jpa-plugin", List.of(generatedFile))));

            // Should NOT throw — gracefully handles corrupted manifest
            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // Manifest should be saved normally with the current file
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Multi-module multi-plugin checksums")
    class MultiModuleMultiPluginChecksums {

        @Test
        @DisplayName("should record checksums from multiple plugins across modules")
        void shouldRecordChecksumsFromMultiplePluginsAcrossModules() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            // JPA plugin generates files in infrastructure module
            Path jpaFile1 = projectRoot.resolve(
                    "target/generated-sources/hexaglue/modules/banking-infra/com/example/OrderEntity.java");
            Path jpaFile2 = projectRoot.resolve(
                    "target/generated-sources/hexaglue/modules/banking-infra/com/example/CustomerEntity.java");
            Files.createDirectories(jpaFile1.getParent());
            Files.writeString(jpaFile1, "@Entity class OrderEntity {}");
            Files.writeString(jpaFile2, "@Entity class CustomerEntity {}");

            // Audit plugin generates report at root level
            Path auditFile = projectRoot.resolve("target/hexaglue/reports/audit.html");
            Files.createDirectories(auditFile.getParent());
            Files.writeString(auditFile, "<html>Audit Report</html>");

            Map<String, String> jpaChecksums = Map.of(
                    jpaFile1.toString(), "sha256:jpa111",
                    jpaFile2.toString(), "sha256:jpa222");
            Map<String, String> auditChecksums = Map.of(auditFile.toString(), "sha256:audit333");

            PluginResult jpaPr = pluginResultWithChecksums("jpa-plugin", List.of(jpaFile1, jpaFile2), jpaChecksums);
            PluginResult auditPr = pluginResultWithChecksums("audit-plugin", List.of(auditFile), auditChecksums);
            EngineResult result = engineResultWith(List.of(jpaPr, auditPr));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            // Verify saved manifest
            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(3);

            // All checksums should be present
            for (String filePath : saved.allFiles()) {
                assertThat(saved.checksumFor(filePath)).isPresent();
            }

            // JPA and audit files should be tracked under their respective plugins
            assertThat(saved.filesForPlugin("jpa-plugin")).hasSize(2);
            assertThat(saved.filesForPlugin("audit-plugin")).hasSize(1);
        }

        @Test
        @DisplayName("should handle plugin with checksums and plugin without checksums")
        void shouldHandlePluginWithChecksumsAndPluginWithout() throws Exception {
            Path projectRoot = projectRoot();
            Files.createDirectories(outputDirectory());

            Path file1 = projectRoot.resolve("target/hexaglue/generated-sources/com/example/WithChecksum.java");
            Path file2 = projectRoot.resolve("target/hexaglue/generated-sources/com/example/WithoutChecksum.java");
            Files.createDirectories(file1.getParent());
            Files.writeString(file1, "class WithChecksum {}");
            Files.writeString(file2, "class WithoutChecksum {}");

            // Plugin 1 has checksums
            PluginResult pr1 =
                    pluginResultWithChecksums("jpa-plugin", List.of(file1), Map.of(file1.toString(), "sha256:abc"));
            // Plugin 2 has no checksums (old-style PluginResult)
            PluginResult pr2 = new PluginResult("living-doc", true, List.of(file2), List.of(), 10L, null, Map.of());
            EngineResult result = engineResultWith(List.of(pr1, pr2));

            ManifestSupport.processManifest(result, projectRoot, outputDirectory(), StaleFilePolicy.WARN, log);

            GenerationManifest saved = GenerationManifest.load(manifestPath());
            assertThat(saved.fileCount()).isEqualTo(2);

            // File 1 should have checksum, file 2 should not
            String path1 = saved.filesForPlugin("jpa-plugin").iterator().next();
            assertThat(saved.checksumFor(path1)).isPresent();

            String path2 = saved.filesForPlugin("living-doc").iterator().next();
            assertThat(saved.checksumFor(path2)).isEmpty();
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
