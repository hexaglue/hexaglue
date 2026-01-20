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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.Annotation;
import io.hexaglue.arch.model.Constructor;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.ConstructorNode;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds {@link TypeStructure} from {@link TypeNode} and context.
 *
 * <p>This builder transforms the graph model representation of a type into
 * the architectural model's {@link TypeStructure}, including mapping of
 * fields with their roles, methods, constructors, and annotations.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Map {@link JavaForm} to {@link TypeNature}</li>
 *   <li>Map {@link JavaModifier} to {@link Modifier}</li>
 *   <li>Build {@link Field} instances with detected {@link FieldRole}</li>
 *   <li>Build {@link Method} and {@link Constructor} instances</li>
 *   <li>Map annotations to the architectural model</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FieldRoleDetector detector = new FieldRoleDetector();
 * TypeStructureBuilder builder = new TypeStructureBuilder(detector);
 * TypeStructure structure = builder.build(typeNode, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class TypeStructureBuilder {

    private static final Map<JavaModifier, Modifier> MODIFIER_MAP = Map.ofEntries(
            Map.entry(JavaModifier.PUBLIC, Modifier.PUBLIC),
            Map.entry(JavaModifier.PROTECTED, Modifier.PROTECTED),
            Map.entry(JavaModifier.PRIVATE, Modifier.PRIVATE),
            Map.entry(JavaModifier.STATIC, Modifier.STATIC),
            Map.entry(JavaModifier.FINAL, Modifier.FINAL),
            Map.entry(JavaModifier.ABSTRACT, Modifier.ABSTRACT),
            Map.entry(JavaModifier.SYNCHRONIZED, Modifier.SYNCHRONIZED),
            Map.entry(JavaModifier.VOLATILE, Modifier.VOLATILE),
            Map.entry(JavaModifier.TRANSIENT, Modifier.TRANSIENT),
            Map.entry(JavaModifier.NATIVE, Modifier.NATIVE),
            Map.entry(JavaModifier.STRICTFP, Modifier.STRICTFP),
            Map.entry(JavaModifier.SEALED, Modifier.SEALED),
            Map.entry(JavaModifier.NON_SEALED, Modifier.NON_SEALED),
            Map.entry(JavaModifier.DEFAULT, Modifier.DEFAULT));

    private final FieldRoleDetector fieldRoleDetector;

    /**
     * Creates a new TypeStructureBuilder.
     *
     * @param fieldRoleDetector the detector for field roles
     * @throws NullPointerException if fieldRoleDetector is null
     */
    public TypeStructureBuilder(FieldRoleDetector fieldRoleDetector) {
        this.fieldRoleDetector = Objects.requireNonNull(fieldRoleDetector, "fieldRoleDetector must not be null");
    }

    /**
     * Builds a TypeStructure from a TypeNode and context.
     *
     * @param typeNode the type node to build from
     * @param context the builder context
     * @return the built TypeStructure
     * @throws NullPointerException if typeNode or context is null
     */
    public TypeStructure build(TypeNode typeNode, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeNature nature = mapFormToNature(typeNode.form());
        Set<Modifier> modifiers = mapModifiers(typeNode.modifiers());

        List<Field> fields = context.graphQuery().fieldsOf(typeNode).stream()
                .map(fn -> buildField(fn, context))
                .toList();

        List<Method> methods = context.graphQuery().methodsOf(typeNode).stream()
                .map(this::buildMethod)
                .toList();

        List<Constructor> constructors = context.graphQuery().constructorsOf(typeNode).stream()
                .map(this::buildConstructor)
                .toList();

        List<Annotation> annotations = mapAnnotations(typeNode.annotations());

        Optional<TypeRef> superClass = typeNode.superType().map(this::mapTypeRef);

        List<TypeRef> interfaces =
                typeNode.interfaces().stream().map(this::mapTypeRef).toList();

        return TypeStructure.builder(nature)
                .modifiers(modifiers)
                .superClass(superClass.orElse(null))
                .interfaces(interfaces)
                .fields(fields)
                .methods(methods)
                .constructors(constructors)
                .annotations(annotations)
                .build();
    }

    private TypeNature mapFormToNature(JavaForm form) {
        return switch (form) {
            case CLASS -> TypeNature.CLASS;
            case INTERFACE -> TypeNature.INTERFACE;
            case RECORD -> TypeNature.RECORD;
            case ENUM -> TypeNature.ENUM;
            case ANNOTATION -> TypeNature.ANNOTATION;
        };
    }

    private Set<Modifier> mapModifiers(Set<JavaModifier> javaModifiers) {
        return javaModifiers.stream()
                .map(MODIFIER_MAP::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Field buildField(FieldNode fieldNode, BuilderContext context) {
        Set<FieldRole> roles = fieldRoleDetector.detect(fieldNode, context);
        TypeRef type = mapTypeRef(fieldNode.type());

        Optional<TypeRef> elementType = Optional.empty();
        if (fieldNode.isCollectionType() && !fieldNode.type().arguments().isEmpty()) {
            elementType = Optional.of(mapTypeRef(fieldNode.type().arguments().get(0)));
        }

        List<Annotation> annotations = mapAnnotations(fieldNode.annotations());

        return Field.builder(fieldNode.simpleName(), type)
                .modifiers(mapModifiers(fieldNode.modifiers()))
                .annotations(annotations)
                .elementType(elementType.orElse(null))
                .roles(roles)
                .build();
    }

    private Method buildMethod(MethodNode methodNode) {
        TypeRef returnType = mapTypeRef(methodNode.returnType());

        List<Parameter> parameters =
                methodNode.parameters().stream().map(this::buildParameter).toList();

        List<Annotation> annotations = mapAnnotations(methodNode.annotations());

        List<TypeRef> thrownExceptions =
                methodNode.thrownTypes().stream().map(this::mapTypeRef).toList();

        return new Method(
                methodNode.simpleName(),
                returnType,
                parameters,
                mapModifiers(methodNode.modifiers()),
                annotations,
                Optional.empty(),
                thrownExceptions);
    }

    private Constructor buildConstructor(ConstructorNode constructorNode) {
        List<Parameter> parameters =
                constructorNode.parameters().stream().map(this::buildParameter).toList();

        List<Annotation> annotations = mapAnnotations(constructorNode.annotations());

        List<TypeRef> thrownExceptions =
                constructorNode.thrownTypes().stream().map(this::mapTypeRef).toList();

        return new Constructor(
                parameters, mapModifiers(constructorNode.modifiers()), annotations, Optional.empty(), thrownExceptions);
    }

    private Parameter buildParameter(ParameterInfo paramInfo) {
        TypeRef type = mapTypeRef(paramInfo.type());
        List<Annotation> annotations = mapAnnotations(paramInfo.annotations());
        return new Parameter(paramInfo.name(), type, annotations);
    }

    private TypeRef mapTypeRef(io.hexaglue.core.frontend.TypeRef coreTypeRef) {
        List<TypeRef> typeArgs =
                coreTypeRef.arguments().stream().map(this::mapTypeRef).toList();

        return new TypeRef(
                coreTypeRef.rawQualifiedName(),
                coreTypeRef.simpleName(),
                typeArgs,
                false, // isPrimitive - we don't track this in core TypeRef
                coreTypeRef.isArray(),
                coreTypeRef.arrayDimensions());
    }

    private List<Annotation> mapAnnotations(List<AnnotationRef> annotationRefs) {
        return annotationRefs.stream().map(this::mapAnnotation).toList();
    }

    private Annotation mapAnnotation(AnnotationRef annotationRef) {
        return Annotation.of(annotationRef.qualifiedName());
    }
}
