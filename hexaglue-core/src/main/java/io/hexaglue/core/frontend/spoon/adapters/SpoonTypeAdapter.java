package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.*;
import java.util.*;
import spoon.reflect.declaration.*;

/**
 * Adapts Spoon's {@link CtType} to {@link JavaType}.
 */
public final class SpoonTypeAdapter implements JavaType {

    private final CtType<?> ctType;
    private List<JavaMember> cachedMembers;

    private SpoonTypeAdapter(CtType<?> ctType) {
        this.ctType = ctType;
    }

    public static JavaType adapt(CtType<?> ctType) {
        return new SpoonTypeAdapter(ctType);
    }

    @Override
    public String simpleName() {
        return ctType.getSimpleName();
    }

    @Override
    public String qualifiedName() {
        return ctType.getQualifiedName();
    }

    @Override
    public String packageName() {
        return ctType.getPackage() == null ? "" : ctType.getPackage().getQualifiedName();
    }

    @Override
    public JavaForm form() {
        if (ctType instanceof CtInterface<?>) {
            return JavaForm.INTERFACE;
        }
        if (ctType instanceof CtEnum<?>) {
            return JavaForm.ENUM;
        }
        if (ctType instanceof CtAnnotationType<?>) {
            return JavaForm.ANNOTATION;
        }
        if (ctType instanceof CtRecord) {
            return JavaForm.RECORD;
        }
        return JavaForm.CLASS;
    }

    @Override
    public Set<JavaModifier> modifiers() {
        return SpoonModifierAdapter.adapt(ctType.getModifiers());
    }

    @Override
    public Optional<TypeRef> superType() {
        if (ctType instanceof CtClass<?> ctClass) {
            var superclass = ctClass.getSuperclass();
            if (superclass == null) {
                return Optional.empty();
            }
            String qn = SpoonTypeRefAdapter.safeQualifiedName(superclass);
            if ("java.lang.Object".equals(qn) || "java.lang.Record".equals(qn) || "java.lang.Enum".equals(qn)) {
                return Optional.empty();
            }
            return Optional.of(SpoonTypeRefAdapter.adapt(superclass));
        }
        return Optional.empty();
    }

    @Override
    public List<TypeRef> interfaces() {
        var refs = ctType.getSuperInterfaces();
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        return refs.stream().map(SpoonTypeRefAdapter::adapt).toList();
    }

    @Override
    public List<JavaMember> members() {
        if (cachedMembers == null) {
            cachedMembers = buildMembers();
        }
        return cachedMembers;
    }

    private List<JavaMember> buildMembers() {
        List<JavaMember> members = new ArrayList<>();

        // Fields
        for (CtField<?> field : ctType.getFields()) {
            members.add(SpoonFieldAdapter.adapt(field, ctType));
        }

        // Constructors (only for classes/records)
        if (ctType instanceof CtClass<?> ctClass) {
            for (CtConstructor<?> ctor : ctClass.getConstructors()) {
                members.add(SpoonConstructorAdapter.adapt(ctor, ctType));
            }
        }

        // Methods
        for (CtMethod<?> method : ctType.getMethods()) {
            members.add(SpoonMethodAdapter.adapt(method, ctType));
        }

        return Collections.unmodifiableList(members);
    }

    @Override
    public List<JavaAnnotation> annotations() {
        return SpoonAnnotationAdapter.adaptAll(ctType.getAnnotations());
    }

    @Override
    public Optional<SourceRef> sourceRef() {
        return SpoonSourceRefAdapter.adapt(ctType.getPosition());
    }
}
