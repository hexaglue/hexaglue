package io.hexaglue.core.graph.model;

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A node representing a field in a Java type.
 */
public final class FieldNode extends MemberNode {

    private final NodeId id;
    private final String simpleName;
    private final NodeId declaringTypeId;
    private final String declaringTypeName;
    private final TypeRef type;
    private final Set<JavaModifier> modifiers;
    private final List<AnnotationRef> annotations;
    private final SourceRef sourceRef;

    private FieldNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.simpleName = Objects.requireNonNull(builder.simpleName, "simpleName is required");
        this.declaringTypeId = Objects.requireNonNull(builder.declaringTypeId, "declaringTypeId is required");
        this.declaringTypeName = Objects.requireNonNull(builder.declaringTypeName, "declaringTypeName is required");
        this.type = Objects.requireNonNull(builder.type, "type is required");
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
        return simpleName;
    }

    @Override
    public String qualifiedName() {
        return declaringTypeName + "#" + simpleName;
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
     * Returns the type of this field.
     */
    public TypeRef type() {
        return type;
    }

    // === Convenience methods ===

    /**
     * Returns true if this field looks like an identity field.
     *
     * <p>A field looks like an identity if:
     * <ul>
     *   <li>It's named "id" or ends with "Id" (e.g., "orderId")</li>
     *   <li>AND it's not a collection or map type (identities are simple values)</li>
     * </ul>
     *
     * <p>This prevents false positives like "linesByProductId" (a Map indexed by product ID)
     * from being considered identity fields.
     */
    public boolean looksLikeIdentity() {
        // Collections and maps are not identity fields
        if (type.isCollectionLike() || type.isMapLike()) {
            return false;
        }
        return simpleName.equals("id") || simpleName.endsWith("Id");
    }

    /**
     * Returns true if this field's type is a common ID type.
     */
    public boolean hasCommonIdType() {
        String typeName = type.rawQualifiedName();
        return typeName.equals("java.util.UUID")
                || typeName.equals("java.lang.Long")
                || typeName.equals("java.lang.String")
                || typeName.equals("long");
    }

    /**
     * Returns true if this field's type is a collection.
     */
    public boolean isCollectionType() {
        return type.isCollectionLike();
    }

    /**
     * Returns true if this field's type is Optional.
     */
    public boolean isOptionalType() {
        return type.isOptionalLike();
    }

    /**
     * Returns true if this field is marked as transient.
     */
    public boolean isTransient() {
        return modifiers.contains(JavaModifier.TRANSIENT);
    }

    public static final class Builder {
        private NodeId id;
        private String simpleName;
        private NodeId declaringTypeId;
        private String declaringTypeName;
        private TypeRef type;
        private Set<JavaModifier> modifiers;
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

        public Builder declaringTypeId(NodeId declaringTypeId) {
            this.declaringTypeId = declaringTypeId;
            return this;
        }

        public Builder declaringTypeName(String declaringTypeName) {
            this.declaringTypeName = declaringTypeName;
            // Auto-compute declaringTypeId if not set
            if (this.declaringTypeId == null) {
                this.declaringTypeId = NodeId.type(declaringTypeName);
            }
            return this;
        }

        public Builder type(TypeRef type) {
            this.type = type;
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

        public FieldNode build() {
            // Auto-compute id if not set
            if (this.id == null && this.declaringTypeName != null && this.simpleName != null) {
                this.id = NodeId.field(declaringTypeName, simpleName);
            }
            return new FieldNode(this);
        }
    }
}
