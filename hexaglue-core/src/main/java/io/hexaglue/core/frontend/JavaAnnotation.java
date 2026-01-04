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

package io.hexaglue.core.frontend;

import java.util.Map;
import java.util.Optional;

/**
 * An annotation present on a Java element.
 *
 * @param annotationType the annotation type reference
 * @param values the annotation attribute values (name -> value)
 */
public record JavaAnnotation(TypeRef annotationType, Map<String, Object> values) {

    /**
     * Returns the fully qualified name of the annotation type.
     */
    public String qualifiedName() {
        return annotationType.rawQualifiedName();
    }

    /**
     * Returns the simple name of the annotation type.
     */
    public String simpleName() {
        return annotationType.simpleName();
    }

    /**
     * Returns a string attribute value.
     */
    public Optional<String> getString(String name) {
        Object v = values.get(name);
        return v instanceof String s ? Optional.of(s) : Optional.empty();
    }

    /**
     * Returns a boolean attribute value.
     */
    public Optional<Boolean> getBoolean(String name) {
        Object v = values.get(name);
        return v instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Returns the "value" attribute as a string.
     */
    public Optional<String> value() {
        return getString("value");
    }
}
