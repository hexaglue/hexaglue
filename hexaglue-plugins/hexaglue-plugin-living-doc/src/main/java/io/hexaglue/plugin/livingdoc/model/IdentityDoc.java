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

/**
 * Documentation model for identity fields.
 *
 * @param fieldName the identity field name
 * @param type the declared type simple name
 * @param underlyingType the underlying type simple name (unwrapped)
 * @param strategy the identity generation strategy
 * @param wrapperKind the wrapper kind (e.g., WRAPPED, PRIMITIVE)
 * @param isWrapped whether the identity is wrapped
 * @param requiresGeneratedValue whether @GeneratedValue is required
 * @param jpaGenerationType the JPA generation type, or null if not applicable
 */
public record IdentityDoc(
        String fieldName,
        String type,
        String underlyingType,
        String strategy,
        String wrapperKind,
        boolean isWrapped,
        boolean requiresGeneratedValue,
        String jpaGenerationType) {}
