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

package io.hexaglue.core.frontend.spoon;

import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.JavaType;
import io.hexaglue.core.frontend.spoon.adapters.SpoonTypeAdapter;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

/**
 * Spoon implementation of {@link JavaSemanticModel}.
 *
 * <p>Wraps a Spoon {@link CtModel} and provides access to types
 * filtered by the configured base package.
 */
final class SpoonSemanticModel implements JavaSemanticModel {

    /**
     * Known @Generated annotation qualified names.
     * Types annotated with any of these are excluded from analysis
     * to prevent HexaGlue from re-processing its own generated code.
     */
    private static final Set<String> GENERATED_ANNOTATIONS = Set.of(
            "javax.annotation.Generated",
            "javax.annotation.processing.Generated", // Java 9+
            "jakarta.annotation.Generated");

    private final CtModel model;
    private final String basePackage;

    SpoonSemanticModel(CtModel model, String basePackage) {
        this.model = model;
        this.basePackage = basePackage;
    }

    @Override
    public Stream<JavaType> types() {
        return model.getAllTypes().stream()
                .filter(this::isInScope)
                .filter(this::isNotGenerated)
                .sorted(Comparator.comparing(CtType::getQualifiedName))
                .map(SpoonTypeAdapter::adapt);
    }

    private boolean isInScope(CtType<?> type) {
        if (basePackage == null || basePackage.isBlank()) {
            return true;
        }
        String pkg = type.getPackage() == null ? "" : type.getPackage().getQualifiedName();
        return pkg.equals(basePackage) || pkg.startsWith(basePackage + ".");
    }

    /**
     * Returns true if the type is NOT annotated with @Generated.
     * This prevents HexaGlue from analyzing code it previously generated,
     * avoiding infinite loops and duplicate generation.
     */
    private boolean isNotGenerated(CtType<?> type) {
        for (CtAnnotation<?> annotation : type.getAnnotations()) {
            String annotationName = annotation.getAnnotationType().getQualifiedName();
            if (GENERATED_ANNOTATIONS.contains(annotationName)) {
                return false;
            }
        }
        return true;
    }
}
