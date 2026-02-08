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

package io.hexaglue.spi.generation;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.plugin.CodeWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ArtifactWriter}.
 *
 * <p>Verifies that {@link ArtifactWriter#of(CodeWriter)} correctly delegates
 * all methods, including multi-module methods added in v5.0.0.
 *
 * @since 5.0.0
 */
@DisplayName("ArtifactWriter")
class ArtifactWriterTest {

    @Nested
    @DisplayName("of() multi-module delegation")
    class MultiModuleDelegation {

        @Test
        @DisplayName("should delegate isMultiModule()")
        void shouldDelegateIsMultiModule() {
            RecordingCodeWriter recorder = new RecordingCodeWriter();
            recorder.multiModule = true;

            ArtifactWriter artifactWriter = ArtifactWriter.of(recorder);

            assertThat(artifactWriter.isMultiModule()).isTrue();
        }

        @Test
        @DisplayName("should delegate writeJavaSource with moduleId")
        void shouldDelegateWriteJavaSourceWithModuleId() throws IOException {
            RecordingCodeWriter recorder = new RecordingCodeWriter();

            ArtifactWriter artifactWriter = ArtifactWriter.of(recorder);
            artifactWriter.writeJavaSource("infra-module", "com.example", "OrderEntity", "content");

            assertThat(recorder.calls).containsExactly("writeJavaSource(infra-module,com.example,OrderEntity)");
        }

        @Test
        @DisplayName("should delegate getOutputDirectory with moduleId")
        void shouldDelegateGetOutputDirectoryWithModuleId() {
            RecordingCodeWriter recorder = new RecordingCodeWriter();
            recorder.moduleOutputDir = Path.of("/tmp/infra/target");

            ArtifactWriter artifactWriter = ArtifactWriter.of(recorder);

            assertThat(artifactWriter.getOutputDirectory("infra-module")).isEqualTo(Path.of("/tmp/infra/target"));
            assertThat(recorder.calls).containsExactly("getOutputDirectory(infra-module)");
        }
    }

    @Nested
    @DisplayName("of() standard delegation")
    class StandardDelegation {

        @Test
        @DisplayName("should delegate writeJavaSource without moduleId")
        void shouldDelegateWriteJavaSource() throws IOException {
            RecordingCodeWriter recorder = new RecordingCodeWriter();

            ArtifactWriter artifactWriter = ArtifactWriter.of(recorder);
            artifactWriter.writeJavaSource("com.example", "OrderEntity", "content");

            assertThat(recorder.calls).containsExactly("writeJavaSource(com.example,OrderEntity)");
        }

        @Test
        @DisplayName("should delegate getOutputDirectory without moduleId")
        void shouldDelegateGetOutputDirectory() {
            RecordingCodeWriter recorder = new RecordingCodeWriter();
            recorder.outputDir = Path.of("/tmp/target");

            ArtifactWriter artifactWriter = ArtifactWriter.of(recorder);

            assertThat(artifactWriter.getOutputDirectory()).isEqualTo(Path.of("/tmp/target"));
        }
    }

    /**
     * A CodeWriter stub that records method calls for verification.
     */
    private static class RecordingCodeWriter implements CodeWriter {

        final List<String> calls = new ArrayList<>();
        boolean multiModule = false;
        Path outputDir = Path.of("/tmp/default");
        Path moduleOutputDir = Path.of("/tmp/module");

        @Override
        public void writeJavaSource(String packageName, String className, String content) {
            calls.add("writeJavaSource(" + packageName + "," + className + ")");
        }

        @Override
        public void writeJavaSource(String moduleId, String packageName, String className, String content) {
            calls.add("writeJavaSource(" + moduleId + "," + packageName + "," + className + ")");
        }

        @Override
        public boolean exists(String packageName, String className) {
            return false;
        }

        @Override
        public void delete(String packageName, String className) {}

        @Override
        public Path getOutputDirectory() {
            return outputDir;
        }

        @Override
        public Path getOutputDirectory(String moduleId) {
            calls.add("getOutputDirectory(" + moduleId + ")");
            return moduleOutputDir;
        }

        @Override
        public boolean isMultiModule() {
            return multiModule;
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
            return Path.of("/tmp/docs");
        }
    }
}
