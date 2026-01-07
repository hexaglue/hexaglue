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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.ir.PortMethod;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Specification for a method in a JPA adapter implementation.
 *
 * <p>This record captures the signature and pattern of a port method that needs
 * to be implemented in the adapter. The pattern helps determine the implementation
 * strategy (delegate to repository, use mapper, etc.).
 *
 * <p>Design decision: Separating method specification from adapter specification
 * allows for clearer responsibility boundaries and easier testing of individual
 * method generation logic.
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * @Override
 * public Order save(Order order) {
 *     OrderEntity entity = mapper.toEntity(order);
 *     OrderEntity saved = repository.save(entity);
 *     return mapper.toDomain(saved);
 * }
 * }</pre>
 *
 * @param name the method name from the port interface
 * @param returnType the JavaPoet return type
 * @param parameters the list of parameter information
 * @param pattern the inferred method pattern (SAVE, FIND_BY_ID, etc.)
 * @since 2.0.0
 */
public record AdapterMethodSpec(
        String name, TypeName returnType, List<ParameterInfo> parameters, MethodPattern pattern) {

    /**
     * Parameter information for method generation.
     *
     * @param name the parameter name
     * @param type the JavaPoet parameter type
     */
    public record ParameterInfo(String name, TypeName type) {}

    /**
     * Creates an AdapterMethodSpec from a SPI PortMethod.
     *
     * <p>This factory method converts the SPI port method signature to the
     * JavaPoet-based generation model and infers the method pattern.
     *
     * <p>Return type handling:
     * <ul>
     *   <li>Wraps collection types (List, Set) appropriately</li>
     *   <li>Handles Optional return types</li>
     *   <li>Resolves qualified class names to TypeName</li>
     * </ul>
     *
     * @param method the port method from the SPI
     * @return an AdapterMethodSpec ready for code generation
     */
    public static AdapterMethodSpec from(PortMethod method) {
        TypeName returnType = resolveTypeName(method.returnType());
        List<ParameterInfo> parameters = method.parameters().stream()
                .map(paramType -> new ParameterInfo(deriveParameterName(paramType), resolveTypeName(paramType)))
                .collect(Collectors.toList());

        MethodPattern pattern = MethodPattern.infer(method.name());

        return new AdapterMethodSpec(method.name(), returnType, parameters, pattern);
    }

    /**
     * Returns true if this method has a return value (not void).
     *
     * @return true if returnType is not VOID
     */
    public boolean hasReturnValue() {
        return !returnType.equals(TypeName.VOID);
    }

    /**
     * Returns true if this method returns an Optional.
     *
     * @return true if returnType is Optional
     */
    public boolean returnsOptional() {
        // Check if the return type string representation contains Optional
        String typeString = returnType.toString();
        return typeString.startsWith("java.util.Optional<") || typeString.equals("java.util.Optional");
    }

    /**
     * Returns true if this method returns a collection.
     *
     * @return true if returnType is List or Set
     */
    public boolean returnsCollection() {
        // Check if the return type string representation contains collection types
        String typeString = returnType.toString();
        return typeString.startsWith("java.util.List<")
                || typeString.startsWith("java.util.Set<")
                || typeString.equals("java.util.List")
                || typeString.equals("java.util.Set");
    }

    /**
     * Returns true if this method has parameters.
     *
     * @return true if parameters list is not empty
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Returns the first parameter, if present.
     *
     * @return Optional containing the first parameter, or empty if no parameters
     */
    public Optional<ParameterInfo> firstParameter() {
        return parameters.isEmpty() ? Optional.empty() : Optional.of(parameters.get(0));
    }

    /**
     * Resolves a qualified type name to a JavaPoet TypeName.
     *
     * <p>Handles:
     * <ul>
     *   <li>Primitive types (delegated to {@link JpaModelUtils#resolveTypeName})</li>
     *   <li>Void type (delegated to {@link JpaModelUtils#resolveTypeName})</li>
     *   <li>Parameterized types (Optional, List, etc.) - specialized handling</li>
     *   <li>Class types (delegated to {@link JpaModelUtils#resolveTypeName})</li>
     * </ul>
     *
     * @param qualifiedName the fully qualified type name
     * @return the resolved JavaPoet TypeName
     */
    private static TypeName resolveTypeName(String qualifiedName) {
        // Handle parameterized types (basic support)
        // TODO: Enhance for complex generic signatures
        if (qualifiedName.contains("<")) {
            // Simple heuristic: extract raw type and use ClassName.bestGuess
            // For full generic support, would need proper parsing
            int genericStart = qualifiedName.indexOf('<');
            String rawType = qualifiedName.substring(0, genericStart);
            return ClassName.bestGuess(rawType);
        }

        // Delegate to JpaModelUtils for primitives and simple types
        return JpaModelUtils.resolveTypeName(qualifiedName);
    }

    /**
     * Derives a parameter name from a type name.
     *
     * <p>Convention: Use lowercase simple name of the type.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code com.example.Order} → {@code order}</li>
     *   <li>{@code java.util.UUID} → {@code uuid}</li>
     * </ul>
     *
     * @param qualifiedTypeName the fully qualified type name
     * @return the derived parameter name
     */
    private static String deriveParameterName(String qualifiedTypeName) {
        // Extract simple name
        int lastDot = qualifiedTypeName.lastIndexOf('.');
        String simpleName = lastDot < 0 ? qualifiedTypeName : qualifiedTypeName.substring(lastDot + 1);

        // Remove generic parameters if present
        int genericStart = simpleName.indexOf('<');
        if (genericStart > 0) {
            simpleName = simpleName.substring(0, genericStart);
        }

        // Convert to camelCase (lowercase first letter)
        if (!simpleName.isEmpty()) {
            return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        }

        return "param";
    }
}
