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

package io.hexaglue.core.plugin;

import io.hexaglue.spi.plugin.DiagnosticReporter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DiagnosticReporter} that collects diagnostics and logs them.
 */
final class CollectingDiagnosticReporter implements DiagnosticReporter {

    private static final Logger log = LoggerFactory.getLogger(CollectingDiagnosticReporter.class);

    private final String pluginId;
    private final List<PluginDiagnostic> diagnostics = new ArrayList<>();

    CollectingDiagnosticReporter(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public void info(String message) {
        log.info("[{}] {}", pluginId, message);
        diagnostics.add(new PluginDiagnostic(PluginDiagnostic.Severity.INFO, message, null));
    }

    @Override
    public void warn(String message) {
        log.warn("[{}] {}", pluginId, message);
        diagnostics.add(new PluginDiagnostic(PluginDiagnostic.Severity.WARNING, message, null));
    }

    @Override
    public void error(String message) {
        log.error("[{}] {}", pluginId, message);
        diagnostics.add(new PluginDiagnostic(PluginDiagnostic.Severity.ERROR, message, null));
    }

    @Override
    public void error(String message, Throwable cause) {
        log.error("[{}] {}", pluginId, message, cause);
        diagnostics.add(new PluginDiagnostic(PluginDiagnostic.Severity.ERROR, message, cause));
    }

    List<PluginDiagnostic> getDiagnostics() {
        return List.copyOf(diagnostics);
    }
}
