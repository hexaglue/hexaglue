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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.arch.model.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortInterfaceValidator}.
 *
 * <p>Validates that ports are correctly checked to be interfaces, not classes,
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class PortInterfaceValidatorTest {

    private static final String BASE_PACKAGE = "com.example.domain.port";

    private PortInterfaceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortInterfaceValidator();
    }

    @Test
    @DisplayName("Should pass when port is interface")
    void shouldPass_whenPortIsInterface() {
        // Given - driving port as interface (default)
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(BASE_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when port is class")
    void shouldFail_whenPortIsClass() {
        // Given - driving port as class (invalid)
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPortWithNature(BASE_PACKAGE + ".OrderService", TypeNature.CLASS)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        // Given - mix of valid and invalid ports
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(BASE_PACKAGE + ".ValidPort") // Valid - interface
                .addDrivingPortWithNature(BASE_PACKAGE + ".InvalidPort1", TypeNature.CLASS) // Invalid - class
                .addDrivenPort(BASE_PACKAGE + ".AnotherValidPort", DrivenPortType.GATEWAY) // Valid - interface
                .addDrivenPortWithNature(
                        BASE_PACKAGE + ".InvalidPort2", DrivenPortType.OTHER, TypeNature.ENUM) // Invalid - enum
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        // Given - empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide structural evidence")
    void shouldProvideStructuralEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPortWithNature(BASE_PACKAGE + ".OrderService", TypeNature.CLASS)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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

    @Test
    @DisplayName("Should check driven ports as well")
    void shouldCheckDrivenPorts() {
        // Given - driven port as class (invalid)
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPortWithNature(BASE_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY, TypeNature.CLASS)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderRepository");
    }
}
