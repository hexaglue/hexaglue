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

package io.hexaglue.spi.arch;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.spi.plugin.PluginContext;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility methods for working with {@link PluginContext} and {@link ArchModelPluginContext}.
 *
 * <p>This class provides convenient methods to:
 * <ul>
 *   <li>Check if a context supports the v4 {@link ArchitecturalModel}</li>
 *   <li>Safely extract the model from a context</li>
 *   <li>Convert a context to {@link ArchModelPluginContext} if possible</li>
 * </ul>
 *
 * <p>Example usage for gradual migration:
 * <pre>{@code
 * public void execute(PluginContext context) {
 *     if (PluginContexts.hasArchModel(context)) {
 *         // Use the new API
 *         ArchitecturalModel model = PluginContexts.getModel(context).orElseThrow();
 *         model.domainEntities().forEach(entity -> ...);
 *     } else {
 *         // Fall back to legacy API
 *         IrSnapshot ir = context.ir();
 *         ir.domain().types().forEach(type -> ...);
 *     }
 * }
 * }</pre>
 *
 * @see ArchModelPluginContext
 * @see ArchitecturalModel
 * @since 4.0.0
 */
public final class PluginContexts {

    private PluginContexts() {
        // Utility class
    }

    /**
     * Checks if the given context supports the v4 {@link ArchitecturalModel}.
     *
     * @param context the plugin context to check
     * @return true if the context is an {@link ArchModelPluginContext}
     * @throws NullPointerException if context is null
     */
    public static boolean hasArchModel(PluginContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return context instanceof ArchModelPluginContext;
    }

    /**
     * Extracts the {@link ArchitecturalModel} from the context if available.
     *
     * <p>This method safely handles contexts that implement {@link ArchModelPluginContext}
     * but were created in legacy mode (without an actual model). In such cases, it returns
     * an empty Optional rather than throwing an exception.
     *
     * @param context the plugin context
     * @return the architectural model, or empty if not available
     * @throws NullPointerException if context is null
     */
    public static Optional<ArchitecturalModel> getModel(PluginContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (context instanceof ArchModelPluginContext archContext) {
            try {
                return Optional.of(archContext.model());
            } catch (IllegalStateException e) {
                // Context was created in legacy mode without a model
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Converts the context to {@link ArchModelPluginContext} if possible.
     *
     * @param context the plugin context
     * @return the arch model context, or empty if not available
     * @throws NullPointerException if context is null
     */
    public static Optional<ArchModelPluginContext> asArchModelContext(PluginContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (context instanceof ArchModelPluginContext archContext) {
            return Optional.of(archContext);
        }
        return Optional.empty();
    }
}
