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

import io.hexaglue.core.engine.OverwritePolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link FileSystemCodeWriter}.
 *
 * @since 5.0.0
 */
@DisplayName("FileSystemCodeWriter")
class FileSystemCodeWriterTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("single-arg constructor (legacy)")
    class SingleArgConstructor {

        @Test
        @DisplayName("should derive resources and docs directories from parent")
        void shouldDeriveDirectoriesFromParent() {
            // Given
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // When
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            // Then
            assertThat(writer.getOutputDirectory()).isEqualTo(outputDir);
            assertThat(writer.getDocsOutputDirectory()).isEqualTo(tempDir.resolve("target/hexaglue/reports"));
        }

        @Test
        @DisplayName("should write Java sources to output directory")
        void shouldWriteJavaSourcesToOutputDirectory() throws IOException {
            // Given
            Path outputDir = Files.createDirectories(tempDir.resolve("target/hexaglue/generated-sources"));
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            // When
            writer.writeJavaSource("com.example", "Order", "class Order {}");

            // Then
            Path expected = outputDir.resolve("com/example/Order.java");
            assertThat(expected).exists();
            assertThat(Files.readString(expected)).isEqualTo("class Order {}");
            assertThat(writer.getGeneratedFiles()).containsExactly(expected);
        }
    }

    @Nested
    @DisplayName("three-arg constructor")
    class ThreeArgConstructor {

        @Test
        @DisplayName("should use explicit directories independently")
        void shouldUseExplicitDirectoriesIndependently() throws IOException {
            // Given: three unrelated directories
            Path sourcesDir = Files.createDirectories(tempDir.resolve("custom-sources"));
            Path resourcesDir = Files.createDirectories(tempDir.resolve("custom-resources"));
            Path docsDir = Files.createDirectories(tempDir.resolve("custom-docs"));

            // When
            FileSystemCodeWriter writer = new FileSystemCodeWriter(sourcesDir, resourcesDir, docsDir);

            // Then
            assertThat(writer.getOutputDirectory()).isEqualTo(sourcesDir);
            assertThat(writer.getDocsOutputDirectory()).isEqualTo(docsDir);
        }

        @Test
        @DisplayName("should write Java sources to explicit sources directory")
        void shouldWriteJavaSourcesToExplicitSourcesDirectory() throws IOException {
            // Given
            Path sourcesDir = Files.createDirectories(tempDir.resolve("src/main/java"));
            Path resourcesDir = tempDir.resolve("resources");
            Path docsDir = tempDir.resolve("docs");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(sourcesDir, resourcesDir, docsDir);

            // When
            writer.writeJavaSource("com.example", "Order", "class Order {}");

            // Then
            Path expected = sourcesDir.resolve("com/example/Order.java");
            assertThat(expected).exists();
            assertThat(Files.readString(expected)).isEqualTo("class Order {}");
        }

        @Test
        @DisplayName("should write resources to explicit resources directory")
        void shouldWriteResourcesToExplicitResourcesDirectory() throws IOException {
            // Given
            Path sourcesDir = tempDir.resolve("sources");
            Path resourcesDir = Files.createDirectories(tempDir.resolve("custom-resources"));
            Path docsDir = tempDir.resolve("docs");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(sourcesDir, resourcesDir, docsDir);

            // When
            writer.writeResource("META-INF/test.txt", "content");

            // Then
            Path expected = resourcesDir.resolve("META-INF/test.txt");
            assertThat(expected).exists();
            assertThat(Files.readString(expected)).isEqualTo("content");
        }

        @Test
        @DisplayName("should write docs to explicit docs directory")
        void shouldWriteDocsToExplicitDocsDirectory() throws IOException {
            // Given
            Path sourcesDir = tempDir.resolve("sources");
            Path resourcesDir = tempDir.resolve("resources");
            Path docsDir = Files.createDirectories(tempDir.resolve("custom-docs"));
            FileSystemCodeWriter writer = new FileSystemCodeWriter(sourcesDir, resourcesDir, docsDir);

            // When
            writer.writeDoc("audit/report.html", "<html>Report</html>");

            // Then
            Path expected = docsDir.resolve("audit/report.html");
            assertThat(expected).exists();
            assertThat(Files.readString(expected)).isEqualTo("<html>Report</html>");
        }

        @Test
        @DisplayName("should reject null sourcesDirectory")
        void shouldRejectNullSourcesDirectory() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FileSystemCodeWriter(null, tempDir, tempDir))
                    .withMessageContaining("sourcesDirectory");
        }

        @Test
        @DisplayName("should reject null resourcesDirectory")
        void shouldRejectNullResourcesDirectory() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FileSystemCodeWriter(tempDir, null, tempDir))
                    .withMessageContaining("resourcesDirectory");
        }

        @Test
        @DisplayName("should reject null docsDirectory")
        void shouldRejectNullDocsDirectory() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FileSystemCodeWriter(tempDir, tempDir, null))
                    .withMessageContaining("docsDirectory");
        }

        @Test
        @DisplayName("should track generated files across all three directories")
        void shouldTrackGeneratedFilesAcrossAllDirectories() throws IOException {
            // Given
            Path sourcesDir = Files.createDirectories(tempDir.resolve("sources"));
            Path resourcesDir = Files.createDirectories(tempDir.resolve("resources"));
            Path docsDir = Files.createDirectories(tempDir.resolve("docs"));
            FileSystemCodeWriter writer = new FileSystemCodeWriter(sourcesDir, resourcesDir, docsDir);

            // When
            writer.writeJavaSource("com.example", "A", "class A {}");
            writer.writeResource("test.txt", "content");
            writer.writeDoc("report.md", "# Report");

            // Then
            assertThat(writer.getGeneratedFiles()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("checksum computation")
    class ChecksumComputation {

        @Test
        @DisplayName("should compute SHA-256 checksum for content")
        void shouldComputeChecksumForContent() {
            String checksum = FileSystemCodeWriter.computeChecksum("class Foo {}");

            assertThat(checksum).startsWith("sha256:");
            assertThat(checksum).hasSize("sha256:".length() + 64);
        }

        @Test
        @DisplayName("should produce same checksum for same content")
        void shouldProduceSameChecksumForSameContent() {
            String checksum1 = FileSystemCodeWriter.computeChecksum("class Foo {}");
            String checksum2 = FileSystemCodeWriter.computeChecksum("class Foo {}");

            assertThat(checksum1).isEqualTo(checksum2);
        }

        @Test
        @DisplayName("should produce different checksums for different content")
        void shouldProduceDifferentChecksumsForDifferentContent() {
            String checksum1 = FileSystemCodeWriter.computeChecksum("class Foo {}");
            String checksum2 = FileSystemCodeWriter.computeChecksum("class Bar {}");

            assertThat(checksum1).isNotEqualTo(checksum2);
        }

        @Test
        @DisplayName("should store checksums for written files")
        void shouldStoreChecksumsForWrittenFiles() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeJavaSource("com.example", "Foo", "class Foo {}");

            Map<Path, String> checksums = writer.getGeneratedFileChecksums();
            assertThat(checksums).hasSize(1);
            assertThat(checksums.values().iterator().next()).startsWith("sha256:");
        }
    }

    @Nested
    @DisplayName("ALWAYS policy")
    class AlwaysPolicy {

        @Test
        @DisplayName("should always overwrite existing file")
        void shouldAlwaysOverwriteExistingFile() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer1 = new FileSystemCodeWriter(outputDir);
            writer1.writeJavaSource("com.example", "Foo", "class Foo { /* v1 */ }");

            FileSystemCodeWriter writer2 = new FileSystemCodeWriter(outputDir);
            writer2.writeJavaSource("com.example", "Foo", "class Foo { /* v2 */ }");

            Path file = outputDir.resolve("com/example/Foo.java");
            assertThat(Files.readString(file)).contains("v2");
        }
    }

    @Nested
    @DisplayName("NEVER policy")
    class NeverPolicy {

        @Test
        @DisplayName("should skip existing file")
        void shouldSkipExistingFile() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            Path file = outputDir.resolve("com/example/Foo.java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "class Foo { /* original */ }");

            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, Map.of());
            writer.writeJavaSource("com.example", "Foo", "class Foo { /* new */ }");

            assertThat(Files.readString(file)).contains("original");
        }

        @Test
        @DisplayName("should write new file regardless of policy")
        void shouldWriteNewFileRegardlessOfPolicy() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, Map.of());

            writer.writeJavaSource("com.example", "Foo", "class Foo {}");

            Path file = outputDir.resolve("com/example/Foo.java");
            assertThat(file).exists();
            assertThat(Files.readString(file)).isEqualTo("class Foo {}");
        }

        @Test
        @DisplayName("should track skipped files in generated files")
        void shouldTrackSkippedFilesInGeneratedFiles() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            Path file = outputDir.resolve("com/example/Foo.java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "original");

            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, Map.of());
            writer.writeJavaSource("com.example", "Foo", "new content");

            assertThat(writer.getGeneratedFiles()).hasSize(1);
            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("IF_UNCHANGED policy")
    class IfUnchangedPolicy {

        @Test
        @DisplayName("should overwrite unchanged file")
        void shouldOverwriteUnchangedFile() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            String originalContent = "class Foo { /* v1 */ }";
            String originalChecksum = FileSystemCodeWriter.computeChecksum(originalContent);

            Path file = outputDir.resolve("com/example/Foo.java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, originalContent);

            Map<Path, String> previousChecksums = Map.of(file, originalChecksum);
            FileSystemCodeWriter writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, previousChecksums);
            writer.writeJavaSource("com.example", "Foo", "class Foo { /* v2 */ }");

            assertThat(Files.readString(file)).contains("v2");
        }

        @Test
        @DisplayName("should skip manually modified file")
        void shouldSkipManuallyModifiedFile() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            String generatedContent = "class Foo { /* generated */ }";
            String generatedChecksum = FileSystemCodeWriter.computeChecksum(generatedContent);

            Path file = outputDir.resolve("com/example/Foo.java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "class Foo { /* manually edited */ }");

            Map<Path, String> previousChecksums = Map.of(file, generatedChecksum);
            FileSystemCodeWriter writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, previousChecksums);
            writer.writeJavaSource("com.example", "Foo", "class Foo { /* v2 */ }");

            assertThat(Files.readString(file)).contains("manually edited");
        }

        @Test
        @DisplayName("should write new file when no previous checksum exists")
        void shouldWriteNewFileWhenNoPreviousChecksum() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            Path file = outputDir.resolve("com/example/Foo.java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "class Foo { /* old */ }");

            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, Map.of());
            writer.writeJavaSource("com.example", "Foo", "class Foo { /* new */ }");

            assertThat(Files.readString(file)).contains("new");
        }
    }

    @Nested
    @DisplayName("checksum tracking across write types")
    class ChecksumTracking {

        @Test
        @DisplayName("should track checksums for resources")
        void shouldTrackChecksumsForResources() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeResource("META-INF/services/foo", "service content");

            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);
        }

        @Test
        @DisplayName("should track checksums for docs")
        void shouldTrackChecksumsForDocs() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeDoc("audit/report.html", "<html>report</html>");

            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);
        }
    }
}
