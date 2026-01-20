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

package io.hexaglue.arch.model;

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of a field in a type.
 *
 * <p>This record captures the field's name, type, modifiers, annotations,
 * and semantic roles. The roles indicate how the field is used in the
 * domain model (e.g., IDENTITY, COLLECTION, AGGREGATE_REFERENCE).</p>
 *
 * <h2>Special Type Information</h2>
 * <ul>
 *   <li><strong>wrappedType</strong>: For identifier types that wrap primitives
 *       (e.g., OrderId wrapping UUID), this contains the wrapped type.</li>
 *   <li><strong>elementType</strong>: For collection fields (List, Set), this
 *       contains the element type.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Simple field
 * Field name = Field.of("name", TypeRef.of("java.lang.String"));
 *
 * // Field with builder
 * Field id = Field.builder("id", TypeRef.of("com.example.OrderId"))
 *     .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
 *     .annotations(List.of(Annotation.of("javax.persistence.Id")))
 *     .wrappedType(TypeRef.of("java.util.UUID"))
 *     .roles(Set.of(FieldRole.IDENTITY))
 *     .build();
 *
 * // Check field role
 * if (id.isIdentity()) {
 *     // Handle as identity field
 * }
 * }</pre>
 *
 * @param name the field name
 * @param type the field type
 * @param modifiers the field modifiers (immutable)
 * @param annotations the annotations on this field (immutable)
 * @param documentation the field's documentation (if present)
 * @param wrappedType the wrapped type for identifier types (if applicable)
 * @param elementType the element type for collections (if applicable)
 * @param roles the semantic roles this field plays (immutable)
 * @since 4.1.0
 */
public record Field(
        String name,
        TypeRef type,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        Optional<TypeRef> wrappedType,
        Optional<TypeRef> elementType,
        Set<FieldRole> roles) {

    /**
     * Creates a new Field.
     *
     * @param name the field name, must not be null or blank
     * @param type the field type, must not be null
     * @param modifiers the modifiers, must not be null
     * @param annotations the annotations, must not be null
     * @param documentation the documentation, must not be null
     * @param wrappedType the wrapped type, must not be null
     * @param elementType the element type, must not be null
     * @param roles the roles, must not be null
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public Field {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(wrappedType, "wrappedType must not be null");
        Objects.requireNonNull(elementType, "elementType must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        roles = Set.copyOf(roles);
    }

    /**
     * Creates a field with the given name and type, with default values for other attributes.
     *
     * @param name the field name
     * @param type the field type
     * @return a new Field
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public static Field of(String name, TypeRef type) {
        return new Field(
                name, type, Set.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Set.of());
    }

    /**
     * Creates a builder for constructing a Field.
     *
     * @param name the field name
     * @param type the field type
     * @return a new builder
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public static Builder builder(String name, TypeRef type) {
        return new Builder(name, type);
    }

    /**
     * Returns whether this field has the given role.
     *
     * @param role the role to check
     * @return true if the field has the role
     */
    public boolean hasRole(FieldRole role) {
        return roles.contains(role);
    }

    /**
     * Returns whether this field is an identity field.
     *
     * @return true if this field has the IDENTITY role
     */
    public boolean isIdentity() {
        return hasRole(FieldRole.IDENTITY);
    }

    /**
     * Returns whether this field is a collection.
     *
     * @return true if this field has the COLLECTION role
     */
    public boolean isCollection() {
        return hasRole(FieldRole.COLLECTION);
    }

    /**
     * Returns whether this field has an annotation with the given qualified name.
     *
     * @param qualifiedName the fully qualified annotation name
     * @return true if the field has the annotation
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations.stream().anyMatch(a -> a.qualifiedName().equals(qualifiedName));
    }

    /**
     * Builder for constructing {@link Field} instances.
     *
     * @since 4.1.0
     */
    public static final class Builder {
        private final String name;
        private final TypeRef type;
        private Set<Modifier> modifiers = Set.of();
        private List<Annotation> annotations = List.of();
        private Optional<String> documentation = Optional.empty();
        private Optional<TypeRef> wrappedType = Optional.empty();
        private Optional<TypeRef> elementType = Optional.empty();
        private Set<FieldRole> roles = Set.of();

        private Builder(String name, TypeRef type) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(type, "type must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            this.name = name;
            this.type = type;
        }

        /**
         * Sets the modifiers.
         *
         * @param modifiers the modifiers
         * @return this builder
         */
        public Builder modifiers(Set<Modifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        /**
         * Sets the annotations.
         *
         * @param annotations the annotations
         * @return this builder
         */
        public Builder annotations(List<Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Sets the documentation.
         *
         * @param documentation the documentation
         * @return this builder
         */
        public Builder documentation(String documentation) {
            this.documentation = Optional.ofNullable(documentation);
            return this;
        }

        /**
         * Sets the wrapped type (for identifier types).
         *
         * @param wrappedType the wrapped type
         * @return this builder
         */
        public Builder wrappedType(TypeRef wrappedType) {
            this.wrappedType = Optional.ofNullable(wrappedType);
            return this;
        }

        /**
         * Sets the element type (for collection types).
         *
         * @param elementType the element type
         * @return this builder
         */
        public Builder elementType(TypeRef elementType) {
            this.elementType = Optional.ofNullable(elementType);
            return this;
        }

        /**
         * Sets the roles.
         *
         * @param roles the roles
         * @return this builder
         */
        public Builder roles(Set<FieldRole> roles) {
            this.roles = roles;
            return this;
        }

        /**
         * Builds the Field.
         *
         * @return a new Field
         */
        public Field build() {
            return new Field(name, type, modifiers, annotations, documentation, wrappedType, elementType, roles);
        }
    }
}
