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

package io.hexaglue.plugin.audit.adapter.diagram;

import io.hexaglue.plugin.audit.domain.model.report.AggregateComponent;
import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.IdentifierComponent;
import io.hexaglue.plugin.audit.domain.model.report.Relationship;
import io.hexaglue.plugin.audit.domain.model.report.ValueObjectComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds Mermaid class diagrams for domain model visualization.
 *
 * <p>Generates a class diagram showing aggregates with their value objects,
 * identifiers, and inter-aggregate references with DDD stereotypes.
 *
 * @since 5.0.0
 */
public class ClassDiagramBuilder {

    /**
     * Builds a domain model class diagram.
     *
     * @param components component details
     * @param relationships relationships including cycles
     * @return Mermaid classDiagram code (without code fence)
     */
    public String build(ComponentDetails components, List<Relationship> relationships) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: Domain Model\n");
        sb.append("---\n");
        sb.append("classDiagram\n");

        if (components.aggregates().isEmpty()) {
            sb.append("    class NoAggregates\n");
            sb.append("    NoAggregates : No Aggregates Detected\n");
            return sb.toString().trim();
        }

        // Group value objects by package (to associate with aggregates)
        Map<String, List<ValueObjectComponent>> vosByPackage = components.valueObjects().stream()
                .collect(Collectors.groupingBy(ValueObjectComponent::packageName));

        // Group identifiers by package
        Map<String, List<IdentifierComponent>> idsByPackage = components.identifiers().stream()
                .collect(Collectors.groupingBy(IdentifierComponent::packageName));

        // Track which VOs/IDs have been rendered
        Set<String> renderedTypes = new HashSet<>();

        // Render each aggregate with its related types
        for (AggregateComponent agg : components.aggregates()) {
            String aggPackage = agg.packageName();

            // Aggregate Root class with inline annotation
            sb.append("    class ").append(agg.name()).append("~AggregateRoot~\n");
            sb.append("    ").append(agg.name()).append(" : +fields ").append(agg.fields()).append("\n");

            // Value Objects in same package
            List<ValueObjectComponent> relatedVOs = vosByPackage.getOrDefault(aggPackage, List.of());
            for (ValueObjectComponent vo : relatedVOs) {
                if (!renderedTypes.contains(vo.name())) {
                    sb.append("    class ").append(vo.name()).append("~ValueObject~\n");
                    renderedTypes.add(vo.name());
                }
            }

            // Identifiers in same package
            List<IdentifierComponent> relatedIds = idsByPackage.getOrDefault(aggPackage, List.of());
            for (IdentifierComponent id : relatedIds) {
                if (!renderedTypes.contains(id.name())) {
                    sb.append("    class ").append(id.name()).append("~Identifier~\n");
                    id.wrappedTypeOpt().ifPresent(wt ->
                            sb.append("    ").append(id.name()).append(" : +wraps ")
                                    .append(simpleTypeName(wt)).append("\n"));
                    renderedTypes.add(id.name());
                }
            }

            // Composition relationships to VOs
            for (ValueObjectComponent vo : relatedVOs) {
                sb.append("    ").append(agg.name()).append(" *-- ").append(vo.name()).append("\n");
            }

            // Composition relationships to IDs
            for (IdentifierComponent id : relatedIds) {
                sb.append("    ").append(agg.name()).append(" *-- ").append(id.name()).append("\n");
            }
        }

        // Inter-aggregate references (dashed lines)
        Set<String> renderedRelationships = new HashSet<>();
        for (Relationship rel : relationships) {
            if ("references".equals(rel.type())) {
                String key = rel.from() + "->" + rel.to();
                String reverseKey = rel.to() + "->" + rel.from();

                // Skip if already rendered or reverse already rendered
                if (renderedRelationships.contains(key) || renderedRelationships.contains(reverseKey)) {
                    continue;
                }

                if (rel.isCycle()) {
                    // Cycle - show bidirectional with warning
                    sb.append("    ")
                            .append(rel.from())
                            .append(" <..> ")
                            .append(rel.to())
                            .append(" : CYCLE\n");
                    renderedRelationships.add(key);
                    renderedRelationships.add(reverseKey);
                } else {
                    sb.append("    ")
                            .append(rel.from())
                            .append(" ..> ")
                            .append(rel.to())
                            .append("\n");
                    renderedRelationships.add(key);
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Extracts simple type name from fully qualified name.
     */
    private String simpleTypeName(String qualifiedName) {
        if (qualifiedName == null) {
            return "unknown";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
