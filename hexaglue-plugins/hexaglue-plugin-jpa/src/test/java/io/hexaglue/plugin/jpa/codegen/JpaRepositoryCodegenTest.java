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

package io.hexaglue.plugin.jpa.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaRepositoryCodegen}.
 *
 * <p>These tests validate the repository interface generation logic using
 * JavaPoet and verify that the generated code matches Spring Data JPA conventions.
 *
 * @since 2.0.0
 */
class JpaRepositoryCodegenTest {

    private static final String TEST_PACKAGE = "com.example.infrastructure.jpa";
    private static final String REPOSITORY_NAME = "OrderRepository";
    private static final ClassName ENTITY_TYPE = ClassName.get(TEST_PACKAGE, "OrderEntity");
    private static final ClassName ID_TYPE = ClassName.get(UUID.class);
    private static final String DOMAIN_NAME = "com.example.domain.Order";

    @Test
    void generate_shouldCreateRepositoryInterface() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        TypeSpec repositoryInterface = JpaRepositoryCodegen.generate(spec);
        String generatedCode = repositoryInterface.toString();

        // Then
        assertThat(repositoryInterface).isNotNull();
        assertThat(generatedCode).contains("interface " + REPOSITORY_NAME);
    }

    @Test
    void generate_shouldHaveGeneratedAnnotation() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        TypeSpec repositoryInterface = JpaRepositoryCodegen.generate(spec);
        String generatedCode = repositoryInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("@javax.annotation.processing.Generated")
                .contains("io.hexaglue.plugin.jpa");
    }

    @Test
    void generate_shouldHaveRepositoryAnnotation() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        TypeSpec repositoryInterface = JpaRepositoryCodegen.generate(spec);
        String generatedCode = repositoryInterface.toString();

        // Then
        assertThat(generatedCode).contains("@org.springframework.stereotype.Repository");
    }

    @Test
    void generate_shouldExtendJpaRepository() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        TypeSpec repositoryInterface = JpaRepositoryCodegen.generate(spec);
        String generatedCode = repositoryInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("extends org.springframework.data.jpa.repository.JpaRepository<")
                .contains("com.example.infrastructure.jpa.OrderEntity")
                .contains("java.util.UUID");
    }

    @Test
    void generate_shouldBePublicInterface() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        TypeSpec repositoryInterface = JpaRepositoryCodegen.generate(spec);
        String generatedCode = repositoryInterface.toString();

        // Then
        assertThat(generatedCode).contains("public interface");
    }

    @Test
    void generate_shouldIncludeJavadoc() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        TypeSpec repositoryInterface = JpaRepositoryCodegen.generate(spec);
        String generatedCode = repositoryInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("Spring Data JPA repository")
                .contains("save(entity)")
                .contains("findById(id)");
    }

    @Test
    void generate_shouldHandleNullSpec() {
        // When/Then
        assertThatThrownBy(() -> JpaRepositoryCodegen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RepositorySpec cannot be null");
    }

    @Test
    void generateFile_shouldCreateJavaFile() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        JavaFile javaFile = JpaRepositoryCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(javaFile).isNotNull();
        assertThat(generatedCode).startsWith("package " + TEST_PACKAGE + ";").contains("interface " + REPOSITORY_NAME);
    }

    @Test
    void generateFile_shouldHaveCorrectPackage() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        JavaFile javaFile = JpaRepositoryCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(generatedCode).startsWith("package " + TEST_PACKAGE + ";");
    }

    @Test
    void generateFile_shouldIncludeNecessaryImports() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        JavaFile javaFile = JpaRepositoryCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(generatedCode)
                .contains("import java.util.UUID")
                .contains("import javax.annotation.processing.Generated")
                .contains("import org.springframework.data.jpa.repository.JpaRepository")
                .contains("import org.springframework.stereotype.Repository");
    }

    @Test
    void generateFile_shouldHandleNullSpec() {
        // When/Then
        assertThatThrownBy(() -> JpaRepositoryCodegen.generateFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RepositorySpec cannot be null");
    }

    @Test
    void generateFile_shouldProduceValidCode() {
        // Given
        RepositorySpec spec =
                new RepositorySpec(TEST_PACKAGE, REPOSITORY_NAME, ENTITY_TYPE, ID_TYPE, DOMAIN_NAME, List.of());

        // When
        JavaFile javaFile = JpaRepositoryCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then - Verify the structure of the generated code
        assertThat(generatedCode)
                .contains("package " + TEST_PACKAGE + ";")
                .contains("public interface " + REPOSITORY_NAME)
                .contains("extends JpaRepository<OrderEntity, UUID>")
                .contains("@Generated")
                .contains("@Repository");
    }
}
