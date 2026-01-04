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
