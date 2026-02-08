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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link GenerationManifest}.
 *
 * @since 5.0.0
 */
@DisplayName("GenerationManifest")
class GenerationManifestTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("recording files")
    class RecordingFiles {

        @Test
        @DisplayName("should track files by plugin")
        void shouldTrackFilesByPlugin() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));

            manifest.recordFile("jpa", Path.of("src/main/java/com/example/OrderEntity.java"));
            manifest.recordFile("jpa", Path.of("src/main/java/com/example/OrderRepository.java"));
            manifest.recordFile("audit", Path.of("target/reports/audit.html"));

            assertThat(manifest.filesForPlugin("jpa")).hasSize(2);
            assertThat(manifest.filesForPlugin("audit")).hasSize(1);
            assertThat(manifest.filesForPlugin("unknown")).isEmpty();
            assertThat(manifest.allFiles()).hasSize(3);
            assertThat(manifest.fileCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should reject null pluginId")
        void shouldRejectNullPluginId() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));

            assertThatNullPointerException().isThrownBy(() -> manifest.recordFile(null, Path.of("file.java")));
        }

        @Test
        @DisplayName("should reject null filePath")
        void shouldRejectNullFilePath() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));

            assertThatNullPointerException().isThrownBy(() -> manifest.recordFile("jpa", null));
        }
    }

    @Nested
    @DisplayName("stale file detection")
    class StaleFileDetection {

        @Test
        @DisplayName("should detect files removed between builds")
        void shouldDetectFilesRemovedBetweenBuilds() {
            GenerationManifest previous = new GenerationManifest(tempDir.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("OrderEntity.java"));
            previous.recordFile("jpa", Path.of("CustomerEntity.java"));
            previous.recordFile("jpa", Path.of("AddressEmbeddable.java"));

            GenerationManifest current = new GenerationManifest(tempDir.resolve("curr.txt"));
            current.recordFile("jpa", Path.of("OrderEntity.java"));
            // CustomerEntity and AddressEmbeddable no longer generated

            assertThat(current.computeStaleFiles(previous))
                    .containsExactlyInAnyOrder("CustomerEntity.java", "AddressEmbeddable.java");
        }

        @Test
        @DisplayName("should return empty set when no previous manifest")
        void shouldReturnEmptySetWhenNoPreviousManifest() {
            GenerationManifest current = new GenerationManifest(tempDir.resolve("curr.txt"));
            current.recordFile("jpa", Path.of("OrderEntity.java"));

            assertThat(current.computeStaleFiles(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty set when no files removed")
        void shouldReturnEmptySetWhenNoFilesRemoved() {
            GenerationManifest previous = new GenerationManifest(tempDir.resolve("prev.txt"));
            previous.recordFile("jpa", Path.of("OrderEntity.java"));

            GenerationManifest current = new GenerationManifest(tempDir.resolve("curr.txt"));
            current.recordFile("jpa", Path.of("OrderEntity.java"));

            assertThat(current.computeStaleFiles(previous)).isEmpty();
        }
    }

    @Nested
    @DisplayName("checksums")
    class Checksums {

        @Test
        @DisplayName("should record file with checksum")
        void shouldRecordFileWithChecksum() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));

            manifest.recordFile("jpa", Path.of("OrderEntity.java"), "sha256:abc123");

            assertThat(manifest.allFiles()).containsExactly("OrderEntity.java");
            assertThat(manifest.fileCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return checksum for recorded file")
        void shouldReturnChecksumForRecordedFile() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));
            manifest.recordFile("jpa", Path.of("OrderEntity.java"), "sha256:abc123");

            assertThat(manifest.checksumFor("OrderEntity.java")).hasValue("sha256:abc123");
        }

        @Test
        @DisplayName("should return empty for unknown file")
        void shouldReturnEmptyForUnknownFile() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));
            manifest.recordFile("jpa", Path.of("OrderEntity.java"), "sha256:abc123");

            assertThat(manifest.checksumFor("Unknown.java")).isEmpty();
        }

        @Test
        @DisplayName("should save and load checksum round-trip")
        void shouldSaveAndLoadChecksumRoundTrip() throws IOException {
            Path manifestPath = tempDir.resolve("manifest.txt");
            GenerationManifest original = new GenerationManifest(manifestPath);
            original.recordFile("jpa", Path.of("OrderEntity.java"), "sha256:abc123");
            original.recordFile("jpa", Path.of("OrderRepo.java"), "sha256:def456");
            original.save();

            GenerationManifest loaded = GenerationManifest.load(manifestPath);
            assertThat(loaded.checksumFor("OrderEntity.java")).hasValue("sha256:abc123");
            assertThat(loaded.checksumFor("OrderRepo.java")).hasValue("sha256:def456");
            assertThat(loaded.allFiles()).hasSize(2);
        }

        @Test
        @DisplayName("should load legacy manifest without checksums")
        void shouldLoadLegacyManifestWithoutChecksums() throws IOException {
            Path manifestPath = tempDir.resolve("manifest.txt");
            // Write a legacy manifest (no pipe separator)
            Files.writeString(manifestPath, """
                    # HexaGlue Generation Manifest
                    # Generated at: 2026-01-01T00:00:00Z
                    #
                    # plugin: jpa
                    OrderEntity.java
                    OrderRepo.java
                    """);

            GenerationManifest loaded = GenerationManifest.load(manifestPath);
            assertThat(loaded.allFiles()).hasSize(2);
            assertThat(loaded.checksumFor("OrderEntity.java")).isEmpty();
            assertThat(loaded.checksumFor("OrderRepo.java")).isEmpty();
        }

        @Test
        @DisplayName("should handle null checksum")
        void shouldHandleNullChecksum() {
            GenerationManifest manifest = new GenerationManifest(tempDir.resolve("manifest.txt"));
            manifest.recordFile("jpa", Path.of("OrderEntity.java"), null);

            assertThat(manifest.allFiles()).containsExactly("OrderEntity.java");
            assertThat(manifest.checksumFor("OrderEntity.java")).isEmpty();
        }

        @Test
        @DisplayName("should save file without checksum when null")
        void shouldSaveFileWithoutChecksumWhenNull() throws IOException {
            Path manifestPath = tempDir.resolve("manifest.txt");
            GenerationManifest manifest = new GenerationManifest(manifestPath);
            manifest.recordFile("jpa", Path.of("OrderEntity.java"), null);
            manifest.recordFile("jpa", Path.of("OrderRepo.java"), "sha256:abc123");
            manifest.save();

            String content = Files.readString(manifestPath);
            assertThat(content).contains("OrderEntity.java\n"); // No pipe
            assertThat(content).contains("OrderRepo.java|sha256:abc123"); // With pipe
        }
    }

    @Nested
    @DisplayName("persistence")
    class Persistence {

        @Test
        @DisplayName("should save and load manifest round-trip")
        void shouldSaveAndLoadManifestRoundTrip() throws IOException {
            Path manifestPath = tempDir.resolve("manifest.txt");
            GenerationManifest original = new GenerationManifest(manifestPath);
            original.recordFile("jpa", Path.of("com/example/OrderEntity.java"));
            original.recordFile("jpa", Path.of("com/example/OrderRepo.java"));
            original.recordFile("audit", Path.of("reports/audit.html"));

            original.save();

            assertThat(manifestPath).exists();

            GenerationManifest loaded = GenerationManifest.load(manifestPath);
            assertThat(loaded.allFiles()).containsExactlyInAnyOrderElementsOf(original.allFiles());
            assertThat(loaded.filesForPlugin("jpa")).hasSize(2);
            assertThat(loaded.filesForPlugin("audit")).hasSize(1);
        }

        @Test
        @DisplayName("should return empty manifest when file does not exist")
        void shouldReturnEmptyManifestWhenFileDoesNotExist() throws IOException {
            Path nonExistent = tempDir.resolve("nonexistent/manifest.txt");

            GenerationManifest loaded = GenerationManifest.load(nonExistent);

            assertThat(loaded.allFiles()).isEmpty();
            assertThat(loaded.fileCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("saved manifest should contain header comments")
        void savedManifestShouldContainHeaderComments() throws IOException {
            Path manifestPath = tempDir.resolve("manifest.txt");
            GenerationManifest manifest = new GenerationManifest(manifestPath);
            manifest.recordFile("jpa", Path.of("Test.java"));

            manifest.save();

            String content = Files.readString(manifestPath);
            assertThat(content).contains("# HexaGlue Generation Manifest");
            assertThat(content).contains("# plugin: jpa");
            assertThat(content).contains("Test.java");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null manifestPath")
        void shouldRejectNullManifestPath() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new GenerationManifest(null))
                    .withMessageContaining("manifestPath");
        }
    }
}
