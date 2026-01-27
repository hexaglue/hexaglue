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
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.model.ir.MethodKind;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaAdapterCodegen}.
 *
 * <p>These tests validate the JPA adapter generation logic using JavaPoet
 * and verify that the generated code matches Spring component conventions.
 *
 * @since 3.0.0
 */
class JpaAdapterCodegenTest {

    private static final String TEST_PACKAGE = "com.example.infrastructure.jpa";
    private static final String ADAPTER_NAME = "OrderJpaAdapter";
    private static final ClassName PORT_INTERFACE = ClassName.get("com.example.port", "OrderRepository");
    private static final TypeName DOMAIN_TYPE = ClassName.get("com.example.domain", "Order");
    private static final TypeName ENTITY_TYPE = ClassName.get(TEST_PACKAGE, "OrderEntity");
    private static final TypeName REPOSITORY_TYPE = ClassName.get(TEST_PACKAGE, "OrderJpaRepository");
    private static final TypeName MAPPER_TYPE = ClassName.get(TEST_PACKAGE, "OrderMapper");

    @Test
    void generate_shouldCreatePublicClass() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode).contains("public class " + ADAPTER_NAME);
    }

    @Test
    void generate_shouldAddGeneratedAnnotation() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode)
                .contains("@javax.annotation.processing.Generated")
                .contains("io.hexaglue.plugin.jpa.JpaPlugin");
    }

    @Test
    void generate_shouldAddComponentAnnotation() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode).contains("@org.springframework.stereotype.Component");
    }

    @Test
    void generate_shouldImplementPortInterface() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then: The interface name may be short or fully qualified depending on context
        assertThat(generatedCode)
                .containsAnyOf("implements OrderRepository", "implements com.example.port.OrderRepository");
    }

    @Test
    void generate_shouldAddRepositoryField() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode).contains("private final").contains("OrderJpaRepository repository");
    }

    @Test
    void generate_shouldAddMapperField() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode).contains("private final").contains("OrderMapper mapper");
    }

    @Test
    void generate_shouldAddConstructorWithInjection() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode)
                .contains("public " + ADAPTER_NAME + "(")
                .contains("repository,")
                .contains("mapper)")
                .contains("this.repository = repository")
                .contains("this.mapper = mapper");
    }

    @Test
    void generate_shouldGenerateMethodsWithOverride() {
        // Given
        AdapterMethodSpec saveMethod = AdapterMethodSpec.of(
                "save",
                DOMAIN_TYPE,
                List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(saveMethod),
                null,
                true);

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then
        assertThat(generatedCode)
                .contains("@java.lang.Override")
                .containsAnyOf("public Order save(", "public com.example.domain.Order save(");
    }

    @Test
    void generate_shouldHandleMultipleMethods() {
        // Given
        AdapterMethodSpec saveMethod = AdapterMethodSpec.of(
                "save",
                DOMAIN_TYPE,
                List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());
        AdapterMethodSpec findByIdMethod = AdapterMethodSpec.of(
                "findById",
                ClassName.get("java.util", "Optional"),
                List.of(new AdapterMethodSpec.ParameterInfo("uuid", TypeName.get(UUID.class), true)),
                MethodKind.FIND_BY_ID,
                Optional.empty());
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(saveMethod, findByIdMethod),
                null,
                true);

        // When
        TypeSpec adapter = JpaAdapterCodegen.generate(spec);
        String generatedCode = adapter.toString();

        // Then: Verify both methods are present
        assertThat(generatedCode).contains("save(").contains("findById(");
    }

    @Test
    void generate_shouldThrowForNullSpec() {
        // When/Then
        assertThatThrownBy(() -> JpaAdapterCodegen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AdapterSpec cannot be null");
    }

    @Test
    void generate_shouldThrowForNullClassName() {
        // Given
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                null,
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(),
                null,
                true);

        // When/Then
        assertThatThrownBy(() -> JpaAdapterCodegen.generate(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("class name cannot be null or empty");
    }

    @Test
    void generate_shouldThrowForEmptyClassName() {
        // Given
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                "",
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(),
                null,
                true);

        // When/Then
        assertThatThrownBy(() -> JpaAdapterCodegen.generate(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("class name cannot be null or empty");
    }

    @Test
    void generate_shouldThrowForNullImplementedPorts() {
        // Given
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                null,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(),
                null,
                true);

        // When/Then
        assertThatThrownBy(() -> JpaAdapterCodegen.generate(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must implement at least one port");
    }

    @Test
    void generate_shouldThrowForEmptyImplementedPorts() {
        // Given
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                List.of(),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(),
                null,
                true);

        // When/Then
        assertThatThrownBy(() -> JpaAdapterCodegen.generate(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must implement at least one port");
    }

    @Test
    void generateFile_shouldCreateJavaFile() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        JavaFile javaFile = JpaAdapterCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(javaFile).isNotNull();
        assertThat(generatedCode).startsWith("package " + TEST_PACKAGE + ";").contains("class " + ADAPTER_NAME);
    }

    @Test
    void generateFile_shouldHaveCorrectPackage() {
        // Given
        AdapterSpec spec = createSimpleAdapterSpec();

        // When
        JavaFile javaFile = JpaAdapterCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(generatedCode).startsWith("package " + TEST_PACKAGE + ";");
    }

    @Test
    void generateFile_shouldIncludeNecessaryImports() {
        // Given
        AdapterMethodSpec saveMethod = AdapterMethodSpec.of(
                "save",
                DOMAIN_TYPE,
                List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(saveMethod),
                null,
                true);

        // When
        JavaFile javaFile = JpaAdapterCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(generatedCode)
                .contains("import javax.annotation.processing.Generated")
                .contains("import org.springframework.stereotype.Component");
    }

    @Test
    void generateFile_shouldProduceValidAdapterStructure() {
        // Given
        AdapterMethodSpec saveMethod = AdapterMethodSpec.of(
                "save",
                DOMAIN_TYPE,
                List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());
        AdapterSpec spec = new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(saveMethod),
                null,
                true);

        // When
        JavaFile javaFile = JpaAdapterCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then - Verify complete adapter structure
        assertThat(generatedCode)
                .contains("package " + TEST_PACKAGE + ";")
                .contains("@Generated")
                .contains("@Component")
                .contains("public class " + ADAPTER_NAME)
                .contains("implements OrderRepository")
                .contains("private final OrderJpaRepository repository")
                .contains("private final OrderMapper mapper")
                .contains("public " + ADAPTER_NAME + "(")
                .contains("@Override")
                .contains("public Order save(");
    }

    /**
     * Helper method to create a simple adapter spec for testing.
     */
    private AdapterSpec createSimpleAdapterSpec() {
        return new AdapterSpec(
                TEST_PACKAGE,
                ADAPTER_NAME,
                List.of(PORT_INTERFACE),
                DOMAIN_TYPE,
                ENTITY_TYPE,
                REPOSITORY_TYPE,
                MAPPER_TYPE,
                List.of(),
                null,
                true); // idInfo can be null for tests
    }
}
