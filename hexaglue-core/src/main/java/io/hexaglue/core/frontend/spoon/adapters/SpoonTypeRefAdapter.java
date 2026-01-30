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

import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Adapts Spoon's {@link CtTypeReference} to {@link TypeRef}.
 */
public final class SpoonTypeRefAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SpoonTypeRefAdapter.class);

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
            LOG.debug("Failed to adapt type reference, falling back to toString: {}", e.getMessage());
            return ref.toString();
        }
    }
}
