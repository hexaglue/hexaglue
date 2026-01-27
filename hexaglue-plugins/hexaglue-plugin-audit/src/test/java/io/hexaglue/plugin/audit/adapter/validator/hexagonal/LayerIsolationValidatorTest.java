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
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LayerIsolationValidator}.
 *
 * <p>Validates that layers respect isolation rules using the v5 ArchType API:
 * <ul>
 *   <li>Domain → Domain only</li>
 *   <li>Application → Domain and Ports only</li>
 *   <li>Ports → Domain only</li>
 * </ul>
 *
 * <p><strong>Note:</strong> In v5 API, the validator uses ArchKind to determine layers.
 * Infrastructure types are not part of the classified model, so infrastructure layer
 * violations are only detectable when dependencies point to external types.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class LayerIsolationValidatorTest {

    private static final String DOMAIN_PACKAGE = "com.example.domain";
    private static final String APP_PACKAGE = "com.example.application";
    private static final String PORT_PACKAGE = "com.example.domain.port";

    private LayerIsolationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LayerIsolationValidator();
    }

    @Test
    @DisplayName("Should pass when domain depends only on domain")
    void shouldPass_whenDomainDependsOnlyOnDomain() {
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
    @DisplayName("Should fail when domain depends on application")
    void shouldFail_whenDomainDependsOnApplication() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", APP_PACKAGE + ".OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:layer-isolation");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message())
                .contains("DOMAIN")
                .contains("Order")
                .contains("APPLICATION")
                .contains("not allowed");
    }

    @Test
    @DisplayName("Should fail when domain depends on port")
    void shouldFail_whenDomainDependsOnPort() {
        // Given - Domain should not depend on ports
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", PORT_PACKAGE + ".OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("DOMAIN").contains("PORT");
    }

    @Test
    @DisplayName("Should detect multiple layer violations")
    void shouldDetectMultipleLayerViolations() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addApplicationService(APP_PACKAGE + ".Service1")
                .addApplicationService(APP_PACKAGE + ".Service2")
                .addDrivenPort(PORT_PACKAGE + ".Repo", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", APP_PACKAGE + ".Service1")
                .addDependency(DOMAIN_PACKAGE + ".Order", PORT_PACKAGE + ".Repo")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("Should pass when codebase has no dependencies")
    void shouldPass_whenNoDependencies() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addApplicationService(APP_PACKAGE + ".Service")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide dependency evidence")
    void shouldProvideDependencyEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", APP_PACKAGE + ".OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description()).contains("Layer DOMAIN can only depend on");
    }

    @Test
    @DisplayName("Should pass when model has no type registry")
    void shouldPass_whenNoTypeRegistry() {
        // Given - empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
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
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:layer-isolation");
    }

    @Test
    @DisplayName("Should ignore external dependencies")
    void shouldIgnoreExternalDependencies() {
        // Given - Domain depends on external library (not in model)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", "java.util.List")
                .addDependency(DOMAIN_PACKAGE + ".Order", "org.slf4j.Logger")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - external deps are ignored
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should allow port to depend on domain")
    void shouldAllowPortToDependOnDomain() {
        // Given - Port using domain types (for parameters/return types)
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(PORT_PACKAGE + ".OrderRepository", DOMAIN_PACKAGE + ".Order")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when port depends on application")
    void shouldFail_whenPortDependsOnApplication() {
        // Given - Port should not depend on application
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(PORT_PACKAGE + ".OrderRepository", APP_PACKAGE + ".OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("PORT").contains("APPLICATION");
    }
}
