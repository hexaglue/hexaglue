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

import java.nio.file.Path;
import java.util.Objects;

/**
 * Single source of truth for constructing multi-module output directory paths.
 *
 * <p>In a multi-module project, generated sources are placed under the reactor root's
 * {@code target/} directory so that child module {@code mvn clean} phases do not erase
 * generated code. This class centralizes the path construction logic to avoid
 * duplication across the lifecycle participant, reactor mojos, and engine config builder.</p>
 *
 * <p>Default layout:
 * <pre>
 *   {reactorBaseDir}/target/generated-sources/hexaglue/modules/{moduleId}/
 * </pre>
 *
 * <p>The base sources path ({@code target/generated-sources/hexaglue}) is configurable
 * to support future per-plugin output directory customisation.</p>
 *
 * @since 5.0.0
 */
public final class MultiModuleOutputResolver {

    /**
     * Default base path for generated sources, relative to the reactor root.
     */
    static final String DEFAULT_SOURCES_BASE = "target/generated-sources/hexaglue";

    /**
     * Default base path for generated resources, relative to the reactor root.
     */
    static final String DEFAULT_RESOURCES_BASE = "target/generated-resources/hexaglue";

    /**
     * Default base path for reports, relative to the reactor root.
     */
    static final String DEFAULT_REPORTS_BASE = "target/hexaglue/reports";

    private final Path reactorBaseDir;
    private final String sourcesBase;
    private final String resourcesBase;
    private final String reportsBase;

    /**
     * Creates a resolver with default base paths.
     *
     * @param reactorBaseDir the root directory of the reactor (parent project)
     */
    public MultiModuleOutputResolver(Path reactorBaseDir) {
        this(reactorBaseDir, DEFAULT_SOURCES_BASE, DEFAULT_RESOURCES_BASE, DEFAULT_REPORTS_BASE);
    }

    /**
     * Creates a resolver with custom base paths.
     *
     * @param reactorBaseDir the root directory of the reactor
     * @param sourcesBase relative path for generated sources under reactorBaseDir
     * @param resourcesBase relative path for generated resources under reactorBaseDir
     * @param reportsBase relative path for reports under reactorBaseDir
     */
    public MultiModuleOutputResolver(
            Path reactorBaseDir, String sourcesBase, String resourcesBase, String reportsBase) {
        this.reactorBaseDir = Objects.requireNonNull(reactorBaseDir, "reactorBaseDir must not be null");
        this.sourcesBase = Objects.requireNonNull(sourcesBase, "sourcesBase must not be null");
        this.resourcesBase = Objects.requireNonNull(resourcesBase, "resourcesBase must not be null");
        this.reportsBase = Objects.requireNonNull(reportsBase, "reportsBase must not be null");
    }

    /**
     * Returns the generated sources directory for a given module.
     *
     * @param moduleId the module identifier (typically the Maven artifactId)
     * @return the absolute path for generated sources of this module
     */
    public Path resolveSourcesDirectory(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        return reactorBaseDir.resolve(sourcesBase).resolve("modules").resolve(moduleId);
    }

    /**
     * Returns the generated resources directory for a given module.
     *
     * @param moduleId the module identifier
     * @return the absolute path for generated resources of this module
     */
    public Path resolveResourcesDirectory(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        return reactorBaseDir.resolve(resourcesBase).resolve("modules").resolve(moduleId);
    }

    /**
     * Returns the reports directory (shared across all modules).
     *
     * <p>Reports (audit, living-doc) are reactor-level artifacts, not per-module.</p>
     *
     * @return the absolute path for the reports directory
     */
    public Path resolveReportsDirectory() {
        return reactorBaseDir.resolve(reportsBase);
    }

    /**
     * Returns the default output directory for the reactor (used as fallback
     * for non-module-specific files like reports).
     *
     * @return the default sources output directory
     */
    public Path resolveDefaultOutputDirectory() {
        return reactorBaseDir.resolve(sourcesBase);
    }

    /**
     * Returns the reactor base directory.
     *
     * @return the reactor root path
     */
    public Path reactorBaseDir() {
        return reactorBaseDir;
    }
}
