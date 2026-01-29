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
 * Syntactic representation of a method.
 *
 * @since 4.0.0
 */
public interface MethodSyntax {

    /**
     * Returns the method name.
     *
     * @return the method name
     */
    String name();

    /**
     * Returns the return type.
     *
     * @return the return type reference
     */
    TypeRef returnType();

    /**
     * Returns the method parameters.
     *
     * @return an immutable list of parameters
     */
    List<ParameterSyntax> parameters();

    /**
     * Returns the types thrown by this method.
     *
     * @return an immutable list of thrown types
     */
    List<TypeRef> thrownTypes();

    /**
     * Returns the annotations on this method.
     *
     * @return an immutable list of annotations
     */
    List<AnnotationSyntax> annotations();

    /**
     * Returns the method modifiers.
     *
     * @return an immutable set of modifiers
     */
    Set<Modifier> modifiers();

    /**
     * Returns whether this is a default method (in an interface).
     *
     * @return true if default
     */
    boolean isDefault();

    /**
     * Returns whether this method is abstract.
     *
     * @return true if abstract
     */
    boolean isAbstract();

    /**
     * Returns the Javadoc documentation of this method, if present.
     *
     * @return the documentation, or empty if not documented
     * @since 5.0.0
     */
    default Optional<String> documentation() {
        return Optional.empty();
    }

    /**
     * Returns the source location of this method.
     *
     * @return the source location
     */
    SourceLocation sourceLocation();

    /**
     * Returns the method body, if available.
     *
     * <p>The body is loaded lazily and contains source code and analysis.</p>
     *
     * @return an Optional containing the method body
     */
    Optional<MethodBodySyntax> body();

    // ===== Convenience methods =====

    /**
     * Returns whether this method has a specific annotation.
     *
     * @param annotationQualifiedName the qualified name of the annotation
     * @return true if the method has the annotation
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
     * Returns whether this method is static.
     *
     * @return true if static
     */
    default boolean isStatic() {
        return modifiers().contains(Modifier.STATIC);
    }

    /**
     * Returns whether this method is public.
     *
     * @return true if public
     */
    default boolean isPublic() {
        return modifiers().contains(Modifier.PUBLIC);
    }

    /**
     * Returns the method signature for identification.
     *
     * <p>Format: "(Type1,Type2)" for parameters.</p>
     *
     * @return the signature string
     */
    default String signature() {
        StringBuilder sb = new StringBuilder("(");
        List<ParameterSyntax> params = parameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(params.get(i).type().simpleName());
        }
        sb.append(")");
        return sb.toString();
    }
}
