/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Package organization style detection.
 *
 * <p>The {@link io.hexaglue.core.graph.style.StyleDetector} analyzes package
 * patterns to detect the codebase organization style, which influences
 * classification confidence.
 *
 * <h2>Detected Styles</h2>
 * <ul>
 *   <li><b>HEXAGONAL</b> - Ports &amp; Adapters (ports.in, ports.out, adapters)</li>
 *   <li><b>BY_LAYER</b> - Layer-based (controller, service, repository)</li>
 *   <li><b>BY_FEATURE</b> - Feature-based modules</li>
 *   <li><b>FLAT</b> - Single package</li>
 *   <li><b>UNKNOWN</b> - No clear pattern</li>
 * </ul>
 *
 * @see io.hexaglue.core.graph.style.StyleDetector Style detection logic
 * @see io.hexaglue.core.graph.style.PackageOrganizationStyle Style enum
 */
package io.hexaglue.core.graph.style;
