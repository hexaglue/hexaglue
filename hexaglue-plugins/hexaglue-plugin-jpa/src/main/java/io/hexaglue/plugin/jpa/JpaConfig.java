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
