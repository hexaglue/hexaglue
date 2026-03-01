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
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.ir.Cardinality;
import io.hexaglue.arch.model.ir.MethodKind;
import io.hexaglue.arch.model.ir.PortMethod;
import io.hexaglue.arch.model.ir.TypeRef;
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

    public AdapterMethodSpec {
        parameters = List.copyOf(parameters);
    }

    /**
     * Parameter information for method generation.
     *
     * @param name the parameter name
     * @param type the JavaPoet parameter type
     * @param isIdentity true if this parameter represents the aggregate's identity
     * @param isSingleValueVO true if this parameter is a single-value record Value Object
     *                        that needs unwrapping via mapper.map() before passing to the JPA repository
     * @since 2.0.0
     */
    public record ParameterInfo(String name, TypeName type, boolean isIdentity, boolean isSingleValueVO) {

        /**
         * Backward-compatible constructor for parameters that are not single-value VOs.
         *
         * @param name the parameter name
         * @param type the JavaPoet parameter type
         * @param isIdentity true if this parameter represents the aggregate's identity
         */
        public ParameterInfo(String name, TypeName type, boolean isIdentity) {
            this(name, type, isIdentity, false);
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
     * Creates an AdapterMethodSpec from a Method from the architectural model.
     *
     * <p>This factory method converts the model method to the
     * JavaPoet-based generation model, inferring the method kind from
     * the method name pattern.
     *
     * <p>Issue 8 fix: If a DomainIndex is provided, single-value record Value Object
     * parameters are flagged so adapter strategies can unwrap them via mapper.map().
     *
     * @param method the method from the architectural model
     * @param domainIndex optional domain index for single-value VO detection
     * @return an AdapterMethodSpec ready for code generation
     * @since 2.0.0
     */
    public static AdapterMethodSpec fromV5(io.hexaglue.arch.model.Method method, Optional<DomainIndex> domainIndex) {
        TypeName returnType = resolveTypeNameFromSyntax(method.returnType());
        List<ParameterInfo> parameters = method.parameters().stream()
                .map(param -> new ParameterInfo(
                        param.name(),
                        resolveTypeNameFromSyntax(param.type()),
                        isIdentityParameter(param.name()),
                        isSingleValueVOParameter(param.type(), domainIndex)))
                .collect(Collectors.toList());

        MethodKind kind = inferMethodKind(method.name());
        Optional<String> targetProperty = extractTargetProperty(method.name(), kind);
        Cardinality returnCardinality = inferCardinalityFromTypeName(returnType);

        return new AdapterMethodSpec(method.name(), returnType, parameters, kind, targetProperty, returnCardinality);
    }

    /**
     * Creates an AdapterMethodSpec from a Method from the architectural model.
     *
     * <p>This factory method converts the model method to the
     * JavaPoet-based generation model, inferring the method kind from
     * the method name pattern.
     *
     * @param method the method from the architectural model
     * @return an AdapterMethodSpec ready for code generation
     * @since 5.0.0
     */
    public static AdapterMethodSpec fromV5(io.hexaglue.arch.model.Method method) {
        TypeName returnType = resolveTypeNameFromSyntax(method.returnType());
        List<ParameterInfo> parameters = method.parameters().stream()
                .map(param -> new ParameterInfo(
                        param.name(), resolveTypeNameFromSyntax(param.type()), isIdentityParameter(param.name())))
                .collect(Collectors.toList());

        // Infer method kind from name pattern
        MethodKind kind = inferMethodKind(method.name());
        Optional<String> targetProperty = extractTargetProperty(method.name(), kind);

        // Infer cardinality from return type
        Cardinality returnCardinality = inferCardinalityFromTypeName(returnType);

        return new AdapterMethodSpec(method.name(), returnType, parameters, kind, targetProperty, returnCardinality);
    }

    /**
     * Infers the MethodKind from the method name pattern.
     *
     * @param name the method name
     * @return the inferred MethodKind
     */
    private static MethodKind inferMethodKind(String name) {
        if (name.equals("save")) {
            return MethodKind.SAVE;
        }
        if (name.equals("saveAll")) {
            return MethodKind.SAVE_ALL;
        }
        // C1 fix: recognize common naming conventions for find-by-id operations
        if (name.equals("findById") || name.equals("getById") || name.equals("loadById") || name.equals("fetchById")) {
            return MethodKind.FIND_BY_ID;
        }
        if (name.equals("findAll")) {
            return MethodKind.FIND_ALL;
        }
        // Recognize findAll<Property> (no "By") as implicit boolean query
        // e.g., "findAllActive" → FIND_ALL_BY_PROPERTY with targetProperty "active"
        if (name.startsWith("findAll") && name.length() > "findAll".length() && !name.startsWith("findAllBy")) {
            return MethodKind.FIND_ALL_BY_PROPERTY;
        }
        if (name.equals("existsById")) {
            return MethodKind.EXISTS_BY_ID;
        }
        if (name.equals("count") || name.equals("countAll")) {
            return MethodKind.COUNT_ALL;
        }
        if (name.equals("delete") || name.equals("deleteById")) {
            return MethodKind.DELETE_BY_ID;
        }
        if (name.equals("deleteAll") || name.equals("deleteAllById")) {
            return MethodKind.DELETE_ALL;
        }
        if (name.startsWith("findBy") && !name.startsWith("findAllBy")) {
            return MethodKind.FIND_BY_PROPERTY;
        }
        if (name.startsWith("findAllBy")) {
            return MethodKind.FIND_ALL_BY_PROPERTY;
        }
        if (name.startsWith("existsBy")) {
            return MethodKind.EXISTS_BY_PROPERTY;
        }
        if (name.startsWith("countBy")) {
            return MethodKind.COUNT_BY_PROPERTY;
        }
        if (name.startsWith("deleteBy")) {
            return MethodKind.DELETE_BY_PROPERTY;
        }
        if (name.startsWith("streamBy")) {
            return MethodKind.STREAM_BY_PROPERTY;
        }
        return MethodKind.CUSTOM;
    }

    /**
     * Extracts the target property from the method name for property-based methods.
     *
     * @param name the method name
     * @param kind the method kind
     * @return the target property name, or empty if not applicable
     */
    private static Optional<String> extractTargetProperty(String name, MethodKind kind) {
        return switch (kind) {
            case FIND_BY_PROPERTY -> extractPropertyFromPrefix(name, "findBy");
            case FIND_ALL_BY_PROPERTY -> {
                // Try "findAllBy" first (e.g., "findAllByStatus"), then "findAll" (e.g., "findAllActive")
                Optional<String> result = extractPropertyFromPrefix(name, "findAllBy");
                yield result.isPresent() ? result : extractPropertyFromPrefix(name, "findAll");
            }
            case EXISTS_BY_PROPERTY -> extractPropertyFromPrefix(name, "existsBy");
            case COUNT_BY_PROPERTY -> extractPropertyFromPrefix(name, "countBy");
            case DELETE_BY_PROPERTY -> extractPropertyFromPrefix(name, "deleteBy");
            case STREAM_BY_PROPERTY -> extractPropertyFromPrefix(name, "streamBy");
            default -> Optional.empty();
        };
    }

    private static Optional<String> extractPropertyFromPrefix(String name, String prefix) {
        if (name.startsWith(prefix) && name.length() > prefix.length()) {
            String property = name.substring(prefix.length());
            // Convert first char to lowercase (e.g., "Email" -> "email")
            return Optional.of(Character.toLowerCase(property.charAt(0)) + property.substring(1));
        }
        return Optional.empty();
    }

    /**
     * Heuristic to detect identity parameters (e.g., "id", "orderId").
     */
    private static boolean isIdentityParameter(String name) {
        return "id".equals(name) || name.endsWith("Id");
    }

    /**
     * Checks if a parameter type is a single-value record Value Object.
     *
     * <p>Issue 8 fix: Single-value record VOs (like {@code Email} wrapping {@code String})
     * need unwrapping via mapper.map() in adapter strategies, because the JPA repository
     * uses the unwrapped primitive type.
     *
     * @param typeRef the parameter type reference
     * @param domainIndex optional domain index for VO lookup
     * @return true if the type is a single-value record VO
     * @since 2.0.0
     */
    private static boolean isSingleValueVOParameter(
            io.hexaglue.syntax.TypeRef typeRef, Optional<DomainIndex> domainIndex) {
        if (domainIndex.isEmpty()) {
            return false;
        }
        String qualifiedName = typeRef.qualifiedName();
        return domainIndex
                .get()
                .valueObjects()
                .filter(vo -> vo.id().qualifiedName().equals(qualifiedName))
                .anyMatch(vo -> vo.isSingleValue() && vo.structure().isRecord());
    }

    /**
     * Resolves a TypeRef (from hexaglue-syntax) to a JavaPoet TypeName.
     *
     * @since 5.0.0
     */
    private static TypeName resolveTypeNameFromSyntax(io.hexaglue.syntax.TypeRef typeRef) {
        if (typeRef == null) {
            return TypeName.VOID;
        }

        // Handle void
        if ("void".equals(typeRef.qualifiedName())) {
            return TypeName.VOID;
        }

        // Handle primitives
        if (typeRef.isPrimitive()) {
            return switch (typeRef.qualifiedName()) {
                case "boolean" -> TypeName.BOOLEAN;
                case "byte" -> TypeName.BYTE;
                case "short" -> TypeName.SHORT;
                case "int" -> TypeName.INT;
                case "long" -> TypeName.LONG;
                case "float" -> TypeName.FLOAT;
                case "double" -> TypeName.DOUBLE;
                case "char" -> TypeName.CHAR;
                default -> ClassName.bestGuess(typeRef.qualifiedName());
            };
        }

        // Handle parameterized types
        if (!typeRef.typeArguments().isEmpty()) {
            ClassName rawType = ClassName.bestGuess(typeRef.qualifiedName());
            TypeName[] typeArgs = typeRef.typeArguments().stream()
                    .map(AdapterMethodSpec::resolveTypeNameFromSyntax)
                    .toArray(TypeName[]::new);
            return ParameterizedTypeName.get(rawType, typeArgs);
        }

        // Simple class type
        return ClassName.bestGuess(typeRef.qualifiedName());
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
