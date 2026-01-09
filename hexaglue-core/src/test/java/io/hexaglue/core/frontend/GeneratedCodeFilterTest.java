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

package io.hexaglue.core.frontend;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.GeneratedCodeFilter.FilterStatistics;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GeneratedCodeFilter}.
 */
class GeneratedCodeFilterTest {

    @Test
    void isUserCode_shouldReturnTrueForRegularTypes() {
        TypeNode type = createType("com.example.Order");

        assertThat(GeneratedCodeFilter.isUserCode(type)).isTrue();
        assertThat(GeneratedCodeFilter.isGenerated(type)).isFalse();
    }

    @Test
    void isGenerated_shouldDetectJavaxGeneratedAnnotation() {
        TypeNode type = createTypeWithAnnotation("com.example.Order", "javax.annotation.Generated");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedAnnotation(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectJakartaGeneratedAnnotation() {
        TypeNode type = createTypeWithAnnotation("com.example.Order", "jakarta.annotation.Generated");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedAnnotation(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectLombokGeneratedAnnotation() {
        TypeNode type = createTypeWithAnnotation("com.example.Order", "lombok.Generated");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedAnnotation(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectGeneratedSourcesPath() {
        TypeNode type = createTypeWithPath("com.example.Order", "/target/generated-sources/annotations/Order.java");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.isInGeneratedSourcePath(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectBuildGeneratedPath() {
        TypeNode type = createTypeWithPath("com.example.Order", "/build/generated/sources/Order.java");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.isInGeneratedSourcePath(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectLombokPackage() {
        TypeNode type = createType("lombok.experimental.UtilityClass");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedPackagePattern(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectMapStructInternalPackage() {
        TypeNode type = createType("org.mapstruct.ap.internal.model.Mapper");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedPackagePattern(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectLombokBuilderSuffix() {
        TypeNode type = createType("com.example.Order$Builder");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedClassNamePattern(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectMapStructMapperSuffix() {
        TypeNode type = createType("com.example.OrderMapperImpl_");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedClassNamePattern(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectJooqRecordSuffix() {
        TypeNode type = createType("com.example.OrderRecord");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedClassNamePattern(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectJooqTableSuffix() {
        TypeNode type = createType("com.example.OrderTable");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedClassNamePattern(type)).isTrue();
    }

    @Test
    void isGenerated_shouldDetectHibernateProxySuffix() {
        TypeNode type = createType("com.example.Order$HibernateProxy$abc123");

        assertThat(GeneratedCodeFilter.isGenerated(type)).isTrue();
        assertThat(GeneratedCodeFilter.hasGeneratedClassNamePattern(type)).isTrue();
    }

    @Test
    void filterOut_shouldRemoveGeneratedTypes() {
        List<TypeNode> types = List.of(
                createType("com.example.Order"),
                createTypeWithAnnotation("com.example.Generated1", "javax.annotation.Generated"),
                createType("com.example.Customer"),
                createType("lombok.experimental.Test"));

        List<TypeNode> filtered = GeneratedCodeFilter.filterOut(types);

        assertThat(filtered).hasSize(2).extracting(TypeNode::simpleName).containsExactly("Order", "Customer");
    }

    @Test
    void filterOut_shouldReturnEmptyListForEmptyInput() {
        List<TypeNode> types = List.of();

        List<TypeNode> filtered = GeneratedCodeFilter.filterOut(types);

        assertThat(filtered).isEmpty();
    }

    @Test
    void filterOut_shouldReturnAllTypesWhenNoneGenerated() {
        List<TypeNode> types = List.of(
                createType("com.example.Order"), createType("com.example.Customer"), createType("com.example.Product"));

        List<TypeNode> filtered = GeneratedCodeFilter.filterOut(types);

        assertThat(filtered).hasSize(3);
    }

    @Test
    void filterOut_shouldReturnEmptyListWhenAllGenerated() {
        List<TypeNode> types = List.of(
                createTypeWithAnnotation("com.example.Gen1", "javax.annotation.Generated"),
                createType("lombok.experimental.Test"),
                createType("com.example.Order$Builder"));

        List<TypeNode> filtered = GeneratedCodeFilter.filterOut(types);

        assertThat(filtered).isEmpty();
    }

    @Test
    void statistics_shouldComputeCorrectly() {
        List<TypeNode> types = List.of(
                createType("com.example.Order"),
                createTypeWithAnnotation("com.example.Gen1", "javax.annotation.Generated"),
                createTypeWithPath("com.example.Gen2", "/target/generated-sources/Gen2.java"),
                createType("lombok.experimental.Test"),
                createType("com.example.Order$Builder"),
                createType("com.example.Customer"));

        FilterStatistics stats = GeneratedCodeFilter.statistics(types);

        assertThat(stats.total()).isEqualTo(6);
        assertThat(stats.user()).isEqualTo(2);
        assertThat(stats.generated()).isEqualTo(4);
        assertThat(stats.byAnnotation()).isEqualTo(1);
        assertThat(stats.byPath()).isEqualTo(1);
        assertThat(stats.byPackage()).isEqualTo(1);
        assertThat(stats.byClassName()).isEqualTo(1);
    }

    @Test
    void statistics_shouldCalculatePercentages() {
        List<TypeNode> types = List.of(
                createType("com.example.Order"),
                createTypeWithAnnotation("com.example.Gen1", "javax.annotation.Generated"),
                createTypeWithAnnotation("com.example.Gen2", "lombok.Generated"),
                createType("com.example.Customer"));

        FilterStatistics stats = GeneratedCodeFilter.statistics(types);

        assertThat(stats.userPercentage()).isEqualTo(50.0);
        assertThat(stats.generatedPercentage()).isEqualTo(50.0);
    }

    @Test
    void statistics_shouldHandleEmptyList() {
        FilterStatistics stats = GeneratedCodeFilter.statistics(List.of());

        assertThat(stats.total()).isZero();
        assertThat(stats.user()).isZero();
        assertThat(stats.generated()).isZero();
        assertThat(stats.userPercentage()).isZero();
        assertThat(stats.generatedPercentage()).isZero();
    }

    @Test
    void statistics_shouldProvideSummary() {
        List<TypeNode> types = List.of(
                createType("com.example.Order"),
                createTypeWithAnnotation("com.example.Gen1", "javax.annotation.Generated"));

        FilterStatistics stats = GeneratedCodeFilter.statistics(types);
        String summary = stats.summary();

        assertThat(summary)
                .contains("total=2")
                .contains("user=1")
                .contains("generated=1")
                .containsAnyOf("50.0%", "50,0%");
    }

    @Test
    void predicates_shouldWorkCorrectly() {
        TypeNode userType = createType("com.example.Order");
        TypeNode generatedType = createTypeWithAnnotation("com.example.Gen", "javax.annotation.Generated");

        assertThat(GeneratedCodeFilter.isUserCode().test(userType)).isTrue();
        assertThat(GeneratedCodeFilter.isUserCode().test(generatedType)).isFalse();

        assertThat(GeneratedCodeFilter.isGenerated().test(userType)).isFalse();
        assertThat(GeneratedCodeFilter.isGenerated().test(generatedType)).isTrue();
    }

    @Test
    void hasGeneratedAnnotation_shouldReturnFalseForNonGeneratedTypes() {
        TypeNode type = createType("com.example.Order");

        assertThat(GeneratedCodeFilter.hasGeneratedAnnotation(type)).isFalse();
    }

    @Test
    void isInGeneratedSourcePath_shouldReturnFalseForRegularPaths() {
        TypeNode type = createTypeWithPath("com.example.Order", "/src/main/java/Order.java");

        assertThat(GeneratedCodeFilter.isInGeneratedSourcePath(type)).isFalse();
    }

    @Test
    void isInGeneratedSourcePath_shouldReturnFalseForTypesWithoutSourceRef() {
        TypeNode type = createType("com.example.Order");

        assertThat(GeneratedCodeFilter.isInGeneratedSourcePath(type)).isFalse();
    }

    @Test
    void hasGeneratedPackagePattern_shouldReturnFalseForUserPackages() {
        TypeNode type = createType("com.example.Order");

        assertThat(GeneratedCodeFilter.hasGeneratedPackagePattern(type)).isFalse();
    }

    @Test
    void hasGeneratedClassNamePattern_shouldReturnFalseForRegularNames() {
        TypeNode type = createType("com.example.Order");

        assertThat(GeneratedCodeFilter.hasGeneratedClassNamePattern(type)).isFalse();
    }

    // === Helper methods ===

    private TypeNode createType(String qualifiedName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .build();
    }

    private TypeNode createTypeWithAnnotation(String qualifiedName, String annotationType) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .annotations(List.of(AnnotationRef.of(annotationType)))
                .build();
    }

    private TypeNode createTypeWithPath(String qualifiedName, String path) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .sourceRef(SourceRef.ofLine(path, 1))
                .build();
    }
}
