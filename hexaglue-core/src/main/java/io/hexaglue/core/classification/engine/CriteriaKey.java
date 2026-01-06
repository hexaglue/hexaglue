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

/**
 * Utility for extracting stable keys from criteria.
 *
 * <p>This utility provides a consistent way to get a criteria identifier
 * that can be used for configuration, logging, and debugging purposes.
 *
 * <p>If the criteria implements {@link IdentifiedCriteria}, its {@code id()}
 * is returned. Otherwise, falls back to the criteria's {@code name()}.
 */
public final class CriteriaKey {

    private CriteriaKey() {
        // Utility class
    }

    /**
     * Extracts a stable key from a criteria.
     *
     * @param criteria the criteria to extract the key from
     * @return the stable key (id if available, name otherwise)
     */
    public static String of(ClassificationCriteria<?> criteria) {
        if (criteria instanceof IdentifiedCriteria ic) {
            return ic.id();
        }
        return criteria.name();
    }

    /**
     * Extracts a stable key from any object that might be a criteria.
     *
     * <p>This method handles:
     * <ul>
     *   <li>{@link IdentifiedCriteria} → returns {@code id()}</li>
     *   <li>{@link ClassificationCriteria} → returns {@code name()}</li>
     *   <li>Other → returns {@code toString()}</li>
     * </ul>
     *
     * @param criteria the object to extract the key from
     * @return the stable key
     */
    public static String ofAny(Object criteria) {
        if (criteria instanceof IdentifiedCriteria ic) {
            return ic.id();
        }
        if (criteria instanceof ClassificationCriteria<?> cc) {
            return cc.name();
        }
        return criteria.toString();
    }
}
