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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Information about a diagram in the report.
 * Note: The actual Mermaid content is in DiagramSet, this is just metadata.
 *
 * @param title diagram title
 * @param description what the diagram shows
 * @since 5.0.0
 */
public record DiagramInfo(String title, String description) {

    /**
     * Creates diagram info with validation.
     */
    public DiagramInfo {
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(description, "description is required");
    }
}
