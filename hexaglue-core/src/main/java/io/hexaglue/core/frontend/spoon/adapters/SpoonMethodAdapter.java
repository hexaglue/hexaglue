package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.*;
import java.util.*;
import spoon.reflect.declaration.*;

/**
 * Adapts Spoon's {@link CtMethod} to {@link JavaMethod}.
 */
public final class SpoonMethodAdapter implements JavaMethod {

    private final CtMethod<?> ctMethod;
    private final CtType<?> declaringType;

    private SpoonMethodAdapter(CtMethod<?> ctMethod, CtType<?> declaringType) {
        this.ctMethod = ctMethod;
        this.declaringType = declaringType;
    }

    public static JavaMethod adapt(CtMethod<?> ctMethod, CtType<?> declaringType) {
        return new SpoonMethodAdapter(ctMethod, declaringType);
    }

    @Override
    public String simpleName() {
        return ctMethod.getSimpleName();
    }

    @Override
    public String qualifiedName() {
        return declaringType.getQualifiedName() + "#" + signature();
    }

    private String signature() {
        StringBuilder sb = new StringBuilder(ctMethod.getSimpleName());
        sb.append("(");
        var params = ctMethod.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(SpoonTypeRefAdapter.safeQualifiedName(params.get(i).getType()));
        }
        sb.append(")");
        return sb.toString();
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
        return SpoonModifierAdapter.adapt(ctMethod.getModifiers());
    }

    @Override
    public TypeRef returnType() {
        return SpoonTypeRefAdapter.adapt(ctMethod.getType());
    }

    @Override
    public List<JavaParameter> parameters() {
        return ctMethod.getParameters().stream()
                .map(SpoonParameterAdapter::adapt)
                .toList();
    }

    @Override
    public List<TypeRef> thrownTypes() {
        var thrown = ctMethod.getThrownTypes();
        if (thrown == null || thrown.isEmpty()) {
            return List.of();
        }
        return thrown.stream().map(SpoonTypeRefAdapter::adapt).toList();
    }

    @Override
    public boolean isDefault() {
        return declaringType instanceof CtInterface<?> && ctMethod.getBody() != null;
    }

    @Override
    public List<JavaAnnotation> annotations() {
        return SpoonAnnotationAdapter.adaptAll(ctMethod.getAnnotations());
    }

    @Override
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctMethod.getPosition());
    }
}
