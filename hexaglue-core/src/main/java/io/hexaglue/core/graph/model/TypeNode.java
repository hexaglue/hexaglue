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

package io.hexaglue.core.graph.model;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A node representing a Java type (class, interface, record, enum, or annotation).
 */
public final class TypeNode extends Node {

    private final NodeId id;
    private final String simpleName;
    private final String qualifiedName;
    private final String packageName;
    private final JavaForm form;
    private final Set<JavaModifier> modifiers;
    private final TypeRef superType;
    private final List<TypeRef> interfaces;
    private final List<AnnotationRef> annotations;
    private final SourceRef sourceRef;

    private TypeNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.simpleName = Objects.requireNonNull(builder.simpleName, "simpleName is required");
        this.qualifiedName = Objects.requireNonNull(builder.qualifiedName, "qualifiedName is required");
        this.packageName = Objects.requireNonNull(builder.packageName, "packageName is required");
        this.form = Objects.requireNonNull(builder.form, "form is required");
        this.modifiers = builder.modifiers != null ? Set.copyOf(builder.modifiers) : Set.of();
        this.superType = builder.superType;
        this.interfaces = builder.interfaces != null ? List.copyOf(builder.interfaces) : List.of();
        this.annotations = builder.annotations != null ? List.copyOf(builder.annotations) : List.of();
        this.sourceRef = builder.sourceRef;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public NodeId id() {
        return id;
    }

    @Override
    public String simpleName() {
        return simpleName;
    }

    @Override
    public String qualifiedName() {
        return qualifiedName;
    }

    @Override
    public String packageName() {
        return packageName;
    }

    @Override
    public Optional<SourceRef> sourceRef() {
        return Optional.ofNullable(sourceRef);
    }

    @Override
    public List<AnnotationRef> annotations() {
        return annotations;
    }

    /**
     * Returns the syntactic form of this type.
     */
    public JavaForm form() {
        return form;
    }

    /**
     * Returns the modifiers on this type.
     */
    public Set<JavaModifier> modifiers() {
        return modifiers;
    }

    /**
     * Returns the supertype, if any.
     */
    public Optional<TypeRef> superType() {
        return Optional.ofNullable(superType);
    }

    /**
     * Returns the interfaces implemented or extended by this type.
     */
    public List<TypeRef> interfaces() {
        return interfaces;
    }

    // === Convenience methods ===

    public boolean isClass() {
        return form == JavaForm.CLASS;
    }

    public boolean isInterface() {
        return form == JavaForm.INTERFACE;
    }

    public boolean isRecord() {
        return form == JavaForm.RECORD;
    }

    public boolean isEnum() {
        return form == JavaForm.ENUM;
    }

    public boolean isAnnotation() {
        return form == JavaForm.ANNOTATION;
    }

    public boolean isPublic() {
        return modifiers.contains(JavaModifier.PUBLIC);
    }

    public boolean isAbstract() {
        return modifiers.contains(JavaModifier.ABSTRACT);
    }

    public boolean isFinal() {
        return modifiers.contains(JavaModifier.FINAL);
    }

    /**
     * Returns true if this type name ends with "Repository".
     */
    public boolean hasRepositorySuffix() {
        return simpleName.endsWith("Repository");
    }

    /**
     * Returns true if this type name ends with "Gateway".
     */
    public boolean hasGatewaySuffix() {
        return simpleName.endsWith("Gateway");
    }

    /**
     * Returns true if this type name ends with "UseCase" or "Service".
     */
    public boolean hasUseCaseSuffix() {
        return simpleName.endsWith("UseCase") || simpleName.endsWith("Service");
    }

    /**
     * Returns true if this type name ends with "Id".
     */
    public boolean hasIdSuffix() {
        return simpleName.endsWith("Id");
    }

    /**
     * Returns true if this type name ends with "Event".
     */
    public boolean hasEventSuffix() {
        return simpleName.endsWith("Event");
    }

    public static final class Builder {
        private NodeId id;
        private String simpleName;
        private String qualifiedName;
        private String packageName;
        private JavaForm form;
        private Set<JavaModifier> modifiers;
        private TypeRef superType;
        private List<TypeRef> interfaces;
        private List<AnnotationRef> annotations;
        private SourceRef sourceRef;

        private Builder() {}

        public Builder id(NodeId id) {
            this.id = id;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            // Auto-compute id if not set
            if (this.id == null) {
                this.id = NodeId.type(qualifiedName);
            }
            // Auto-compute packageName if not set
            if (this.packageName == null) {
                int lastDot = qualifiedName.lastIndexOf('.');
                this.packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
            }
            // Auto-compute simpleName if not set
            if (this.simpleName == null) {
                int lastDot = qualifiedName.lastIndexOf('.');
                this.simpleName = lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
            }
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder form(JavaForm form) {
            this.form = form;
            return this;
        }

        public Builder modifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder superType(TypeRef superType) {
            this.superType = superType;
            return this;
        }

        public Builder interfaces(List<TypeRef> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        public Builder annotations(List<AnnotationRef> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder sourceRef(SourceRef sourceRef) {
            this.sourceRef = sourceRef;
            return this;
        }

        public TypeNode build() {
            return new TypeNode(this);
        }
    }
}
