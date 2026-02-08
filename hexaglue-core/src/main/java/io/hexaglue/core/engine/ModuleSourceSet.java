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

import io.hexaglue.arch.model.index.ModuleRole;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Describes the source layout of a single module within a multi-module project.
 *
 * <p>A ModuleSourceSet captures all the information HexaGlue needs to locate
 * source files for parsing and to route generated code to the correct output
 * directory within a specific module.</p>
 *
 * @param moduleId unique identifier for this module (typically the Maven artifactId)
 * @param role the architectural role of this module
 * @param sourceRoots source directories containing Java files to analyze
 * @param classpathEntries classpath entries for type resolution
 * @param outputDirectory directory for generated sources in this module
 * @param baseDir the root directory of this module
 * @since 5.0.0
 */
public record ModuleSourceSet(
        String moduleId,
        ModuleRole role,
        List<Path> sourceRoots,
        List<Path> classpathEntries,
        Path outputDirectory,
        Path baseDir) {

    /**
     * Creates a new ModuleSourceSet with validation and defensive copies.
     *
     * @param moduleId unique identifier, must not be null or blank
     * @param role the architectural role, must not be null
     * @param sourceRoots source directories, must not be null
     * @param classpathEntries classpath entries, must not be null
     * @param outputDirectory output directory, must not be null
     * @param baseDir the root directory, must not be null
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if moduleId is blank
     */
    public ModuleSourceSet {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
        Objects.requireNonNull(classpathEntries, "classpathEntries must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(baseDir, "baseDir must not be null");
        if (moduleId.isBlank()) {
            throw new IllegalArgumentException("moduleId must not be blank");
        }
        sourceRoots = List.copyOf(sourceRoots);
        classpathEntries = List.copyOf(classpathEntries);
    }
}
