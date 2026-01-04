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

package io.hexaglue.core.graph.query;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Fluent query interface for the application graph.
 *
 * <p>Provides a composable way to query types, members, and relationships.
 */
public interface GraphQuery {

    // === Type queries ===

    /**
     * Returns all types.
     */
    Stream<TypeNode> types();

    /**
     * Returns types matching the given predicate.
     */
    Stream<TypeNode> types(Predicate<TypeNode> predicate);

    /**
     * Returns types in the given package.
     */
    Stream<TypeNode> typesInPackage(String packageName);

    /**
     * Returns types with the given form.
     */
    Stream<TypeNode> typesWithForm(JavaForm form);

    /**
     * Returns types annotated with the given annotation.
     */
    Stream<TypeNode> typesAnnotatedWith(String annotationQualifiedName);

    /**
     * Returns interfaces.
     */
    default Stream<TypeNode> interfaces() {
        return typesWithForm(JavaForm.INTERFACE);
    }

    /**
     * Returns classes (non-interface, non-record, non-enum).
     */
    default Stream<TypeNode> classes() {
        return typesWithForm(JavaForm.CLASS);
    }

    /**
     * Returns records.
     */
    default Stream<TypeNode> records() {
        return typesWithForm(JavaForm.RECORD);
    }

    /**
     * Returns enums.
     */
    default Stream<TypeNode> enums() {
        return typesWithForm(JavaForm.ENUM);
    }

    /**
     * Returns types with "Repository" suffix.
     */
    default Stream<TypeNode> repositories() {
        return types(TypeNode::hasRepositorySuffix);
    }

    /**
     * Returns types with "Id" suffix.
     */
    default Stream<TypeNode> identifiers() {
        return types(TypeNode::hasIdSuffix);
    }

    // === Member queries ===

    /**
     * Returns all fields.
     */
    Stream<FieldNode> fields();

    /**
     * Returns fields matching the given predicate.
     */
    Stream<FieldNode> fields(Predicate<FieldNode> predicate);

    /**
     * Returns all methods.
     */
    Stream<MethodNode> methods();

    /**
     * Returns methods matching the given predicate.
     */
    Stream<MethodNode> methods(Predicate<MethodNode> predicate);

    /**
     * Returns all constructors.
     */
    Stream<ConstructorNode> constructors();

    // === Relationship queries ===

    /**
     * Returns the fields of the given type.
     */
    List<FieldNode> fieldsOf(TypeNode type);

    /**
     * Returns the methods of the given type.
     */
    List<MethodNode> methodsOf(TypeNode type);

    /**
     * Returns the constructors of the given type.
     */
    List<ConstructorNode> constructorsOf(TypeNode type);

    /**
     * Returns the supertype of the given type.
     */
    Optional<TypeNode> supertypeOf(TypeNode type);

    /**
     * Returns the interfaces implemented by the given type.
     */
    List<TypeNode> interfacesOf(TypeNode type);

    /**
     * Returns the subtypes of the given type.
     */
    List<TypeNode> subtypesOf(TypeNode type);

    /**
     * Returns the implementors of the given interface.
     */
    List<TypeNode> implementorsOf(TypeNode interfaceType);

    /**
     * Returns the types that use the given type in their signature.
     */
    List<TypeNode> usersInSignatureOf(TypeNode type);

    // === Lookup queries ===

    /**
     * Returns the type with the given qualified name.
     */
    Optional<TypeNode> type(String qualifiedName);

    /**
     * Returns the type with the given node id.
     */
    Optional<TypeNode> type(NodeId id);

    /**
     * Returns the field with the given id.
     */
    Optional<FieldNode> field(NodeId id);

    /**
     * Returns the method with the given id.
     */
    Optional<MethodNode> method(NodeId id);

    // === Style queries ===

    /**
     * Returns the detected package organization style.
     */
    PackageOrganizationStyle packageOrganizationStyle();

    /**
     * Returns the confidence level of the style detection.
     */
    ConfidenceLevel styleConfidence();

    /**
     * Returns true if the detected style supports port direction distinction
     * (e.g., HEXAGONAL with ports.in/ports.out packages).
     */
    default boolean supportsPortDirection() {
        return packageOrganizationStyle().supportsPortDirection();
    }

    /**
     * Returns true if a known style was detected with sufficient confidence.
     */
    default boolean hasConfidentStyle() {
        return packageOrganizationStyle().isKnown()
                && (styleConfidence() == ConfidenceLevel.HIGH || styleConfidence() == ConfidenceLevel.EXPLICIT);
    }

    // === Graph access ===

    /**
     * Returns the underlying application graph.
     */
    io.hexaglue.core.graph.ApplicationGraph graph();
}
