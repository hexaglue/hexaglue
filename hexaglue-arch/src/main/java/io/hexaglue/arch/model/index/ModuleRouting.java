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

package io.hexaglue.arch.model.index;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility for automatic module routing based on {@link ModuleRole}.
 *
 * <p>Plugins use this to resolve the target module for generated code when no
 * explicit {@code targetModule} is configured. The resolution is intentionally
 * conservative: it only returns a module when exactly one module has the requested
 * role. If zero or multiple modules match, the result is empty, and the caller
 * is expected to handle the fallback (typically writing to the default output).</p>
 *
 * @since 5.0.0
 */
public final class ModuleRouting {

    private ModuleRouting() {}

    /**
     * Returns the unique module ID for the given role, if exactly one module has it.
     *
     * <p>Returns {@link Optional#empty()} in two cases:
     * <ul>
     *   <li>No module has the requested role (no match)</li>
     *   <li>Multiple modules have the requested role (ambiguous)</li>
     * </ul>
     *
     * @param moduleIndex the module index to search
     * @param role the role to look for
     * @return the unique module ID, or empty if resolution is ambiguous or impossible
     * @throws NullPointerException if any argument is null
     */
    public static Optional<String> resolveUniqueModuleByRole(ModuleIndex moduleIndex, ModuleRole role) {
        Objects.requireNonNull(moduleIndex, "moduleIndex must not be null");
        Objects.requireNonNull(role, "role must not be null");

        List<ModuleDescriptor> matches = moduleIndex.modulesByRole(role).toList();
        if (matches.size() == 1) {
            return Optional.of(matches.get(0).moduleId());
        }
        return Optional.empty();
    }
}
