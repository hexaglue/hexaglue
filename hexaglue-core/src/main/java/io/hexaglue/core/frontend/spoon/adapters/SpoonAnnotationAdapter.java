package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.JavaAnnotation;
import io.hexaglue.core.frontend.TypeRef;
import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Adapts Spoon's {@link CtAnnotation} to {@link JavaAnnotation}.
 */
public final class SpoonAnnotationAdapter {

    private SpoonAnnotationAdapter() {}

    public static List<JavaAnnotation> adaptAll(List<CtAnnotation<?>> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }
        return annotations.stream().map(SpoonAnnotationAdapter::adapt).toList();
    }

    @SuppressWarnings("rawtypes") // Spoon API returns raw Map<String, CtExpression>
    public static JavaAnnotation adapt(CtAnnotation<?> annotation) {
        TypeRef annotationType = SpoonTypeRefAdapter.adapt(annotation.getAnnotationType());

        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, CtExpression> rawValues = annotation.getValues();
        if (rawValues != null) {
            rawValues.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> values.put(e.getKey(), simplifyValue(e.getValue())));
        }

        return new JavaAnnotation(annotationType, Collections.unmodifiableMap(values));
    }

    private static Object simplifyValue(Object value) {
        if (value == null) {
            return null;
        }
        // Handle Spoon expression wrappers
        if (value instanceof CtLiteral<?> literal) {
            Object literalValue = literal.getValue();
            return literalValue != null ? literalValue : "null";
        }
        if (value instanceof CtFieldRead<?> fieldRead) {
            CtFieldReference<?> fieldRef = fieldRead.getVariable();
            return fieldRef != null ? fieldRef.getQualifiedName() : value.toString();
        }
        if (value instanceof CtTypeAccess<?> typeAccess) {
            CtTypeReference<?> typeRef = typeAccess.getAccessedType();
            return SpoonTypeRefAdapter.safeQualifiedName(typeRef);
        }
        if (value instanceof CtNewArray<?> newArray) {
            return newArray.getElements().stream()
                    .map(SpoonAnnotationAdapter::simplifyValue)
                    .toList();
        }
        if (value instanceof CtAnnotation<?> nestedAnnotation) {
            return adapt(nestedAnnotation).toString();
        }
        // Handle raw types (from getAllValues)
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CtTypeReference<?> typeRef) {
            return SpoonTypeRefAdapter.safeQualifiedName(typeRef);
        }
        if (value instanceof CtFieldReference<?> fieldRef) {
            return fieldRef.getQualifiedName();
        }
        if (value instanceof CtEnumValue<?> enumValue) {
            return enumValue.getSimpleName();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(SpoonAnnotationAdapter::simplifyValue).toList();
        }
        if (value instanceof Set<?> set) {
            return set.stream()
                    .map(SpoonAnnotationAdapter::simplifyValue)
                    .sorted(Comparator.comparing(Object::toString))
                    .toList();
        }
        // Fallback
        return value.toString();
    }
}
