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

import io.hexaglue.arch.model.index.ModuleRole;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Detects the architectural {@link ModuleRole} of a Maven module from its {@code artifactId}.
 *
 * <p>This is a pure-function utility class with no state. It matches the artifactId suffix
 * (case-insensitive) against a table of well-known conventions. The longest matching suffix
 * wins to avoid ambiguity (e.g. {@code -infrastructure} matches before {@code -infra}).</p>
 *
 * <p>Returns {@link Optional#empty()} when no convention matches, allowing the caller
 * to fall back to a default (typically {@link ModuleRole#SHARED}).</p>
 *
 * @since 5.0.0
 */
final class ModuleRoleDetector {

    /**
     * Suffix-to-role mapping entry, sorted by descending suffix length within each role
     * so that the longest match wins when iterating.
     */
    private record SuffixMapping(String suffix, ModuleRole role) {}

    /**
     * All known suffix mappings, sorted by descending suffix length so the first match
     * for a given artifactId is always the longest (most specific) suffix.
     */
    private static final List<SuffixMapping> SUFFIX_MAPPINGS = List.of(
            // INFRASTRUCTURE (longest first)
            new SuffixMapping("-infrastructure", ModuleRole.INFRASTRUCTURE),
            new SuffixMapping("-persistence", ModuleRole.INFRASTRUCTURE),
            new SuffixMapping("-infra", ModuleRole.INFRASTRUCTURE),
            new SuffixMapping("-db", ModuleRole.INFRASTRUCTURE),
            new SuffixMapping("-jpa", ModuleRole.INFRASTRUCTURE),
            // APPLICATION
            new SuffixMapping("-application", ModuleRole.APPLICATION),
            new SuffixMapping("-usecases", ModuleRole.APPLICATION),
            new SuffixMapping("-service", ModuleRole.APPLICATION),
            // ASSEMBLY
            new SuffixMapping("-bootstrap", ModuleRole.ASSEMBLY),
            new SuffixMapping("-starter", ModuleRole.ASSEMBLY),
            new SuffixMapping("-boot", ModuleRole.ASSEMBLY),
            new SuffixMapping("-app", ModuleRole.ASSEMBLY),
            // API
            new SuffixMapping("-gateway", ModuleRole.API),
            new SuffixMapping("-rest", ModuleRole.API),
            new SuffixMapping("-web", ModuleRole.API),
            new SuffixMapping("-api", ModuleRole.API),
            // DOMAIN
            new SuffixMapping("-domain", ModuleRole.DOMAIN),
            new SuffixMapping("-model", ModuleRole.DOMAIN),
            new SuffixMapping("-core", ModuleRole.DOMAIN),
            // SHARED
            new SuffixMapping("-shared", ModuleRole.SHARED),
            new SuffixMapping("-common", ModuleRole.SHARED),
            new SuffixMapping("-utils", ModuleRole.SHARED),
            new SuffixMapping("-util", ModuleRole.SHARED));

    private ModuleRoleDetector() {}

    /**
     * Detects the {@link ModuleRole} from a Maven artifactId by matching its suffix
     * against well-known conventions.
     *
     * <p>The match is case-insensitive. When the artifactId contains multiple hyphenated
     * segments (e.g. {@code banking-core-service}), the last matching suffix wins.</p>
     *
     * <p>An exact match on the suffix word alone (without the hyphen prefix) also works,
     * e.g. {@code "core"} matches {@link ModuleRole#DOMAIN}.</p>
     *
     * @param artifactId the Maven artifactId (may be {@code null} or blank)
     * @return the detected role, or {@link Optional#empty()} if no convention matches
     */
    static Optional<ModuleRole> detect(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return Optional.empty();
        }

        String lower = artifactId.toLowerCase(Locale.ROOT);

        // Try suffix matching: iterate sorted list (longest suffix first per role group)
        // and pick the longest overall match
        SuffixMapping bestMatch = null;

        for (SuffixMapping mapping : SUFFIX_MAPPINGS) {
            boolean matches = lower.endsWith(mapping.suffix())
                    || lower.equals(mapping.suffix().substring(1)); // exact match without hyphen

            if (matches
                    && (bestMatch == null
                            || mapping.suffix().length() > bestMatch.suffix().length())) {
                bestMatch = mapping;
            }
        }

        return Optional.ofNullable(bestMatch).map(SuffixMapping::role);
    }
}
