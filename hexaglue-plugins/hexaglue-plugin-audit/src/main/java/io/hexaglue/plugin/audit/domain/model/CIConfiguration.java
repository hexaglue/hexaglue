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

import java.util.Objects;

/**
 * Represents a CI/CD platform integration configuration.
 *
 * <p>This immutable value object encapsulates a ready-to-use CI/CD configuration
 * file for running HexaGlue architecture audits. It includes the platform type,
 * configuration content (YAML or Groovy), suggested filename, and a description
 * of what the configuration does.
 *
 * <p>The configuration content is platform-specific and includes:
 * <ul>
 *   <li>Build environment setup (Java version, dependencies)</li>
 *   <li>HexaGlue audit goal execution</li>
 *   <li>Health score validation against configured thresholds</li>
 *   <li>Report artifact generation and storage</li>
 *   <li>Optional PR/MR comment integration</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * CIConfiguration config = new CIConfiguration(
 *     CIPlatform.GITHUB_ACTIONS,
 *     yamlContent,
 *     ".github/workflows/hexaglue-audit.yml",
 *     "GitHub Actions workflow for HexaGlue architecture audit"
 * );
 *
 * System.out.println("Save to: " + config.filename());
 * Files.writeString(Path.of(config.filename()), config.configContent());
 * }</pre>
 *
 * @param platform      the CI/CD platform this configuration targets
 * @param configContent the configuration file content (YAML or Groovy)
 * @param filename      the suggested filename for this configuration
 * @param description   human-readable description of what the configuration does
 * @since 1.0.0
 */
public record CIConfiguration(CIPlatform platform, String configContent, String filename, String description) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if configContent or filename is blank
     */
    public CIConfiguration {
        Objects.requireNonNull(platform, "platform required");
        Objects.requireNonNull(configContent, "configContent required");
        Objects.requireNonNull(filename, "filename required");
        Objects.requireNonNull(description, "description required");

        if (configContent.isBlank()) {
            throw new IllegalArgumentException("configContent cannot be blank");
        }
        if (filename.isBlank()) {
            throw new IllegalArgumentException("filename cannot be blank");
        }
    }

    /**
     * Returns a summary suitable for display in reports or console output.
     *
     * <p>The summary includes the platform name, filename, and description.
     *
     * @return a formatted summary string
     */
    public String summary() {
        return String.format(
                "%s Configuration\nFile: %s\nDescription: %s", platform.displayName(), filename, description);
    }
}
