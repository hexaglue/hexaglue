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

package io.hexaglue.arch.model;

import io.hexaglue.syntax.TypeForm;

/**
 * The nature (form) of a type in the architectural model.
 *
 * <p>This enum mirrors {@link TypeForm} from the syntax module but provides
 * a semantic separation between the syntax layer and the architectural layer.</p>
 *
 * @since 4.1.0
 */
public enum TypeNature {

    /**
     * A class (non-record).
     */
    CLASS,

    /**
     * An interface.
     */
    INTERFACE,

    /**
     * A record (Java 16+).
     */
    RECORD,

    /**
     * An enum.
     */
    ENUM,

    /**
     * An annotation type.
     */
    ANNOTATION;

    /**
     * Converts a {@link TypeForm} to a {@link TypeNature}.
     *
     * @param form the type form to convert
     * @return the corresponding TypeNature
     */
    public static TypeNature fromTypeForm(TypeForm form) {
        return switch (form) {
            case CLASS -> CLASS;
            case INTERFACE -> INTERFACE;
            case RECORD -> RECORD;
            case ENUM -> ENUM;
            case ANNOTATION -> ANNOTATION;
        };
    }
}
