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

import io.hexaglue.core.engine.ModuleSourceSet;
import io.hexaglue.spi.plugin.CodeWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link CodeWriter} implementation that routes generated code to module-specific output directories.
 *
 * <p>In a multi-module project, each module has its own output directory for generated sources.
 * This writer maintains a map of module-specific {@link FileSystemCodeWriter} instances and
 * delegates write operations to the appropriate writer based on the target module ID.</p>
 *
 * <p>Documentation and resource files are written to a shared default output directory,
 * as they are typically project-wide (audit reports, architecture documentation).</p>
 *
 * @since 5.0.0
 */
final class MultiModuleCodeWriter implements CodeWriter {

    private final Map<String, FileSystemCodeWriter> writers;
    private final FileSystemCodeWriter defaultWriter;

    /**
     * Creates a MultiModuleCodeWriter using each module's default output directory.
     *
     * @param modules the module source sets describing each module's output directory
     * @param defaultOutputDirectory the default output directory for non-module-specific files
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if modules is empty
     */
    MultiModuleCodeWriter(List<ModuleSourceSet> modules, Path defaultOutputDirectory) {
        this(modules, defaultOutputDirectory, null);
    }

    /**
     * Creates a MultiModuleCodeWriter with an optional per-plugin output directory override.
     *
     * <p>When {@code pluginOutputOverride} is provided:
     * <ul>
     *   <li>If <strong>relative</strong>, it is resolved against each module's
     *       {@link ModuleSourceSet#baseDir() baseDir}. This supports configurations
     *       like {@code src/main/java/} where each module writes to its own source tree.</li>
     *   <li>If <strong>absolute</strong>, it is used directly for all modules.</li>
     * </ul>
     *
     * <p>When {@code pluginOutputOverride} is {@code null}, each module uses its
     * default {@link ModuleSourceSet#outputDirectory()}.</p>
     *
     * @param modules the module source sets describing each module
     * @param defaultOutputDirectory the default output directory for non-module-specific files
     * @param pluginOutputOverride per-plugin output directory override (may be null)
     * @throws NullPointerException if modules or defaultOutputDirectory is null
     * @throws IllegalArgumentException if modules is empty
     * @since 5.0.0
     */
    MultiModuleCodeWriter(List<ModuleSourceSet> modules, Path defaultOutputDirectory, Path pluginOutputOverride) {
        Objects.requireNonNull(modules, "modules must not be null");
        Objects.requireNonNull(defaultOutputDirectory, "defaultOutputDirectory must not be null");
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("modules must not be empty for multi-module writer");
        }

        this.defaultWriter = new FileSystemCodeWriter(defaultOutputDirectory);
        this.writers = new HashMap<>();
        for (ModuleSourceSet module : modules) {
            Path effectiveOutput = resolveModuleOutput(module, pluginOutputOverride);
            writers.put(module.moduleId(), new FileSystemCodeWriter(effectiveOutput));
        }
    }

    /**
     * Resolves the effective output directory for a module, applying the plugin override if present.
     */
    private static Path resolveModuleOutput(ModuleSourceSet module, Path pluginOutputOverride) {
        if (pluginOutputOverride == null) {
            return module.outputDirectory();
        }
        if (pluginOutputOverride.isAbsolute()) {
            return pluginOutputOverride;
        }
        // Relative path: resolve against the module's base directory
        return module.baseDir().resolve(pluginOutputOverride);
    }

    // =========================================================================
    // Multi-module API
    // =========================================================================

    @Override
    public boolean isMultiModule() {
        return true;
    }

    @Override
    public void writeJavaSource(String moduleId, String packageName, String className, String content)
            throws IOException {
        writerFor(moduleId).writeJavaSource(packageName, className, content);
    }

    @Override
    public Path getOutputDirectory(String moduleId) {
        return writerFor(moduleId).getOutputDirectory();
    }

    // =========================================================================
    // Default (mono-module) API — delegates to defaultWriter
    // =========================================================================

    @Override
    public void writeJavaSource(String packageName, String className, String content) throws IOException {
        defaultWriter.writeJavaSource(packageName, className, content);
    }

    @Override
    public boolean exists(String packageName, String className) {
        return defaultWriter.exists(packageName, className);
    }

    @Override
    public void delete(String packageName, String className) throws IOException {
        defaultWriter.delete(packageName, className);
    }

    @Override
    public Path getOutputDirectory() {
        return defaultWriter.getOutputDirectory();
    }

    @Override
    public void writeResource(String path, String content) throws IOException {
        defaultWriter.writeResource(path, content);
    }

    @Override
    public boolean resourceExists(String path) {
        return defaultWriter.resourceExists(path);
    }

    @Override
    public void deleteResource(String path) throws IOException {
        defaultWriter.deleteResource(path);
    }

    @Override
    public void writeDoc(String path, String content) throws IOException {
        defaultWriter.writeDoc(path, content);
    }

    @Override
    public boolean docExists(String path) {
        return defaultWriter.docExists(path);
    }

    @Override
    public void deleteDoc(String path) throws IOException {
        defaultWriter.deleteDoc(path);
    }

    @Override
    public Path getDocsOutputDirectory() {
        return defaultWriter.getDocsOutputDirectory();
    }

    // =========================================================================
    // Internal API
    // =========================================================================

    /**
     * Returns all files generated by all writers (all modules + default).
     */
    List<Path> getGeneratedFiles() {
        List<Path> allFiles = new java.util.ArrayList<>(defaultWriter.getGeneratedFiles());
        for (FileSystemCodeWriter writer : writers.values()) {
            allFiles.addAll(writer.getGeneratedFiles());
        }
        return List.copyOf(allFiles);
    }

    private FileSystemCodeWriter writerFor(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        FileSystemCodeWriter writer = writers.get(moduleId);
        if (writer == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleId + ". Known modules: " + writers.keySet());
        }
        return writer;
    }
}
