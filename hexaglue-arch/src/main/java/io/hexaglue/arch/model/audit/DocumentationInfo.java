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

package io.hexaglue.arch.model.audit;

import java.util.List;

/**
 * Documentation information for a code unit.
 *
 * <p>This record captures metrics about documentation coverage and quality.
 *
 * @param hasJavadoc        whether the unit has Javadoc
 * @param javadocCoverage   percentage of public elements with Javadoc (0-100)
 * @param missingDocs       list of elements missing documentation
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record DocumentationInfo(boolean hasJavadoc, int javadocCoverage, List<String> missingDocs) {

    /**
     * Compact constructor with defensive copy.
     */
    public DocumentationInfo {
        missingDocs = missingDocs != null ? List.copyOf(missingDocs) : List.of();
    }

    /**
     * Returns true if documentation coverage is adequate.
     *
     * @return true if coverage >= 80%
     */
    public boolean isWellDocumented() {
        return javadocCoverage >= 80;
    }

    /**
     * Returns true if there is missing documentation.
     *
     * @return true if missingDocs is not empty
     */
    public boolean hasMissingDocs() {
        return !missingDocs.isEmpty();
    }
}
