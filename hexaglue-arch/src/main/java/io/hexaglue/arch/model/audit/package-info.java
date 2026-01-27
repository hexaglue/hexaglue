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

/**
 * Audit model types for architectural analysis and code quality assessment.
 *
 * <p>This package contains data models used by the audit subsystem to represent:
 * <ul>
 *   <li>Code units and their structural information</li>
 *   <li>Quality and architecture metrics (Lakos, coupling, etc.)</li>
 *   <li>Rule violations and dependency analysis</li>
 *   <li>Classification enums (layer, role, severity)</li>
 * </ul>
 *
 * <p>These types were migrated from {@code io.hexaglue.spi.audit} to
 * properly separate data models (in arch) from plugin contracts (in spi).
 *
 * @since 5.0.0
 */
package io.hexaglue.arch.model.audit;
