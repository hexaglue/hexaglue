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

package io.hexaglue.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validates that {@code targetModule} references in plugin configurations point to
 * actual modules in the reactor.
 *
 * <p>This validation catches configuration errors early (at build time) rather than
 * letting them propagate as obscure file-not-found errors during code generation.</p>
 *
 * @since 5.0.0
 */
final class TargetModuleValidator {

    private TargetModuleValidator() {}

    /**
     * Result of a target module validation.
     *
     * @param errors list of human-readable error messages; empty if validation passed
     */
    record ValidationResult(List<String> errors) {

        /**
         * Returns {@code true} if all targetModule references are valid.
         */
        boolean isValid() {
            return errors.isEmpty();
        }
    }

    /**
     * Validates that every {@code targetModule} value in the given plugin configurations
     * refers to a known module in the reactor.
     *
     * <p>Only string-typed {@code targetModule} values are validated. Non-string or null
     * values are silently ignored.</p>
     *
     * @param pluginConfigs the plugin configurations (plugin ID → config map)
     * @param knownModuleIds the set of known module IDs from the reactor
     * @return the validation result
     */
    static ValidationResult validate(Map<String, Map<String, Object>> pluginConfigs, Set<String> knownModuleIds) {
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : pluginConfigs.entrySet()) {
            String pluginId = entry.getKey();
            Map<String, Object> config = entry.getValue();

            Object targetModule = config.get("targetModule");
            if (targetModule instanceof String moduleId && !moduleId.isBlank()) {
                if (!knownModuleIds.contains(moduleId)) {
                    errors.add(String.format(
                            "Plugin '%s' references targetModule '%s' which is not a known reactor module. "
                                    + "Known modules: %s",
                            pluginId, moduleId, new TreeSet<>(knownModuleIds)));
                }
            }
        }

        return new ValidationResult(List.copyOf(errors));
    }
}
