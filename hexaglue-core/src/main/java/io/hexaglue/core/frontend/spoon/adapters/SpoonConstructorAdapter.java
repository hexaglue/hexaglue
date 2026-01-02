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
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctConstructor.getPosition());
    }
}
