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

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A node representing a method in a Java type.
 */
public final class MethodNode extends MemberNode {

    private final NodeId id;
    private final String simpleName;
    private final NodeId declaringTypeId;
    private final String declaringTypeName;
    private final TypeRef returnType;
    private final List<ParameterInfo> parameters;
    private final List<TypeRef> thrownTypes;
    private final Set<JavaModifier> modifiers;
    private final List<AnnotationRef> annotations;
    private final SourceRef sourceRef;
    private final OptionalInt cyclomaticComplexity;

    private MethodNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.simpleName = Objects.requireNonNull(builder.simpleName, "simpleName is required");
        this.declaringTypeId = Objects.requireNonNull(builder.declaringTypeId, "declaringTypeId is required");
        this.declaringTypeName = Objects.requireNonNull(builder.declaringTypeName, "declaringTypeName is required");
        this.returnType = Objects.requireNonNull(builder.returnType, "returnType is required");
        this.parameters = builder.parameters != null ? List.copyOf(builder.parameters) : List.of();
        this.thrownTypes = builder.thrownTypes != null ? List.copyOf(builder.thrownTypes) : List.of();
        this.modifiers = builder.modifiers != null ? Set.copyOf(builder.modifiers) : Set.of();
        this.annotations = builder.annotations != null ? List.copyOf(builder.annotations) : List.of();
        this.sourceRef = builder.sourceRef;
        this.cyclomaticComplexity =
                builder.cyclomaticComplexity != null ? builder.cyclomaticComplexity : OptionalInt.empty();
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
        return declaringTypeName + "#" + simpleName + "(" + parameterTypesSignature() + ")";
    }

    @Override
    public NodeId declaringTypeId() {
        return declaringTypeId;
    }

    @Override
    public String declaringTypeName() {
        return declaringTypeName;
    }

    @Override
    public Set<JavaModifier> modifiers() {
        return modifiers;
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
     * Returns the return type of this method.
     */
    public TypeRef returnType() {
        return returnType;
    }

    /**
     * Returns the parameters of this method.
     */
    public List<ParameterInfo> parameters() {
        return parameters;
    }

    /**
     * Returns the thrown exception types.
     */
    public List<TypeRef> thrownTypes() {
        return thrownTypes;
    }

    /**
     * Returns the cyclomatic complexity of this method, if calculated.
     *
     * <p>Cyclomatic complexity measures the number of linearly independent paths
     * through a method's control flow. A value of 1 indicates a method with no
     * branching, while higher values indicate more complex control flow.
     *
     * @return the cyclomatic complexity, or empty if not calculated
     * @since 5.0.0
     */
    public OptionalInt cyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    /**
     * Returns the parameter types as a comma-separated string.
     */
    public String parameterTypesSignature() {
        return parameters.stream().map(p -> p.type().rawQualifiedName()).collect(Collectors.joining(","));
    }

    // === Convenience methods ===

    /**
     * Returns true if this method returns void.
     */
    public boolean isVoid() {
        return returnType.rawQualifiedName().equals("void");
    }

    /**
     * Returns true if this method returns Optional.
     */
    public boolean returnsOptional() {
        return returnType.isOptionalLike();
    }

    /**
     * Returns true if this method returns a collection.
     */
    public boolean returnsCollection() {
        return returnType.isCollectionLike();
    }

    /**
     * Returns true if this method is abstract.
     */
    public boolean isAbstract() {
        return modifiers.contains(JavaModifier.ABSTRACT);
    }

    /**
     * Returns true if this method looks like a getter (getX/isX).
     */
    public boolean looksLikeGetter() {
        return parameters.isEmpty() && !isVoid() && (simpleName.startsWith("get") || simpleName.startsWith("is"));
    }

    /**
     * Returns true if this method looks like a setter (setX with 1 param).
     */
    public boolean looksLikeSetter() {
        return parameters.size() == 1 && isVoid() && simpleName.startsWith("set");
    }

    /**
     * Returns the number of parameters.
     */
    public int parameterCount() {
        return parameters.size();
    }

    public static final class Builder {
        private NodeId id;
        private String simpleName;
        private NodeId declaringTypeId;
        private String declaringTypeName;
        private TypeRef returnType;
        private List<ParameterInfo> parameters;
        private List<TypeRef> thrownTypes;
        private Set<JavaModifier> modifiers;
        private List<AnnotationRef> annotations;
        private SourceRef sourceRef;
        private OptionalInt cyclomaticComplexity;

        private Builder() {}

        public Builder id(NodeId id) {
            this.id = id;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder declaringTypeId(NodeId declaringTypeId) {
            this.declaringTypeId = declaringTypeId;
            return this;
        }

        public Builder declaringTypeName(String declaringTypeName) {
            this.declaringTypeName = declaringTypeName;
            if (this.declaringTypeId == null) {
                this.declaringTypeId = NodeId.type(declaringTypeName);
            }
            return this;
        }

        public Builder returnType(TypeRef returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder parameters(List<ParameterInfo> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder thrownTypes(List<TypeRef> thrownTypes) {
            this.thrownTypes = thrownTypes;
            return this;
        }

        public Builder modifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
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

        /**
         * Sets the cyclomatic complexity of the method.
         *
         * @param complexity the cyclomatic complexity value
         * @return this builder
         * @since 5.0.0
         */
        public Builder cyclomaticComplexity(int complexity) {
            this.cyclomaticComplexity = OptionalInt.of(complexity);
            return this;
        }

        /**
         * Sets the cyclomatic complexity from an OptionalInt.
         *
         * @param complexity the complexity or empty
         * @return this builder
         * @since 5.0.0
         */
        public Builder cyclomaticComplexity(OptionalInt complexity) {
            this.cyclomaticComplexity = complexity != null ? complexity : OptionalInt.empty();
            return this;
        }

        public MethodNode build() {
            // Auto-compute id if not set
            if (this.id == null && this.declaringTypeName != null && this.simpleName != null) {
                String paramTypes = parameters != null
                        ? parameters.stream()
                                .map(p -> p.type().rawQualifiedName())
                                .collect(Collectors.joining(","))
                        : "";
                this.id = NodeId.method(declaringTypeName, simpleName, paramTypes);
            }
            return new MethodNode(this);
        }
    }
}
