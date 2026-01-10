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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.applicationClass;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.domainClass;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DependencyInversionValidator}.
 *
 * <p>Validates that the Dependency Inversion Principle is correctly enforced:
 * application components should depend on abstractions (interfaces) rather than
 * concrete infrastructure implementations.
 */
class DependencyInversionValidatorTest {

    private DependencyInversionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DependencyInversionValidator();
    }

    @Test
    @DisplayName("Should pass when application depends on infrastructure interface")
    void shouldPass_whenApplicationDependsOnInfrastructureInterface() {
        // Given: Application service depends on infrastructure interface (correct)
        CodeUnit infraInterface = infraInterface("OrderRepositoryPort");
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .addUnit(infraInterface)
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.OrderRepositoryPort")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when application depends on concrete infrastructure class")
    void shouldFail_whenApplicationDependsOnConcreteInfrastructureClass() {
        // Given: Application service depends on concrete infrastructure class
        CodeUnit concreteInfra = infraClass("JpaOrderRepository");
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .addUnit(concreteInfra)
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.JpaOrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:dependency-inversion");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message())
                .contains("OrderService")
                .contains("JpaOrderRepository")
                .contains("concrete Infrastructure")
                .contains("abstraction/interface");
    }

    @Test
    @DisplayName("Should fail when application depends on infrastructure enum")
    void shouldFail_whenApplicationDependsOnInfrastructureEnum() {
        // Given: Application service depends on infrastructure enum
        CodeUnit infraEnum = infraEnum("DatabaseType");
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("ConnectionManager"))
                .addUnit(infraEnum)
                .addDependency("com.example.application.ConnectionManager", "com.example.infrastructure.DatabaseType")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:dependency-inversion");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("DatabaseType").contains("concrete");
    }

    @Test
    @DisplayName("Should fail when application depends on infrastructure record")
    void shouldFail_whenApplicationDependsOnInfrastructureRecord() {
        // Given: Application service depends on infrastructure record
        CodeUnit infraRecord = infraRecord("JpaConfiguration");
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("DatabaseService"))
                .addUnit(infraRecord)
                .addDependency("com.example.application.DatabaseService", "com.example.infrastructure.JpaConfiguration")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:dependency-inversion");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("JpaConfiguration").contains("concrete");
    }

    @Test
    @DisplayName("Should pass when application has no dependencies")
    void shouldPass_whenApplicationHasNoDependencies() {
        // Given: Application service with no dependencies
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application depends on domain")
    void shouldPass_whenApplicationDependsOnDomain() {
        // Given: Application depends on domain (allowed)
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .addUnit(domainClass("Order"))
                .addDependency("com.example.application.OrderService", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should detect multiple violations in single unit")
    void shouldDetectMultipleViolationsInSingleUnit() {
        // Given: Application service depends on multiple concrete infrastructure classes
        CodeUnit service = applicationClass("OrderService");
        CodeUnit concreteRepo = infraClass("JpaOrderRepository");
        CodeUnit concreteAdapter = infraClass("StripePaymentAdapter");
        CodeUnit concreteClient = infraClass("EmailClientImpl");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(service)
                .addUnit(concreteRepo)
                .addUnit(concreteAdapter)
                .addUnit(concreteClient)
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.JpaOrderRepository")
                .addDependency(
                        "com.example.application.OrderService", "com.example.infrastructure.StripePaymentAdapter")
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.EmailClientImpl")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(3);
        assertThat(violations).allMatch(v -> v.constraintId().value().equals("hexagonal:dependency-inversion"));
        assertThat(violations).allMatch(v -> v.severity() == Severity.CRITICAL);
        assertThat(violations).allMatch(v -> v.affectedTypes().get(0).equals("com.example.application.OrderService"));
    }

    @Test
    @DisplayName("Should detect violations across multiple units")
    void shouldDetectViolationsAcrossMultipleUnits() {
        // Given: Multiple application services with violations
        CodeUnit service1 = applicationClass("OrderService");
        CodeUnit service2 = applicationClass("PaymentService");
        CodeUnit concreteRepo = infraClass("JpaOrderRepository");
        CodeUnit concreteAdapter = infraClass("StripeAdapter");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(service1)
                .addUnit(service2)
                .addUnit(concreteRepo)
                .addUnit(concreteAdapter)
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.JpaOrderRepository")
                .addDependency("com.example.application.PaymentService", "com.example.infrastructure.StripeAdapter")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .flatExtracting(v -> v.affectedTypes())
                .containsExactlyInAnyOrder(
                        "com.example.application.OrderService", "com.example.application.PaymentService");
    }

    @Test
    @DisplayName("Should pass when mixing interface and concrete dependencies correctly")
    void shouldPass_whenMixingInterfaceAndConcreteDependenciesCorrectly() {
        // Given: Application depends on infrastructure interface + domain concrete (both allowed)
        CodeUnit service = applicationClass("OrderService");
        CodeUnit infraInterface = infraInterface("OrderRepository");
        CodeUnit domainEntity = domainClass("Order");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(service)
                .addUnit(infraInterface)
                .addUnit(domainEntity)
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.OrderRepository")
                .addDependency("com.example.application.OrderService", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail only for concrete infrastructure dependencies")
    void shouldFail_onlyForConcreteInfrastructureDependencies() {
        // Given: Mix of valid and invalid dependencies
        CodeUnit service = applicationClass("OrderService");
        CodeUnit infraInterface = infraInterface("OrderRepositoryPort");
        CodeUnit infraConcrete = infraClass("JpaOrderRepository");
        CodeUnit domainEntity = domainClass("Order");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(service)
                .addUnit(infraInterface)
                .addUnit(infraConcrete)
                .addUnit(domainEntity)
                .addDependency(
                        "com.example.application.OrderService", "com.example.infrastructure.OrderRepositoryPort") // OK
                .addDependency(
                        "com.example.application.OrderService",
                        "com.example.infrastructure.JpaOrderRepository") // VIOLATION
                .addDependency("com.example.application.OrderService", "com.example.domain.Order") // OK
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("JpaOrderRepository");
    }

    @Test
    @DisplayName("Should ignore domain and infrastructure inter-layer dependencies")
    void shouldIgnore_domainAndInfrastructureInterLayerDependencies() {
        // Given: Domain and infrastructure have their own dependencies (not checked by this validator)
        CodeUnit domainEntity = domainClass("Order");
        CodeUnit infraClass = infraClass("JpaOrderRepository");
        CodeUnit anotherInfraClass = infraClass("HibernateConfig");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainEntity)
                .addUnit(infraClass)
                .addUnit(anotherInfraClass)
                .addDependency("com.example.domain.Order", "com.example.infrastructure.JpaOrderRepository") // Not
                // checked
                .addDependency(
                        "com.example.infrastructure.JpaOrderRepository",
                        "com.example.infrastructure.HibernateConfig") // Not checked
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide dependency evidence with details")
    void shouldProvideDependencyEvidenceWithDetails() {
        // Given
        CodeUnit service = applicationClass("OrderService");
        CodeUnit concreteInfra = infraClass("JpaOrderRepository");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(service)
                .addUnit(concreteInfra)
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.JpaOrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0))
                .extracting(e -> e.description())
                .asString()
                .contains("Dependency Inversion Principle")
                .contains("CLASS");
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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:dependency-inversion");
    }

    @Test
    @DisplayName("Should pass when no application layer units exist")
    void shouldPass_whenNoApplicationLayerUnitsExist() {
        // Given: Only domain and infrastructure, no application
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(infraClass("JpaOrderRepository"))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty codebase gracefully")
    void shouldHandleEmptyCodebaseGracefully() {
        // Given: Empty codebase
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    // === Helper Methods ===

    /**
     * Creates an infrastructure interface code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit representing an infrastructure interface
     */
    private static CodeUnit infraInterface(String simpleName) {
        String qualifiedName = "com.example.infrastructure." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.INTERFACE,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates an infrastructure class code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit representing an infrastructure class
     */
    private static CodeUnit infraClass(String simpleName) {
        String qualifiedName = "com.example.infrastructure." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates an infrastructure enum code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit representing an infrastructure enum
     */
    private static CodeUnit infraEnum(String simpleName) {
        String qualifiedName = "com.example.infrastructure." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.ENUM,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates an infrastructure record code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit representing an infrastructure record
     */
    private static CodeUnit infraRecord(String simpleName) {
        String qualifiedName = "com.example.infrastructure." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.RECORD,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    private static CodeMetrics defaultMetrics() {
        return new CodeMetrics(50, 5, 3, 2, 80.0);
    }

    private static DocumentationInfo defaultDocumentation() {
        return new DocumentationInfo(true, 100, List.of());
    }
}
