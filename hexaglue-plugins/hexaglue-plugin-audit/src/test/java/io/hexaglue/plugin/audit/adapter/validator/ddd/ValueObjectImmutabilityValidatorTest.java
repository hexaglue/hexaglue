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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.BehavioralEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValueObjectImmutabilityValidator}.
 *
 * <p>Validates that value objects are correctly checked for immutability (no setters)
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class ValueObjectImmutabilityValidatorTest {

    private ValueObjectImmutabilityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValueObjectImmutabilityValidator();
    }

    @Test
    @DisplayName("Should pass when value object has no setters")
    void shouldPass_whenValueObjectHasNoSetters() {
        // Given - immutable value object
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Money", false)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when value object has setter")
    void shouldFail_whenValueObjectHasSetter() {
        // Given - value object with setter
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Money", true)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:value-object-immutable");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message())
                .contains("Money")
                .contains("setter method")
                .contains("immutability");
    }

    @Test
    @DisplayName("Should check all value objects")
    void shouldCheckAllValueObjects() {
        // Given - multiple value objects, one with setter
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Money", false) // Valid
                .addValueObject("com.example.domain.Address", true) // Invalid - has setter
                .addValueObject("com.example.domain.Email", false) // Valid
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("Address");
    }

    @Test
    @DisplayName("Should pass when codebase has no value objects")
    void shouldPass_whenNoValueObjects() {
        // Given - empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide behavioral evidence")
    void shouldProvideBehavioralEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Money", true)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0)).isInstanceOf(BehavioralEvidence.class);

        BehavioralEvidence evidence =
                (BehavioralEvidence) violations.get(0).evidence().get(0);
        assertThat(evidence.description()).contains("Setter method detected");
        assertThat(evidence.methodName()).isEqualTo("setValue");
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
        assertThat(validator.constraintId().value()).isEqualTo("ddd:value-object-immutable");
    }
}
