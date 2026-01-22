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
 * Tests for {@link DependencyDirectionValidator}.
 *
 * <p>Validates that dependencies flow in the correct direction using the v5 ArchType API.
 * Domain and Application should not depend on Infrastructure.
 *
 * <p><strong>Note:</strong> In v5 API, the validator checks dependencies from domain/application
 * types to types classified as "infrastructure" by ArchKind. Since infrastructure types are not
 * classified in the model (only domain, ports, and application types are), violations to
 * external infrastructure are detected via external dependency patterns.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class DependencyDirectionValidatorTest {

    private static final String DOMAIN_PACKAGE = "com.example.domain";
    private static final String APP_PACKAGE = "com.example.application";
    private static final String PORT_PACKAGE = "com.example.domain.port";

    private DependencyDirectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DependencyDirectionValidator();
    }

    @Test
    @DisplayName("Should pass when domain has no dependencies")
    void shouldPass_whenDomainHasNoDependencies() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when domain depends on domain")
    void shouldPass_whenDomainDependsOnDomain() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addEntity(DOMAIN_PACKAGE + ".Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", DOMAIN_PACKAGE + ".Customer")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application depends on domain")
    void shouldPass_whenApplicationDependsOnDomain() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", DOMAIN_PACKAGE + ".Order")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application depends on ports")
    void shouldPass_whenApplicationDependsOnPorts() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should ignore external dependencies")
    void shouldIgnoreExternalDependencies() {
        // Given - Dependencies to external libraries are not tracked in the model
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", "java.util.List")
                .addDependency(DOMAIN_PACKAGE + ".Order", "org.slf4j.Logger")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - external deps are not in the model so ignored
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when empty model")
    void shouldPass_whenEmptyModel() {
        // Given
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when no dependencies between model types")
    void shouldPass_whenNoDependenciesBetweenModelTypes() {
        // Given - Types exist but no dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.BLOCKER);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:dependency-direction");
    }

    @Test
    @DisplayName("Should handle complex dependency graph")
    void shouldHandleComplexDependencyGraph() {
        // Given - Multiple types with valid dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addEntity(DOMAIN_PACKAGE + ".OrderLine")
                .addValueObject(DOMAIN_PACKAGE + ".Money")
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", DOMAIN_PACKAGE + ".OrderLine")
                .addDependency(DOMAIN_PACKAGE + ".Order", DOMAIN_PACKAGE + ".Money")
                .addDependency(APP_PACKAGE + ".OrderService", DOMAIN_PACKAGE + ".Order")
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - all valid dependencies
        assertThat(violations).isEmpty();
    }
}
