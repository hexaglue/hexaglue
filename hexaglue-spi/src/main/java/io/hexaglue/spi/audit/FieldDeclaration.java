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

package io.hexaglue.spi.audit;

import java.util.Set;

/**
 * Declaration information for a field in a code unit.
 *
 * @param name        the field name
 * @param type        the field type (qualified name)
 * @param modifiers   the field modifiers (e.g., "private", "final")
 * @param annotations the annotation qualified names
 * @since 3.0.0
 */
public record FieldDeclaration(String name, String type, Set<String> modifiers, Set<String> annotations) {

    /**
     * Compact constructor with defensive copies.
     */
    public FieldDeclaration {
        modifiers = modifiers != null ? Set.copyOf(modifiers) : Set.of();
        annotations = annotations != null ? Set.copyOf(annotations) : Set.of();
    }
}
