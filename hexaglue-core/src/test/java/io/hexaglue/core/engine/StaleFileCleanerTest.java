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

package io.hexaglue.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link StaleFileCleaner}.
 *
 * @since 5.0.0
 */
@DisplayName("StaleFileCleaner")
class StaleFileCleanerTest {

    @TempDir
    Path tempDir;

    private Path projectRoot;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
    }

    @Nested
    @DisplayName("with no previous manifest")
    class NoPreviousManifest {

        @Test
        @DisplayName("should return empty result when previous manifest is null")
        void shouldReturnEmptyResultWhenPreviousIsNull() {
            GenerationManifest current = new GenerationManifest(projectRoot.resolve("manifest.txt"));
            current.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));

            StaleFileCleaner.Result result = StaleFileCleaner.clean(current, null, projectRoot, StaleFilePolicy.WARN);

            assertThat(result.staleFiles()).isEmpty();
            assertThat(result.deletedFiles()).isEmpty();
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("with no stale files")
    class NoStaleFiles {

        @Test
        @DisplayName("should return empty result when all files are still generated")
        void shouldReturnEmptyResultWhenNoStaleFiles() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));
            current.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.WARN);

            assertThat(result.staleFiles()).isEmpty();
            assertThat(result.deletedFiles()).isEmpty();
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("filtering target/ vs src/")
    class FilteringTargetVsSrc {

        @Test
        @DisplayName("should ignore stale files in target/ directories")
        void shouldIgnoreStaleFilesInTargetDirectories() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("target/generated-sources/hexaglue/com/example/OrderEntity.java"));
            previous.recordFile("audit", Path.of("target/hexaglue/reports/audit.html"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));
            // None regenerated — but they're all in target/, so should be ignored

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.WARN);

            assertThat(result.staleFiles()).isEmpty();
        }

        @Test
        @DisplayName("should detect stale files in src/ directories")
        void shouldDetectStaleFilesInSrcDirectories() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/CustomerEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));
            current.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));
            // CustomerEntity no longer generated

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.WARN);

            assertThat(result.staleFiles()).containsExactly("src/main/java/com/example/CustomerEntity.java");
        }

        @Test
        @DisplayName("should only report src/ files when both target/ and src/ are stale")
        void shouldOnlyReportSrcFilesWhenBothStale() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));
            previous.recordFile("audit", Path.of("target/hexaglue/reports/audit.html"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));
            // Neither regenerated

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.WARN);

            assertThat(result.staleFiles())
                    .containsExactly("src/main/java/com/example/OrderEntity.java")
                    .doesNotContain("target/hexaglue/reports/audit.html");
        }
    }

    @Nested
    @DisplayName("WARN policy")
    class WarnPolicy {

        @Test
        @DisplayName("should succeed even with stale files")
        void shouldSucceedEvenWithStaleFiles() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.WARN);

            assertThat(result.staleFiles()).hasSize(1);
            assertThat(result.deletedFiles()).isEmpty();
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("DELETE policy")
    class DeletePolicy {

        @Test
        @DisplayName("should delete stale files from filesystem")
        void shouldDeleteStaleFilesFromFilesystem() throws IOException {
            // Create a real file on disk
            Path staleFile = projectRoot.resolve("src/main/java/com/example/CustomerEntity.java");
            Files.createDirectories(staleFile.getParent());
            Files.writeString(staleFile, "class CustomerEntity {}");

            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/CustomerEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.DELETE);

            assertThat(result.staleFiles()).hasSize(1);
            assertThat(result.deletedFiles()).hasSize(1);
            assertThat(result.success()).isTrue();
            assertThat(staleFile).doesNotExist();
        }

        @Test
        @DisplayName("should handle already-deleted stale files gracefully")
        void shouldHandleAlreadyDeletedStaleFilesGracefully() {
            // File does not exist on disk, but is in the previous manifest
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/GhostEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.DELETE);

            assertThat(result.staleFiles()).hasSize(1);
            assertThat(result.deletedFiles()).isEmpty();
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("should not delete files in target/")
        void shouldNotDeleteFilesInTarget() throws IOException {
            Path targetFile = projectRoot.resolve("target/generated-sources/hexaglue/com/example/OrderEntity.java");
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, "class OrderEntity {}");

            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("target/generated-sources/hexaglue/com/example/OrderEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.DELETE);

            assertThat(result.staleFiles()).isEmpty();
            assertThat(result.deletedFiles()).isEmpty();
            assertThat(targetFile).exists();
        }
    }

    @Nested
    @DisplayName("FAIL policy")
    class FailPolicy {

        @Test
        @DisplayName("should fail when stale src/ files exist")
        void shouldFailWhenStaleSrcFilesExist() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("src/main/java/com/example/CustomerEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.FAIL);

            assertThat(result.staleFiles()).hasSize(1);
            assertThat(result.deletedFiles()).isEmpty();
            assertThat(result.success()).isFalse();
            assertThat(result.failureMessage()).isPresent();
            assertThat(result.failureMessage().get()).contains("CustomerEntity.java");
        }

        @Test
        @DisplayName("should succeed when no stale src/ files")
        void shouldSucceedWhenNoStaleSrcFiles() {
            GenerationManifest previous = new GenerationManifest(projectRoot.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("target/generated-sources/hexaglue/com/example/OrderEntity.java"));

            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            StaleFileCleaner.Result result =
                    StaleFileCleaner.clean(current, previous, projectRoot, StaleFilePolicy.FAIL);

            assertThat(result.success()).isTrue();
            assertThat(result.failureMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null current manifest")
        void shouldRejectNullCurrentManifest() {
            assertThatThrownBy(() -> StaleFileCleaner.clean(null, null, projectRoot, StaleFilePolicy.WARN))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("current");
        }

        @Test
        @DisplayName("should reject null projectRoot")
        void shouldRejectNullProjectRoot() {
            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            assertThatThrownBy(() -> StaleFileCleaner.clean(current, null, null, StaleFilePolicy.WARN))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("projectRoot");
        }

        @Test
        @DisplayName("should reject null policy")
        void shouldRejectNullPolicy() {
            GenerationManifest current = new GenerationManifest(projectRoot.resolve("curr.txt"));

            assertThatThrownBy(() -> StaleFileCleaner.clean(current, null, projectRoot, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("policy");
        }
    }
}
