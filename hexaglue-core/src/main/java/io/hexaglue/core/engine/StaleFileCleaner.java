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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles cleanup of stale generated files in {@code src/} directories.
 *
 * <p>Files in {@code target/} are excluded since {@code mvn clean} handles those.
 * Only files in {@code src/} directories need explicit management because they
 * survive build clean operations.</p>
 *
 * <p>The cleanup policy is configurable via {@link StaleFilePolicy}:
 * <ul>
 *   <li>{@code WARN}: log a warning for each stale file (default)</li>
 *   <li>{@code DELETE}: delete stale files from the filesystem</li>
 *   <li>{@code FAIL}: fail the build if stale files are detected</li>
 * </ul>
 *
 * @since 5.0.0
 */
public final class StaleFileCleaner {

    private static final Logger log = LoggerFactory.getLogger(StaleFileCleaner.class);

    private StaleFileCleaner() {}

    /**
     * Detects stale files and applies the configured policy.
     *
     * <p>Stale files are files present in the previous manifest but absent from the
     * current one. Only files in {@code src/} directories are considered; files in
     * {@code target/} are ignored as they are cleaned by {@code mvn clean}.</p>
     *
     * @param current the current build manifest (must not be null)
     * @param previous the previous build manifest (may be null, meaning no previous build)
     * @param projectRoot the project root directory for resolving relative file paths
     * @param policy the policy to apply to stale files
     * @return the result of the cleanup operation
     */
    public static Result clean(
            GenerationManifest current, GenerationManifest previous, Path projectRoot, StaleFilePolicy policy) {
        Objects.requireNonNull(current, "current manifest must not be null");
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        // Compute all stale files, then filter to only src/ paths
        Set<String> allStale = current.computeStaleFiles(previous);
        List<String> srcStale = allStale.stream()
                .filter(StaleFileCleaner::isSourceFile)
                .sorted()
                .collect(Collectors.toList());

        if (srcStale.isEmpty()) {
            return new Result(List.of(), List.of(), true, Optional.empty());
        }

        return switch (policy) {
            case WARN -> applyWarnPolicy(srcStale);
            case DELETE -> applyDeletePolicy(srcStale, projectRoot);
            case FAIL -> applyFailPolicy(srcStale);
        };
    }

    private static Result applyWarnPolicy(List<String> staleFiles) {
        for (String file : staleFiles) {
            log.warn("Stale generated file detected: {}", file);
        }
        log.warn(
                "{} stale generated file(s) in src/ directories. "
                        + "Run hexaglue:clean-stale to remove them or set staleFilePolicy=DELETE",
                staleFiles.size());
        return new Result(List.copyOf(staleFiles), List.of(), true, Optional.empty());
    }

    private static Result applyDeletePolicy(List<String> staleFiles, Path projectRoot) {
        List<String> deleted = new ArrayList<>();
        for (String file : staleFiles) {
            Path resolved = projectRoot.resolve(file);
            if (Files.exists(resolved)) {
                try {
                    Files.delete(resolved);
                    deleted.add(file);
                    log.info("Deleted stale generated file: {}", file);
                } catch (IOException e) {
                    log.warn("Failed to delete stale file {}: {}", file, e.getMessage());
                }
            } else {
                log.debug("Stale file already absent from filesystem: {}", file);
            }
        }
        if (!deleted.isEmpty()) {
            log.info("Deleted {} stale generated file(s)", deleted.size());
        }
        return new Result(List.copyOf(staleFiles), List.copyOf(deleted), true, Optional.empty());
    }

    private static Result applyFailPolicy(List<String> staleFiles) {
        String message = String.format(
                "%d stale generated file(s) detected in src/ directories: %s. "
                        + "Remove them manually or set staleFilePolicy=DELETE",
                staleFiles.size(), String.join(", ", staleFiles));
        log.error(message);
        return new Result(List.copyOf(staleFiles), List.of(), false, Optional.of(message));
    }

    /**
     * Returns {@code true} if the file path represents a source file (in {@code src/}).
     *
     * <p>Files in {@code target/} are ephemeral and managed by {@code mvn clean}.</p>
     */
    private static boolean isSourceFile(String filePath) {
        return filePath.startsWith("src/") || filePath.startsWith("src\\");
    }

    /**
     * Result of a stale file cleanup operation.
     *
     * @param staleFiles all detected stale files in {@code src/} directories
     * @param deletedFiles files that were actually deleted (only with DELETE policy)
     * @param success whether the operation succeeded (false only with FAIL policy when stale files exist)
     * @param failureMessage the failure message (present only when success is false)
     * @since 5.0.0
     */
    public record Result(
            List<String> staleFiles, List<String> deletedFiles, boolean success, Optional<String> failureMessage) {

        /**
         * Compact constructor with defensive copies.
         */
        public Result {
            staleFiles = List.copyOf(staleFiles);
            deletedFiles = List.copyOf(deletedFiles);
            failureMessage = failureMessage != null ? failureMessage : Optional.empty();
        }
    }
}
