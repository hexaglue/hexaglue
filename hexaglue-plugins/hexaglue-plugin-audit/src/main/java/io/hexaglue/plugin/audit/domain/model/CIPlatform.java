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

package io.hexaglue.plugin.audit.domain.model;

/**
 * Enumeration of supported CI/CD platforms for integration.
 *
 * <p>This enumeration defines the continuous integration platforms for which
 * HexaGlue can generate ready-to-use configuration files. Each platform has
 * different configuration syntax and capabilities.
 *
 * <p><strong>Supported platforms:</strong>
 * <ul>
 *   <li>{@link #GITHUB_ACTIONS} - GitHub Actions YAML workflows</li>
 *   <li>{@link #GITLAB_CI} - GitLab CI/CD YAML pipelines</li>
 *   <li>{@link #JENKINS} - Jenkins declarative pipeline (Groovy)</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * CIPlatform platform = CIPlatform.GITHUB_ACTIONS;
 * String displayName = platform.displayName(); // "GitHub Actions"
 * String filename = platform.defaultFilename(); // ".github/workflows/hexaglue-audit.yml"
 * }</pre>
 *
 * @since 1.0.0
 */
public enum CIPlatform {

    /**
     * GitHub Actions workflow platform.
     *
     * <p>Uses YAML configuration placed in {@code .github/workflows/} directory.
     * Supports matrix builds, artifact uploads, and PR comments via GitHub CLI.
     */
    GITHUB_ACTIONS("GitHub Actions", ".github/workflows/hexaglue-audit.yml"),

    /**
     * GitLab CI/CD platform.
     *
     * <p>Uses YAML configuration in root {@code .gitlab-ci.yml} file or included files.
     * Supports stages, artifacts, and merge request comments.
     */
    GITLAB_CI("GitLab CI", ".gitlab-ci-hexaglue.yml"),

    /**
     * Jenkins declarative pipeline platform.
     *
     * <p>Uses Groovy-based Jenkinsfile for pipeline definition.
     * Supports stages, post-build actions, and build quality gates.
     */
    JENKINS("Jenkins", "Jenkinsfile.hexaglue");

    private final String displayName;
    private final String defaultFilename;

    /**
     * Constructs a CI platform enumeration value.
     *
     * @param displayName     human-readable platform name
     * @param defaultFilename suggested filename for the configuration
     */
    CIPlatform(String displayName, String defaultFilename) {
        this.displayName = displayName;
        this.defaultFilename = defaultFilename;
    }

    /**
     * Returns the human-readable display name of the platform.
     *
     * @return the display name (e.g., "GitHub Actions")
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the suggested default filename for the configuration.
     *
     * <p>This filename follows the platform's conventions and best practices.
     *
     * @return the default configuration filename
     */
    public String defaultFilename() {
        return defaultFilename;
    }
}
