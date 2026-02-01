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

package io.hexaglue.plugin.jpa.util;

import java.util.List;

/**
 * Utility for detecting Spring Data query method patterns that embed
 * the filter value directly in the method name.
 *
 * <p>Spring Data supports query derivation where certain condition suffixes
 * make the method valid without parameters. For example:
 * <ul>
 *   <li>{@code findByActiveTrue()} — no parameter needed, filters where active = true</li>
 *   <li>{@code findByNameIsNull()} — no parameter needed, filters where name IS NULL</li>
 *   <li>{@code existsByDeletedFalse()} — no parameter needed, checks existence where deleted = false</li>
 * </ul>
 *
 * <p>These suffixes are:
 * {@code True}, {@code False}, {@code IsTrue}, {@code IsFalse},
 * {@code Null}, {@code IsNull}, {@code NotNull}, {@code IsNotNull}.
 *
 * @since 5.0.0
 */
public final class SpringDataQuerySupport {

    /**
     * Embedded condition suffixes recognized by Spring Data, ordered longest-first
     * to avoid partial matches (e.g., {@code IsNotNull} before {@code Null}).
     */
    private static final List<String> EMBEDDED_CONDITION_SUFFIXES =
            List.of("IsNotNull", "IsNull", "IsTrue", "IsFalse", "NotNull", "Null", "True", "False");

    private SpringDataQuerySupport() {
        // Utility class — prevent instantiation
    }

    /**
     * Returns {@code true} if the method name ends with a Spring Data
     * embedded condition suffix that makes parameters unnecessary.
     *
     * @param methodName the query method name (e.g., {@code "findByActiveTrue"})
     * @return {@code true} if the name ends with an embedded condition suffix
     * @since 5.0.0
     */
    public static boolean hasEmbeddedConditionSuffix(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return false;
        }
        for (String suffix : EMBEDDED_CONDITION_SUFFIXES) {
            if (methodName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
