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

import com.palantir.javapoet.TypeName;

/**
 * Utility methods for JPA model spec generation.
 *
 * <p>This utility class provides common operations needed across multiple model
 * specification classes to eliminate code duplication and ensure consistency.
 *
 * <p>Design decision: Centralizing these utilities promotes DRY principle and
 * makes it easier to maintain naming conventions and type resolution logic.
 *
 * <h3>Key Capabilities:</h3>
 * <ul>
 *   <li>Package name transformation (domain → infrastructure.jpa)</li>
 *   <li>Field name conversion (camelCase → snake_case)</li>
 *   <li>Type name resolution (qualified names → JavaPoet TypeName)</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class JpaModelUtils {

    private JpaModelUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Derives the infrastructure package name from the domain package.
     *
     * <p>Convention: Replace "domain" with "infrastructure.jpa" in the package name.
     * This follows Hexagonal Architecture principles by separating domain code from
     * infrastructure adapters.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code com.example.domain} → {@code com.example.infrastructure.jpa}</li>
     *   <li>{@code com.example.order.domain} → {@code com.example.order.infrastructure.jpa}</li>
     *   <li>{@code com.example.payment} → {@code com.example.payment.infrastructure.jpa}</li>
     * </ul>
     *
     * @param domainPackage the domain package name
     * @return the infrastructure package name
     * @throws IllegalArgumentException if domainPackage is null or empty
     */
    public static String deriveInfrastructurePackage(String domainPackage) {
        if (domainPackage == null || domainPackage.isEmpty()) {
            throw new IllegalArgumentException("Domain package cannot be null or empty");
        }

        if (domainPackage.endsWith(".domain")) {
            return domainPackage.substring(0, domainPackage.length() - 7) + ".infrastructure.jpa";
        }
        // Fallback: just append .infrastructure.jpa
        return domainPackage + ".infrastructure.jpa";
    }

    /**
     * Converts a camelCase field name to snake_case column name.
     *
     * <p>This is the standard SQL naming convention for database column names.
     * The conversion handles sequences of uppercase letters correctly.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code firstName} → {@code first_name}</li>
     *   <li>{@code id} → {@code id}</li>
     *   <li>{@code totalAmount} → {@code total_amount}</li>
     *   <li>{@code XMLParser} → {@code xml_parser}</li>
     * </ul>
     *
     * @param camelCase the camelCase field name
     * @return the snake_case column name
     * @throws IllegalArgumentException if camelCase is null or empty
     */
    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }

        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Resolves a qualified type name to a JavaPoet TypeName.
     *
     * <p>Delegates to {@link io.hexaglue.plugin.jpa.util.TypeMappings#toJpaType(String)}
     * to ensure consistent type mapping across the plugin.
     *
     * <p>Handles both primitive types and class types. For class types,
     * uses {@code ClassName.bestGuess()} to handle inner classes and
     * package inference correctly.
     *
     * <p>Design decision: This method does NOT handle parameterized types
     * (generics). For generic types, use specialized methods in the calling
     * classes or parse the type parameters separately.
     *
     * @param qualifiedName the fully qualified type name
     * @return the resolved JavaPoet TypeName
     * @throws IllegalArgumentException if qualifiedName is null or empty
     * @see io.hexaglue.plugin.jpa.util.TypeMappings#toJpaType(String)
     */
    public static TypeName resolveTypeName(String qualifiedName) {
        return io.hexaglue.plugin.jpa.util.TypeMappings.toJpaType(qualifiedName);
    }
}
