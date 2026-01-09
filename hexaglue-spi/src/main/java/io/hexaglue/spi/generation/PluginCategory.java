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

package io.hexaglue.spi.generation;

/**
 * The category of a HexaGlue plugin.
 *
 * <p>Plugin categories determine the plugin's primary purpose and execution context.
 * This categorization enables better plugin organization, filtering, and targeted
 * execution (e.g., "run only generator plugins" or "run only audit plugins").
 *
 * <p>Categories:
 * <ul>
 *   <li><b>GENERATOR</b>: Generates code artifacts (JPA entities, REST controllers, etc.)</li>
 *   <li><b>AUDIT</b>: Analyzes code quality, architecture compliance, documentation</li>
 *   <li><b>ANALYSIS</b>: Performs custom analysis without generating code or auditing</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum PluginCategory {

    /**
     * Code generation plugin.
     *
     * <p>Plugins in this category generate code artifacts based on the analyzed
     * domain model. Examples include:
     * <ul>
     *   <li>JPA entity generation</li>
     *   <li>REST controller generation</li>
     *   <li>GraphQL schema generation</li>
     *   <li>Database migration scripts</li>
     * </ul>
     *
     * <p>Generator plugins typically:
     * <ol>
     *   <li>Read the classification snapshot</li>
     *   <li>Filter relevant domain types</li>
     *   <li>Generate code using templates or builders</li>
     *   <li>Write files using the ArtifactWriter</li>
     * </ol>
     */
    GENERATOR,

    /**
     * Code audit and quality assurance plugin.
     *
     * <p>Plugins in this category analyze the codebase for quality, compliance,
     * and architectural issues. Examples include:
     * <ul>
     *   <li>Hexagonal architecture compliance checking</li>
     *   <li>DDD pattern validation</li>
     *   <li>Documentation coverage analysis</li>
     *   <li>Dependency violation detection</li>
     * </ul>
     *
     * <p>Audit plugins typically:
     * <ol>
     *   <li>Analyze the codebase structure</li>
     *   <li>Check against defined rules</li>
     *   <li>Collect violations and metrics</li>
     *   <li>Generate audit reports</li>
     * </ol>
     */
    AUDIT,

    /**
     * Custom analysis plugin.
     *
     * <p>Plugins in this category perform specialized analysis that doesn't fit
     * the GENERATOR or AUDIT categories. Examples include:
     * <ul>
     *   <li>Dependency graph visualization</li>
     *   <li>Metrics collection</li>
     *   <li>Custom reporting</li>
     *   <li>Integration with external tools</li>
     * </ul>
     *
     * <p>Analysis plugins have the most flexibility and can perform any
     * custom processing of the classification data.
     */
    ANALYSIS
}
