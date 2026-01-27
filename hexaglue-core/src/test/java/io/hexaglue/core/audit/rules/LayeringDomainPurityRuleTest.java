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

package io.hexaglue.core.audit.rules;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.audit.CodeMetrics;
import io.hexaglue.arch.model.audit.CodeUnit;
import io.hexaglue.arch.model.audit.CodeUnitKind;
import io.hexaglue.arch.model.audit.DocumentationInfo;
import io.hexaglue.arch.model.audit.FieldDeclaration;
import io.hexaglue.arch.model.audit.LayerClassification;
import io.hexaglue.arch.model.audit.MethodDeclaration;
import io.hexaglue.arch.model.audit.RoleClassification;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.arch.model.audit.Severity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LayeringDomainPurityRule}.
 */
class LayeringDomainPurityRuleTest {

    private LayeringDomainPurityRule rule;

    @BeforeEach
    void setUp() {
        rule = new LayeringDomainPurityRule();
    }

    @Test
    @DisplayName("should have correct rule metadata")
    void shouldHaveCorrectMetadata() {
        assertThat(rule.id()).isEqualTo("hexaglue.layer.domain-purity");
        assertThat(rule.name()).isEqualTo("Domain Layer Purity");
        assertThat(rule.defaultSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("should pass for domain type with no infrastructure dependencies")
    void shouldPassForPureDomainType() {
        CodeUnit unit = createDomainUnit(
                "com.example.domain.Order",
                List.of(createField("id", "java.lang.Long")),
                List.of(createMethod("getTotal", "java.math.BigDecimal")));

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should detect infrastructure field type in domain")
    void shouldDetectInfrastructureField() {
        CodeUnit unit = createDomainUnit(
                "com.example.domain.Order",
                List.of(createField("repository", "com.example.infrastructure.OrderRepository")),
                List.of());

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).ruleId()).isEqualTo("hexaglue.layer.domain-purity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("infrastructure type");
    }

    @Test
    @DisplayName("should detect JPA annotation on domain field")
    void shouldDetectJpaAnnotationOnField() {
        FieldDeclaration field =
                new FieldDeclaration("id", "java.lang.Long", Set.of("private"), Set.of("jakarta.persistence.Id"));

        CodeUnit unit = createDomainUnit("com.example.domain.Order", List.of(field), List.of());

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("infrastructure annotation");
    }

    @Test
    @DisplayName("should detect infrastructure return type in domain method")
    void shouldDetectInfrastructureReturnType() {
        MethodDeclaration method = new MethodDeclaration(
                "save", "org.springframework.data.repository.CrudRepository", List.of(), Set.of("public"), Set.of(), 5);

        CodeUnit unit = createDomainUnit("com.example.domain.Order", List.of(), List.of(method));

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("returns infrastructure type");
    }

    @Test
    @DisplayName("should detect infrastructure parameter type in domain method")
    void shouldDetectInfrastructureParameter() {
        MethodDeclaration method = new MethodDeclaration(
                "process", "void", List.of("jakarta.persistence.EntityManager"), Set.of("public"), Set.of(), 3);

        CodeUnit unit = createDomainUnit("com.example.domain.Order", List.of(), List.of(method));

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("parameter of infrastructure type");
    }

    @Test
    @DisplayName("should skip non-domain types")
    void shouldSkipNonDomainTypes() {
        CodeUnit unit = new CodeUnit(
                "com.example.presentation.OrderController",
                CodeUnitKind.CLASS,
                LayerClassification.PRESENTATION,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(createField("repository", "com.example.infrastructure.OrderRepository")),
                createMetrics(),
                createDocumentation());

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should allow standard Java types in domain")
    void shouldAllowStandardJavaTypes() {
        CodeUnit unit = createDomainUnit(
                "com.example.domain.Order",
                List.of(createField("items", "java.util.List"), createField("createdAt", "java.time.Instant")),
                List.of(
                        createMethod("getId", "java.lang.Long"),
                        createMethod("calculateTotal", "java.math.BigDecimal")));

        List<RuleViolation> violations = rule.check(unit);

        assertThat(violations).isEmpty();
    }

    // Helper methods

    private CodeUnit createDomainUnit(
            String qualifiedName, List<FieldDeclaration> fields, List<MethodDeclaration> methods) {
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                methods,
                fields,
                createMetrics(),
                createDocumentation());
    }

    private FieldDeclaration createField(String name, String type) {
        return new FieldDeclaration(name, type, Set.of("private"), Set.of());
    }

    private MethodDeclaration createMethod(String name, String returnType) {
        return new MethodDeclaration(name, returnType, List.of(), Set.of("public"), Set.of(), 1);
    }

    private CodeMetrics createMetrics() {
        return new CodeMetrics(100, 5, 10, 5, 85.0);
    }

    private DocumentationInfo createDocumentation() {
        return new DocumentationInfo(true, 80, List.of());
    }
}
