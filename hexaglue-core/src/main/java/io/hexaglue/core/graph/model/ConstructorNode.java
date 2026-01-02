package io.hexaglue.core.graph.model;

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A node representing a constructor in a Java type.
 */
public final class ConstructorNode extends MemberNode {

    private final NodeId id;
    private final NodeId declaringTypeId;
    private final String declaringTypeName;
    private final List<ParameterInfo> parameters;
    private final List<TypeRef> thrownTypes;
    private final Set<JavaModifier> modifiers;
    private final List<AnnotationRef> annotations;
    private final SourceRef sourceRef;

    private ConstructorNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.declaringTypeId = Objects.requireNonNull(builder.declaringTypeId, "declaringTypeId is required");
        this.declaringTypeName = Objects.requireNonNull(builder.declaringTypeName, "declaringTypeName is required");
        this.parameters = builder.parameters != null ? List.copyOf(builder.parameters) : List.of();
        this.thrownTypes = builder.thrownTypes != null ? List.copyOf(builder.thrownTypes) : List.of();
        this.modifiers = builder.modifiers != null ? Set.copyOf(builder.modifiers) : Set.of();
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
        // Constructors use the simple name of the declaring type
        int lastDot = declaringTypeName.lastIndexOf('.');
        return lastDot > 0 ? declaringTypeName.substring(lastDot + 1) : declaringTypeName;
    }

    @Override
    public String qualifiedName() {
        return declaringTypeName + "#<init>(" + parameterTypesSignature() + ")";
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
     * Returns the parameters of this constructor.
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
     * Returns the parameter types as a comma-separated string.
     */
    public String parameterTypesSignature() {
        return parameters.stream().map(p -> p.type().rawQualifiedName()).collect(Collectors.joining(","));
    }

    /**
     * Returns the number of parameters.
     */
    public int parameterCount() {
        return parameters.size();
    }

    /**
     * Returns true if this is the default (no-arg) constructor.
     */
    public boolean isDefault() {
        return parameters.isEmpty();
    }

    public static final class Builder {
        private NodeId id;
        private NodeId declaringTypeId;
        private String declaringTypeName;
        private List<ParameterInfo> parameters;
        private List<TypeRef> thrownTypes;
        private Set<JavaModifier> modifiers;
        private List<AnnotationRef> annotations;
        private SourceRef sourceRef;

        private Builder() {}

        public Builder id(NodeId id) {
            this.id = id;
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

        public ConstructorNode build() {
            // Auto-compute id if not set
            if (this.id == null && this.declaringTypeName != null) {
                String paramTypes = parameters != null
                        ? parameters.stream()
                                .map(p -> p.type().rawQualifiedName())
                                .collect(Collectors.joining(","))
                        : "";
                this.id = NodeId.constructor(declaringTypeName, paramTypes);
            }
            return new ConstructorNode(this);
        }
    }
}
