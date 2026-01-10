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

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.MethodDeclaration;
import java.util.List;

/**
 * Calculates the ratio of boilerplate code to total code in domain types.
 *
 * <p>This calculator estimates the percentage of methods that are boilerplate
 * (getters, setters, equals, hashCode, toString, constructors, and builder patterns)
 * versus domain logic. A high boilerplate ratio may indicate opportunities for
 * using records, Lombok, or other code generation tools.
 *
 * <p><strong>Metric:</strong> code.boilerplate.ratio<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if > 50%<br>
 * <strong>Interpretation:</strong> Lower is better. High boilerplate ratio suggests
 * the domain model might benefit from modern Java features (records) or code
 * generation tools. It may also indicate an anemic domain model if most code is
 * data access rather than business logic.
 *
 * <h2>Boilerplate Detection</h2>
 * <p>Methods are classified as boilerplate if they match these patterns:
 * <ul>
 *   <li><strong>Getters:</strong> Methods starting with "get" or "is" with no parameters</li>
 *   <li><strong>Setters:</strong> Methods starting with "set" with exactly one parameter and void return</li>
 *   <li><strong>Standard methods:</strong> equals, hashCode, toString, clone, finalize</li>
 *   <li><strong>Constructors:</strong> Methods with null or empty return type</li>
 *   <li><strong>Builder pattern:</strong> Methods named "builder", "build", "with*"</li>
 * </ul>
 *
 * <h2>Scope</h2>
 * <p>This metric only analyzes domain layer types (aggregate roots, entities, value
 * objects, domain services) as boilerplate in other layers serves different purposes
 * and is often necessary.
 *
 * @since 1.0.0
 */
public class BoilerplateMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "code.boilerplate.ratio";
    private static final double WARNING_THRESHOLD = 50.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    @Override
    public Metric calculate(Codebase codebase) {
        List<CodeUnit> domainTypes = codebase.unitsInLayer(LayerClassification.DOMAIN);

        if (domainTypes.isEmpty()) {
            return Metric.of(
                    METRIC_NAME, 0.0, "%", "Percentage of boilerplate code in domain types (no domain types found)");
        }

        int totalMethods = 0;
        int boilerplateMethods = 0;

        for (CodeUnit unit : domainTypes) {
            // Skip interfaces as they don't have boilerplate implementations
            if (unit.kind() == CodeUnitKind.INTERFACE) {
                continue;
            }

            List<MethodDeclaration> methods = unit.methods();
            totalMethods += methods.size();

            for (MethodDeclaration method : methods) {
                if (isBoilerplate(method)) {
                    boilerplateMethods++;
                }
            }
        }

        if (totalMethods == 0) {
            return Metric.of(
                    METRIC_NAME, 0.0, "%", "Percentage of boilerplate code in domain types (no methods found)");
        }

        double ratio = (double) boilerplateMethods / totalMethods * 100.0;

        return Metric.of(
                METRIC_NAME,
                ratio,
                "%",
                "Percentage of boilerplate code in domain types",
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }

    /**
     * Determines if a method is boilerplate code.
     *
     * @param method the method to check
     * @return true if the method appears to be boilerplate
     */
    private boolean isBoilerplate(MethodDeclaration method) {
        String name = method.name();
        String returnType = method.returnType();
        int paramCount = method.parameterTypes().size();

        // Constructors
        if (isConstructor(method)) {
            return true;
        }

        // Getters: getName(), isActive() - no parameters
        if ((name.startsWith("get") || name.startsWith("is")) && paramCount == 0) {
            return true;
        }

        // Setters: setName(...) - one parameter, void return
        if (name.startsWith("set") && paramCount == 1 && "void".equals(returnType)) {
            return true;
        }

        // Standard Object methods
        if (isStandardObjectMethod(name)) {
            return true;
        }

        // Builder pattern methods
        if (isBuilderPattern(name)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a method is a constructor.
     *
     * <p>Constructors have no return type (null or empty string).
     *
     * @param method the method to check
     * @return true if the method is a constructor
     */
    private boolean isConstructor(MethodDeclaration method) {
        String returnType = method.returnType();
        return returnType == null || returnType.isEmpty();
    }

    /**
     * Checks if a method is a standard Object method.
     *
     * @param name the method name
     * @return true if the method is equals, hashCode, toString, clone, or finalize
     */
    private boolean isStandardObjectMethod(String name) {
        return name.equals("equals")
                || name.equals("hashCode")
                || name.equals("toString")
                || name.equals("clone")
                || name.equals("finalize");
    }

    /**
     * Checks if a method follows the builder pattern.
     *
     * @param name the method name
     * @return true if the method is a builder pattern method
     */
    private boolean isBuilderPattern(String name) {
        return name.equals("builder") || name.equals("build") || name.startsWith("with");
    }
}
