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

import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import io.hexaglue.spi.plugin.SimpleTemplateEngine;
import io.hexaglue.spi.plugin.TemplateEngine;
import java.util.Optional;

/**
 * Default implementation of {@link PluginContext}.
 */
final class DefaultPluginContext implements PluginContext {

    private final String pluginId;
    private final IrSnapshot ir;
    private final PluginConfig config;
    private final CodeWriter writer;
    private final DiagnosticReporter diagnostics;
    private final PluginOutputStore outputStore;
    private final TemplateEngine templateEngine;

    DefaultPluginContext(
            String pluginId,
            IrSnapshot ir,
            PluginConfig config,
            CodeWriter writer,
            DiagnosticReporter diagnostics,
            PluginOutputStore outputStore) {
        this.pluginId = pluginId;
        this.ir = ir;
        this.config = config;
        this.writer = writer;
        this.diagnostics = diagnostics;
        this.outputStore = outputStore;
        this.templateEngine = new SimpleTemplateEngine();
    }

    @Override
    public IrSnapshot ir() {
        return ir;
    }

    @Override
    public PluginConfig config() {
        return config;
    }

    @Override
    public CodeWriter writer() {
        return writer;
    }

    @Override
    public DiagnosticReporter diagnostics() {
        return diagnostics;
    }

    @Override
    public <T> void setOutput(String key, T value) {
        outputStore.set(pluginId, key, value);
    }

    @Override
    public <T> Optional<T> getOutput(String fromPluginId, String key, Class<T> type) {
        return outputStore.get(fromPluginId, key, type);
    }

    @Override
    public String currentPluginId() {
        return pluginId;
    }

    @Override
    public TemplateEngine templates() {
        return templateEngine;
    }
}
