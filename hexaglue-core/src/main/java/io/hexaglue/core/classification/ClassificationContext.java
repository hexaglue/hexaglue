package io.hexaglue.core.classification;

import io.hexaglue.core.graph.model.NodeId;
import java.util.Map;
import java.util.Optional;

/**
 * Context holding domain classification results for use during port classification.
 *
 * <p>This enables a two-pass classification where:
 * <ol>
 *   <li>Pass 1: Classify domain types (entities, value objects, aggregates)</li>
 *   <li>Pass 2: Classify ports using knowledge of domain classifications</li>
 * </ol>
 *
 * <p>For example, a port that manipulates aggregate roots in its signatures
 * is likely a repository, which can be determined more accurately when we
 * already know which types are aggregates.
 *
 * @param domainClassifications the domain type classifications from Pass 1
 */
public record ClassificationContext(Map<NodeId, ClassificationResult> domainClassifications) {

    /**
     * Creates an empty context (no domain classifications available).
     */
    public static ClassificationContext empty() {
        return new ClassificationContext(Map.of());
    }

    /**
     * Returns the classification result for a type, if available.
     */
    public Optional<ClassificationResult> getClassification(NodeId typeId) {
        return Optional.ofNullable(domainClassifications.get(typeId));
    }

    /**
     * Returns true if the type is classified as AGGREGATE_ROOT.
     */
    public boolean isAggregate(NodeId typeId) {
        ClassificationResult result = domainClassifications.get(typeId);
        return result != null
                && result.status() == ClassificationStatus.CLASSIFIED
                && "AGGREGATE_ROOT".equals(result.kind());
    }

    /**
     * Returns true if the type is classified as ENTITY or AGGREGATE_ROOT.
     */
    public boolean isEntity(NodeId typeId) {
        ClassificationResult result = domainClassifications.get(typeId);
        if (result == null || result.status() != ClassificationStatus.CLASSIFIED) {
            return false;
        }
        String kind = result.kind();
        return "ENTITY".equals(kind) || "AGGREGATE_ROOT".equals(kind);
    }

    /**
     * Returns true if the type is classified as VALUE_OBJECT.
     */
    public boolean isValueObject(NodeId typeId) {
        ClassificationResult result = domainClassifications.get(typeId);
        return result != null
                && result.status() == ClassificationStatus.CLASSIFIED
                && "VALUE_OBJECT".equals(result.kind());
    }

    /**
     * Returns true if the type is classified as IDENTIFIER.
     */
    public boolean isIdentifier(NodeId typeId) {
        ClassificationResult result = domainClassifications.get(typeId);
        return result != null
                && result.status() == ClassificationStatus.CLASSIFIED
                && "IDENTIFIER".equals(result.kind());
    }

    /**
     * Returns the domain kind of a type, or null if not classified.
     */
    public String getKind(NodeId typeId) {
        ClassificationResult result = domainClassifications.get(typeId);
        if (result == null || result.status() != ClassificationStatus.CLASSIFIED) {
            return null;
        }
        return result.kind();
    }

    /**
     * Returns true if there are any domain classifications available.
     */
    public boolean hasClassifications() {
        return !domainClassifications.isEmpty();
    }

    /**
     * Returns the number of domain classifications.
     */
    public int size() {
        return domainClassifications.size();
    }
}
