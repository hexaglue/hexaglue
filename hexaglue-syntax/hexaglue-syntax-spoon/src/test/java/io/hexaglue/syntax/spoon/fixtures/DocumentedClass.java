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

package io.hexaglue.syntax.spoon.fixtures;

/**
 * A documented class used as a test fixture for Javadoc extraction.
 *
 * <p>This class contains fields and methods with and without Javadoc
 * to verify that the extraction pipeline works correctly.</p>
 *
 * @since 5.0.0
 * @author HexaGlue Team
 */
public class DocumentedClass {

    /**
     * The documented field holding a name value.
     */
    private String documentedField;

    private int undocumentedField;

    /**
     * Returns the documented field value.
     *
     * @return the documented field value, never null
     */
    public String getDocumentedField() {
        return documentedField;
    }

    public int getUndocumentedField() {
        return undocumentedField;
    }
}
