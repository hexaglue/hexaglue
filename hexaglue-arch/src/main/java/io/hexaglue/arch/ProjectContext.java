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

package io.hexaglue.arch;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Context information about the project being analyzed.
 *
 * <p>Captures project-level metadata such as name, base package,
 * source directories, and other configuration.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ProjectContext context = new ProjectContext(
 *     "my-app",
 *     "com.example.myapp",
 *     Path.of("src/main/java"),
 *     Optional.of("1.0.0")
 * );
 * }</pre>
 *
 * @param name the project name
 * @param basePackage the base package being analyzed
 * @param sourceDirectory the source directory path
 * @param version the project version (optional)
 * @since 4.0.0
 */
public record ProjectContext(String name, String basePackage, Path sourceDirectory, Optional<String> version) {

    /**
     * Creates a new ProjectContext instance.
     *
     * @param name the project name, must not be null or blank
     * @param basePackage the base package, must not be null or blank
     * @param sourceDirectory the source directory, must not be null
     * @param version the optional version
     * @throws NullPointerException if required fields are null
     * @throws IllegalArgumentException if name or basePackage is blank
     */
    public ProjectContext {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        Objects.requireNonNull(sourceDirectory, "sourceDirectory must not be null");
        Objects.requireNonNull(version, "version must not be null (use Optional.empty())");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage must not be blank");
        }
    }

    /**
     * Creates a ProjectContext without version information.
     *
     * @param name the project name
     * @param basePackage the base package
     * @param sourceDirectory the source directory
     * @return a new ProjectContext
     */
    public static ProjectContext of(String name, String basePackage, Path sourceDirectory) {
        return new ProjectContext(name, basePackage, sourceDirectory, Optional.empty());
    }

    /**
     * Creates a ProjectContext with version information.
     *
     * @param name the project name
     * @param basePackage the base package
     * @param sourceDirectory the source directory
     * @param version the version string
     * @return a new ProjectContext
     */
    public static ProjectContext of(String name, String basePackage, Path sourceDirectory, String version) {
        return new ProjectContext(name, basePackage, sourceDirectory, Optional.of(version));
    }

    /**
     * Creates a minimal ProjectContext for testing.
     *
     * @param name the project name
     * @param basePackage the base package
     * @return a new ProjectContext
     */
    public static ProjectContext forTesting(String name, String basePackage) {
        return new ProjectContext(name, basePackage, Path.of("src/main/java"), Optional.empty());
    }
}
