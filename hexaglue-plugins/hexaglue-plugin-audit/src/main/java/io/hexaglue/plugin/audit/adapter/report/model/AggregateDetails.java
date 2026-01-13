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

package io.hexaglue.plugin.audit.adapter.report.model;

import io.hexaglue.spi.audit.AggregateInfo;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Detailed information about an aggregate for audit reports.
 *
 * @param rootName         simple name of the aggregate root
 * @param rootQualifiedName fully qualified name of the aggregate root
 * @param entityCount      number of entities in the aggregate
 * @param valueObjectCount number of value objects in the aggregate
 * @param hasRepository    whether a repository port exists for this aggregate
 * @param repositoryName   simple name of the repository (if exists)
 * @param cohesion         cohesion score (0.0 to 1.0), or -1 if not calculated
 * @param status           overall status (OK, WARNING based on cohesion)
 * @since 3.0.0
 */
public record AggregateDetails(
        String rootName,
        String rootQualifiedName,
        int entityCount,
        int valueObjectCount,
        boolean hasRepository,
        String repositoryName,
        double cohesion,
        Status status) {

    public AggregateDetails {
        Objects.requireNonNull(rootName, "rootName required");
        Objects.requireNonNull(rootQualifiedName, "rootQualifiedName required");
        Objects.requireNonNull(status, "status required");
    }

    /**
     * Status of the aggregate based on its metrics.
     */
    public enum Status {
        /** All metrics are healthy */
        OK,
        /** Some metrics need attention (e.g., low cohesion) */
        WARNING,
        /** Metrics indicate a problem (e.g., very low cohesion, no repository) */
        PROBLEM
    }

    /**
     * Builds a list of AggregateDetails from an ArchitectureQuery.
     *
     * @param architectureQuery the architecture query
     * @return list of aggregate details
     */
    public static List<AggregateDetails> fromQuery(ArchitectureQuery architectureQuery) {
        if (architectureQuery == null) {
            return List.of();
        }

        List<AggregateInfo> aggregates = architectureQuery.findAggregates();
        List<AggregateDetails> details = new ArrayList<>();

        for (AggregateInfo aggregate : aggregates) {
            String rootQualifiedName = aggregate.rootType();
            String rootName = extractSimpleName(rootQualifiedName);
            int entityCount = aggregate.entities().size();
            int voCount = aggregate.valueObjects().size();

            // Get repository
            Optional<String> repoOpt = architectureQuery.findRepositoryForAggregate(rootQualifiedName);
            boolean hasRepository = repoOpt.isPresent();
            String repositoryName = repoOpt.map(AggregateDetails::extractSimpleName).orElse("");

            // Get cohesion
            double cohesion = architectureQuery.calculateAggregateCohesion(rootQualifiedName)
                    .orElse(-1.0);

            // Determine status
            Status status = determineStatus(cohesion, hasRepository);

            details.add(new AggregateDetails(
                    rootName,
                    rootQualifiedName,
                    entityCount,
                    voCount,
                    hasRepository,
                    repositoryName,
                    cohesion,
                    status));
        }

        return details;
    }

    private static Status determineStatus(double cohesion, boolean hasRepository) {
        // Problem if no repository
        if (!hasRepository) {
            return Status.PROBLEM;
        }
        // Warning if cohesion is below 0.7
        if (cohesion >= 0 && cohesion < 0.7) {
            return Status.WARNING;
        }
        return Status.OK;
    }

    private static String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Returns an empty list placeholder.
     */
    public static List<AggregateDetails> empty() {
        return List.of();
    }
}
