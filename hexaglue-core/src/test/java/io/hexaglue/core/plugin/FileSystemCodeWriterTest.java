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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @DisplayName("delete and exists methods")
    class DeleteAndExists {

        @Test
        @DisplayName("should remove from generated files on delete")
        void shouldRemoveFromGeneratedFilesOnDelete() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            writer.writeJavaSource("com.example", "Foo", "class Foo {}");
            assertThat(writer.getGeneratedFiles()).hasSize(1);

            writer.delete("com.example", "Foo");
            assertThat(writer.getGeneratedFiles()).isEmpty();
        }

        @Test
        @DisplayName("should remove resource from tracking on deleteResource")
        void shouldRemoveResourceFromTrackingOnDeleteResource() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            writer.writeResource("META-INF/test.txt", "content");
            assertThat(writer.getGeneratedFiles()).hasSize(1);

            writer.deleteResource("META-INF/test.txt");
            assertThat(writer.getGeneratedFiles()).isEmpty();
        }

        @Test
        @DisplayName("should remove doc from tracking on deleteDoc")
        void shouldRemoveDocFromTrackingOnDeleteDoc() throws IOException {
            Path outputDir = tempDir.resolve("target/hexaglue/generated-sources");
            FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDir);

            writer.writeDoc("report.md", "# Report");
            assertThat(writer.getGeneratedFiles()).hasSize(1);

            writer.deleteDoc("report.md");
            assertThat(writer.getGeneratedFiles()).isEmpty();
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
