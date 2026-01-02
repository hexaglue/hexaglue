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
