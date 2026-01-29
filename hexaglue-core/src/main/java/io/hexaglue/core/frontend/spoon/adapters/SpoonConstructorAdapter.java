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

import io.hexaglue.core.frontend.*;
import java.util.*;
import spoon.reflect.declaration.*;

/**
 * Adapts Spoon's {@link CtConstructor} to {@link JavaConstructor}.
 */
public final class SpoonConstructorAdapter implements JavaConstructor {

    private final CtConstructor<?> ctConstructor;
    private final CtType<?> declaringType;

    private SpoonConstructorAdapter(CtConstructor<?> ctConstructor, CtType<?> declaringType) {
        this.ctConstructor = ctConstructor;
        this.declaringType = declaringType;
    }

    public static JavaConstructor adapt(CtConstructor<?> ctConstructor, CtType<?> declaringType) {
        return new SpoonConstructorAdapter(ctConstructor, declaringType);
    }

    @Override
    public String simpleName() {
        return declaringType.getSimpleName();
    }

    @Override
    public String qualifiedName() {
        return declaringType.getQualifiedName() + "#<init>(" + paramTypes() + ")";
    }

    private String paramTypes() {
        return ctConstructor.getParameters().stream()
                .map(p -> SpoonTypeRefAdapter.safeQualifiedName(p.getType()))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    @Override
    public String packageName() {
        return declaringType.getPackage() == null
                ? ""
                : declaringType.getPackage().getQualifiedName();
    }

    @Override
    public String declaringTypeQualifiedName() {
        return declaringType.getQualifiedName();
    }

    @Override
    public Set<JavaModifier> modifiers() {
        return SpoonModifierAdapter.adapt(ctConstructor.getModifiers());
    }

    @Override
    public List<JavaParameter> parameters() {
        return ctConstructor.getParameters().stream()
                .map(SpoonParameterAdapter::adapt)
                .toList();
    }

    @Override
    public List<TypeRef> thrownTypes() {
        var thrown = ctConstructor.getThrownTypes();
        if (thrown == null || thrown.isEmpty()) {
            return List.of();
        }
        return thrown.stream().map(SpoonTypeRefAdapter::adapt).toList();
    }

    @Override
    public List<JavaAnnotation> annotations() {
        return SpoonAnnotationAdapter.adaptAll(ctConstructor.getAnnotations());
    }

    @Override
    public Optional<String> documentation() {
        String docComment = ctConstructor.getDocComment();
        return cleanJavadoc(docComment);
    }

    @Override
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctConstructor.getPosition());
    }

    private static Optional<String> cleanJavadoc(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String cleaned = java.util.Arrays.stream(raw.split("\n"))
                .map(String::trim)
                .map(line -> line.startsWith("*") ? line.substring(1).trim() : line)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("@"))
                .collect(java.util.stream.Collectors.joining(" "));
        return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
    }
}
