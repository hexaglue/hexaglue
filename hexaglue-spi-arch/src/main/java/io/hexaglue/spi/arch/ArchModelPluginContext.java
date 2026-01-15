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

/**
 * Extended plugin context that provides access to the v4 {@link ArchitecturalModel}.
 *
 * <p>This interface extends {@link PluginContext} with the unified architectural model,
 * enabling plugins to leverage the new classification system, element registry, and
 * relationship store introduced in HexaGlue v4.
 *
 * <p>Plugins that want to use the v4 API should:
 * <ol>
 *   <li>Depend on {@code hexaglue-spi-arch} instead of just {@code hexaglue-spi}</li>
 *   <li>Check if the context is an instance of {@code ArchModelPluginContext}</li>
 *   <li>Use {@link #model()} to access the architectural model</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * public void execute(PluginContext context) {
 *     if (context instanceof ArchModelPluginContext archContext) {
 *         ArchitecturalModel model = archContext.model();
 *
 *         // Use the new API
 *         model.domainEntities().forEach(entity -> {
 *             System.out.println(entity.id().simpleName());
 *             System.out.println(entity.classificationTrace().explain());
 *         });
 *
 *         model.drivenPorts().forEach(port -> {
 *             // Access classification trace for audit
 *             port.classificationTrace().remediationHints();
 *         });
 *     } else {
 *         // Fall back to legacy IrSnapshot API
 *         IrSnapshot ir = context.ir();
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @see ArchitecturalModel
 * @see PluginContext
 * @since 4.0.0
 */
public interface ArchModelPluginContext extends PluginContext {

    /**
     * Returns the architectural model.
     *
     * <p>The model provides access to:
     * <ul>
     *   <li>Domain elements (aggregates, entities, value objects, identifiers, events)</li>
     *   <li>Port elements (driving ports, driven ports)</li>
     *   <li>Classification traces with explanations and remediation hints</li>
     *   <li>Element relationships via the relationship store</li>
     *   <li>Analysis metadata (parser name, duration, types analyzed)</li>
     * </ul>
     *
     * <p>The model is immutable and safe to traverse concurrently.
     *
     * @return the architectural model (never null)
     */
    ArchitecturalModel model();
}
