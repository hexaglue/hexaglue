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

package io.hexaglue.plugin.livingdoc.model;

import io.hexaglue.arch.model.ArchKind;
import java.util.Objects;

/**
 * Immutable entry in the domain glossary.
 *
 * <p>Each entry represents an architectural type with its ubiquitous language term,
 * definition (from Javadoc when available), classification kind, and location.
 *
 * @param term the simple name of the type (e.g., "Order")
 * @param definition the Javadoc description or a fallback label (e.g., "Order (Aggregate Root)")
 * @param archKind the architectural classification (e.g., AGGREGATE_ROOT, VALUE_OBJECT)
 * @param qualifiedName the fully qualified class name (e.g., "com.example.order.domain.Order")
 * @param packageName the package name (e.g., "com.example.order.domain")
 * @since 5.0.0
 */
public record GlossaryEntry(
        String term, String definition, ArchKind archKind, String qualifiedName, String packageName) {

    /**
     * Compact constructor enforcing non-null constraints.
     */
    public GlossaryEntry {
        Objects.requireNonNull(term, "term must not be null");
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(archKind, "archKind must not be null");
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(packageName, "packageName must not be null");
    }
}
