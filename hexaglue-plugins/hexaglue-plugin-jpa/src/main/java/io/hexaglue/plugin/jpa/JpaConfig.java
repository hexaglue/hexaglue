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
 * Configuration record for the JPA plugin.
 *
 * <p>This record holds all configurable options for JPA code generation.
 * Configuration values are loaded from the hexaglue.yaml plugin configuration
 * section via {@link #from(PluginConfig)}.
 *
 * <h3>Configuration Example:</h3>
 * <pre>
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       entitySuffix: Entity
 *       repositorySuffix: JpaRepository
 *       adapterSuffix: Adapter
 *       mapperSuffix: Mapper
 *       tablePrefix: app_
 *       enableAuditing: true
 *       enableOptimisticLocking: true
 *       generateRepositories: true
 *       generateMappers: true
 *       generateAdapters: true
 * </pre>
 *
 * @param entitySuffix suffix for generated JPA entity classes (default: "Entity")
 * @param embeddableSuffix suffix for generated JPA embeddable classes (default: "Embeddable")
 * @param repositorySuffix suffix for Spring Data repository interfaces (default: "JpaRepository")
 * @param adapterSuffix suffix for port adapter classes (default: "Adapter")
 * @param mapperSuffix suffix for MapStruct mapper interfaces (default: "Mapper")
 * @param tablePrefix prefix for database table names (default: "")
 * @param enableAuditing true to add JPA auditing annotations (createdDate, lastModifiedDate)
 * @param enableOptimisticLocking true to add @Version field for optimistic locking
 * @param generateRepositories true to generate Spring Data JPA repository interfaces
 * @param generateMappers true to generate MapStruct mapper interfaces
 * @param generateAdapters true to generate port adapter implementations
 * @param generateEmbeddables true to generate JPA embeddable classes for value objects
 * @since 2.0.0
 */
public record JpaConfig(
        String entitySuffix,
        String embeddableSuffix,
        String repositorySuffix,
        String adapterSuffix,
        String mapperSuffix,
        String tablePrefix,
        boolean enableAuditing,
        boolean enableOptimisticLocking,
        boolean generateRepositories,
        boolean generateMappers,
        boolean generateAdapters,
        boolean generateEmbeddables) {

    /**
     * Creates configuration from plugin config.
     */
    static JpaConfig from(PluginConfig config) {
        return new JpaConfig(
                config.getString("entitySuffix", "Entity"),
                config.getString("embeddableSuffix", "Embeddable"),
                config.getString("repositorySuffix", "JpaRepository"),
                config.getString("adapterSuffix", "Adapter"),
                config.getString("mapperSuffix", "Mapper"),
                config.getString("tablePrefix", ""),
                config.getBoolean("enableAuditing", false),
                config.getBoolean("enableOptimisticLocking", false),
                config.getBoolean("generateRepositories", true),
                config.getBoolean("generateMappers", true),
                config.getBoolean("generateAdapters", true),
                config.getBoolean("generateEmbeddables", true));
    }

    /**
     * Creates default configuration.
     */
    static JpaConfig defaults() {
        return new JpaConfig(
                "Entity", "Embeddable", "JpaRepository", "Adapter", "Mapper", "", false, false, true, true, true, true);
    }
}
