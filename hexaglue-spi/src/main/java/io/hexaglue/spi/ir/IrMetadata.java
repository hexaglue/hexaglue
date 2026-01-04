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

package io.hexaglue.spi.ir;

import java.time.Instant;

/**
 * Metadata about the IR analysis.
 *
 * @param basePackage the base package that was analyzed
 * @param timestamp when the analysis was performed
 * @param engineVersion the HexaGlue engine version
 * @param typeCount total number of types analyzed
 * @param portCount total number of ports detected
 */
public record IrMetadata(String basePackage, Instant timestamp, String engineVersion, int typeCount, int portCount) {}
