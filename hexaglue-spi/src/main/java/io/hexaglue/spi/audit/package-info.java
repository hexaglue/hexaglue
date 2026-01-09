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

/**
 * Code audit and quality assurance plugin infrastructure.
 *
 * <p>This package provides the specialized API for building code audit plugins.
 * Audit plugins analyze codebases for quality, compliance, and architectural
 * issues. They apply configurable rules and produce detailed reports with
 * violations and metrics.
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Code Unit</b>: The basic element being audited (class, interface, etc.)</li>
 *   <li><b>Audit Rule</b>: A check for specific quality or compliance issues</li>
 *   <li><b>Rule Violation</b>: A detected issue with severity and location</li>
 *   <li><b>Audit Snapshot</b>: Complete audit results with violations and metrics</li>
 * </ul>
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link io.hexaglue.spi.audit.AuditPlugin} - Plugin interface for auditors</li>
 *   <li>{@link io.hexaglue.spi.audit.AuditContext} - Context with codebase and rules</li>
 *   <li>{@link io.hexaglue.spi.audit.AuditSnapshot} - Complete audit results</li>
 *   <li>{@link io.hexaglue.spi.audit.AuditRule} - Rule interface for checks</li>
 *   <li>{@link io.hexaglue.spi.audit.RuleViolation} - Detected violation</li>
 *   <li>{@link io.hexaglue.spi.audit.CodeUnit} - Code element being audited</li>
 *   <li>{@link io.hexaglue.spi.audit.Codebase} - Collection of code units</li>
 * </ul>
 *
 * <h2>Architecture Classifications</h2>
 * <ul>
 *   <li>{@link io.hexaglue.spi.audit.LayerClassification} - Architectural layer</li>
 *   <li>{@link io.hexaglue.spi.audit.RoleClassification} - Architectural role (DDD patterns)</li>
 *   <li>{@link io.hexaglue.spi.audit.DetectedArchitectureStyle} - Overall architecture style</li>
 * </ul>
 *
 * <h2>Metrics</h2>
 * <ul>
 *   <li>{@link io.hexaglue.spi.audit.CodeMetrics} - Code-level metrics (complexity, LOC)</li>
 *   <li>{@link io.hexaglue.spi.audit.QualityMetrics} - Overall quality metrics</li>
 *   <li>{@link io.hexaglue.spi.audit.ArchitectureMetrics} - Architecture metrics (coupling, cohesion)</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.spi.audit;
