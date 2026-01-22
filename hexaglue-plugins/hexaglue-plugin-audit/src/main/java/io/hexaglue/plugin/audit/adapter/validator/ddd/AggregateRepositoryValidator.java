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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that aggregate roots have corresponding repositories.
 *
 * <p>DDD Principle: Aggregates are the consistency boundary and the unit of
 * retrieval. Each aggregate root should have a dedicated repository interface
 * for persistence and retrieval operations.
 *
 * <p>This validator checks that for each aggregate root, there exists a
 * corresponding repository interface (typically following naming conventions
 * like "{AggregateName}Repository").
 *
 * <p><strong>Constraint:</strong> ddd:aggregate-repository<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Repositories are the standard way to access aggregates.
 * Missing repositories may indicate incomplete aggregate design or improper
 * persistence handling.
 *
 * @since 1.0.0
 */
public class AggregateRepositoryValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:aggregate-repository");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates aggregate-repository relationships using the v5 ArchitecturalModel API.
     *
     * @param model the architectural model containing domain types and ports
     * @param codebase the codebase (not used in v5)
     * @param query the architecture query (not used in v5)
     * @return list of violations
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Check if domain index and port index are available
        if (model.domainIndex().isEmpty() || model.portIndex().isEmpty()) {
            return violations; // Cannot validate without indices
        }

        var domainIndex = model.domainIndex().get();
        var portIndex = model.portIndex().get();

        // Find all aggregate roots
        List<AggregateRoot> aggregates = domainIndex.aggregateRoots().toList();

        // Find all repositories
        List<DrivenPort> repositories = portIndex.repositories().toList();

        for (AggregateRoot aggregate : aggregates) {
            // Check if aggregate has a driven port (repository) reference
            boolean hasRepositoryViaModel = aggregate.hasDrivenPort();

            // Also check if there's a repository that manages this aggregate
            boolean hasRepositoryViaPortIndex =
                    repositories.stream().anyMatch(repo -> isRepositoryFor(repo, aggregate));

            if (!hasRepositoryViaModel && !hasRepositoryViaPortIndex) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.MAJOR)
                        .message("Aggregate root '%s' has no repository interface"
                                .formatted(aggregate.id().simpleName()))
                        .affectedType(aggregate.id().qualifiedName())
                        .location(SourceLocation.of(aggregate.id().qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Aggregate roots should be managed by repositories",
                                aggregate.id().qualifiedName()))
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks if the repository is for the given aggregate.
     *
     * <p>This uses the v5 API {@link DrivenPort#managedAggregate()} to determine
     * if the repository manages this aggregate. Fallback to naming conventions
     * if managed aggregate is not available.</p>
     *
     * @param repository the repository driven port
     * @param aggregate the aggregate root
     * @return true if the repository is for this aggregate
     */
    private boolean isRepositoryFor(DrivenPort repository, AggregateRoot aggregate) {
        // Check if repository explicitly manages this aggregate (v5 API)
        if (repository.managedAggregate().isPresent()) {
            String managedQName = repository.managedAggregate().get().qualifiedName();
            return managedQName.equals(aggregate.id().qualifiedName());
        }

        // Fallback: check naming convention
        String expectedRepoName = aggregate.id().simpleName() + "Repository";
        return repository.id().simpleName().equals(expectedRepoName);
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
