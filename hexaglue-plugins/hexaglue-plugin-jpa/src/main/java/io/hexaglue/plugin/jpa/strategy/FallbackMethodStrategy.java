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
import com.palantir.javapoet.ParameterSpec;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import javax.lang.model.element.Modifier;

/**
 * Fallback strategy for CUSTOM method patterns.
 *
 * <p>This strategy is used as a catch-all for methods that don't match any
 * standard CRUD pattern. It generates a stub method that throws
 * {@link UnsupportedOperationException}, signaling to developers that custom
 * implementation is required.
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public List<Order> findByStatus(OrderStatus status) {
 *     throw new UnsupportedOperationException("Method not implemented: findByStatus");
 * }
 * }</pre>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Always supports: This strategy is the last resort and always returns true</li>
 *   <li>Compile-time safety: Generates valid Java code that compiles but fails at runtime</li>
 *   <li>Clear error message: Includes method name in exception for easy debugging</li>
 *   <li>Developer guidance: Signals that manual implementation or Spring Data query derivation is needed</li>
 * </ul>
 *
 * <h3>When This Strategy Is Used:</h3>
 * <p>This strategy handles CUSTOM pattern methods including:
 * <ul>
 *   <li>Spring Data query methods: {@code findByStatus}, {@code findByNameContaining}</li>
 *   <li>Complex business queries: {@code findActiveOrdersAfter}, {@code calculateTotalRevenue}</li>
 *   <li>Aggregation methods: {@code sumOrderTotals}, {@code averageOrderValue}</li>
 * </ul>
 *
 * <p><b>Future Enhancement:</b> This could be extended to detect Spring Data query
 * method patterns and generate appropriate delegation code.
 *
 * @since 2.0.0
 */
public final class FallbackMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        // Fallback strategy supports all methods (catch-all)
        return true;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Generate method body with UnsupportedOperationException
        builder.addStatement(
                "throw new $T($S)", UnsupportedOperationException.class, "Method not implemented: " + method.name());

        return builder.build();
    }
}
