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
import io.hexaglue.syntax.ConstructorSyntax;
import io.hexaglue.syntax.MethodBodySyntax;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.ParameterSyntax;
import io.hexaglue.syntax.SourceLocation;
import io.hexaglue.syntax.TypeRef;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

/**
 * Spoon implementation of {@link ConstructorSyntax}.
 *
 * <p>Wraps a Spoon {@link CtConstructor} to provide a parser-agnostic representation
 * of a Java constructor declaration.</p>
 *
 * @since 4.0.0
 */
public final class SpoonConstructorSyntax implements ConstructorSyntax {

    private final CtConstructor<?> ctConstructor;

    /**
     * Creates a new SpoonConstructorSyntax wrapping the given Spoon constructor.
     *
     * @param ctConstructor the Spoon constructor to wrap (must not be null)
     * @throws NullPointerException if ctConstructor is null
     */
    public SpoonConstructorSyntax(CtConstructor<?> ctConstructor) {
        this.ctConstructor = Objects.requireNonNull(ctConstructor, "ctConstructor must not be null");
    }

    @Override
    public List<ParameterSyntax> parameters() {
        return ctConstructor.getParameters().stream()
                .map(this::convertParameter)
                .toList();
    }

    @Override
    public List<TypeRef> thrownTypes() {
        return ctConstructor.getThrownTypes().stream().map(this::convertTypeRef).toList();
    }

    @Override
    public List<AnnotationSyntax> annotations() {
        return ctConstructor.getAnnotations().stream()
                .map(SpoonAnnotationSyntax::new)
                .map(a -> (AnnotationSyntax) a)
                .toList();
    }

    @Override
    public Set<Modifier> modifiers() {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        for (ModifierKind mod : ctConstructor.getModifiers()) {
            convertModifier(mod).ifPresent(result::add);
        }
        return Set.copyOf(result);
    }

    @Override
    public Optional<String> documentation() {
        return JavadocCleaner.clean(ctConstructor.getDocComment());
    }

    @Override
    public SourceLocation sourceLocation() {
        var position = ctConstructor.getPosition();
        if (position.isValidPosition()) {
            Path filePath = position.getFile() != null ? position.getFile().toPath() : null;
            return new SourceLocation(
                    filePath, position.getLine(), position.getColumn(), position.getEndLine(), position.getEndColumn());
        }
        return SourceLocation.unknown();
    }

    @Override
    public Optional<MethodBodySyntax> body() {
        // Body analysis is deferred - returning empty for now
        return Optional.empty();
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

    private ParameterSyntax convertParameter(CtParameter<?> param) {
        String name = param.getSimpleName();
        TypeRef type = convertTypeRef(param.getType());
        List<AnnotationSyntax> annotations = param.getAnnotations().stream()
                .map(SpoonAnnotationSyntax::new)
                .map(a -> (AnnotationSyntax) a)
                .toList();
        boolean isVarArgs = param.isVarArgs();
        return new ParameterSyntax(name, type, annotations, isVarArgs);
    }
}
