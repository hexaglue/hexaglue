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

package io.hexaglue.plugin.livingdoc.util;

import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.ir.PortKind;

/**
 * Maps {@link DrivenPortType} to {@link PortKind} for documentation purposes.
 *
 * <p>This utility centralizes the mapping logic that was previously duplicated
 * across content selectors.</p>
 *
 * @since 5.0.0
 */
public final class PortKindMapper {

    private PortKindMapper() {
        // Utility class
    }

    /**
     * Maps a driven port type to the corresponding documentation port kind.
     *
     * @param portType the driven port type from the architectural model
     * @return the corresponding port kind for documentation
     */
    public static PortKind from(DrivenPortType portType) {
        return switch (portType) {
            case REPOSITORY -> PortKind.REPOSITORY;
            case GATEWAY -> PortKind.GATEWAY;
            case EVENT_PUBLISHER -> PortKind.EVENT_PUBLISHER;
            case NOTIFICATION -> PortKind.GENERIC;
            case OTHER -> PortKind.GENERIC;
        };
    }
}
