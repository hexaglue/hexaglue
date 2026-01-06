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

package io.hexaglue.core.classification.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import org.yaml.snakeyaml.Yaml;

/**
 * A {@link CriteriaProfile} implementation that loads priority overrides from YAML.
 *
 * <p>The YAML format is a simple map of criteria keys to priorities:
 * <pre>{@code
 * # Priority overrides for classification criteria
 * priorities:
 *   explicit-aggregate-root: 100
 *   explicit-entity: 100
 *   repository-dominant: 85
 *   has-identity: 65
 * }</pre>
 *
 * <p>Criteria keys correspond to the values returned by {@link CriteriaKey#of}.
 * For criteria implementing {@link IdentifiedCriteria}, this is the {@code id()};
 * otherwise, it's the {@code name()}.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Load from classpath resource
 * CriteriaProfile profile = YamlCriteriaProfile.fromResource("profiles/strict.yaml");
 *
 * // Load from file
 * CriteriaProfile profile = YamlCriteriaProfile.fromPath(Paths.get("config/priorities.yaml"));
 *
 * // Use with classifier
 * DomainClassifier classifier = new DomainClassifier(DomainClassifier.defaultCriteria(), profile);
 * }</pre>
 */
public final class YamlCriteriaProfile implements CriteriaProfile {

    private static final String PRIORITIES_KEY = "priorities";

    private final Map<String, Integer> priorities;
    private final String source;

    private YamlCriteriaProfile(Map<String, Integer> priorities, String source) {
        this.priorities = Map.copyOf(priorities);
        this.source = source;
    }

    /**
     * Loads a profile from a classpath resource.
     *
     * @param resourcePath the classpath resource path (e.g., "profiles/strict.yaml")
     * @return the loaded profile
     * @throws IllegalArgumentException if the resource is not found
     * @throws UncheckedIOException if reading fails
     */
    public static YamlCriteriaProfile fromResource(String resourcePath) {
        return fromResource(resourcePath, YamlCriteriaProfile.class.getClassLoader());
    }

    /**
     * Loads a profile from a classpath resource using the specified class loader.
     *
     * @param resourcePath the classpath resource path
     * @param classLoader the class loader to use
     * @return the loaded profile
     * @throws IllegalArgumentException if the resource is not found
     * @throws UncheckedIOException if reading fails
     */
    public static YamlCriteriaProfile fromResource(String resourcePath, ClassLoader classLoader) {
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return fromInputStream(is, "resource:" + resourcePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load profile from resource: " + resourcePath, e);
        }
    }

    /**
     * Loads a profile from a file path.
     *
     * @param path the path to the YAML file
     * @return the loaded profile
     * @throws UncheckedIOException if reading fails
     */
    public static YamlCriteriaProfile fromPath(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return fromReader(reader, "file:" + path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load profile from path: " + path, e);
        }
    }

    /**
     * Loads a profile from an input stream.
     *
     * @param inputStream the input stream to read from
     * @param source a description of the source (for error messages)
     * @return the loaded profile
     */
    public static YamlCriteriaProfile fromInputStream(InputStream inputStream, String source) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(inputStream);
        return parseYaml(root, source);
    }

    /**
     * Loads a profile from a reader.
     *
     * @param reader the reader to read from
     * @param source a description of the source (for error messages)
     * @return the loaded profile
     */
    public static YamlCriteriaProfile fromReader(Reader reader, String source) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(reader);
        return parseYaml(root, source);
    }

    /**
     * Loads a profile from YAML content string.
     *
     * @param yamlContent the YAML content
     * @return the loaded profile
     */
    public static YamlCriteriaProfile fromString(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(yamlContent);
        return parseYaml(root, "string");
    }

    private static YamlCriteriaProfile parseYaml(Map<String, Object> root, String source) {
        if (root == null) {
            return new YamlCriteriaProfile(Collections.emptyMap(), source);
        }

        Object prioritiesObj = root.get(PRIORITIES_KEY);
        if (prioritiesObj == null) {
            return new YamlCriteriaProfile(Collections.emptyMap(), source);
        }

        if (!(prioritiesObj instanceof Map)) {
            throw new IllegalArgumentException(
                    "Invalid YAML format in " + source + ": 'priorities' must be a map");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rawPriorities = (Map<String, Object>) prioritiesObj;

        Map<String, Integer> priorities = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : rawPriorities.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Integer) {
                priorities.put(key, (Integer) value);
            } else if (value instanceof Number) {
                priorities.put(key, ((Number) value).intValue());
            } else {
                throw new IllegalArgumentException(
                        "Invalid priority value for '" + key + "' in " + source + ": expected integer, got " + value);
            }
        }

        return new YamlCriteriaProfile(priorities, source);
    }

    @Override
    public OptionalInt priorityFor(String criteriaKey) {
        Integer priority = priorities.get(criteriaKey);
        return priority != null ? OptionalInt.of(priority) : OptionalInt.empty();
    }

    /**
     * Returns the source description of this profile.
     *
     * @return the source (e.g., "resource:profiles/strict.yaml")
     */
    public String source() {
        return source;
    }

    /**
     * Returns an unmodifiable view of all priority overrides.
     *
     * @return the priority overrides map
     */
    public Map<String, Integer> priorities() {
        return priorities;
    }

    @Override
    public String toString() {
        return "YamlCriteriaProfile[source=" + source + ", overrides=" + priorities.size() + "]";
    }
}
