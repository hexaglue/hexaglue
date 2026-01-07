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
 * Documentation model for property relation information.
 *
 * @param kind the relation kind
 * @param targetType the target type qualified name
 * @param owning whether this is the owning side
 * @param mappedBy the mapped-by property name, or null
 * @param isBidirectional whether the relation is bidirectional
 * @param isEmbedded whether this is an embedded relation
 */
public record RelationInfoDoc(
        String kind, String targetType, boolean owning, String mappedBy, boolean isBidirectional, boolean isEmbedded) {}
