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

package io.hexaglue.spi.audit;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a codebase being audited.
 *
 * @param name         the codebase name (project name)
 * @param basePackage  the base package of the application
 * @param units        all code units in the codebase
 * @param dependencies the dependency map (unit qualified name -> set of dependencies)
 * @since 3.0.0
 */
public record Codebase(String name, String basePackage, List<CodeUnit> units, Map<String, Set<String>> dependencies) {

    /**
     * Compact constructor with defensive copies.
     */
    public Codebase {
        units = units != null ? List.copyOf(units) : List.of();
        dependencies = dependencies != null ? Map.copyOf(dependencies) : Map.of();
    }

    /**
     * Returns code units in a specific layer.
     *
     * @param layer the layer to filter by
     * @return list of units in that layer
     */
    public List<CodeUnit> unitsInLayer(LayerClassification layer) {
        return units.stream().filter(u -> u.layer() == layer).toList();
    }

    /**
     * Returns code units with a specific role.
     *
     * @param role the role to filter by
     * @return list of units with that role
     */
    public List<CodeUnit> unitsWithRole(RoleClassification role) {
        return units.stream().filter(u -> u.role() == role).toList();
    }

    /**
     * Returns the total number of code units.
     *
     * @return the unit count
     */
    public int unitCount() {
        return units.size();
    }
}
