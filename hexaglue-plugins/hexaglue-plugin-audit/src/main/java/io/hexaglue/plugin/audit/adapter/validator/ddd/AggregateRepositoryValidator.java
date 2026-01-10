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

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.RoleClassification;
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

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Find all aggregate roots
        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        // Find all repositories (ports with REPOSITORY role)
        List<CodeUnit> repositories = codebase.unitsWithRole(RoleClassification.REPOSITORY);

        for (CodeUnit aggregate : aggregates) {
            // Check if there's a repository for this aggregate
            boolean hasRepository = repositories.stream().anyMatch(repo -> isRepositoryFor(repo, aggregate));

            if (!hasRepository) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.MAJOR)
                        .message("Aggregate root '%s' has no repository interface".formatted(aggregate.simpleName()))
                        .affectedType(aggregate.qualifiedName())
                        .location(SourceLocation.of(aggregate.qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Aggregate roots should be managed by repositories", aggregate.qualifiedName()))
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks if the repository is for the given aggregate.
     *
     * <p>This uses naming conventions and dependency analysis:
     * <ul>
     *   <li>Repository name matches pattern "{AggregateName}Repository"</li>
     *   <li>Repository has methods returning or accepting the aggregate type</li>
     * </ul>
     *
     * @param repository the repository code unit
     * @param aggregate  the aggregate code unit
     * @return true if the repository is for this aggregate
     */
    private boolean isRepositoryFor(CodeUnit repository, CodeUnit aggregate) {
        // Check naming convention
        String expectedRepoName = aggregate.simpleName() + "Repository";
        if (repository.simpleName().equals(expectedRepoName)) {
            return true;
        }

        // Check if repository methods reference the aggregate
        // (This is a simplified check - in practice would need type analysis)
        return repository.methods().stream()
                .anyMatch(method ->
                        // Check return type or parameters contain aggregate name
                        method.returnType().contains(aggregate.simpleName())
                                || method.parameterTypes().stream()
                                        .anyMatch(paramType -> paramType.contains(aggregate.simpleName())));
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
