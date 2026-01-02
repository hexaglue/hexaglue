package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.*;
import java.util.*;
import spoon.reflect.declaration.*;

/**
 * Adapts Spoon's {@link CtParameter} to {@link JavaParameter}.
 */
public final class SpoonParameterAdapter implements JavaParameter {

    private final CtParameter<?> ctParameter;

    private SpoonParameterAdapter(CtParameter<?> ctParameter) {
        this.ctParameter = ctParameter;
    }

    public static JavaParameter adapt(CtParameter<?> ctParameter) {
        return new SpoonParameterAdapter(ctParameter);
    }

    @Override
    public String name() {
        return ctParameter.getSimpleName();
    }

    @Override
    public TypeRef type() {
        return SpoonTypeRefAdapter.adapt(ctParameter.getType());
    }

    @Override
    public List<JavaAnnotation> annotations() {
        return SpoonAnnotationAdapter.adaptAll(ctParameter.getAnnotations());
    }

    @Override
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctParameter.getPosition());
    }
}
