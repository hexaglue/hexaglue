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

package io.hexaglue.plugin.audit.adapter.validator.hexagonal;

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.port;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.withUnits;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortInterfaceValidator}.
 *
 * <p>Validates that ports are correctly checked to be interfaces, not classes.
 */
class PortInterfaceValidatorTest {

    private PortInterfaceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortInterfaceValidator();
    }

    @Test
    @DisplayName("Should pass when port is interface")
    void shouldPass_whenPortIsInterface() {
        // Given
        Codebase codebase = withUnits(port("OrderService", CodeUnitKind.INTERFACE));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when port is class")
    void shouldFail_whenPortIsClass() {
        // Given
        Codebase codebase = withUnits(port("OrderService", CodeUnitKind.CLASS));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:port-interface");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message())
                .contains("OrderService")
                .contains("not an interface")
                .contains("CLASS");
    }

    @Test
    @DisplayName("Should check all ports")
    void shouldCheckAllPorts() {
        // Given
        Codebase codebase = withUnits(
                port("ValidPort", CodeUnitKind.INTERFACE), // Valid
                port("InvalidPort1", CodeUnitKind.CLASS), // Invalid
                port("AnotherValidPort", CodeUnitKind.INTERFACE), // Valid
                port("InvalidPort2", CodeUnitKind.ENUM) // Invalid
                );

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(v -> v.message())
                .anyMatch(msg -> msg.contains("InvalidPort1"))
                .anyMatch(msg -> msg.contains("InvalidPort2"));
    }

    @Test
    @DisplayName("Should pass when codebase has no ports")
    void shouldPass_whenNoPorts() {
        // Given
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
        Codebase codebase = withUnits(port("OrderService", CodeUnitKind.CLASS));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("Ports must be interfaces")
                .contains("Dependency Inversion Principle");
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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:port-interface");
    }
}
