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

package io.hexaglue.plugin.rest.strategy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.plugin.rest.model.PathVariableSpec;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;

/**
 * Shared utility methods for HTTP verb strategies.
 *
 * <p>Package-private: not part of the public API.
 *
 * @since 3.1.0
 */
final class StrategyHelper {

    private static final Set<String> COLLECTION_TYPES =
            Set.of("java.util.List", "java.util.Set", "java.util.Collection");

    private static final Set<String> BOOLEAN_TYPES = Set.of("boolean", "java.lang.Boolean");

    private static final Set<String> NUMERIC_TYPES = Set.of("int", "long", "java.lang.Integer", "java.lang.Long");

    private StrategyHelper() {
        /* prevent instantiation */
    }

    /**
     * Checks whether the first parameter of a use case is the identity type of the given aggregate.
     *
     * @param useCase   the use case
     * @param aggregate the aggregate root (nullable)
     * @return true if the first parameter type matches the aggregate's identity field type
     */
    static boolean isFirstParamIdentity(UseCase useCase, AggregateRoot aggregate) {
        if (aggregate == null) {
            return false;
        }
        List<Parameter> params = useCase.method().parameters();
        if (params.isEmpty()) {
            return false;
        }
        String paramType = params.get(0).type().qualifiedName();
        String identityType = aggregate.identityField().type().qualifiedName();
        return paramType.equals(identityType);
    }

    /**
     * Creates a {@link PathVariableSpec} for the aggregate identity.
     *
     * @param aggregate the aggregate root
     * @return the path variable spec with name "id", using the wrapped type
     */
    static PathVariableSpec identityPathVariable(AggregateRoot aggregate) {
        return new PathVariableSpec("id", "id", toTypeName(aggregate.effectiveIdentityType()), true);
    }

    /**
     * Converts a TypeRef to a JavaPoet TypeName.
     */
    static TypeName toTypeName(TypeRef typeRef) {
        return switch (typeRef.qualifiedName()) {
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "boolean" -> TypeName.BOOLEAN;
            case "double" -> TypeName.DOUBLE;
            case "float" -> TypeName.FLOAT;
            case "byte" -> TypeName.BYTE;
            case "short" -> TypeName.SHORT;
            case "char" -> TypeName.CHAR;
            case "void" -> TypeName.VOID;
            default -> {
                String qn = typeRef.qualifiedName();
                int lastDot = qn.lastIndexOf('.');
                if (lastDot < 0) {
                    yield ClassName.bestGuess(qn);
                }
                yield ClassName.get(qn.substring(0, lastDot), qn.substring(lastDot + 1));
            }
        };
    }

    /**
     * Checks whether the use case returns a collection type.
     *
     * @param useCase the use case
     * @return true if return type is List, Set, or Collection
     */
    static boolean isCollectionReturn(UseCase useCase) {
        return COLLECTION_TYPES.contains(useCase.method().returnType().qualifiedName());
    }

    /**
     * Checks whether the use case returns a boolean type.
     *
     * @param useCase the use case
     * @return true if return type is boolean or Boolean
     */
    static boolean isBooleanReturn(UseCase useCase) {
        return BOOLEAN_TYPES.contains(useCase.method().returnType().qualifiedName());
    }

    /**
     * Checks whether the use case returns a numeric type (int/long/Integer/Long).
     *
     * @param useCase the use case
     * @return true if return type is int, long, Integer, or Long
     */
    static boolean isNumericReturn(UseCase useCase) {
        return NUMERIC_TYPES.contains(useCase.method().returnType().qualifiedName());
    }

    /**
     * Checks whether the use case has a void return type.
     *
     * @param useCase the use case
     * @return true if the return type is void
     */
    static boolean isVoidReturn(UseCase useCase) {
        String returnType = useCase.method().returnType().qualifiedName();
        return "void".equals(returnType);
    }
}
