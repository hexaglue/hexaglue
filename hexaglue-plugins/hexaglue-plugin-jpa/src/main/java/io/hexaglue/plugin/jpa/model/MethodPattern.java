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

package io.hexaglue.plugin.jpa.model;

import java.util.Locale;

/**
 * Common method patterns found in repository interfaces.
 *
 * <p>This enum categorizes repository methods into standard CRUD operations
 * and custom queries. It helps the adapter generator determine which implementation
 * strategy to use.
 *
 * <p>Design decision: Pattern matching on method names allows us to generate
 * optimized implementations for standard operations while delegating complex
 * queries to Spring Data JPA's query derivation mechanism.
 *
 * <h3>Pattern Recognition Rules:</h3>
 * <ul>
 *   <li><b>SAVE:</b> save, create, persist, store, add, update, upsert</li>
 *   <li><b>FIND_BY_ID:</b> findById, getById, loadById, findOne</li>
 *   <li><b>FIND_ALL:</b> findAll, getAll, list, listAll</li>
 *   <li><b>DELETE:</b> delete, remove, deleteById, removeById</li>
 *   <li><b>EXISTS:</b> exists, existsById, contains</li>
 *   <li><b>COUNT:</b> count, countAll</li>
 *   <li><b>CUSTOM:</b> any other method (Spring Data will derive query)</li>
 * </ul>
 *
 * @since 2.0.0
 */
public enum MethodPattern {

    /**
     * Save or update an entity.
     * <p>Examples: {@code save(order)}, {@code update(order)}, {@code persist(order)}
     */
    SAVE,

    /**
     * Find an entity by its ID.
     * <p>Examples: {@code findById(id)}, {@code getById(id)}, {@code loadById(id)}
     */
    FIND_BY_ID,

    /**
     * Find all entities.
     * <p>Examples: {@code findAll()}, {@code getAll()}, {@code list()}
     */
    FIND_ALL,

    /**
     * Delete an entity.
     * <p>Examples: {@code delete(order)}, {@code remove(order)}, {@code deleteById(id)}
     */
    DELETE,

    /**
     * Check if an entity exists.
     * <p>Examples: {@code existsById(id)}, {@code exists(id)}, {@code contains(id)}
     */
    EXISTS,

    /**
     * Count entities.
     * <p>Examples: {@code count()}, {@code countAll()}
     */
    COUNT,

    /**
     * Custom query method.
     * <p>Examples: {@code findByStatus(status)}, {@code findActiveOrdersAfter(date)}
     * <p>These methods will be delegated to Spring Data JPA's query derivation.
     */
    CUSTOM;

    /**
     * Infers the method pattern from a method name.
     *
     * <p>This method uses case-insensitive prefix matching to categorize
     * repository methods. It follows Spring Data JPA naming conventions.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Normalize method name to lowercase</li>
     *   <li>Check for standard operation prefixes (save, find, delete, etc.)</li>
     *   <li>Return CUSTOM if no pattern matches</li>
     * </ol>
     *
     * @param methodName the method name to analyze
     * @return the inferred method pattern
     */
    public static MethodPattern infer(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return CUSTOM;
        }

        String normalized = methodName.toLowerCase(Locale.ROOT);

        // SAVE patterns
        if (normalized.startsWith("save")
                || normalized.startsWith("create")
                || normalized.startsWith("persist")
                || normalized.startsWith("store")
                || normalized.startsWith("add")
                || normalized.startsWith("update")
                || normalized.startsWith("upsert")) {
            return SAVE;
        }

        // FIND_BY_ID patterns
        if (normalized.matches("find(by)?id")
                || normalized.matches("get(by)?id")
                || normalized.matches("load(by)?id")
                || normalized.equals("findone")
                || normalized.equals("getone")) {
            return FIND_BY_ID;
        }

        // FIND_ALL patterns
        if (normalized.equals("findall")
                || normalized.equals("getall")
                || normalized.equals("list")
                || normalized.equals("listall")
                || normalized.equals("all")) {
            return FIND_ALL;
        }

        // DELETE patterns
        if (normalized.startsWith("delete") || normalized.startsWith("remove")) {
            return DELETE;
        }

        // EXISTS patterns
        if (normalized.startsWith("exists") || normalized.equals("contains")) {
            return EXISTS;
        }

        // COUNT patterns
        if (normalized.startsWith("count")) {
            return COUNT;
        }

        // Default to CUSTOM for any other pattern
        return CUSTOM;
    }
}
