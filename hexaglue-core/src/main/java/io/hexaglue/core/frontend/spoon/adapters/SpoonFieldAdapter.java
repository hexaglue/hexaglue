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
 * Adapts Spoon's {@link CtField} to {@link JavaField}.
 */
public final class SpoonFieldAdapter implements JavaField {

    private final CtField<?> ctField;
    private final CtType<?> declaringType;

    private SpoonFieldAdapter(CtField<?> ctField, CtType<?> declaringType) {
        this.ctField = ctField;
        this.declaringType = declaringType;
    }

    public static JavaField adapt(CtField<?> ctField, CtType<?> declaringType) {
        return new SpoonFieldAdapter(ctField, declaringType);
    }

    @Override
    public String simpleName() {
        return ctField.getSimpleName();
    }

    @Override
    public String qualifiedName() {
        return declaringType.getQualifiedName() + "#" + ctField.getSimpleName();
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
        return SpoonModifierAdapter.adapt(ctField.getModifiers());
    }

    @Override
    public TypeRef type() {
        return SpoonTypeRefAdapter.adapt(ctField.getType());
    }

    @Override
    public Optional<String> initialValue() {
        var expr = ctField.getDefaultExpression();
        if (expr == null) {
            return Optional.empty();
        }
        return Optional.of(expr.toString());
    }

    @Override
    public List<JavaAnnotation> annotations() {
        return SpoonAnnotationAdapter.adaptAll(ctField.getAnnotations());
    }

    @Override
    public Optional<String> documentation() {
        String docComment = ctField.getDocComment();
        return cleanJavadoc(docComment);
    }

    @Override
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctField.getPosition());
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
