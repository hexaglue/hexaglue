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
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.SourceLocation;
import io.hexaglue.syntax.TypeRef;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

/**
 * Spoon implementation of {@link FieldSyntax}.
 *
 * <p>Wraps a Spoon {@link CtField} to provide a parser-agnostic representation
 * of a Java field declaration.</p>
 *
 * @since 4.0.0
 */
public final class SpoonFieldSyntax implements FieldSyntax {

    private final CtField<?> ctField;

    /**
     * Creates a new SpoonFieldSyntax wrapping the given Spoon field.
     *
     * @param ctField the Spoon field to wrap (must not be null)
     * @throws NullPointerException if ctField is null
     */
    public SpoonFieldSyntax(CtField<?> ctField) {
        this.ctField = Objects.requireNonNull(ctField, "ctField must not be null");
    }

    @Override
    public String name() {
        return ctField.getSimpleName();
    }

    @Override
    public TypeRef type() {
        return convertTypeRef(ctField.getType());
    }

    @Override
    public Set<Modifier> modifiers() {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        for (ModifierKind mod : ctField.getModifiers()) {
            convertModifier(mod).ifPresent(result::add);
        }
        return Set.copyOf(result);
    }

    @Override
    public List<AnnotationSyntax> annotations() {
        return ctField.getAnnotations().stream()
                .map(SpoonAnnotationSyntax::new)
                .map(a -> (AnnotationSyntax) a)
                .toList();
    }

    @Override
    public Optional<String> initializer() {
        var expr = ctField.getDefaultExpression();
        return expr != null ? Optional.of(expr.toString()) : Optional.empty();
    }

    @Override
    public Optional<String> documentation() {
        return JavadocCleaner.clean(ctField.getDocComment());
    }

    @Override
    public SourceLocation sourceLocation() {
        var position = ctField.getPosition();
        if (position.isValidPosition()) {
            Path filePath = position.getFile() != null ? position.getFile().toPath() : null;
            return new SourceLocation(
                    filePath, position.getLine(), position.getColumn(), position.getEndLine(), position.getEndColumn());
        }
        return SourceLocation.unknown();
    }

    // ===== Helper methods =====

    private Optional<Modifier> convertModifier(ModifierKind mod) {
        return switch (mod) {
            case PUBLIC -> Optional.of(Modifier.PUBLIC);
            case PROTECTED -> Optional.of(Modifier.PROTECTED);
            case PRIVATE -> Optional.of(Modifier.PRIVATE);
            case STATIC -> Optional.of(Modifier.STATIC);
            case FINAL -> Optional.of(Modifier.FINAL);
            case ABSTRACT -> Optional.of(Modifier.ABSTRACT);
            case NATIVE -> Optional.of(Modifier.NATIVE);
            case SYNCHRONIZED -> Optional.of(Modifier.SYNCHRONIZED);
            case TRANSIENT -> Optional.of(Modifier.TRANSIENT);
            case VOLATILE -> Optional.of(Modifier.VOLATILE);
            case STRICTFP -> Optional.of(Modifier.STRICTFP);
            case SEALED -> Optional.of(Modifier.SEALED);
            case NON_SEALED -> Optional.of(Modifier.NON_SEALED);
        };
    }

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
