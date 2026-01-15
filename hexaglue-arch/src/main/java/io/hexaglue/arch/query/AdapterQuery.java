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

package io.hexaglue.arch.query;

import io.hexaglue.arch.ArchElement;

/**
 * Specialized query interface for adapters (driving and driven).
 *
 * <h2>Example</h2>
 * <pre>{@code
 * List<ArchElement> adapters = query.drivingAdapters()
 *     .inPackage("com.example.adapters")
 *     .toList();
 * }</pre>
 *
 * @since 4.0.0
 */
public interface AdapterQuery<T extends ArchElement> extends ElementQuery<T> {
    // Adapter-specific methods can be added here in the future
}
