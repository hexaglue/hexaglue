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
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.spi.ir.MethodKind;
import io.hexaglue.spi.ir.MethodParameter;
import io.hexaglue.spi.ir.PortMethod;
import io.hexaglue.spi.ir.TypeRef;
import java.util.List;
import java.util.Optional;

/**
 * Specification for a derived query method to be generated in the Spring Data JPA repository.
 *
 * <p>This record captures information needed to generate custom query methods that extend
 * beyond the basic CRUD operations provided by {@code JpaRepository}.
 *
 * <p>Examples of derived methods:
 * <ul>
 *   <li>{@code Optional<OrderEntity> findByEmail(String email)}</li>
 *   <li>{@code boolean existsByEmail(String email)}</li>
 *   <li>{@code List<OrderEntity> findAllByStatus(String status)}</li>
 *   <li>{@code long countByStatus(String status)}</li>
 * </ul>
 *
 * @param methodName the method name (e.g., "findByEmail", "existsByStatus")
 * @param returnType the JavaPoet return type
 * @param parameters the method parameters
 * @param kind the method classification from SPI
 * @since 3.0.0
 */
public record DerivedMethodSpec(
        String methodName, TypeName returnType, List<ParameterSpec> parameters, MethodKind kind) {

    public DerivedMethodSpec {
        parameters = List.copyOf(parameters);
    }

    /**
     * Parameter specification for derived methods.
     *
     * @param name the parameter name
     * @param type the JavaPoet type
     */
    public record ParameterSpec(String name, TypeName type) {}

    /**
     * Creates a DerivedMethodSpec from a SPI PortMethod.
     *
     * <p>This factory method transforms the SPI representation to the plugin's internal
     * specification, converting types to JavaPoet TypeNames for code generation.
     *
     * @param method the SPI port method
     * @param entityTypeName the entity class name to use for return types
     * @return a new DerivedMethodSpec, or null if the method should not be generated
     */
    public static DerivedMethodSpec from(PortMethod method, TypeName entityTypeName) {
        // Only generate derived methods for property-based queries
        if (!isPropertyBasedMethod(method.kind())) {
            return null;
        }

        String methodName = method.name();
        TypeName returnType = resolveReturnType(method, entityTypeName);
        List<ParameterSpec> params = method.parameters().stream()
                .map(DerivedMethodSpec::toParameterSpec)
                .toList();

        return new DerivedMethodSpec(methodName, returnType, params, method.kind());
    }

    /**
     * Creates a DerivedMethodSpec from a Method from the architectural model.
     *
     * <p>This factory method transforms the v5 representation to the plugin's internal
     * specification, inferring method kind from the method name pattern.
     *
     * <p>C4 fix: If a DomainIndex is provided, Identifier types in parameters are
     * resolved to their wrapped types (e.g., CustomerId → UUID).
     *
     * @param method the Method from the architectural model
     * @param entityTypeName the entity class name to use for return types
     * @param domainIndex optional domain index for Identifier type resolution
     * @return a new DerivedMethodSpec, or null if the method should not be generated
     * @since 5.0.0
     */
    public static DerivedMethodSpec fromV5(
            io.hexaglue.arch.model.Method method,
            TypeName entityTypeName,
            Optional<DomainIndex> domainIndex) {
        String methodName = method.name();

        // Infer method kind from name pattern
        MethodKind kind = inferMethodKind(methodName);

        // Only generate derived methods for property-based queries
        if (!isPropertyBasedMethod(kind)) {
            return null;
        }

        TypeName returnType = resolveReturnType(method, kind, entityTypeName);
        List<ParameterSpec> params = buildParameters(method, domainIndex);

        return new DerivedMethodSpec(methodName, returnType, params, kind);
    }

    /**
     * Creates a DerivedMethodSpec from a Method from the architectural model.
     *
     * @param method the Method from the architectural model
     * @param entityTypeName the entity class name to use for return types
     * @return a new DerivedMethodSpec, or null if the method should not be generated
     * @since 5.0.0
     * @deprecated Use {@link #fromV5(io.hexaglue.arch.model.Method, TypeName, Optional)} instead
     */
    @Deprecated
    public static DerivedMethodSpec fromV5(io.hexaglue.arch.model.Method method, TypeName entityTypeName) {
        return fromV5(method, entityTypeName, Optional.empty());
    }

    /**
     * Infers the MethodKind from the method name pattern.
     */
    private static MethodKind inferMethodKind(String name) {
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
        if (name.startsWith("findById") || name.equals("findById")) {
            return MethodKind.FIND_BY_ID;
        }
        if (name.equals("save")) {
            return MethodKind.SAVE;
        }
        if (name.equals("saveAll")) {
            return MethodKind.SAVE_ALL;
        }
        if (name.equals("delete") || name.equals("deleteById")) {
            return MethodKind.DELETE_BY_ID;
        }
        if (name.equals("deleteAll") || name.equals("deleteAllById")) {
            return MethodKind.DELETE_ALL;
        }
        return MethodKind.CUSTOM;
    }

    /**
     * Resolves the return type from Method.
     *
     * @since 5.0.0
     */
    private static TypeName resolveReturnType(
            io.hexaglue.arch.model.Method method, MethodKind kind, TypeName entityTypeName) {
        // Boolean for exists methods
        if (kind == MethodKind.EXISTS_BY_PROPERTY) {
            return TypeName.BOOLEAN;
        }

        // Long for count methods
        if (kind == MethodKind.COUNT_BY_PROPERTY) {
            return TypeName.LONG;
        }

        // Long for delete methods
        if (kind == MethodKind.DELETE_BY_PROPERTY) {
            return ClassName.get(Long.class);
        }

        io.hexaglue.syntax.TypeRef returnType = method.returnType();
        if (returnType == null || "void".equals(returnType.qualifiedName())) {
            return TypeName.VOID;
        }

        // Optional<Entity> for single-result finders
        if (returnType.qualifiedName().startsWith("java.util.Optional")) {
            return ParameterizedTypeName.get(ClassName.get("java.util", "Optional"), entityTypeName);
        }

        // List<Entity> for multi-result finders
        if (returnType.qualifiedName().startsWith("java.util.List")
                || returnType.qualifiedName().startsWith("java.util.Collection")) {
            return ParameterizedTypeName.get(ClassName.get("java.util", "List"), entityTypeName);
        }

        // Stream<Entity> for stream methods
        if (returnType.qualifiedName().startsWith("java.util.stream.Stream")) {
            return ParameterizedTypeName.get(ClassName.get("java.util.stream", "Stream"), entityTypeName);
        }

        // Fallback to Optional<Entity> for safety
        return ParameterizedTypeName.get(ClassName.get("java.util", "Optional"), entityTypeName);
    }

    /**
     * Builds parameters from Method.
     *
     * <p>C4 fix: If a DomainIndex is provided, Identifier types are resolved
     * to their wrapped types (e.g., CustomerId → UUID).
     *
     * @since 5.0.0
     */
    private static List<ParameterSpec> buildParameters(
            io.hexaglue.arch.model.Method method, Optional<DomainIndex> domainIndex) {
        return method.parameters().stream()
                .map(param -> new ParameterSpec(
                        param.name(), resolveTypeNameWithIdentifierUnwrap(param.type(), domainIndex)))
                .toList();
    }

    /**
     * Resolves a TypeRef to a JavaPoet TypeName, unwrapping Identifier types.
     *
     * <p>C4 fix: If the type is an Identifier (e.g., CustomerId, OrderId), returns
     * the wrapped type (e.g., UUID) instead. This ensures JpaRepository methods
     * use primitive types compatible with Spring Data.
     *
     * @param typeRef the type reference
     * @param domainIndex optional domain index for Identifier lookup
     * @return the resolved TypeName (unwrapped if Identifier)
     * @since 5.0.0
     */
    private static TypeName resolveTypeNameWithIdentifierUnwrap(
            io.hexaglue.syntax.TypeRef typeRef, Optional<DomainIndex> domainIndex) {
        // C4 fix: Check if the type is an Identifier and unwrap it
        if (domainIndex.isPresent()) {
            String qualifiedName = typeRef.qualifiedName();
            Optional<Identifier> identifierOpt = domainIndex.get().identifiers()
                    .filter(id -> id.id().qualifiedName().equals(qualifiedName))
                    .findFirst();
            if (identifierOpt.isPresent()) {
                // Use the wrapped type (e.g., UUID) instead of the Identifier type
                io.hexaglue.syntax.TypeRef wrappedType = identifierOpt.get().wrappedType();
                return resolveTypeName(wrappedType);
            }
        }
        return resolveTypeName(typeRef);
    }

    /**
     * Resolves a TypeRef (from hexaglue-syntax) to a JavaPoet TypeName.
     *
     * @since 5.0.0
     */
    private static TypeName resolveTypeName(io.hexaglue.syntax.TypeRef typeRef) {
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
                case "void" -> TypeName.VOID;
                default -> ClassName.bestGuess(typeRef.qualifiedName());
            };
        }

        // Handle parameterized types
        if (!typeRef.typeArguments().isEmpty()) {
            TypeName rawType = ClassName.bestGuess(typeRef.qualifiedName());
            TypeName[] typeArgs = typeRef.typeArguments().stream()
                    .map(DerivedMethodSpec::resolveTypeName)
                    .toArray(TypeName[]::new);
            return ParameterizedTypeName.get((ClassName) rawType, typeArgs);
        }

        // Simple class type
        return ClassName.bestGuess(typeRef.qualifiedName());
    }

    /**
     * Returns true if this method kind requires a derived method declaration.
     *
     * <p>ID-based methods (findById, existsById, etc.) are provided by JpaRepository.
     * CRUD methods (save, delete, count) are also provided by JpaRepository.
     * Only property-based methods need to be declared explicitly.
     */
    private static boolean isPropertyBasedMethod(MethodKind kind) {
        return switch (kind) {
            case FIND_BY_PROPERTY,
                    FIND_ALL_BY_PROPERTY,
                    EXISTS_BY_PROPERTY,
                    COUNT_BY_PROPERTY,
                    DELETE_BY_PROPERTY,
                    STREAM_BY_PROPERTY -> true;
            default -> false;
        };
    }

    /**
     * Resolves the JavaPoet return type from the SPI PortMethod.
     *
     * <p>Handles:
     * <ul>
     *   <li>Optional&lt;DomainType&gt; → Optional&lt;EntityType&gt;</li>
     *   <li>List&lt;DomainType&gt; → List&lt;EntityType&gt;</li>
     *   <li>boolean → boolean</li>
     *   <li>long → long</li>
     * </ul>
     */
    private static TypeName resolveReturnType(PortMethod method, TypeName entityTypeName) {
        TypeRef returnType = method.returnType();

        // Boolean for exists methods
        if (method.kind() == MethodKind.EXISTS_BY_PROPERTY) {
            return TypeName.BOOLEAN;
        }

        // Long for count methods
        if (method.kind() == MethodKind.COUNT_BY_PROPERTY) {
            return TypeName.LONG;
        }

        // Long for delete methods (nullable for safety)
        if (method.kind() == MethodKind.DELETE_BY_PROPERTY) {
            return ClassName.get(Long.class);
        }

        // Optional<Entity> for single-result finders
        if (returnType.isOptionalLike()) {
            return ParameterizedTypeName.get(ClassName.get("java.util", "Optional"), entityTypeName);
        }

        // List<Entity> for multi-result finders
        if (returnType.isCollectionLike()) {
            return ParameterizedTypeName.get(ClassName.get("java.util", "List"), entityTypeName);
        }

        // Stream<Entity> for stream methods
        if (returnType.isStreamLike()) {
            return ParameterizedTypeName.get(ClassName.get("java.util.stream", "Stream"), entityTypeName);
        }

        // Fallback to Optional<Entity> for safety
        return ParameterizedTypeName.get(ClassName.get("java.util", "Optional"), entityTypeName);
    }

    /**
     * Converts a SPI MethodParameter to a JavaPoet ParameterSpec.
     */
    private static ParameterSpec toParameterSpec(MethodParameter param) {
        TypeName typeName = resolveTypeName(param.type());
        return new ParameterSpec(param.name(), typeName);
    }

    /**
     * Resolves a SPI TypeRef to a JavaPoet TypeName.
     */
    private static TypeName resolveTypeName(TypeRef typeRef) {
        // Handle primitives
        if (typeRef.primitive()) {
            return switch (typeRef.qualifiedName()) {
                case "boolean" -> TypeName.BOOLEAN;
                case "byte" -> TypeName.BYTE;
                case "short" -> TypeName.SHORT;
                case "int" -> TypeName.INT;
                case "long" -> TypeName.LONG;
                case "float" -> TypeName.FLOAT;
                case "double" -> TypeName.DOUBLE;
                case "char" -> TypeName.CHAR;
                case "void" -> TypeName.VOID;
                default -> ClassName.bestGuess(typeRef.qualifiedName());
            };
        }

        // Handle parameterized types
        if (typeRef.isParameterized()) {
            TypeName rawType = ClassName.bestGuess(typeRef.qualifiedName());
            TypeName[] typeArgs = typeRef.typeArguments().stream()
                    .map(DerivedMethodSpec::resolveTypeName)
                    .toArray(TypeName[]::new);
            return ParameterizedTypeName.get((ClassName) rawType, typeArgs);
        }

        // Simple class type
        return ClassName.bestGuess(typeRef.qualifiedName());
    }
}
