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
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctField.getPosition());
    }
}
