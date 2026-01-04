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

package io.hexaglue.plugin.jpa;

import io.hexaglue.spi.plugin.PluginConfig;

/**
 * Configuration for the JPA plugin.
 */
record JpaConfig(
        String entitySuffix,
        String repositorySuffix,
        String adapterSuffix,
        String mapperSuffix,
        String tablePrefix,
        boolean enableAuditing,
        boolean enableOptimisticLocking,
        boolean generateRepositories,
        boolean generateMappers,
        boolean generateAdapters) {

    /**
     * Creates configuration from plugin config.
     */
    static JpaConfig from(PluginConfig config) {
        return new JpaConfig(
                config.getString("entitySuffix", "Entity"),
                config.getString("repositorySuffix", "JpaRepository"),
                config.getString("adapterSuffix", "Adapter"),
                config.getString("mapperSuffix", "Mapper"),
                config.getString("tablePrefix", ""),
                config.getBoolean("enableAuditing", false),
                config.getBoolean("enableOptimisticLocking", false),
                config.getBoolean("generateRepositories", true),
                config.getBoolean("generateMappers", true),
                config.getBoolean("generateAdapters", true));
    }

    /**
     * Creates default configuration.
     */
    static JpaConfig defaults() {
        return new JpaConfig("Entity", "JpaRepository", "Adapter", "Mapper", "", false, false, true, true, true);
    }
}
