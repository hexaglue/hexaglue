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

/**
 * A named Java element.
 */
public interface JavaNamed {

    /**
     * Returns the simple name (without package/enclosing type).
     */
    String simpleName();

    /**
     * Returns the fully qualified name.
     */
    String qualifiedName();

    /**
     * Returns the package name.
     */
    String packageName();
}
