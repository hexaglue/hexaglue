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

package io.hexaglue.core.style;

/**
 * Detected architecture style.
 *
 * <p>The architecture style influences how HexaGlue interprets package structures,
 * classifies components, and generates adapters.
 *
 * <p>Supported styles:
 * <ul>
 *   <li><b>DDD_HEXAGONAL</b>: Hexagonal Architecture with DDD patterns
 *       (domain/, ports/, adapters/)</li>
 *   <li><b>DDD_ONION</b>: Onion Architecture with DDD patterns
 *       (core/, application/, infrastructure/)</li>
 *   <li><b>LAYERED_TRADITIONAL</b>: Traditional layered architecture
 *       (controller/, service/, repository/, model/)</li>
 *   <li><b>CLEAN_ARCHITECTURE</b>: Clean Architecture by Robert C. Martin
 *       (entities/, usecases/, gateways/, frameworks/)</li>
 *   <li><b>MODULAR_MONOLITH</b>: Modular monolith with bounded contexts
 *       (contextA/, contextB/, shared/)</li>
 *   <li><b>UNKNOWN</b>: No recognizable architecture style detected</li>
 * </ul>
 */
public enum ArchitectureStyle {

    /**
     * Hexagonal Architecture with DDD patterns.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Package structure: domain/, ports/, adapters/</li>
     *   <li>Ports often split into ports.in/ and ports.out/</li>
     *   <li>Clear separation between core domain and infrastructure</li>
     *   <li>Dependency rule: dependencies point inward</li>
     * </ul>
     */
    DDD_HEXAGONAL("DDD Hexagonal Architecture"),

    /**
     * Onion Architecture with DDD patterns.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Package structure: core/, application/, infrastructure/</li>
     *   <li>Domain model at the center</li>
     *   <li>Application services orchestrate domain logic</li>
     *   <li>Infrastructure at the outermost layer</li>
     * </ul>
     */
    DDD_ONION("DDD Onion Architecture"),

    /**
     * Traditional layered architecture.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Package structure: controller/, service/, repository/, model/</li>
     *   <li>Horizontal layers (presentation, business, data)</li>
     *   <li>Dependencies flow downward through layers</li>
     *   <li>Less emphasis on domain modeling</li>
     * </ul>
     */
    LAYERED_TRADITIONAL("Traditional Layered Architecture"),

    /**
     * Clean Architecture by Robert C. Martin.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Package structure: entities/, usecases/, gateways/, frameworks/</li>
     *   <li>Entities contain enterprise business rules</li>
     *   <li>Use cases contain application business rules</li>
     *   <li>Dependencies point toward entities</li>
     * </ul>
     */
    CLEAN_ARCHITECTURE("Clean Architecture"),

    /**
     * Modular monolith with bounded contexts.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Top-level packages represent bounded contexts</li>
     *   <li>Each context has its own architecture (often hexagonal or layered)</li>
     *   <li>Contexts communicate through well-defined interfaces</li>
     *   <li>Shared kernel in a common package</li>
     * </ul>
     */
    MODULAR_MONOLITH("Modular Monolith"),

    /**
     * No recognizable architecture style detected.
     *
     * <p>HexaGlue will use conservative defaults and basic heuristics
     * when this style is detected.
     */
    UNKNOWN("Unknown Architecture");

    private final String description;

    ArchitectureStyle(String description) {
        this.description = description;
    }

    /**
     * Returns a human-readable description of this architecture style.
     */
    public String description() {
        return description;
    }

    /**
     * Returns true if this is a known architecture style.
     */
    public boolean isKnown() {
        return this != UNKNOWN;
    }

    /**
     * Returns true if this style emphasizes DDD patterns.
     */
    public boolean isDddBased() {
        return this == DDD_HEXAGONAL || this == DDD_ONION;
    }

    /**
     * Returns true if this style uses hexagonal/ports-and-adapters patterns.
     */
    public boolean isHexagonal() {
        return this == DDD_HEXAGONAL;
    }
}
