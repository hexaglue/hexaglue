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
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.entity;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.withUnits;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EntityIdentityValidator}.
 *
 * <p>Validates that entities and aggregate roots are correctly checked for identity fields.
 */
class EntityIdentityValidatorTest {

    private EntityIdentityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EntityIdentityValidator();
    }

    @Test
    @DisplayName("Should pass when entity has identity field")
    void shouldPass_whenEntityHasIdentity() {
        // Given
        Codebase codebase = withUnits(entity("Order", true));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when entity is missing identity field")
    void shouldFail_whenEntityMissingIdentity() {
        // Given
        Codebase codebase = withUnits(entity("Order", false));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:entity-identity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("Order").contains("no identity field");
        assertThat(violations.get(0).affectedTypes()).contains("com.example.domain.Order");
    }

    @Test
    @DisplayName("Should pass when aggregate has identity field")
    void shouldPass_whenAggregateHasIdentity() {
        // Given
        Codebase codebase = withUnits(aggregate("Customer", true));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when aggregate is missing identity field")
    void shouldFail_whenAggregateMissingIdentity() {
        // Given
        Codebase codebase = withUnits(aggregate("Customer", false));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:entity-identity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("Customer").contains("no identity field");
    }

    @Test
    @DisplayName("Should check both entities and aggregates")
    void shouldCheckBothEntitiesAndAggregates() {
        // Given: Two entities - one valid, one invalid
        Codebase codebase = withUnits(
                entity("Order", true), // Valid - has ID
                entity("LineItem", false), // Invalid - no ID
                aggregate("Customer", true), // Valid - has ID
                aggregate("Address", false) // Invalid - no ID
                );

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should find 2 violations
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(v -> v.message())
                .anyMatch(msg -> msg.contains("LineItem"))
                .anyMatch(msg -> msg.contains("Address"));
    }

    @Test
    @DisplayName("Should pass when codebase has no entities")
    void shouldPass_whenNoEntities() {
        // Given: Empty codebase
        Codebase codebase = withUnits();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide structural evidence")
    void shouldProvideStructuralEvidence() {
        // Given
        Codebase codebase = withUnits(entity("Order", false));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("identity")
                .contains("distinguish instances");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:entity-identity");
    }
}
