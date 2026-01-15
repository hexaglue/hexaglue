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
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.ir.Cardinality;
import io.hexaglue.spi.ir.MethodKind;
import io.hexaglue.spi.ir.PortMethod;
import io.hexaglue.spi.ir.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Specification for a method in a JPA adapter implementation.
 *
 * <p>This record captures the signature and classification of a port method that needs
 * to be implemented in the adapter. The kind comes directly from the SPI and helps
 * determine the implementation strategy (delegate to repository, use mapper, etc.).
 *
 * <p>Design decision: The classification is done by the Core (via PortMethodClassifier)
 * and exposed through MethodKind in the SPI. The plugin consumes this classification
 * rather than re-inferring it from the method name.
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
 * @param kind the method classification from SPI (SAVE, FIND_BY_ID, etc.)
 * @param targetProperty the property targeted by property-based queries (from SPI)
 * @param returnCardinality the cardinality of the return type from SPI (SINGLE, OPTIONAL, COLLECTION)
 * @since 3.0.0
 */
public record AdapterMethodSpec(
        String name,
        TypeName returnType,
        List<ParameterInfo> parameters,
        MethodKind kind,
        Optional<String> targetProperty,
        Cardinality returnCardinality) {

    /**
     * Parameter information for method generation.
     *
     * @param name the parameter name
     * @param type the JavaPoet parameter type
     * @param isIdentity true if this parameter represents the aggregate's identity
     */
    public record ParameterInfo(String name, TypeName type, boolean isIdentity) {

        /**
         * Creates a ParameterInfo without identity flag (defaults to false).
         *
         * @param name the parameter name
         * @param type the parameter type
         * @deprecated Use the full constructor with isIdentity flag.
         */
        @Deprecated
        public ParameterInfo(String name, TypeName type) {
            this(name, type, false);
        }
    }

    /**
     * Creates an AdapterMethodSpec with inferred cardinality from the return type.
     *
     * <p>This factory method is provided for backward compatibility with tests
     * that don't have access to SPI TypeRef cardinality. It infers cardinality
     * from the string representation of the return type.
     *
     * @param name the method name
     * @param returnType the return type
     * @param parameters the parameters
     * @param kind the method kind
     * @param targetProperty the target property
     * @return an AdapterMethodSpec with inferred cardinality
     */
    public static AdapterMethodSpec of(
            String name,
            TypeName returnType,
            List<ParameterInfo> parameters,
            MethodKind kind,
            Optional<String> targetProperty) {
        Cardinality inferredCardinality = inferCardinalityFromTypeName(returnType);
        return new AdapterMethodSpec(name, returnType, parameters, kind, targetProperty, inferredCardinality);
    }

    /**
     * Infers cardinality from a JavaPoet TypeName by analyzing its string representation.
     */
    private static Cardinality inferCardinalityFromTypeName(TypeName typeName) {
        String typeString = typeName.toString();
        if (typeString.startsWith("java.util.Optional") || typeString.equals("java.util.Optional")) {
            return Cardinality.OPTIONAL;
        }
        if (typeString.startsWith("java.util.List")
                || typeString.startsWith("java.util.Set")
                || typeString.equals("java.util.List")
                || typeString.equals("java.util.Set")) {
            return Cardinality.COLLECTION;
        }
        return Cardinality.SINGLE;
    }

    /**
     * Creates an AdapterMethodSpec from a SPI PortMethod.
     *
     * <p>This factory method converts the SPI port method to the
     * JavaPoet-based generation model, consuming the classification
     * directly from the SPI rather than re-inferring it.
     *
     * @param method the port method from the SPI
     * @return an AdapterMethodSpec ready for code generation
     */
    public static AdapterMethodSpec from(PortMethod method) {
        TypeName returnType = resolveTypeName(method.returnType());
        List<ParameterInfo> parameters = method.parameters().stream()
                .map(param -> new ParameterInfo(param.name(), resolveTypeName(param.type()), param.isIdentity()))
                .collect(Collectors.toList());

        // Consume classification directly from SPI - no local re-inference
        MethodKind kind = method.kind();
        Optional<String> targetProperty = method.targetProperty();

        // Extract cardinality from SPI TypeRef for accurate Optional/Collection detection
        Cardinality returnCardinality = method.returnType().cardinality();

        return new AdapterMethodSpec(method.name(), returnType, parameters, kind, targetProperty, returnCardinality);
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
     * <p>Uses the cardinality from the SPI TypeRef for accurate detection,
     * rather than parsing the string representation of the return type.
     *
     * @return true if returnType is Optional
     */
    public boolean returnsOptional() {
        return returnCardinality == Cardinality.OPTIONAL;
    }

    /**
     * Returns true if this method returns a collection.
     *
     * <p>Uses the cardinality from the SPI TypeRef for accurate detection,
     * rather than parsing the string representation of the return type.
     *
     * @return true if returnType is List or Set
     */
    public boolean returnsCollection() {
        return returnCardinality == Cardinality.COLLECTION;
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
     * Returns true if the first parameter is an identity parameter.
     *
     * @return true if the first parameter represents the aggregate's identity
     */
    public boolean hasIdentityParameter() {
        return firstParameter().map(ParameterInfo::isIdentity).orElse(false);
    }

    /**
     * Resolves a TypeRef to a JavaPoet TypeName.
     *
     * <p>This method properly handles parameterized types like {@code Optional<Order>}
     * or {@code List<Order>} by recursively resolving type arguments and constructing
     * a {@link ParameterizedTypeName}.
     *
     * @param typeRef the type reference from SPI
     * @return the resolved JavaPoet TypeName with preserved generic parameters
     */
    private static TypeName resolveTypeName(TypeRef typeRef) {
        // Handle parameterized types properly (e.g., Optional<Order>, List<Order>)
        if (typeRef.isParameterized() && !typeRef.typeArguments().isEmpty()) {
            ClassName rawType = ClassName.bestGuess(typeRef.qualifiedName());
            TypeName[] typeArgs = typeRef.typeArguments().stream()
                    .map(AdapterMethodSpec::resolveTypeName)
                    .toArray(TypeName[]::new);
            return ParameterizedTypeName.get(rawType, typeArgs);
        }

        // Delegate to JpaModelUtils for primitives and simple types
        return JpaModelUtils.resolveTypeName(typeRef.qualifiedName());
    }
}
