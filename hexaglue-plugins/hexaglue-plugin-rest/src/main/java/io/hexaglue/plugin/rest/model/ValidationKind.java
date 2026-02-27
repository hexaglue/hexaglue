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

package io.hexaglue.plugin.rest.model;

/**
 * Bean Validation annotation kind for DTO fields.
 *
 * @since 3.1.0
 */
public enum ValidationKind {
    /** @NotNull for non-null reference types. */
    NOT_NULL,
    /** @NotBlank for non-blank strings. */
    NOT_BLANK,
    /** @NotEmpty for non-empty collections. */
    NOT_EMPTY,
    /** @Positive for positive numbers. */
    POSITIVE,
    /** @Email for email format. */
    EMAIL,
    /** No validation annotation. */
    NONE
}
