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

package io.hexaglue.core.classification.port;

import io.hexaglue.core.classification.ClassificationCriteria;

/**
 * Specialized criteria for port classification that includes port direction.
 */
public interface PortClassificationCriteria extends ClassificationCriteria<PortKind> {

    /**
     * Returns the direction of the port this criteria targets.
     */
    PortDirection targetDirection();
}
