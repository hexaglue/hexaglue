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
import io.hexaglue.spi.arch.ArchModelPluginContext;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.SimpleTemplateEngine;
import io.hexaglue.spi.plugin.TemplateEngine;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link ArchModelPluginContext}.
 *
 * <p>This implementation supports both the legacy {@link IrSnapshot} API and the
 * v4 {@link ArchitecturalModel} API. Plugins can use {@code instanceof ArchModelPluginContext}
 * to check for v4 support and access the model via {@link #model()}.
 *
 * @since 4.0.0
 */
final class DefaultPluginContext implements ArchModelPluginContext {

    private final String pluginId;
    private final IrSnapshot ir;
    private final ArchitecturalModel model;
    private final PluginConfig config;
    private final CodeWriter writer;
    private final DiagnosticReporter diagnostics;
    private final PluginOutputStore outputStore;
    private final TemplateEngine templateEngine;
    private final ArchitectureQuery architectureQuery;

    /**
     * Creates a context with both legacy IR and v4 model support.
     *
     * @param pluginId the plugin identifier
     * @param ir the legacy IR snapshot (for backward compatibility)
     * @param model the v4 architectural model (may be null in legacy mode)
     * @param config the plugin configuration
     * @param writer the code writer
     * @param diagnostics the diagnostic reporter
     * @param outputStore the output store for inter-plugin communication
     * @param architectureQuery the architecture query (may be null)
     */
    DefaultPluginContext(
            String pluginId,
            IrSnapshot ir,
            ArchitecturalModel model,
            PluginConfig config,
            CodeWriter writer,
            DiagnosticReporter diagnostics,
            PluginOutputStore outputStore,
            ArchitectureQuery architectureQuery) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId must not be null");
        this.ir = Objects.requireNonNull(ir, "ir must not be null");
        this.model = model; // May be null in legacy mode
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        this.outputStore = Objects.requireNonNull(outputStore, "outputStore must not be null");
        this.templateEngine = new SimpleTemplateEngine();
        this.architectureQuery = architectureQuery;
    }

    /**
     * Creates a context with legacy IR only (backward compatibility).
     */
    DefaultPluginContext(
            String pluginId,
            IrSnapshot ir,
            PluginConfig config,
            CodeWriter writer,
            DiagnosticReporter diagnostics,
            PluginOutputStore outputStore,
            ArchitectureQuery architectureQuery) {
        this(pluginId, ir, null, config, writer, diagnostics, outputStore, architectureQuery);
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

    @Override
    public Optional<ArchitectureQuery> architectureQuery() {
        return Optional.ofNullable(architectureQuery);
    }

    @Override
    public ArchitecturalModel model() {
        if (model == null) {
            throw new IllegalStateException(
                    "ArchitecturalModel not available. This context was created in legacy mode. "
                            + "Use ir() for backward compatibility or ensure the context was created with a model.");
        }
        return model;
    }

    /**
     * Returns true if this context has an {@link ArchitecturalModel}.
     *
     * @return true if model() will return a valid model
     */
    public boolean hasModel() {
        return model != null;
    }
}
