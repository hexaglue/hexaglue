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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import io.hexaglue.spi.plugin.SimpleTemplateEngine;
import io.hexaglue.spi.plugin.TemplateEngine;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link PluginContext}.
 *
 * <p>This implementation provides plugins with access to the unified
 * {@link ArchitecturalModel} and all necessary facilities for code generation.
 *
 * @since 4.0.0
 */
final class DefaultPluginContext implements PluginContext {

    private final String pluginId;
    private final ArchitecturalModel model;
    private final PluginConfig config;
    private final CodeWriter writer;
    private final DiagnosticReporter diagnostics;
    private final PluginOutputStore outputStore;
    private final TemplateEngine templateEngine;
    private final ArchitectureQuery architectureQuery;

    /**
     * Creates a plugin context with the v4 architectural model.
     *
     * @param pluginId the plugin identifier
     * @param model the v4 architectural model (never null)
     * @param config the plugin configuration
     * @param writer the code writer
     * @param diagnostics the diagnostic reporter
     * @param outputStore the output store for inter-plugin communication
     * @param architectureQuery the architecture query (may be null)
     */
    DefaultPluginContext(
            String pluginId,
            ArchitecturalModel model,
            PluginConfig config,
            CodeWriter writer,
            DiagnosticReporter diagnostics,
            PluginOutputStore outputStore,
            ArchitectureQuery architectureQuery) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        this.outputStore = Objects.requireNonNull(outputStore, "outputStore must not be null");
        this.templateEngine = new SimpleTemplateEngine();
        this.architectureQuery = architectureQuery;
    }

    @Override
    public ArchitecturalModel model() {
        return model;
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

    @Override
    public Optional<ArchitectureQuery> architectureQuery() {
        return Optional.ofNullable(architectureQuery);
    }
}
