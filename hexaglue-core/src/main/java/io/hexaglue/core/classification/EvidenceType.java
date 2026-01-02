package io.hexaglue.core.classification;

/**
 * Type of evidence supporting a classification.
 *
 * <p>Used to categorize and explain why a type was classified
 * in a particular way.
 */
public enum EvidenceType {

    /**
     * Evidence from an annotation (e.g., @AggregateRoot, @Entity).
     */
    ANNOTATION,

    /**
     * Evidence from naming patterns (e.g., suffix "Repository", "UseCase").
     */
    NAMING,

    /**
     * Evidence from type structure (e.g., has identity field, is immutable).
     */
    STRUCTURE,

    /**
     * Evidence from relationships (e.g., used in Repository signature).
     */
    RELATIONSHIP,

    /**
     * Evidence from package location (e.g., in ".ports.in" package).
     */
    PACKAGE
}
