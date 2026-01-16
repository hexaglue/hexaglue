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

package io.hexaglue.spi.ir;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata about the IR analysis.
 *
 * @param basePackage the base package that was analyzed
 * @param projectName the name of the analyzed project (e.g., from Maven project.name)
 * @param projectVersion the version of the analyzed project (e.g., "1.0.0")
 * @param timestamp when the analysis was performed
 * @param engineVersion the HexaGlue engine version
 * @param typeCount total number of types analyzed
 * @param portCount total number of ports detected
 * @since 3.0.0
 * @deprecated Use {@link io.hexaglue.arch.ProjectContext} instead.
 *             Scheduled for removal in v5.0.0.
 * @see io.hexaglue.arch.ProjectContext
 */
@Deprecated(forRemoval = true, since = "4.0.0")
public record IrMetadata(
        String basePackage,
        String projectName,
        String projectVersion,
        Instant timestamp,
        String engineVersion,
        int typeCount,
        int portCount) {

    /**
     * Compact constructor with defaults for optional fields.
     */
    public IrMetadata {
        Objects.requireNonNull(basePackage, "basePackage required");
        Objects.requireNonNull(timestamp, "timestamp required");
        Objects.requireNonNull(engineVersion, "engineVersion required");
        // projectName and projectVersion can be null - will use defaults
        if (projectName == null || projectName.isBlank()) {
            projectName = inferProjectName(basePackage);
        }
        if (projectVersion == null || projectVersion.isBlank()) {
            projectVersion = "unknown";
        }
    }

    /**
     * Creates metadata without project info (for backward compatibility).
     *
     * @param basePackage the base package
     * @param timestamp the timestamp
     * @param engineVersion the engine version
     * @param typeCount type count
     * @param portCount port count
     * @return metadata with inferred project name
     */
    public static IrMetadata withDefaults(
            String basePackage, Instant timestamp, String engineVersion, int typeCount, int portCount) {
        return new IrMetadata(basePackage, null, null, timestamp, engineVersion, typeCount, portCount);
    }

    /**
     * Infers a project name from the base package.
     *
     * <p>Uses the last segment of the base package as project name.
     * For example: "com.example.ecommerce" → "ecommerce"
     */
    private static String inferProjectName(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return "unknown";
        }
        int lastDot = basePackage.lastIndexOf('.');
        return lastDot >= 0 ? basePackage.substring(lastDot + 1) : basePackage;
    }
}
