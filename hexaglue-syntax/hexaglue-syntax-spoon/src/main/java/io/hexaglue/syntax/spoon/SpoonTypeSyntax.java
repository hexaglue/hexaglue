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
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.MethodSyntax;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.SourceLocation;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeParameterSyntax;
import io.hexaglue.syntax.TypeRef;
import io.hexaglue.syntax.TypeSyntax;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

/**
 * Spoon implementation of {@link TypeSyntax}.
 *
 * <p>Wraps a Spoon {@link CtType} to provide a parser-agnostic representation
 * of a Java type declaration.</p>
 *
 * @since 4.0.0
 */
public final class SpoonTypeSyntax implements TypeSyntax {

    private final CtType<?> ctType;

    /**
     * Creates a new SpoonTypeSyntax wrapping the given Spoon type.
     *
     * @param ctType the Spoon type to wrap (must not be null)
     * @throws NullPointerException if ctType is null
     */
    public SpoonTypeSyntax(CtType<?> ctType) {
        this.ctType = Objects.requireNonNull(ctType, "ctType must not be null");
    }

    @Override
    public String qualifiedName() {
        return ctType.getQualifiedName();
    }

    @Override
    public String simpleName() {
        return ctType.getSimpleName();
    }

    @Override
    public String packageName() {
        var pkg = ctType.getPackage();
        return pkg != null ? pkg.getQualifiedName() : "";
    }

    @Override
    public TypeForm form() {
        if (ctType instanceof CtRecord) {
            return TypeForm.RECORD;
        } else if (ctType instanceof CtEnum) {
            return TypeForm.ENUM;
        } else if (ctType instanceof CtAnnotationType) {
            return TypeForm.ANNOTATION;
        } else if (ctType instanceof CtInterface) {
            return TypeForm.INTERFACE;
        } else if (ctType instanceof CtClass) {
            return TypeForm.CLASS;
        }
        // Default to CLASS for unknown types
        return TypeForm.CLASS;
    }

    @Override
    public Set<Modifier> modifiers() {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        for (ModifierKind mod : ctType.getModifiers()) {
            convertModifier(mod).ifPresent(result::add);
        }
        return Set.copyOf(result);
    }

    @Override
    public Optional<TypeRef> superType() {
        if (ctType instanceof CtClass<?> ctClass) {
            CtTypeReference<?> superclass = ctClass.getSuperclass();
            if (superclass != null && !isObject(superclass)) {
                return Optional.of(convertTypeRef(superclass));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<TypeRef> interfaces() {
        Set<CtTypeReference<?>> superInterfaces = ctType.getSuperInterfaces();
        return superInterfaces.stream().map(this::convertTypeRef).toList();
    }

    @Override
    public List<TypeParameterSyntax> typeParameters() {
        List<CtTypeParameter> formalTypeParameters = ctType.getFormalCtTypeParameters();
        return formalTypeParameters.stream().map(this::convertTypeParameter).toList();
    }

    @Override
    public List<FieldSyntax> fields() {
        return ctType.getFields().stream()
                .map(SpoonFieldSyntax::new)
                .map(f -> (FieldSyntax) f)
                .toList();
    }

    @Override
    public List<MethodSyntax> methods() {
        return ctType.getMethods().stream()
                .map(SpoonMethodSyntax::new)
                .map(m -> (MethodSyntax) m)
                .toList();
    }

    @Override
    public List<ConstructorSyntax> constructors() {
        if (ctType instanceof CtClass<?> ctClass) {
            return ctClass.getConstructors().stream()
                    .map(SpoonConstructorSyntax::new)
                    .map(c -> (ConstructorSyntax) c)
                    .toList();
        }
        return List.of();
    }

    @Override
    public List<AnnotationSyntax> annotations() {
        return ctType.getAnnotations().stream()
                .map(SpoonAnnotationSyntax::new)
                .map(a -> (AnnotationSyntax) a)
                .toList();
    }

    @Override
    public Optional<String> documentation() {
        return JavadocCleaner.clean(ctType.getDocComment());
    }

    @Override
    public SourceLocation sourceLocation() {
        var position = ctType.getPosition();
        if (position.isValidPosition()) {
            Path filePath = position.getFile() != null ? position.getFile().toPath() : null;
            return new SourceLocation(
                    filePath, position.getLine(), position.getColumn(), position.getEndLine(), position.getEndColumn());
        }
        return SourceLocation.unknown();
    }

    // ===== Helper methods =====

    /**
     * Converts a Spoon modifier to our Modifier enum.
     */
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

    /**
     * Converts a Spoon type reference to our TypeRef.
     */
    private TypeRef convertTypeRef(CtTypeReference<?> ref) {
        String qualifiedName = ref.getQualifiedName();
        String simpleName = ref.getSimpleName();
        boolean isPrimitive = ref.isPrimitive();
        boolean isArray = ref.isArray();
        int arrayDimensions = 0;
        if (isArray && ref instanceof spoon.reflect.reference.CtArrayTypeReference<?> arrayRef) {
            // Count array dimensions
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

    /**
     * Converts a Spoon type parameter to our TypeParameterSyntax.
     */
    private TypeParameterSyntax convertTypeParameter(CtTypeParameter param) {
        String name = param.getSimpleName();
        List<TypeRef> bounds =
                param.getSuperclass() != null ? List.of(convertTypeRef(param.getSuperclass())) : List.of();
        return new TypeParameterSyntax(name, bounds);
    }

    /**
     * Checks if the type reference is java.lang.Object.
     */
    private boolean isObject(CtTypeReference<?> ref) {
        return "java.lang.Object".equals(ref.getQualifiedName());
    }
}
