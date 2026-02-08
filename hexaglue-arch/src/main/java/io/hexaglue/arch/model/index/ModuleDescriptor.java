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

package io.hexaglue.arch.model.index;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Describes a module in a multi-module project.
 *
 * <p>A module descriptor captures the identity, architectural role, and layout
 * of a single Maven module within a multi-module project. HexaGlue uses this
 * information to route generated code to the appropriate module.</p>
 *
 * @param moduleId unique identifier for this module (typically the Maven artifactId)
 * @param role the architectural role of this module
 * @param baseDir the root directory of this module
 * @param sourceRoots source directories to analyze (e.g., {@code src/main/java})
 * @param basePackage the base package for this module, may be null if not specified
 * @since 5.0.0
 */
public record ModuleDescriptor(
        String moduleId, ModuleRole role, Path baseDir, List<Path> sourceRoots, String basePackage) {

    /**
     * Creates a new ModuleDescriptor with validation and defensive copies.
     *
     * @param moduleId unique identifier, must not be null or blank
     * @param role the architectural role, must not be null
     * @param baseDir the root directory, must not be null
     * @param sourceRoots source directories, must not be null
     * @param basePackage the base package, may be null
     * @throws NullPointerException if moduleId, role, baseDir, or sourceRoots is null
     * @throws IllegalArgumentException if moduleId is blank
     */
    public ModuleDescriptor {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(baseDir, "baseDir must not be null");
        Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
        if (moduleId.isBlank()) {
            throw new IllegalArgumentException("moduleId must not be blank");
        }
        sourceRoots = List.copyOf(sourceRoots);
    }

    /**
     * Creates a ModuleDescriptor with default source roots derived from the base directory.
     *
     * <p>The default source root is {@code baseDir/src/main/java}. The base package
     * is left unspecified (null).</p>
     *
     * @param moduleId unique identifier for this module
     * @param role the architectural role
     * @param baseDir the root directory
     * @return a new ModuleDescriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if moduleId is blank
     */
    public static ModuleDescriptor of(String moduleId, ModuleRole role, Path baseDir) {
        Objects.requireNonNull(baseDir, "baseDir must not be null");
        return new ModuleDescriptor(moduleId, role, baseDir, List.of(baseDir.resolve("src/main/java")), null);
    }
}
