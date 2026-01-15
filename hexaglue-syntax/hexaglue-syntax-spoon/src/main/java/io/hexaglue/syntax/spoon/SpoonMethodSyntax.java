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
import io.hexaglue.syntax.MethodBodySyntax;
import io.hexaglue.syntax.MethodSyntax;
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
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

/**
 * Spoon implementation of {@link MethodSyntax}.
 *
 * <p>Wraps a Spoon {@link CtMethod} to provide a parser-agnostic representation
 * of a Java method declaration.</p>
 *
 * @since 4.0.0
 */
public final class SpoonMethodSyntax implements MethodSyntax {

    private final CtMethod<?> ctMethod;

    /**
     * Creates a new SpoonMethodSyntax wrapping the given Spoon method.
     *
     * @param ctMethod the Spoon method to wrap (must not be null)
     * @throws NullPointerException if ctMethod is null
     */
    public SpoonMethodSyntax(CtMethod<?> ctMethod) {
        this.ctMethod = Objects.requireNonNull(ctMethod, "ctMethod must not be null");
    }

    @Override
    public String name() {
        return ctMethod.getSimpleName();
    }

    @Override
    public TypeRef returnType() {
        return convertTypeRef(ctMethod.getType());
    }

    @Override
    public List<ParameterSyntax> parameters() {
        return ctMethod.getParameters().stream().map(this::convertParameter).toList();
    }

    @Override
    public List<TypeRef> thrownTypes() {
        return ctMethod.getThrownTypes().stream().map(this::convertTypeRef).toList();
    }

    @Override
    public List<AnnotationSyntax> annotations() {
        return ctMethod.getAnnotations().stream()
                .map(SpoonAnnotationSyntax::new)
                .map(a -> (AnnotationSyntax) a)
                .toList();
    }

    @Override
    public Set<Modifier> modifiers() {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        for (ModifierKind mod : ctMethod.getModifiers()) {
            convertModifier(mod).ifPresent(result::add);
        }
        return Set.copyOf(result);
    }

    @Override
    public boolean isDefault() {
        return ctMethod.isDefaultMethod();
    }

    @Override
    public boolean isAbstract() {
        return ctMethod.isAbstract();
    }

    @Override
    public SourceLocation sourceLocation() {
        var position = ctMethod.getPosition();
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
        // Full implementation would analyze method body for invocations, field accesses, etc.
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
