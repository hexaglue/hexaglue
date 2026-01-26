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

package io.hexaglue.plugin.jpa.util;

import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.DomainIndex;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Detects bidirectional relationships between domain types.
 *
 * <p>In JPA, bidirectional relationships require one side to be the "owning side"
 * (with the foreign key) and the other to be the "inverse side" (with mappedBy).
 * This detector analyzes domain models to automatically determine which relationships
 * are bidirectional and which field names should be used for mappedBy.
 *
 * <p>Bidirectional detection rules:
 * <ul>
 *   <li>OneToMany ↔ ManyToOne: The ManyToOne side is always owning</li>
 *   <li>OneToOne: The side with the foreign key annotation wins, otherwise first found</li>
 *   <li>ManyToMany: Both sides are owning (no mappedBy), unless explicitly annotated</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * // Domain model
 * record Order(OrderId id, List<OrderLine> lines) {}
 * record OrderLine(OrderLineId id, Order order) {}
 *
 * // Detection result:
 * // "com.example.Order#lines" → "order" (means: Order.lines should have mappedBy="order")
 * }</pre>
 *
 * @since 5.0.0
 */
public final class BidirectionalDetector {

    private BidirectionalDetector() {
        // Utility class
    }

    /**
     * Detects all bidirectional relationships in the domain model.
     *
     * <p>Returns a map where:
     * <ul>
     *   <li>Key: "{typeFqn}#{fieldName}" for the inverse side (ONE_TO_MANY)</li>
     *   <li>Value: field name on the owning side (the mappedBy value)</li>
     * </ul>
     *
     * @param domainIndex the domain index containing all types
     * @return map of inverse field keys to owning field names
     */
    public static Map<String, String> detectBidirectionalMappings(DomainIndex domainIndex) {
        Map<String, String> mappings = new HashMap<>();

        // Collect all types with their structures
        List<TypeWithStructure> allTypes = collectAllTypes(domainIndex);

        // For each type, find ONE_TO_MANY relations and check for inverse MANY_TO_ONE
        for (TypeWithStructure typeInfo : allTypes) {
            analyzeTypeRelations(typeInfo, allTypes, mappings, domainIndex);
        }

        return mappings;
    }

    /**
     * Gets the mappedBy value for a field if it's the inverse side of a bidirectional relation.
     *
     * @param mappings the bidirectional mappings from {@link #detectBidirectionalMappings}
     * @param typeFqn the fully qualified name of the type containing the field
     * @param fieldName the name of the field
     * @return the mappedBy value if this is an inverse side, null otherwise
     */
    public static String getMappedBy(Map<String, String> mappings, String typeFqn, String fieldName) {
        return mappings.get(typeFqn + "#" + fieldName);
    }

    private static List<TypeWithStructure> collectAllTypes(DomainIndex domainIndex) {
        List<TypeWithStructure> result = new java.util.ArrayList<>();

        domainIndex.aggregateRoots().forEach(agg ->
                result.add(new TypeWithStructure(agg.id().qualifiedName(), agg.structure())));

        domainIndex.entities().forEach(entity ->
                result.add(new TypeWithStructure(entity.id().qualifiedName(), entity.structure())));

        return result;
    }

    private static void analyzeTypeRelations(
            TypeWithStructure source,
            List<TypeWithStructure> allTypes,
            Map<String, String> mappings,
            DomainIndex domainIndex) {

        if (source.structure() == null) {
            return;
        }

        for (Field field : source.structure().fields()) {
            // Only process collection fields (potential ONE_TO_MANY)
            if (!field.hasRole(FieldRole.COLLECTION)) {
                continue;
            }

            // Get target type from collection element
            String targetFqn = field.elementType()
                    .map(t -> t.qualifiedName())
                    .orElse(null);

            if (targetFqn == null) {
                continue;
            }

            // Skip if target is an Identifier (cross-aggregate reference)
            if (isIdentifierType(targetFqn, domainIndex)) {
                continue;
            }

            // Find target type structure
            Optional<TypeWithStructure> targetOpt = allTypes.stream()
                    .filter(t -> t.fqn().equals(targetFqn))
                    .findFirst();

            if (targetOpt.isEmpty() || targetOpt.get().structure() == null) {
                continue;
            }

            // Look for inverse field in target (a reference back to source)
            Optional<Field> inverseField = findInverseField(
                    targetOpt.get().structure(),
                    source.fqn());

            if (inverseField.isPresent()) {
                // Found bidirectional pair!
                // The ONE_TO_MANY side (source.field) is inverse, needs mappedBy
                // The MANY_TO_ONE side (target.inverseField) is owning
                String key = source.fqn() + "#" + field.name();
                mappings.put(key, inverseField.get().name());
            }
        }
    }

    private static Optional<Field> findInverseField(TypeStructure targetStructure, String sourceFqn) {
        return targetStructure.fields().stream()
                .filter(f -> !f.hasRole(FieldRole.COLLECTION)) // MANY_TO_ONE is not a collection
                .filter(f -> f.type().qualifiedName().equals(sourceFqn))
                .findFirst();
    }

    private static boolean isIdentifierType(String typeFqn, DomainIndex domainIndex) {
        return domainIndex.identifiers()
                .anyMatch(id -> id.id().qualifiedName().equals(typeFqn));
    }

    /**
     * Helper record to hold type info with its structure.
     */
    private record TypeWithStructure(String fqn, TypeStructure structure) {}
}
