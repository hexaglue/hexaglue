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

package io.hexaglue.spi.plugin;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes generated source files, resources, and documentation.
 *
 * <p>Generated Java sources are placed in {@code target/generated-sources/hexaglue/}
 * and automatically added to the compilation source path.
 *
 * <p>Documentation files are placed in a configurable output directory,
 * typically {@code docs/} or {@code target/generated-docs/}.
 */
public interface CodeWriter {

    // =========================================================================
    // Java source generation
    // =========================================================================

    /**
     * Writes a Java source file.
     *
     * @param packageName the package name (e.g., "com.example.infrastructure")
     * @param className the simple class name (e.g., "OrderEntity")
     * @param content the complete Java source code
     * @throws IOException if writing fails
     */
    void writeJavaSource(String packageName, String className, String content) throws IOException;

    /**
     * Checks if a Java source file already exists.
     *
     * <p>Useful for plugins that want to avoid overwriting user-modified files
     * or to implement incremental generation.
     *
     * @param packageName the package name
     * @param className the simple class name
     * @return true if the file exists
     */
    boolean exists(String packageName, String className);

    /**
     * Deletes a previously generated Java source file.
     *
     * <p>Useful for cleanup operations or when regenerating files that should
     * replace previous versions.
     *
     * @param packageName the package name
     * @param className the simple class name
     * @throws IOException if deletion fails
     */
    void delete(String packageName, String className) throws IOException;

    /**
     * Returns the output directory for generated sources.
     *
     * @return the output directory path
     */
    Path getOutputDirectory();

    // =========================================================================
    // Resource generation
    // =========================================================================

    /**
     * Writes a resource file.
     *
     * @param path the resource path relative to resources root (e.g., "META-INF/services/...")
     * @param content the file content
     * @throws IOException if writing fails
     */
    void writeResource(String path, String content) throws IOException;

    /**
     * Checks if a resource file already exists.
     *
     * @param path the resource path relative to resources root
     * @return true if the resource exists
     */
    boolean resourceExists(String path);

    /**
     * Deletes a previously generated resource file.
     *
     * @param path the resource path relative to resources root
     * @throws IOException if deletion fails
     */
    void deleteResource(String path) throws IOException;

    // =========================================================================
    // Documentation generation
    // =========================================================================

    /**
     * Writes a documentation file (markdown, text, etc.).
     *
     * <p>Example:
     * <pre>{@code
     * writer.writeDoc("architecture/domain-model.md", """
     *     # Domain Model
     *
     *     ## Aggregates
     *     - Order
     *     - Customer
     *     """);
     * }</pre>
     *
     * @param path the documentation file path (e.g., "api/ports.md")
     * @param content the file content
     * @throws IOException if writing fails
     */
    void writeDoc(String path, String content) throws IOException;

    /**
     * Writes a markdown documentation file.
     *
     * <p>Convenience method that ensures the path ends with {@code .md}.
     *
     * @param path the documentation file path (without or with .md extension)
     * @param content the markdown content
     * @throws IOException if writing fails
     */
    default void writeMarkdown(String path, String content) throws IOException {
        String mdPath = path.endsWith(".md") ? path : path + ".md";
        writeDoc(mdPath, content);
    }

    /**
     * Checks if a documentation file already exists.
     *
     * @param path the documentation file path
     * @return true if the file exists
     */
    boolean docExists(String path);

    /**
     * Deletes a previously generated documentation file.
     *
     * @param path the documentation file path
     * @throws IOException if deletion fails
     */
    void deleteDoc(String path) throws IOException;

    /**
     * Returns the output directory for generated documentation.
     *
     * @return the documentation output directory path
     */
    Path getDocsOutputDirectory();
}
