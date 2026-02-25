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

package io.hexaglue.maven;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.project.MavenProject;

/**
 * Resolves source roots for Spoon analysis, substituting delombok output
 * directories when available.
 *
 * <p>When a delombok output directory exists (indicating Lombok annotation
 * expansion was performed), this resolver replaces the original source root
 * with the delombok output. This ensures Spoon analyzes expanded Java code
 * with explicit getters, setters, etc.
 *
 * <p>Source roots without a corresponding delombok output are passed through
 * unchanged. Additional source roots (e.g., generated sources from other
 * plugins) are always included as-is.
 *
 * @since 6.0.0
 */
final class MojoSourceRootsResolver {

    static final String DELOMBOK_OUTPUT_SUBDIR = "hexaglue/delombok-sources";

    private MojoSourceRootsResolver() {}

    /**
     * Resolves source roots, substituting delombok output when available.
     *
     * <p>If the delombok output directory exists ({@code target/hexaglue/delombok-sources}),
     * the original source directory ({@code src/main/java}) is replaced by the delombok
     * directory. Other compile source roots (generated sources, etc.) are kept as-is.
     *
     * @param project the Maven project
     * @return immutable list of resolved source root paths
     */
    static List<Path> resolveSourceRoots(MavenProject project) {
        String buildDirectory = project.getBuild().getDirectory();
        if (buildDirectory != null) {
            Path delombokDir = Path.of(buildDirectory, DELOMBOK_OUTPUT_SUBDIR);

            if (Files.isDirectory(delombokDir)) {
                String originalSourceDir = project.getBuild().getSourceDirectory();
                return project.getCompileSourceRoots().stream()
                        .map(root -> root.equals(originalSourceDir) ? delombokDir : Path.of(root))
                        .filter(Files::isDirectory)
                        .toList();
            }
        }

        return project.getCompileSourceRoots().stream().map(Path::of).toList();
    }

    /**
     * Builds a path remapping table from delombok paths to original paths.
     *
     * <p>Used by diagnostics to report errors against original source files
     * instead of the delombok intermediary files.
     *
     * @param project the Maven project
     * @return map from delombok path prefix to original source path prefix,
     *         or empty map if delombok is not active
     */
    static Map<Path, Path> buildPathRemapping(MavenProject project) {
        String buildDirectory = project.getBuild().getDirectory();
        if (buildDirectory != null) {
            Path delombokDir = Path.of(buildDirectory, DELOMBOK_OUTPUT_SUBDIR);

            if (Files.isDirectory(delombokDir)) {
                Path originalSourceDir = Path.of(project.getBuild().getSourceDirectory());
                return Map.of(
                        delombokDir.toAbsolutePath().normalize(),
                        originalSourceDir.toAbsolutePath().normalize());
            }
        }
        return Map.of();
    }
}
