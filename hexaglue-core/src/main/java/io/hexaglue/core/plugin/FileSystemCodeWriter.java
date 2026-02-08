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

import io.hexaglue.core.engine.OverwritePolicy;
import io.hexaglue.spi.plugin.CodeWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CodeWriter} implementation that writes to the file system.
 *
 * <p>Directory structure (default convention):
 * <ul>
 *   <li>Java sources: {@code target/generated-sources/hexaglue/}</li>
 *   <li>Resources: {@code target/generated-resources/hexaglue/}</li>
 *   <li>Reports/Documentation: {@code target/hexaglue/reports/}</li>
 * </ul>
 *
 * <p>The three directories can also be specified explicitly via the
 * {@linkplain #FileSystemCodeWriter(Path, Path, Path) 3-arg constructor},
 * which avoids fragile parent-path derivation when the output directory
 * follows a non-standard layout (e.g. {@code src/main/java/}).
 *
 * <p>Supports {@link OverwritePolicy} to protect manually edited files when
 * writing to {@code src/main/java/}. Each written file's SHA-256 checksum
 * is tracked for later manifest storage.
 *
 * @since 5.0.0 added 3-arg constructor
 * @since 5.0.0 added OverwritePolicy and checksum support
 */
final class FileSystemCodeWriter implements CodeWriter {

    private static final Logger log = LoggerFactory.getLogger(FileSystemCodeWriter.class);

    private final Path outputDirectory;
    private final Path resourcesDirectory;
    private final Path docsDirectory;
    private final OverwritePolicy overwritePolicy;
    private final Map<Path, String> previousChecksums;
    private final List<Path> generatedFiles = new ArrayList<>();
    private final Map<Path, String> checksums = new HashMap<>();

    /**
     * Creates a writer with explicit directories for sources, resources, and documentation.
     *
     * <p>This is the primary constructor. Use it when the three output directories
     * cannot be derived from a single base path (e.g. per-plugin or multi-module layouts).
     *
     * @param sourcesDirectory directory for generated Java source files
     * @param resourcesDirectory directory for generated resource files
     * @param docsDirectory directory for generated documentation/reports
     * @since 5.0.0
     */
    FileSystemCodeWriter(Path sourcesDirectory, Path resourcesDirectory, Path docsDirectory) {
        this.outputDirectory = Objects.requireNonNull(sourcesDirectory, "sourcesDirectory must not be null");
        this.resourcesDirectory = Objects.requireNonNull(resourcesDirectory, "resourcesDirectory must not be null");
        this.docsDirectory = Objects.requireNonNull(docsDirectory, "docsDirectory must not be null");
        this.overwritePolicy = OverwritePolicy.ALWAYS;
        this.previousChecksums = Map.of();
    }

    /**
     * Convenience constructor that derives resources and docs directories from
     * the parent of {@code outputDirectory}.
     *
     * <p>Assumes the layout: {@code <base>/generated-sources} where {@code <base>}
     * also contains {@code generated-resources} and {@code reports}.
     *
     * @param outputDirectory directory for generated Java source files
     */
    FileSystemCodeWriter(Path outputDirectory) {
        this(outputDirectory, OverwritePolicy.ALWAYS, Map.of());
    }

    /**
     * Creates a writer with overwrite policy and previous checksums for edit protection.
     *
     * @param outputDirectory directory for generated Java source files
     * @param overwritePolicy the overwrite policy to apply
     * @param previousChecksums checksums from the previous manifest (absolute path to checksum)
     * @since 5.0.0
     */
    FileSystemCodeWriter(Path outputDirectory, OverwritePolicy overwritePolicy, Map<Path, String> previousChecksums) {
        this.outputDirectory = outputDirectory;
        // outputDirectory = target/hexaglue/generated-sources (or target/hexaglue/reports for audit)
        // hexaglueBase = target/hexaglue
        Path hexaglueBase = outputDirectory.getParent();
        this.resourcesDirectory = hexaglueBase.resolve("generated-resources");
        this.docsDirectory = hexaglueBase.resolve("reports");
        this.overwritePolicy = overwritePolicy != null ? overwritePolicy : OverwritePolicy.ALWAYS;
        this.previousChecksums = previousChecksums != null ? previousChecksums : Map.of();
    }

    // =========================================================================
    // Java source generation
    // =========================================================================

    @Override
    public void writeJavaSource(String packageName, String className, String content) throws IOException {
        Path file = resolveJavaSourcePath(packageName, className);
        String contentChecksum = computeChecksum(content);

        if (shouldSkipWrite(file, contentChecksum)) {
            generatedFiles.add(file);
            checksums.put(file, contentChecksum);
            return;
        }

        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        generatedFiles.add(file);
        checksums.put(file, contentChecksum);
    }

    @Override
    public boolean exists(String packageName, String className) {
        return Files.exists(resolveJavaSourcePath(packageName, className));
    }

    @Override
    public void delete(String packageName, String className) throws IOException {
        Path file = resolveJavaSourcePath(packageName, className);
        if (Files.exists(file)) {
            Files.delete(file);
            generatedFiles.remove(file);
            checksums.remove(file);
        }
    }

    @Override
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    private Path resolveJavaSourcePath(String packageName, String className) {
        String relativePath = packageName.replace('.', '/') + "/" + className + ".java";
        return outputDirectory.resolve(relativePath);
    }

    // =========================================================================
    // Resource generation
    // =========================================================================

    @Override
    public void writeResource(String path, String content) throws IOException {
        Path file = resourcesDirectory.resolve(path);
        String contentChecksum = computeChecksum(content);

        if (shouldSkipWrite(file, contentChecksum)) {
            generatedFiles.add(file);
            checksums.put(file, contentChecksum);
            return;
        }

        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        generatedFiles.add(file);
        checksums.put(file, contentChecksum);
    }

    @Override
    public boolean resourceExists(String path) {
        return Files.exists(resourcesDirectory.resolve(path));
    }

    @Override
    public void deleteResource(String path) throws IOException {
        Path file = resourcesDirectory.resolve(path);
        if (Files.exists(file)) {
            Files.delete(file);
            generatedFiles.remove(file);
            checksums.remove(file);
        }
    }

    // =========================================================================
    // Documentation generation
    // =========================================================================

    @Override
    public void writeDoc(String path, String content) throws IOException {
        Path file = docsDirectory.resolve(path);
        String contentChecksum = computeChecksum(content);

        if (shouldSkipWrite(file, contentChecksum)) {
            generatedFiles.add(file);
            checksums.put(file, contentChecksum);
            return;
        }

        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        generatedFiles.add(file);
        checksums.put(file, contentChecksum);
    }

    @Override
    public boolean docExists(String path) {
        return Files.exists(docsDirectory.resolve(path));
    }

    @Override
    public void deleteDoc(String path) throws IOException {
        Path file = docsDirectory.resolve(path);
        if (Files.exists(file)) {
            Files.delete(file);
            generatedFiles.remove(file);
            checksums.remove(file);
        }
    }

    @Override
    public Path getDocsOutputDirectory() {
        return docsDirectory;
    }

    // =========================================================================
    // Overwrite policy
    // =========================================================================

    /**
     * Determines whether a write should be skipped based on the overwrite policy.
     */
    private boolean shouldSkipWrite(Path file, String newContentChecksum) {
        if (!Files.exists(file)) {
            return false;
        }
        return switch (overwritePolicy) {
            case ALWAYS -> false;
            case NEVER -> {
                log.info("Skipped {} (overwrite policy: NEVER)", file.getFileName());
                yield true;
            }
            case IF_UNCHANGED -> {
                String previousChecksum = previousChecksums.get(file);
                if (previousChecksum == null) {
                    yield false;
                }
                String existingChecksum = computeFileChecksum(file);
                if (!previousChecksum.equals(existingChecksum)) {
                    log.warn("Skipped {}: manually modified since last generation", file.getFileName());
                    yield true;
                }
                yield false;
            }
        };
    }

    /**
     * Computes the SHA-256 checksum of a string content.
     *
     * @param content the content to hash
     * @return the checksum in the format {@code sha256:<hex>}
     */
    static String computeChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
    }

    /**
     * Computes the SHA-256 checksum of an existing file on disk.
     */
    private static String computeFileChecksum(Path file) {
        try {
            byte[] content = Files.readAllBytes(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            return "";
        }
    }

    // =========================================================================
    // Internal API
    // =========================================================================

    /**
     * Returns all files generated by this writer.
     */
    List<Path> getGeneratedFiles() {
        return List.copyOf(generatedFiles);
    }

    /**
     * Returns checksums for all generated files (including skipped ones).
     *
     * @return an unmodifiable map of absolute file path to checksum string
     * @since 5.0.0
     */
    Map<Path, String> getGeneratedFileChecksums() {
        return Map.copyOf(checksums);
    }
}
