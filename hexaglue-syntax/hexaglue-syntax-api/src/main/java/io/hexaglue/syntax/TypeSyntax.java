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

package io.hexaglue.syntax;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Syntactic representation of a type (class, interface, record, enum, annotation).
 *
 * <p>This is the main abstraction for AST representation, independent of
 * the underlying parser (Spoon, JDT, JavaParser).</p>
 *
 * @since 4.0.0
 */
public interface TypeSyntax {

    // ===== Identification =====

    /**
     * Returns the fully qualified name of this type.
     *
     * @return the qualified name (e.g., "com.example.Order")
     */
    String qualifiedName();

    /**
     * Returns the simple name of this type (without package).
     *
     * @return the simple name (e.g., "Order")
     */
    String simpleName();

    /**
     * Returns the package name of this type.
     *
     * @return the package name (e.g., "com.example")
     */
    String packageName();

    // ===== Structure =====

    /**
     * Returns the form of this type (CLASS, INTERFACE, RECORD, ENUM, ANNOTATION).
     *
     * @return the type form
     */
    TypeForm form();

    /**
     * Returns the modifiers of this type.
     *
     * @return an immutable set of modifiers
     */
    Set<Modifier> modifiers();

    /**
     * Returns the supertype, if any.
     *
     * @return an Optional containing the supertype reference
     */
    Optional<TypeRef> superType();

    /**
     * Returns the implemented interfaces.
     *
     * @return an immutable list of interface references
     */
    List<TypeRef> interfaces();

    /**
     * Returns the type parameters (generics).
     *
     * @return an immutable list of type parameters
     */
    List<TypeParameterSyntax> typeParameters();

    // ===== Members =====

    /**
     * Returns the fields declared in this type.
     *
     * @return an immutable list of fields
     */
    List<FieldSyntax> fields();

    /**
     * Returns the methods declared in this type.
     *
     * @return an immutable list of methods
     */
    List<MethodSyntax> methods();

    /**
     * Returns the constructors declared in this type.
     *
     * @return an immutable list of constructors
     */
    List<ConstructorSyntax> constructors();

    // ===== Annotations =====

    /**
     * Returns the annotations on this type.
     *
     * @return an immutable list of annotations
     */
    List<AnnotationSyntax> annotations();

    // ===== Documentation =====

    /**
     * Returns the Javadoc documentation of this type, if present.
     *
     * @return the documentation, or empty if not documented
     * @since 5.0.0
     */
    default Optional<String> documentation() {
        return Optional.empty();
    }

    // ===== Source =====

    /**
     * Returns the source location of this type.
     *
     * @return the source location
     */
    SourceLocation sourceLocation();

    // ===== Convenience methods =====

    /**
     * Returns whether this type has a specific annotation.
     *
     * @param annotationQualifiedName the qualified name of the annotation
     * @return true if the type has the annotation
     */
    default boolean hasAnnotation(String annotationQualifiedName) {
        return annotations().stream().anyMatch(a -> a.qualifiedName().equals(annotationQualifiedName));
    }

    /**
     * Returns the annotation with the given name, if present.
     *
     * @param annotationQualifiedName the qualified name of the annotation
     * @return an Optional containing the annotation
     */
    default Optional<AnnotationSyntax> getAnnotation(String annotationQualifiedName) {
        return annotations().stream()
                .filter(a -> a.qualifiedName().equals(annotationQualifiedName))
                .findFirst();
    }

    /**
     * Returns the field with the given name, if present.
     *
     * @param fieldName the field name
     * @return an Optional containing the field
     */
    default Optional<FieldSyntax> getField(String fieldName) {
        return fields().stream().filter(f -> f.name().equals(fieldName)).findFirst();
    }

    /**
     * Returns the method with the given name (first match).
     *
     * <p>For overloaded methods, use {@link #getMethod(String, String)} with signature.</p>
     *
     * @param methodName the method name
     * @return an Optional containing the method
     */
    default Optional<MethodSyntax> getMethod(String methodName) {
        return methods().stream().filter(m -> m.name().equals(methodName)).findFirst();
    }

    /**
     * Returns the method with the given name and signature.
     *
     * @param methodName the method name
     * @param signature the signature (e.g., "(String,int)")
     * @return an Optional containing the method
     */
    default Optional<MethodSyntax> getMethod(String methodName, String signature) {
        return methods().stream()
                .filter(m -> m.name().equals(methodName) && m.signature().equals(signature))
                .findFirst();
    }

    /**
     * Returns whether this type is a class (not interface, record, enum, or annotation).
     *
     * @return true if this is a class
     */
    default boolean isClass() {
        return form() == TypeForm.CLASS;
    }

    /**
     * Returns whether this type is an interface.
     *
     * @return true if this is an interface
     */
    default boolean isInterface() {
        return form() == TypeForm.INTERFACE;
    }

    /**
     * Returns whether this type is a record.
     *
     * @return true if this is a record
     */
    default boolean isRecord() {
        return form() == TypeForm.RECORD;
    }

    /**
     * Returns whether this type is an enum.
     *
     * @return true if this is an enum
     */
    default boolean isEnum() {
        return form() == TypeForm.ENUM;
    }

    /**
     * Returns whether this type is abstract.
     *
     * @return true if abstract
     */
    default boolean isAbstract() {
        return modifiers().contains(Modifier.ABSTRACT);
    }

    /**
     * Returns whether this type is final.
     *
     * @return true if final
     */
    default boolean isFinal() {
        return modifiers().contains(Modifier.FINAL);
    }
}
