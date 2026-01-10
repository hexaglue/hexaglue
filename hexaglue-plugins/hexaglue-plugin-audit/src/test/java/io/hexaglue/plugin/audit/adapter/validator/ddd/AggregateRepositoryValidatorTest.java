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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.aggregate;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.repository;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateRepositoryValidator}.
 *
 * <p>Validates that aggregates are correctly checked for corresponding repositories.
 */
class AggregateRepositoryValidatorTest {

    private AggregateRepositoryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AggregateRepositoryValidator();
    }

    @Test
    @DisplayName("Should pass when aggregate has repository")
    void shouldPass_whenAggregateHasRepository() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addUnit(repository("Order"))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when aggregate is missing repository")
    void shouldFail_whenAggregateMissingRepository() {
        // Given
        Codebase codebase =
                new TestCodebaseBuilder().addUnit(aggregate("Order")).build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:aggregate-repository");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message()).contains("Order").contains("no repository interface");
    }

    @Test
    @DisplayName("Should check all aggregates")
    void shouldCheckAllAggregates() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addUnit(repository("Order"))
                .addUnit(aggregate("Customer"))
                .addUnit(aggregate("Product")) // Missing repository
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(v -> v.message())
                .anyMatch(msg -> msg.contains("Customer"))
                .anyMatch(msg -> msg.contains("Product"));
    }

    @Test
    @DisplayName("Should pass when codebase has no aggregates")
    void shouldPass_whenNoAggregates() {
        // Given
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide structural evidence")
    void shouldProvideStructuralEvidence() {
        // Given
        Codebase codebase =
                new TestCodebaseBuilder().addUnit(aggregate("Order")).build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("Aggregate roots")
                .contains("repositories");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:aggregate-repository");
    }
}
