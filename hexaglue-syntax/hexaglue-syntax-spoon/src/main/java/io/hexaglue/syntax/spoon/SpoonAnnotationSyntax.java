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

package io.hexaglue.syntax.spoon;

import io.hexaglue.syntax.AnnotationSyntax;
import io.hexaglue.syntax.AnnotationValue;
import io.hexaglue.syntax.TypeRef;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.reference.CtTypeReference;

/**
 * Spoon implementation of {@link AnnotationSyntax}.
 *
 * <p>Wraps a Spoon {@link CtAnnotation} to provide a parser-agnostic representation
 * of a Java annotation with ALL its values preserved.</p>
 *
 * @since 4.0.0
 */
public final class SpoonAnnotationSyntax implements AnnotationSyntax {

    private final CtAnnotation<?> ctAnnotation;

    /**
     * Creates a new SpoonAnnotationSyntax wrapping the given Spoon annotation.
     *
     * @param ctAnnotation the Spoon annotation to wrap (must not be null)
     * @throws NullPointerException if ctAnnotation is null
     */
    public SpoonAnnotationSyntax(CtAnnotation<?> ctAnnotation) {
        this.ctAnnotation = Objects.requireNonNull(ctAnnotation, "ctAnnotation must not be null");
    }

    @Override
    public String qualifiedName() {
        var type = ctAnnotation.getAnnotationType();
        return type != null ? type.getQualifiedName() : "";
    }

    @Override
    public String simpleName() {
        var type = ctAnnotation.getAnnotationType();
        return type != null ? type.getSimpleName() : "";
    }

    @Override
    @SuppressWarnings("unchecked") // Spoon returns raw Map<String, CtExpression>
    public Map<String, AnnotationValue> values() {
        Map<String, AnnotationValue> result = new HashMap<>();
        Map<String, CtExpression<?>> spoonValues = (Map<String, CtExpression<?>>) (Map<?, ?>) ctAnnotation.getValues();

        for (Map.Entry<String, CtExpression<?>> entry : spoonValues.entrySet()) {
            convertExpression(entry.getValue()).ifPresent(converted -> result.put(entry.getKey(), converted));
        }

        return Map.copyOf(result);
    }

    // ===== Helper methods =====

    /**
     * Converts a Spoon expression to our AnnotationValue.
     *
     * @param expr the Spoon expression to convert
     * @return an Optional containing the converted AnnotationValue, or empty if the expression is null
     */
    private Optional<AnnotationValue> convertExpression(CtExpression<?> expr) {
        if (expr == null) {
            return Optional.empty();
        }

        // String literal
        if (expr instanceof CtLiteral<?> literal) {
            Object value = literal.getValue();
            if (value instanceof String stringValue) {
                return Optional.of(AnnotationValue.ofString(stringValue));
            }
            // Other primitives (int, boolean, etc.)
            return Optional.of(AnnotationValue.ofPrimitive(value));
        }

        // Enum value
        if (expr instanceof CtFieldRead<?> fieldRead) {
            var targetType = fieldRead.getTarget();
            if (targetType instanceof CtTypeAccess<?> typeAccess) {
                String enumType = typeAccess.getAccessedType().getQualifiedName();
                String constantName = fieldRead.getVariable().getSimpleName();
                return Optional.of(AnnotationValue.ofEnum(enumType, constantName));
            }
        }

        // Class reference
        if (expr instanceof CtTypeAccess<?> typeAccess) {
            TypeRef typeRef = convertTypeRef(typeAccess.getAccessedType());
            return Optional.of(AnnotationValue.ofClass(typeRef));
        }

        // Nested annotation
        if (expr instanceof CtAnnotation<?> nestedAnnotation) {
            return Optional.of(AnnotationValue.ofAnnotation(new SpoonAnnotationSyntax(nestedAnnotation)));
        }

        // Array
        if (expr instanceof CtNewArray<?> newArray) {
            List<AnnotationValue> elements = newArray.getElements().stream()
                    .map(this::convertExpression)
                    .flatMap(Optional::stream)
                    .toList();
            return Optional.of(AnnotationValue.ofArray(elements));
        }

        // Default: try to convert as a literal string representation
        return Optional.of(AnnotationValue.ofString(expr.toString()));
    }

    /**
     * Converts a Spoon type reference to our TypeRef.
     */
    private TypeRef convertTypeRef(CtTypeReference<?> ref) {
        String qualifiedName = ref.getQualifiedName();
        String simpleName = ref.getSimpleName();
        boolean isPrimitive = ref.isPrimitive();
        boolean isArray = ref.isArray();
        int arrayDimensions = 0;
        if (isArray && ref instanceof spoon.reflect.reference.CtArrayTypeReference<?> arrayRef) {
            CtTypeReference<?> component = arrayRef;
            while (component instanceof spoon.reflect.reference.CtArrayTypeReference<?> arr) {
                arrayDimensions++;
                component = arr.getArrayType();
            }
        }

        List<TypeRef> typeArguments =
                ref.getActualTypeArguments().stream().map(this::convertTypeRef).toList();

        return new TypeRef(qualifiedName, simpleName, typeArguments, isPrimitive, isArray, arrayDimensions);
    }
}
