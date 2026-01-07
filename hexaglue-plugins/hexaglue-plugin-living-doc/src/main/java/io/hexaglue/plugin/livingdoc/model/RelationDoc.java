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
 * Documentation model for domain relations.
 *
 * @param propertyName the property name
 * @param targetType the target type simple name
 * @param targetKind the target domain kind
 * @param kind the relation kind
 * @param isOwning whether this is the owning side
 * @param isBidirectional whether the relation is bidirectional
 * @param mappedBy the mapped-by property name, or null
 * @param cascade the cascade strategy
 * @param fetch the fetch strategy
 * @param orphanRemoval whether orphan removal is enabled
 */
public record RelationDoc(
        String propertyName,
        String targetType,
        String targetKind,
        String kind,
        boolean isOwning,
        boolean isBidirectional,
        String mappedBy,
        String cascade,
        String fetch,
        boolean orphanRemoval) {}
