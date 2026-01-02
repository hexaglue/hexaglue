package io.hexaglue.spi.ir;

import java.util.Optional;

/**
 * Information about a relationship between domain types.
 *
 * <p>This captures the metadata needed for JPA relationship mapping.
 *
 * @param kind the relationship kind (ONE_TO_ONE, ONE_TO_MANY, etc.)
 * @param targetType the fully qualified name of the target type
 * @param mappedBy the name of the field on the inverse side (for bidirectional relationships)
 * @param owning true if this is the owning side of the relationship
 */
public record RelationInfo(RelationKind kind, String targetType, String mappedBy, boolean owning) {

    /**
     * Creates a relation info for a unidirectional relationship (owning by default).
     */
    public static RelationInfo unidirectional(RelationKind kind, String targetType) {
        return new RelationInfo(kind, targetType, null, true);
    }

    /**
     * Creates a relation info for the owning side of a bidirectional relationship.
     */
    public static RelationInfo owning(RelationKind kind, String targetType) {
        return new RelationInfo(kind, targetType, null, true);
    }

    /**
     * Creates a relation info for the inverse side of a bidirectional relationship.
     */
    public static RelationInfo inverse(RelationKind kind, String targetType, String mappedBy) {
        return new RelationInfo(kind, targetType, mappedBy, false);
    }

    /**
     * Returns the mappedBy field name as an Optional.
     */
    public Optional<String> mappedByOpt() {
        return Optional.ofNullable(mappedBy);
    }

    /**
     * Returns true if this is a bidirectional relationship.
     */
    public boolean isBidirectional() {
        return mappedBy != null;
    }

    /**
     * Returns true if this relationship is a collection (ONE_TO_MANY or MANY_TO_MANY).
     */
    public boolean isCollection() {
        return kind == RelationKind.ONE_TO_MANY
                || kind == RelationKind.MANY_TO_MANY
                || kind == RelationKind.ELEMENT_COLLECTION;
    }

    /**
     * Returns true if this is an embedded relationship.
     */
    public boolean isEmbedded() {
        return kind == RelationKind.EMBEDDED || kind == RelationKind.ELEMENT_COLLECTION;
    }
}
