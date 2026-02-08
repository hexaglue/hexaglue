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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.List;
import java.util.Objects;

/**
 * Module topology for multi-module projects.
 *
 * <p>This record captures the layout of modules in a multi-module project,
 * including each module's role, type count, and packages. It is used by
 * renderers to display a "Module Topology" section in the audit report.
 *
 * <p>In mono-module mode, this record is absent (null) from the
 * {@link ArchitectureOverview}.
 *
 * @param modules the list of module information
 * @param summary a human-readable summary of the module topology
 * @since 5.0.0
 */
public record ModuleTopology(List<ModuleInfo> modules, String summary) {

    /**
     * Creates a ModuleTopology with validation.
     */
    public ModuleTopology {
        Objects.requireNonNull(modules, "modules is required");
        Objects.requireNonNull(summary, "summary is required");
        modules = List.copyOf(modules);
    }

    /**
     * Information about a single module.
     *
     * @param moduleId the module identifier
     * @param role the architectural role of the module
     * @param typeCount the number of classified types in this module
     * @param packages the distinct packages found in this module
     * @since 5.0.0
     */
    public record ModuleInfo(String moduleId, String role, int typeCount, List<String> packages) {

        /**
         * Creates a ModuleInfo with validation.
         */
        public ModuleInfo {
            Objects.requireNonNull(moduleId, "moduleId is required");
            Objects.requireNonNull(role, "role is required");
            packages = packages != null ? List.copyOf(packages) : List.of();
        }
    }
}
