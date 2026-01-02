package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.JavaModifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import spoon.reflect.declaration.ModifierKind;

/**
 * Adapts Spoon's {@link ModifierKind} to {@link JavaModifier}.
 */
public final class SpoonModifierAdapter {

    private SpoonModifierAdapter() {}

    public static Set<JavaModifier> adapt(Set<ModifierKind> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return Set.of();
        }

        EnumSet<JavaModifier> result = EnumSet.noneOf(JavaModifier.class);
        for (ModifierKind mod : modifiers) {
            JavaModifier mapped = map(mod);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static JavaModifier map(ModifierKind mod) {
        return switch (mod) {
            case PUBLIC -> JavaModifier.PUBLIC;
            case PROTECTED -> JavaModifier.PROTECTED;
            case PRIVATE -> JavaModifier.PRIVATE;
            case STATIC -> JavaModifier.STATIC;
            case FINAL -> JavaModifier.FINAL;
            case ABSTRACT -> JavaModifier.ABSTRACT;
            case SYNCHRONIZED -> JavaModifier.SYNCHRONIZED;
            case VOLATILE -> JavaModifier.VOLATILE;
            case TRANSIENT -> JavaModifier.TRANSIENT;
            case NATIVE -> JavaModifier.NATIVE;
            case STRICTFP -> JavaModifier.STRICTFP;
            case SEALED -> JavaModifier.SEALED;
            case NON_SEALED -> JavaModifier.NON_SEALED;
            default -> null;
        };
    }
}
