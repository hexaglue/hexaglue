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

import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.GenerationManifest;
import io.hexaglue.core.engine.StaleFileCleaner;
import io.hexaglue.core.engine.StaleFilePolicy;
import io.hexaglue.core.plugin.PluginResult;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Encapsulates manifest and stale file cleanup logic shared by all generator mojos.
 *
 * <p>After each generation run, this utility:
 * <ol>
 *   <li>Loads the previous manifest (if any) from {@code target/hexaglue/manifest.txt}</li>
 *   <li>Builds a new manifest from plugin-generated files</li>
 *   <li>Detects stale files (files in previous manifest but not in current)</li>
 *   <li>Applies the configured {@link StaleFilePolicy}</li>
 *   <li>Saves the current manifest for future builds</li>
 * </ol>
 *
 * @since 5.0.0
 */
final class ManifestSupport {

    static final String MANIFEST_FILENAME = "manifest.txt";

    private ManifestSupport() {}

    /**
     * Builds a manifest from plugin results, runs stale file cleanup,
     * and saves the manifest for future builds.
     *
     * <p>No-op when the engine result has no plugin execution result or
     * when no files were generated.</p>
     *
     * @param result the engine result containing plugin execution data
     * @param projectRoot the project root (for relativizing paths and resolving stale files)
     * @param outputDirectory the output directory (manifest is stored in its parent)
     * @param policy the stale file cleanup policy
     * @param log the Maven log
     * @throws MojoFailureException if FAIL policy triggers on stale files
     * @since 5.0.0
     */
    static void processManifest(
            EngineResult result, Path projectRoot, Path outputDirectory, StaleFilePolicy policy, Log log)
            throws MojoFailureException {
        if (result.pluginResult() == null) {
            log.debug("No plugin execution result - skipping manifest processing");
            return;
        }

        if (result.pluginResult().pluginResults().isEmpty()) {
            log.debug("No plugins executed - skipping manifest processing");
            return;
        }

        // Manifest path: outputDirectory parent (target/hexaglue/) + manifest.txt
        Path manifestPath = outputDirectory.getParent().resolve(MANIFEST_FILENAME);

        // Load previous manifest (empty if first build or after mvn clean)
        GenerationManifest previous;
        try {
            previous = GenerationManifest.load(manifestPath);
        } catch (IOException e) {
            log.warn("Failed to load previous manifest: " + e.getMessage());
            previous = new GenerationManifest(manifestPath);
        }

        // Build current manifest from plugin results (with checksums)
        GenerationManifest current = new GenerationManifest(manifestPath);
        for (PluginResult pr : result.pluginResult().pluginResults()) {
            for (Path file : pr.generatedFiles()) {
                Path relative = projectRoot.relativize(file);
                String checksum = pr.checksums().get(file.toString());
                current.recordFile(pr.pluginId(), relative, checksum);
            }
        }

        if (current.fileCount() == 0) {
            log.debug("No generated files - skipping manifest save");
            return;
        }

        // Detect and handle stale files
        StaleFileCleaner.Result cleanResult = StaleFileCleaner.clean(current, previous, projectRoot, policy);

        // Save current manifest for future builds
        try {
            current.save();
            log.debug("Manifest saved: " + manifestPath + " (" + current.fileCount() + " files)");
        } catch (IOException e) {
            log.warn("Failed to save generation manifest: " + e.getMessage());
        }

        // Handle FAIL policy result
        if (!cleanResult.success()) {
            throw new MojoFailureException(cleanResult.failureMessage().orElse("Stale file cleanup failed"));
        }
    }
}
