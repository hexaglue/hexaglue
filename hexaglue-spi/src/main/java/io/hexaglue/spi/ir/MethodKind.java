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

package io.hexaglue.spi.ir;

/**
 * Classification of port methods according to Spring Data conventions.
 *
 * <p>Based on Spring Data JPA 4.0.1 documentation.
 *
 * @see <a href="https://docs.spring.io/spring-data/jpa/reference/repositories/query-keywords-reference.html">
 *     Spring Data Query Keywords Reference</a>
 * @since 3.0.0
 */
public enum MethodKind {

    // === CRUD de base (save) ===

    /**
     * Save a single entity (e.g., {@code save(entity)}).
     */
    SAVE,

    /**
     * Save multiple entities (e.g., {@code saveAll(entities)}).
     */
    SAVE_ALL,

    // === Recherche par ID ===

    /**
     * Find by identifier (e.g., {@code findById(id)}, {@code getById(id)}, {@code loadById(id)}).
     */
    FIND_BY_ID,

    /**
     * Find all by identifiers (e.g., {@code findAllById(ids)}).
     */
    FIND_ALL_BY_ID,

    /**
     * Check existence by identifier (e.g., {@code existsById(id)}).
     */
    EXISTS_BY_ID,

    /**
     * Delete by identifier (e.g., {@code deleteById(id)}).
     */
    DELETE_BY_ID,

    // === Recherche par propriété (dérivation Spring Data) ===

    /**
     * Find by property (e.g., {@code findByEmail(email)}).
     */
    FIND_BY_PROPERTY,

    /**
     * Find all by property (e.g., {@code findAllByStatus(status)}).
     */
    FIND_ALL_BY_PROPERTY,

    /**
     * Check existence by property (e.g., {@code existsByEmail(email)}).
     */
    EXISTS_BY_PROPERTY,

    /**
     * Count by property (e.g., {@code countByStatus(status)}).
     */
    COUNT_BY_PROPERTY,

    /**
     * Delete by property (e.g., {@code deleteByStatus(status)}).
     */
    DELETE_BY_PROPERTY,

    // === Collections complètes ===

    /**
     * Find all entities (e.g., {@code findAll()}).
     */
    FIND_ALL,

    /**
     * Count all entities (e.g., {@code count()}).
     */
    COUNT_ALL,

    /**
     * Delete all entities (e.g., {@code deleteAll()}).
     */
    DELETE_ALL,

    // === Streaming ===

    /**
     * Stream all entities (e.g., {@code streamAll()}).
     */
    STREAM_ALL,

    /**
     * Stream by property (e.g., {@code streamByStatus(status)}).
     */
    STREAM_BY_PROPERTY,

    // === Top/First (avec limite) ===

    /**
     * Find first result (e.g., {@code findFirst()}, {@code findFirstByStatus(status)}).
     */
    FIND_FIRST,

    /**
     * Find top N results (e.g., {@code findTop10ByStatus(status)}).
     */
    FIND_TOP_N,

    // === Non reconnu ===

    /**
     * Custom method - Spring Data will derive the query from method name.
     */
    CUSTOM
}
