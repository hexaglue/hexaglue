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

package io.hexaglue.core.engine;

import io.hexaglue.spi.ir.SourceRef;

/**
 * A diagnostic message from the analysis.
 *
 * @param severity the severity level
 * @param code the diagnostic code (e.g., "HG-CORE-001")
 * @param message the human-readable message
 * @param sourceRef the source location, if applicable
 */
public record Diagnostic(Severity severity, String code, String message, SourceRef sourceRef) {

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public static Diagnostic info(String code, String message) {
        return new Diagnostic(Severity.INFO, code, message, null);
    }

    public static Diagnostic warning(String code, String message) {
        return new Diagnostic(Severity.WARNING, code, message, null);
    }

    public static Diagnostic warning(String code, String message, SourceRef sourceRef) {
        return new Diagnostic(Severity.WARNING, code, message, sourceRef);
    }

    public static Diagnostic error(String code, String message) {
        return new Diagnostic(Severity.ERROR, code, message, null);
    }

    public static Diagnostic error(String code, String message, SourceRef sourceRef) {
        return new Diagnostic(Severity.ERROR, code, message, sourceRef);
    }
}
