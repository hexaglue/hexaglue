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

import java.util.List;

/**
 * Documentation model for domain properties.
 *
 * @param name the property name
 * @param type the property type qualified name
 * @param cardinality the cardinality (SINGLE, OPTIONAL, COLLECTION)
 * @param nullability the nullability (NULLABLE, NON_NULL, UNKNOWN)
 * @param isIdentity whether this is the identity property
 * @param isEmbedded whether this is an embedded property
 * @param isSimple whether this is a simple type
 * @param isParameterized whether the type is parameterized
 * @param typeArguments type arguments if parameterized, empty list otherwise
 * @param relationInfo relation information if this property has a relation, or null
 */
public record PropertyDoc(
        String name,
        String type,
        String cardinality,
        String nullability,
        boolean isIdentity,
        boolean isEmbedded,
        boolean isSimple,
        boolean isParameterized,
        List<String> typeArguments,
        RelationInfoDoc relationInfo) {}
