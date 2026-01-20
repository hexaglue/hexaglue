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
 * Complete structural description of a type.
 *
 * <p>This record captures all the structural information about a type, including
 * its nature (class, interface, record, etc.), modifiers, inheritance, fields,
 * methods, constructors, and annotations.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Build a class structure
 * TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
 *     .modifiers(Set.of(Modifier.PUBLIC))
 *     .documentation("An order entity")
 *     .superClass(TypeRef.of("com.example.BaseEntity"))
 *     .interfaces(List.of(TypeRef.of("java.io.Serializable")))
 *     .fields(List.of(idField, nameField))
 *     .methods(List.of(getIdMethod, getNameMethod))
 *     .constructors(List.of(noArgCtor, allArgsCtor))
 *     .annotations(List.of(Annotation.of("javax.persistence.Entity")))
 *     .build();
 *
 * // Access fields by role
 * List<Field> identityFields = structure.getFieldsWithRole(FieldRole.IDENTITY);
 *
 * // Check type nature
 * if (structure.isRecord()) {
 *     // Handle record-specific logic
 * }
 * }</pre>
 *
 * @param nature the type nature (class, interface, record, etc.)
 * @param modifiers the type modifiers (immutable)
 * @param documentation the type's documentation (if present)
 * @param superClass the superclass (if any)
 * @param interfaces the implemented/extended interfaces (immutable)
 * @param permittedSubtypes the permitted subtypes for sealed types (immutable)
 * @param fields the fields declared in this type (immutable)
 * @param methods the methods declared in this type (immutable)
 * @param constructors the constructors declared in this type (immutable)
 * @param annotations the annotations on this type (immutable)
 * @param nestedTypes the nested types declared in this type (immutable)
 * @since 4.1.0
 */
public record TypeStructure(
        TypeNature nature,
        Set<Modifier> modifiers,
        Optional<String> documentation,
        Optional<TypeRef> superClass,
        List<TypeRef> interfaces,
        List<TypeRef> permittedSubtypes,
        List<Field> fields,
        List<Method> methods,
        List<Constructor> constructors,
        List<Annotation> annotations,
        List<TypeRef> nestedTypes) {

    /**
     * Creates a new TypeStructure.
     *
     * @param nature the type nature, must not be null
     * @param modifiers the modifiers, must not be null
     * @param documentation the documentation, must not be null
     * @param superClass the superclass, must not be null
     * @param interfaces the interfaces, must not be null
     * @param permittedSubtypes the permitted subtypes, must not be null
     * @param fields the fields, must not be null
     * @param methods the methods, must not be null
     * @param constructors the constructors, must not be null
     * @param annotations the annotations, must not be null
     * @param nestedTypes the nested types, must not be null
     * @throws NullPointerException if any argument is null
     */
    public TypeStructure {
        Objects.requireNonNull(nature, "nature must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(superClass, "superClass must not be null");
        Objects.requireNonNull(interfaces, "interfaces must not be null");
        Objects.requireNonNull(permittedSubtypes, "permittedSubtypes must not be null");
        Objects.requireNonNull(fields, "fields must not be null");
        Objects.requireNonNull(methods, "methods must not be null");
        Objects.requireNonNull(constructors, "constructors must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(nestedTypes, "nestedTypes must not be null");
        modifiers = Set.copyOf(modifiers);
        interfaces = List.copyOf(interfaces);
        permittedSubtypes = List.copyOf(permittedSubtypes);
        fields = List.copyOf(fields);
        methods = List.copyOf(methods);
        constructors = List.copyOf(constructors);
        annotations = List.copyOf(annotations);
        nestedTypes = List.copyOf(nestedTypes);
    }

    /**
     * Creates a builder for constructing a TypeStructure.
     *
     * @param nature the type nature
     * @return a new builder
     * @throws NullPointerException if nature is null
     */
    public static Builder builder(TypeNature nature) {
        return new Builder(nature);
    }

    /**
     * Returns whether this type is a class (including non-record classes).
     *
     * @return true if this is a class
     */
    public boolean isClass() {
        return nature == TypeNature.CLASS;
    }

    /**
     * Returns whether this type is an interface.
     *
     * @return true if this is an interface
     */
    public boolean isInterface() {
        return nature == TypeNature.INTERFACE;
    }

    /**
     * Returns whether this type is a record.
     *
     * @return true if this is a record
     */
    public boolean isRecord() {
        return nature == TypeNature.RECORD;
    }

    /**
     * Returns whether this type is sealed.
     *
     * @return true if this type has the SEALED modifier
     */
    public boolean isSealed() {
        return modifiers.contains(Modifier.SEALED);
    }

    /**
     * Returns the field with the given name.
     *
     * @param name the field name
     * @return the field, or empty if not found
     */
    public Optional<Field> getField(String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    /**
     * Returns all fields with the given role.
     *
     * @param role the role to filter by
     * @return the fields with the role (may be empty)
     */
    public List<Field> getFieldsWithRole(FieldRole role) {
        return fields.stream().filter(f -> f.hasRole(role)).toList();
    }

    /**
     * Builder for constructing {@link TypeStructure} instances.
     *
     * @since 4.1.0
     */
    public static final class Builder {
        private final TypeNature nature;
        private Set<Modifier> modifiers = Set.of();
        private Optional<String> documentation = Optional.empty();
        private Optional<TypeRef> superClass = Optional.empty();
        private List<TypeRef> interfaces = List.of();
        private List<TypeRef> permittedSubtypes = List.of();
        private List<Field> fields = List.of();
        private List<Method> methods = List.of();
        private List<Constructor> constructors = List.of();
        private List<Annotation> annotations = List.of();
        private List<TypeRef> nestedTypes = List.of();

        private Builder(TypeNature nature) {
            Objects.requireNonNull(nature, "nature must not be null");
            this.nature = nature;
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
         * Sets the superclass.
         *
         * @param superClass the superclass
         * @return this builder
         */
        public Builder superClass(TypeRef superClass) {
            this.superClass = Optional.ofNullable(superClass);
            return this;
        }

        /**
         * Sets the interfaces.
         *
         * @param interfaces the interfaces
         * @return this builder
         */
        public Builder interfaces(List<TypeRef> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        /**
         * Sets the permitted subtypes for sealed types.
         *
         * @param permittedSubtypes the permitted subtypes
         * @return this builder
         */
        public Builder permittedSubtypes(List<TypeRef> permittedSubtypes) {
            this.permittedSubtypes = permittedSubtypes;
            return this;
        }

        /**
         * Sets the fields.
         *
         * @param fields the fields
         * @return this builder
         */
        public Builder fields(List<Field> fields) {
            this.fields = fields;
            return this;
        }

        /**
         * Sets the methods.
         *
         * @param methods the methods
         * @return this builder
         */
        public Builder methods(List<Method> methods) {
            this.methods = methods;
            return this;
        }

        /**
         * Sets the constructors.
         *
         * @param constructors the constructors
         * @return this builder
         */
        public Builder constructors(List<Constructor> constructors) {
            this.constructors = constructors;
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
         * Sets the nested types.
         *
         * @param nestedTypes the nested types
         * @return this builder
         */
        public Builder nestedTypes(List<TypeRef> nestedTypes) {
            this.nestedTypes = nestedTypes;
            return this;
        }

        /**
         * Builds the TypeStructure.
         *
         * @return a new TypeStructure
         */
        public TypeStructure build() {
            return new TypeStructure(
                    nature,
                    modifiers,
                    documentation,
                    superClass,
                    interfaces,
                    permittedSubtypes,
                    fields,
                    methods,
                    constructors,
                    annotations,
                    nestedTypes);
        }
    }
}
