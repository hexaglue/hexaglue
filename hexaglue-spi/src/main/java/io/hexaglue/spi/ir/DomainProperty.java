package io.hexaglue.spi.ir;

import java.util.Optional;

/**
 * A property of a domain type.
 *
 * @param name the property name
 * @param type the type reference with full generic information
 * @param cardinality single value, optional, or collection
 * @param nullability whether the property can be null
 * @param isIdentity true if this is the identity property
 * @param isEmbedded true if this property is an embedded value object
 * @param relationInfo information about relationships (null for simple properties)
 */
public record DomainProperty(
        String name,
        TypeRef type,
        Cardinality cardinality,
        Nullability nullability,
        boolean isIdentity,
        boolean isEmbedded,
        RelationInfo relationInfo) {

    /**
     * Backward-compatible constructor without isEmbedded and relationInfo.
     */
    public DomainProperty(
            String name, TypeRef type, Cardinality cardinality, Nullability nullability, boolean isIdentity) {
        this(name, type, cardinality, nullability, isIdentity, false, null);
    }

    /**
     * Returns the fully qualified type name.
     * For collections, returns the element type name.
     *
     * @deprecated Use {@link #type()} for full type information including generics.
     */
    @Deprecated
    public String typeName() {
        if (type.isCollectionLike() || type.isOptionalLike()) {
            TypeRef element = type.unwrapElement();
            return element.qualifiedName();
        }
        return type.qualifiedName();
    }

    /**
     * Returns the relation info as an Optional.
     */
    public Optional<RelationInfo> relationInfoOpt() {
        return Optional.ofNullable(relationInfo);
    }

    /**
     * Returns true if this property has a relationship to another domain type.
     */
    public boolean hasRelation() {
        return relationInfo != null;
    }

    /**
     * Returns true if this is a simple property (no relation, not identity, not embedded).
     */
    public boolean isSimple() {
        return !isIdentity && !isEmbedded && relationInfo == null;
    }

    /**
     * Returns true if this property represents a collection of entities.
     */
    public boolean isEntityCollection() {
        return relationInfo != null && relationInfo.isCollection() && !relationInfo.isEmbedded();
    }

    /**
     * Returns true if this property represents an embedded value object collection.
     */
    public boolean isEmbeddedCollection() {
        return relationInfo != null && relationInfo.kind() == RelationKind.ELEMENT_COLLECTION;
    }
}
