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

import io.hexaglue.spi.plugin.CodeWriter;

/**
 * Alias for CodeWriter to clarify usage in generation context.
 *
 * <p>This type alias provides semantic clarity when passing the file writer
 * to generator plugins. It emphasizes that the writer is used to produce
 * <b>artifacts</b> (generated code, resources, documentation) rather than
 * just "code".
 *
 * <p>In HexaGlue 3.0, "ArtifactWriter" and "CodeWriter" are interchangeable
 * terms. This alias exists purely for API clarity and may evolve into a
 * distinct type in future versions.
 *
 * <p>Usage in generator plugins:
 * <pre>{@code
 * public void generate(GeneratorContext context) {
 *     ArtifactWriter writer = context.writer();
 *
 *     // Write Java sources
 *     writer.writeJavaSource("com.example.infra", "OrderEntity", javaCode);
 *
 *     // Write resources
 *     writer.writeResource("META-INF/persistence.xml", xmlContent);
 *
 *     // Write documentation
 *     writer.writeMarkdown("generated/entities", markdownDoc);
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public interface ArtifactWriter extends CodeWriter {

    /**
     * Wraps a CodeWriter as an ArtifactWriter.
     *
     * @param writer the code writer to wrap
     * @return an ArtifactWriter view
     */
    static ArtifactWriter of(CodeWriter writer) {
        return new ArtifactWriter() {
            @Override
            public void writeJavaSource(String packageName, String className, String content)
                    throws java.io.IOException {
                writer.writeJavaSource(packageName, className, content);
            }

            @Override
            public boolean exists(String packageName, String className) {
                return writer.exists(packageName, className);
            }

            @Override
            public void delete(String packageName, String className) throws java.io.IOException {
                writer.delete(packageName, className);
            }

            @Override
            public java.nio.file.Path getOutputDirectory() {
                return writer.getOutputDirectory();
            }

            @Override
            public void writeResource(String path, String content) throws java.io.IOException {
                writer.writeResource(path, content);
            }

            @Override
            public boolean resourceExists(String path) {
                return writer.resourceExists(path);
            }

            @Override
            public void deleteResource(String path) throws java.io.IOException {
                writer.deleteResource(path);
            }

            @Override
            public void writeDoc(String path, String content) throws java.io.IOException {
                writer.writeDoc(path, content);
            }

            @Override
            public boolean docExists(String path) {
                return writer.docExists(path);
            }

            @Override
            public void deleteDoc(String path) throws java.io.IOException {
                writer.deleteDoc(path);
            }

            @Override
            public java.nio.file.Path getDocsOutputDirectory() {
                return writer.getDocsOutputDirectory();
            }

            @Override
            public void writeJavaSource(String moduleId, String packageName, String className, String content)
                    throws java.io.IOException {
                writer.writeJavaSource(moduleId, packageName, className, content);
            }

            @Override
            public java.nio.file.Path getOutputDirectory(String moduleId) {
                return writer.getOutputDirectory(moduleId);
            }

            @Override
            public boolean isMultiModule() {
                return writer.isMultiModule();
            }
        };
    }
}
