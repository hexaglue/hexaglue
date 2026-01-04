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

import java.util.List;

/**
 * A constructor in a Java type.
 */
public non-sealed interface JavaConstructor extends JavaMember {

    /**
     * Returns the constructor parameters.
     */
    List<JavaParameter> parameters();

    /**
     * Returns the declared thrown types.
     */
    List<TypeRef> thrownTypes();

    /**
     * Returns true if this is a no-arg constructor.
     */
    default boolean isNoArg() {
        return parameters().isEmpty();
    }
}
