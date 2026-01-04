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

package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.SourceRef;
import java.util.Optional;
import spoon.reflect.cu.SourcePosition;

/**
 * Adapts Spoon's {@link SourcePosition} to {@link SourceRef}.
 */
public final class SpoonSourceRefAdapter {

    private SpoonSourceRefAdapter() {}

    public static Optional<SourceRef> adapt(SourcePosition pos) {
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) {
            return Optional.empty();
        }
        return Optional.of(
                new SourceRef(pos.getFile().getPath(), pos.getLine(), Math.max(pos.getLine(), pos.getEndLine())));
    }
}
