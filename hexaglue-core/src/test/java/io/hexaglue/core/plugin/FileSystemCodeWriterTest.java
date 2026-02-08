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

    @Nested
    @DisplayName("NEVER policy on resources and docs")
    class NeverPolicyResourcesAndDocs {

        @Test
        @DisplayName("should skip existing resource with NEVER policy")
        void shouldSkipExistingResource() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            Path resourceFile = outputDir.getParent().resolve("generated-resources/META-INF/test.txt");
            Files.createDirectories(resourceFile.getParent());
            Files.writeString(resourceFile, "original resource");

            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, Map.of());
            writer.writeResource("META-INF/test.txt", "new resource");

            assertThat(Files.readString(resourceFile)).isEqualTo("original resource");
            assertThat(writer.getGeneratedFiles()).hasSize(1);
            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);
        }

        @Test
        @DisplayName("should skip existing doc with NEVER policy")
        void shouldSkipExistingDoc() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            Path docFile = outputDir.getParent().resolve("reports/audit/report.html");
            Files.createDirectories(docFile.getParent());
            Files.writeString(docFile, "<html>original</html>");

            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, Map.of());
            writer.writeDoc("audit/report.html", "<html>new</html>");

            assertThat(Files.readString(docFile)).isEqualTo("<html>original</html>");
            assertThat(writer.getGeneratedFiles()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("IF_UNCHANGED policy on resources and docs")
    class IfUnchangedPolicyResourcesAndDocs {

        @Test
        @DisplayName("should overwrite unchanged resource")
        void shouldOverwriteUnchangedResource() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            String originalContent = "original resource content";
            String originalChecksum = FileSystemCodeWriter.computeChecksum(originalContent);

            Path resourceFile = outputDir.getParent().resolve("generated-resources/META-INF/test.txt");
            Files.createDirectories(resourceFile.getParent());
            Files.writeString(resourceFile, originalContent);

            Map<Path, String> previousChecksums = Map.of(resourceFile, originalChecksum);
            FileSystemCodeWriter writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, previousChecksums);
            writer.writeResource("META-INF/test.txt", "new resource content");

            assertThat(Files.readString(resourceFile)).isEqualTo("new resource content");
        }

        @Test
        @DisplayName("should skip manually modified doc")
        void shouldSkipManuallyModifiedDoc() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            String generatedContent = "<html>generated report</html>";
            String generatedChecksum = FileSystemCodeWriter.computeChecksum(generatedContent);

            Path docFile = outputDir.getParent().resolve("reports/audit/report.html");
            Files.createDirectories(docFile.getParent());
            Files.writeString(docFile, "<html>manually edited report</html>");

            Map<Path, String> previousChecksums = Map.of(docFile, generatedChecksum);
            FileSystemCodeWriter writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, previousChecksums);
            writer.writeDoc("audit/report.html", "<html>v2 report</html>");

            assertThat(Files.readString(docFile)).contains("manually edited");
        }
    }

    @Nested
    @DisplayName("IF_UNCHANGED full lifecycle")
    class IfUnchangedFullLifecycle {

        @Test
        @DisplayName("gen1→manual edit→gen2 should skip modified file and overwrite unchanged file")
        void fullLifecycleWithManualEdit() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // ---- GENERATION 1 ----
            FileSystemCodeWriter gen1Writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, Map.of());
            gen1Writer.writeJavaSource("com.example", "Order", "class Order { /* gen1 */ }");
            gen1Writer.writeJavaSource("com.example", "Customer", "class Customer { /* gen1 */ }");

            // Capture checksums from gen1 (simulating manifest save)
            Map<Path, String> gen1Checksums = gen1Writer.getGeneratedFileChecksums();
            assertThat(gen1Checksums).hasSize(2);

            // ---- MANUAL EDIT of Order.java (user modifies the file) ----
            Path orderFile = outputDir.resolve("com/example/Order.java");
            Files.writeString(orderFile, "class Order { /* manually edited by developer */ }");

            // Customer.java remains untouched (unchanged since gen1)

            // ---- GENERATION 2 (with gen1 checksums as "previous") ----
            FileSystemCodeWriter gen2Writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, gen1Checksums);
            gen2Writer.writeJavaSource("com.example", "Order", "class Order { /* gen2 */ }");
            gen2Writer.writeJavaSource("com.example", "Customer", "class Customer { /* gen2 */ }");

            // Order.java was manually modified → skip (protect manual edit)
            assertThat(Files.readString(orderFile)).contains("manually edited by developer");

            // Customer.java was unchanged → overwrite with gen2
            Path customerFile = outputDir.resolve("com/example/Customer.java");
            assertThat(Files.readString(customerFile)).contains("gen2");

            // Both files are tracked as generated (even the skipped one)
            assertThat(gen2Writer.getGeneratedFiles()).hasSize(2);
            assertThat(gen2Writer.getGeneratedFileChecksums()).hasSize(2);
        }

        @Test
        @DisplayName("gen1→gen2 without manual edit should overwrite all files")
        void fullLifecycleWithoutManualEdit() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // ---- GENERATION 1 ----
            FileSystemCodeWriter gen1Writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, Map.of());
            gen1Writer.writeJavaSource("com.example", "Order", "class Order { /* gen1 */ }");

            Map<Path, String> gen1Checksums = gen1Writer.getGeneratedFileChecksums();

            // No manual edit — file remains as written by gen1

            // ---- GENERATION 2 ----
            FileSystemCodeWriter gen2Writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, gen1Checksums);
            gen2Writer.writeJavaSource("com.example", "Order", "class Order { /* gen2 */ }");

            // File was unchanged → gen2 overwrites it
            Path orderFile = outputDir.resolve("com/example/Order.java");
            assertThat(Files.readString(orderFile)).contains("gen2");
        }

        @Test
        @DisplayName("gen1→gen2→gen3 should track checksums across multiple generations")
        void threeGenerationLifecycle() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // Gen1: initial generation
            FileSystemCodeWriter gen1 = new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, Map.of());
            gen1.writeJavaSource("com.example", "Entity", "class Entity { /* v1 */ }");
            Map<Path, String> gen1Checksums = gen1.getGeneratedFileChecksums();

            // Gen2: same policy, file unchanged → overwrite
            FileSystemCodeWriter gen2 =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, gen1Checksums);
            gen2.writeJavaSource("com.example", "Entity", "class Entity { /* v2 */ }");
            Map<Path, String> gen2Checksums = gen2.getGeneratedFileChecksums();

            Path entityFile = outputDir.resolve("com/example/Entity.java");
            assertThat(Files.readString(entityFile)).contains("v2");

            // Manual edit after gen2
            Files.writeString(entityFile, "class Entity { /* user modified after v2 */ }");

            // Gen3: should skip because file was manually modified after gen2
            FileSystemCodeWriter gen3 =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, gen2Checksums);
            gen3.writeJavaSource("com.example", "Entity", "class Entity { /* v3 */ }");

            assertThat(Files.readString(entityFile)).contains("user modified after v2");
        }
    }

    @Nested
    @DisplayName("IF_UNCHANGED with unreadable file")
    class IfUnchangedUnreadableFile {

        @Test
        @DisplayName("should skip write when existing file cannot be read (treated as modified)")
        void shouldSkipWriteWhenExistingFileCannotBeRead() throws IOException {
            // Scenario: the target path is a directory (not a regular file).
            // Files.exists() returns true, but Files.readAllBytes() throws IOException.
            // computeFileChecksum() catches the IOException and returns "".
            // Since "" != previousChecksum, the file is considered "manually modified" → skip.
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            Path filePath = outputDir.resolve("com/example/Foo.java");

            // Create a DIRECTORY at the file path (simulates unreadable "file")
            Files.createDirectories(filePath);

            // Previous checksum exists for this path
            String previousChecksum = "sha256:abc123def456";
            Map<Path, String> previousChecksums = Map.of(filePath, previousChecksum);

            FileSystemCodeWriter writer =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, previousChecksums);
            writer.writeJavaSource("com.example", "Foo", "class Foo { /* new */ }");

            // The "file" (directory) should still be a directory (write was skipped)
            assertThat(Files.isDirectory(filePath)).isTrue();

            // But the file is still tracked in generated files and checksums
            assertThat(writer.getGeneratedFiles()).hasSize(1);
            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("overwrite policy transitions between generations")
    class OverwritePolicyTransitions {

        @Test
        @DisplayName("ALWAYS→IF_UNCHANGED: gen1 ALWAYS, gen2 IF_UNCHANGED should protect manual edits")
        void alwaysToIfUnchangedShouldProtectManualEdits() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // Gen1: ALWAYS policy (writes everything)
            FileSystemCodeWriter gen1 = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());
            gen1.writeJavaSource("com.example", "Entity", "class Entity { /* v1 */ }");
            Map<Path, String> gen1Checksums = gen1.getGeneratedFileChecksums();

            // Manual edit
            Path entityFile = outputDir.resolve("com/example/Entity.java");
            Files.writeString(entityFile, "class Entity { /* manually edited */ }");

            // Gen2: switch to IF_UNCHANGED (with gen1 checksums)
            FileSystemCodeWriter gen2 =
                    new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, gen1Checksums);
            gen2.writeJavaSource("com.example", "Entity", "class Entity { /* v2 */ }");

            // Manual edit should be protected
            assertThat(Files.readString(entityFile)).contains("manually edited");
        }

        @Test
        @DisplayName("IF_UNCHANGED→ALWAYS: should overwrite previously protected file")
        void ifUnchangedToAlwaysShouldOverwriteProtectedFile() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // Gen1: IF_UNCHANGED
            FileSystemCodeWriter gen1 = new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, Map.of());
            gen1.writeJavaSource("com.example", "Entity", "class Entity { /* v1 */ }");
            Map<Path, String> gen1Checksums = gen1.getGeneratedFileChecksums();

            // Manual edit
            Path entityFile = outputDir.resolve("com/example/Entity.java");
            Files.writeString(entityFile, "class Entity { /* manually edited */ }");

            // Gen2: switch to ALWAYS (ignores checksums, overwrites everything)
            FileSystemCodeWriter gen2 = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, gen1Checksums);
            gen2.writeJavaSource("com.example", "Entity", "class Entity { /* v2 */ }");

            // ALWAYS policy should overwrite regardless of manual edit
            assertThat(Files.readString(entityFile)).contains("v2");
        }

        @Test
        @DisplayName("NEVER→ALWAYS: should overwrite previously protected file")
        void neverToAlwaysShouldOverwritePreviouslyProtected() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // Gen1: NEVER (only writes new files)
            FileSystemCodeWriter gen1 = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, Map.of());
            gen1.writeJavaSource("com.example", "Entity", "class Entity { /* v1 */ }");

            // Gen2: switch to ALWAYS
            FileSystemCodeWriter gen2 = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());
            gen2.writeJavaSource("com.example", "Entity", "class Entity { /* v2 */ }");

            Path entityFile = outputDir.resolve("com/example/Entity.java");
            assertThat(Files.readString(entityFile)).contains("v2");
        }

        @Test
        @DisplayName("IF_UNCHANGED→NEVER: should skip existing file regardless of checksum match")
        void ifUnchangedToNeverShouldSkipExistingFile() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");

            // Gen1: write a file
            FileSystemCodeWriter gen1 = new FileSystemCodeWriter(outputDir, OverwritePolicy.IF_UNCHANGED, Map.of());
            gen1.writeJavaSource("com.example", "Entity", "class Entity { /* v1 */ }");
            Map<Path, String> gen1Checksums = gen1.getGeneratedFileChecksums();

            // Gen2: switch to NEVER (skip all existing, even if unchanged)
            FileSystemCodeWriter gen2 = new FileSystemCodeWriter(outputDir, OverwritePolicy.NEVER, gen1Checksums);
            gen2.writeJavaSource("com.example", "Entity", "class Entity { /* v2 */ }");

            // NEVER should skip, even though the file is unchanged (checksums match)
            Path entityFile = outputDir.resolve("com/example/Entity.java");
            assertThat(Files.readString(entityFile)).contains("v1");
        }
    }

    @Nested
    @DisplayName("delete and exists methods")
    class DeleteAndExists {

        @Test
        @DisplayName("should remove from generated files on delete")
        void shouldRemoveFromGeneratedFilesOnDelete() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeJavaSource("com.example", "Foo", "class Foo {}");
            assertThat(writer.getGeneratedFiles()).hasSize(1);

            writer.delete("com.example", "Foo");
            assertThat(writer.getGeneratedFiles()).isEmpty();
        }

        @Test
        @DisplayName("should remove checksum on delete")
        void shouldRemoveChecksumOnDelete() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeJavaSource("com.example", "Foo", "class Foo {}");
            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);

            writer.delete("com.example", "Foo");
            assertThat(writer.getGeneratedFileChecksums()).isEmpty();
        }

        @Test
        @DisplayName("should remove resource from tracking on deleteResource")
        void shouldRemoveResourceFromTrackingOnDeleteResource() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeResource("META-INF/test.txt", "content");
            assertThat(writer.getGeneratedFiles()).hasSize(1);
            assertThat(writer.getGeneratedFileChecksums()).hasSize(1);

            writer.deleteResource("META-INF/test.txt");
            assertThat(writer.getGeneratedFiles()).isEmpty();
            assertThat(writer.getGeneratedFileChecksums()).isEmpty();
        }

        @Test
        @DisplayName("should remove doc from tracking on deleteDoc")
        void shouldRemoveDocFromTrackingOnDeleteDoc() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir, OverwritePolicy.ALWAYS, Map.of());

            writer.writeDoc("report.md", "# Report");
            assertThat(writer.getGeneratedFiles()).hasSize(1);

            writer.deleteDoc("report.md");
            assertThat(writer.getGeneratedFiles()).isEmpty();
            assertThat(writer.getGeneratedFileChecksums()).isEmpty();
        }

        @Test
        @DisplayName("should return true when file exists")
        void shouldReturnTrueWhenFileExists() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            writer.writeJavaSource("com.example", "Foo", "class Foo {}");

            assertThat(writer.exists("com.example", "Foo")).isTrue();
        }

        @Test
        @DisplayName("should return false when file does not exist")
        void shouldReturnFalseWhenFileDoesNotExist() {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            assertThat(writer.exists("com.example", "NonExistent")).isFalse();
        }

        @Test
        @DisplayName("should check resource existence")
        void shouldCheckResourceExistence() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            assertThat(writer.resourceExists("META-INF/test.txt")).isFalse();
            writer.writeResource("META-INF/test.txt", "content");
            assertThat(writer.resourceExists("META-INF/test.txt")).isTrue();
        }

        @Test
        @DisplayName("should check doc existence")
        void shouldCheckDocExistence() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            assertThat(writer.docExists("report.md")).isFalse();
            writer.writeDoc("report.md", "# Report");
            assertThat(writer.docExists("report.md")).isTrue();
        }
    }
}
