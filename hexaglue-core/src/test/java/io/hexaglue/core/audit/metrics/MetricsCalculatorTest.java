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

package io.hexaglue.core.audit.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.audit.CodeMetrics;
import io.hexaglue.arch.model.audit.CodeUnit;
import io.hexaglue.arch.model.audit.CodeUnitKind;
import io.hexaglue.arch.model.audit.DocumentationInfo;
import io.hexaglue.arch.model.audit.FieldDeclaration;
import io.hexaglue.arch.model.audit.LayerClassification;
import io.hexaglue.arch.model.audit.MethodDeclaration;
import io.hexaglue.arch.model.audit.RoleClassification;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MetricsCalculator}.
 */
class MetricsCalculatorTest {

    private MetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MetricsCalculator();
    }

    @Test
    @DisplayName("should calculate code size metrics correctly")
    void shouldCalculateCodeSizeMetrics() {
        List<CodeUnit> units = List.of(
                createUnit("Order", 100, 10, 5), createUnit("Customer", 80, 8, 3), createUnit("Product", 60, 6, 4));

        CodeSizeMetrics metrics = calculator.calculateCodeSize(units);

        assertThat(metrics.typeCount()).isEqualTo(3);
        assertThat(metrics.methodCount()).isEqualTo(24);
        assertThat(metrics.fieldCount()).isEqualTo(12);
        assertThat(metrics.locTotal()).isEqualTo(240);
        assertThat(metrics.avgMethodsPerType()).isEqualTo(8.0);
        assertThat(metrics.avgLocPerType()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("should handle empty list for code size")
    void shouldHandleEmptyListForCodeSize() {
        CodeSizeMetrics metrics = calculator.calculateCodeSize(List.of());

        assertThat(metrics.typeCount()).isEqualTo(0);
        assertThat(metrics.methodCount()).isEqualTo(0);
        assertThat(metrics.fieldCount()).isEqualTo(0);
        assertThat(metrics.locTotal()).isEqualTo(0);
    }

    @Test
    @DisplayName("should calculate complexity metrics correctly")
    void shouldCalculateComplexityMetrics() {
        List<CodeUnit> units = List.of(
                createUnitWithComplexity("Order", 20, 3),
                createUnitWithComplexity("Customer", 15, 2),
                createUnitWithComplexity("Product", 10, 1));

        ComplexityMetrics metrics = calculator.calculateComplexity(units);

        assertThat(metrics.maxCyclomaticComplexity()).isEqualTo(20);
        assertThat(metrics.avgCyclomaticComplexity()).isCloseTo(7.5, within(0.1));
        assertThat(metrics.methodsAboveThreshold()).isGreaterThan(0);
    }

    @Test
    @DisplayName("should calculate documentation metrics correctly")
    void shouldCalculateDocumentationMetrics() {
        List<CodeUnit> units = List.of(
                createUnitWithDocumentation("Order", true, 90),
                createUnitWithDocumentation("Customer", true, 80),
                createUnitWithDocumentation("Product", false, 60));

        DocumentationMetrics metrics = calculator.calculateDocumentation(units);

        assertThat(metrics.documentedTypesRatio()).isCloseTo(0.666, within(0.01));
        assertThat(metrics.documentedPublicMethodsRatio()).isCloseTo(0.766, within(0.01));
        assertThat(metrics.typesCoveragePercent()).isCloseTo(66.6, within(0.1));
    }

    @Test
    @DisplayName("should calculate all metrics at once")
    void shouldCalculateAllMetrics() {
        List<CodeUnit> units = List.of(createUnit("Order", 100, 10, 5), createUnit("Customer", 80, 8, 3));

        MetricsCalculator.AllMetrics allMetrics = calculator.calculateAll(units);

        assertThat(allMetrics.codeSize().typeCount()).isEqualTo(2);
        assertThat(allMetrics.complexity().maxCyclomaticComplexity()).isGreaterThan(0);
        assertThat(allMetrics.documentation()).isNotNull();
    }

    // Helper methods

    private CodeUnit createUnit(String name, int loc, int methods, int fields) {
        return new CodeUnit(
                "com.example." + name,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                createMethods(methods),
                createFields(fields),
                new CodeMetrics(loc, 5, methods, fields, 80.0),
                new DocumentationInfo(true, 80, List.of()));
    }

    private CodeUnit createUnitWithComplexity(String name, int complexity, int methods) {
        return new CodeUnit(
                "com.example." + name,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                createComplexMethods(methods, complexity),
                List.of(),
                new CodeMetrics(100, complexity, methods, 0, 80.0),
                new DocumentationInfo(true, 80, List.of()));
    }

    private CodeUnit createUnitWithDocumentation(String name, boolean hasJavadoc, int coverage) {
        return new CodeUnit(
                "com.example." + name,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                List.of(),
                List.of(),
                new CodeMetrics(100, 5, 10, 0, 80.0),
                new DocumentationInfo(hasJavadoc, coverage, List.of()));
    }

    private List<MethodDeclaration> createMethods(int count) {
        return List.of(new MethodDeclaration("method1", "void", List.of(), Set.of("public"), Set.of(), 3));
    }

    private List<MethodDeclaration> createComplexMethods(int count, int complexity) {
        return List.of(
                new MethodDeclaration("complexMethod", "void", List.of(), Set.of("public"), Set.of(), complexity));
    }

    private List<FieldDeclaration> createFields(int count) {
        return List.of(new FieldDeclaration("field1", "java.lang.String", Set.of("private"), Set.of()));
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
