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

package io.hexaglue.plugin.jpa.strategy;

import com.palantir.javapoet.MethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;

/**
 * Strategy interface for generating JPA adapter method implementations.
 *
 * <p>This interface follows the Strategy Pattern to encapsulate different
 * implementation approaches for various method patterns (SAVE, FIND_BY_ID, etc.).
 * Each concrete strategy knows how to generate the method body for its specific pattern.
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Strategy Pattern: Each pattern has its own isolated implementation class</li>
 *   <li>Open/Closed Principle: New patterns can be added without modifying existing code</li>
 *   <li>Single Responsibility: Each strategy only handles one specific pattern</li>
 *   <li>Context Object: AdapterContext provides all necessary generation metadata</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MethodBodyStrategy strategy = new SaveMethodStrategy();
 * if (strategy.supports(methodSpec)) {
 *     MethodSpec method = strategy.generate(methodSpec, context);
 *     // Add to class builder
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @see SaveMethodStrategy
 * @see FindByIdMethodStrategy
 * @see MethodStrategyFactory
 */
public interface MethodBodyStrategy {

    /**
     * Checks if this strategy supports the given method specification.
     *
     * <p>Each strategy examines the method's pattern to determine if it can
     * generate an appropriate implementation. The first matching strategy
     * in the chain will be used.
     *
     * @param method the method specification from the port interface
     * @return true if this strategy can handle the method, false otherwise
     */
    boolean supports(AdapterMethodSpec method);

    /**
     * Generates the complete method implementation for the adapter class.
     *
     * <p>This method creates a JavaPoet MethodSpec that includes:
     * <ul>
     *   <li>Method signature (name, return type, parameters)</li>
     *   <li>@Override annotation</li>
     *   <li>Method body with appropriate delegation to repository/mapper</li>
     * </ul>
     *
     * <p>The generated method will properly:
     * <ul>
     *   <li>Map domain objects to entities using the mapper</li>
     *   <li>Delegate to the Spring Data repository</li>
     *   <li>Map entity results back to domain objects</li>
     *   <li>Handle Optional, List, and primitive return types</li>
     * </ul>
     *
     * @param method the method specification from the port interface
     * @param context the adapter context providing type and field information
     * @return the complete JavaPoet MethodSpec ready to be added to the class
     * @throws UnsupportedOperationException if the method cannot be generated
     */
    MethodSpec generate(AdapterMethodSpec method, AdapterContext context);
}
