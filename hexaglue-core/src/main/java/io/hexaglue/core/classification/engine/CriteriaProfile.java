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

package io.hexaglue.core.classification.engine;

import io.hexaglue.core.classification.ClassificationCriteria;
import java.util.OptionalInt;

/**
 * Profile for configuring criteria priorities.
 *
 * <p>A criteria profile allows overriding the default priorities of criteria
 * without modifying the criteria code. This enables experimentation with
 * different priority schemes.
 *
 * <p>The profile is queried using the criteria's key (see {@link CriteriaKey}).
 * If no override is found, the criteria's default priority is used.
 */
public interface CriteriaProfile {

    /**
     * Returns the priority override for a criteria, if any.
     *
     * @param criteriaKey the criteria key (from {@link CriteriaKey#of})
     * @return the overridden priority, or empty to use the default
     */
    OptionalInt priorityFor(String criteriaKey);

    /**
     * Resolves the effective priority for a criteria.
     *
     * <p>Returns the overridden priority if present, otherwise the
     * criteria's default priority.
     *
     * @param criteria the criteria
     * @return the effective priority
     */
    default int resolvePriority(ClassificationCriteria<?> criteria) {
        String key = CriteriaKey.of(criteria);
        return priorityFor(key).orElse(criteria.priority());
    }

    /**
     * Creates the legacy profile that uses criteria's built-in priorities.
     *
     * <p>This profile never overrides priorities - it always defers to
     * the criteria's {@code priority()} method. Use this when you want
     * the original hardcoded behavior.
     *
     * @return the legacy profile
     */
    static CriteriaProfile legacy() {
        return key -> OptionalInt.empty();
    }

    /**
     * Creates a profile with specific priority overrides.
     *
     * @param overrides map of criteria key to priority
     * @return a profile with the specified overrides
     */
    static CriteriaProfile withOverrides(java.util.Map<String, Integer> overrides) {
        return key -> {
            Integer priority = overrides.get(key);
            return priority != null ? OptionalInt.of(priority) : OptionalInt.empty();
        };
    }
}
