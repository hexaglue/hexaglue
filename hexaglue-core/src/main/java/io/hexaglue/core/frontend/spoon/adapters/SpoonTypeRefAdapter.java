package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Adapts Spoon's {@link CtTypeReference} to {@link TypeRef}.
 */
public final class SpoonTypeRefAdapter {

    private SpoonTypeRefAdapter() {}

    public static TypeRef adapt(CtTypeReference<?> ref) {
        if (ref == null) {
            return TypeRef.of("java.lang.Object");
        }

        // Handle arrays
        int dims = 0;
        CtTypeReference<?> current = ref;
        while (current instanceof CtArrayTypeReference<?> arrayRef) {
            dims++;
            current = arrayRef.getComponentType();
        }

        String rawQualifiedName = safeQualifiedName(current);

        // Handle type arguments
        List<TypeRef> arguments = List.of();
        var typeArgs = current.getActualTypeArguments();
        if (typeArgs != null && !typeArgs.isEmpty()) {
            arguments = typeArgs.stream().map(SpoonTypeRefAdapter::adapt).toList();
        }

        return new TypeRef(rawQualifiedName, arguments, dims > 0, dims);
    }

    public static String safeQualifiedName(CtTypeReference<?> ref) {
        if (ref == null) {
            return "java.lang.Object";
        }
        try {
            String qn = ref.getQualifiedName();
            if (qn == null || qn.isBlank()) {
                // Type variable or unresolved
                return ref.toString();
            }
            return qn;
        } catch (Exception e) {
            // Fallback for unresolved types
            return ref.toString();
        }
    }
}
