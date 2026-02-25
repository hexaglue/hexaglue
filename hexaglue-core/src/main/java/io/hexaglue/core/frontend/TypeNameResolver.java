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

package io.hexaglue.core.frontend;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves simple type names to qualified names when Spoon operates in
 * noClasspath mode and cannot determine the full package.
 *
 * <p>In tolerant (noClasspath) mode, Spoon may return simple names (e.g.,
 * {@code User}) instead of qualified names (e.g., {@code com.example.domain.User})
 * for unresolved types. This resolver attempts to reconstruct qualified names
 * using an index of known types.
 *
 * <p>Resolution priority:
 * <ol>
 *   <li>If the name is already qualified (contains a dot), return as-is</li>
 *   <li>If exactly one known type matches the simple name, return it</li>
 *   <li>If multiple candidates exist, prefer the one in the base package</li>
 *   <li>Otherwise, return the original name unchanged</li>
 * </ol>
 *
 * @since 6.0.0
 */
public final class TypeNameResolver {

    private final String basePackage;
    private final Map<String, List<String>> simpleToQualifiedIndex;

    /**
     * Creates a resolver with the given base package and known qualified names.
     *
     * @param basePackage the base package to prefer during resolution
     * @param knownQualifiedNames all known qualified type names from the analysis
     */
    public TypeNameResolver(String basePackage, Collection<String> knownQualifiedNames) {
        this.basePackage = basePackage;
        this.simpleToQualifiedIndex =
                knownQualifiedNames.stream().collect(Collectors.groupingBy(TypeNameResolver::simpleName));
    }

    /**
     * Attempts to resolve a potentially incomplete type name.
     *
     * @param typeName the name as returned by Spoon (may be simple or qualified)
     * @return the resolved qualified name, or the original if unresolvable
     */
    public String resolve(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }

        if (typeName.contains(".")) {
            return typeName; // Already qualified
        }

        List<String> candidates = simpleToQualifiedIndex.getOrDefault(typeName, List.of());

        if (candidates.size() == 1) {
            return candidates.get(0); // Unique match
        }

        // Prefer candidate in basePackage
        Optional<String> inBasePackage =
                candidates.stream().filter(q -> q.startsWith(basePackage)).findFirst();
        if (inBasePackage.isPresent()) {
            return inBasePackage.get();
        }

        return typeName; // Ambiguous or unknown, return as-is
    }

    /**
     * Returns a no-op resolver that always returns the input name unchanged.
     *
     * <p>Used in strict (non-tolerant) mode where all names are already qualified.
     *
     * @return a pass-through resolver
     */
    public static TypeNameResolver identity() {
        return new TypeNameResolver("", List.of());
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
