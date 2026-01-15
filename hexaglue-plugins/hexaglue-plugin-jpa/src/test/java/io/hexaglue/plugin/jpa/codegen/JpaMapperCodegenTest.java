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
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaMapperCodegen}.
 *
 * <p>These tests validate the mapper interface generation logic using JavaPoet
 * and verify that the generated code matches MapStruct conventions.
 *
 * @since 2.0.0
 */
class JpaMapperCodegenTest {

    private static final String TEST_PACKAGE = "com.example.infrastructure.jpa";
    private static final String MAPPER_NAME = "OrderMapper";
    private static final TypeName DOMAIN_TYPE = ClassName.get("com.example.domain", "Order");
    private static final TypeName ENTITY_TYPE = ClassName.get(TEST_PACKAGE, "OrderEntity");

    @Test
    void generate_shouldCreateMapperInterface() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(mapperInterface).isNotNull();
        assertThat(generatedCode).contains("interface " + MAPPER_NAME);
    }

    @Test
    void generate_shouldHaveGeneratedAnnotation() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("@javax.annotation.processing.Generated")
                .contains("io.hexaglue.plugin.jpa");
    }

    @Test
    void generate_shouldHaveMapperAnnotation() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode).contains("@org.mapstruct.Mapper").contains("componentModel = \"spring\"");
    }

    @Test
    void generate_shouldHaveToEntityMethod() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("toEntity(")
                .contains("domain)")
                .contains("Converts a domain object to a JPA entity");
    }

    @Test
    void generate_shouldHaveToDomainMethod() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("toDomain(")
                .contains("entity)")
                .contains("Converts a JPA entity to a domain object");
    }

    @Test
    void generate_shouldIncludeToEntityMappings() {
        // Given
        List<MappingSpec> toEntityMappings =
                List.of(MappingSpec.direct("id", "orderId.value"), MappingSpec.direct("amount", "totalAmount"));

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                toEntityMappings,
                List.of(),
                null,
                List.of(),
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"id\"")
                .contains("source = \"orderId.value\"")
                .contains("target = \"amount\"")
                .contains("source = \"totalAmount\"");
    }

    @Test
    void generate_shouldIncludeToDomainMappings() {
        // Given
        List<MappingSpec> toDomainMappings =
                List.of(MappingSpec.direct("orderId", "id"), MappingSpec.direct("totalAmount", "amount"));

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                toDomainMappings,
                List.of(),
                null,
                List.of(),
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"orderId\"")
                .contains("source = \"id\"")
                .contains("target = \"totalAmount\"")
                .contains("source = \"amount\"");
    }

    @Test
    void generate_shouldHandleIgnoreMappings() {
        // Given
        List<MappingSpec> mappings = List.of(MappingSpec.ignore("createdAt"));

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, mappings, List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"createdAt\"")
                .contains("ignore = true");
    }

    @Test
    void generate_shouldHandleExpressionMappings() {
        // Given
        List<MappingSpec> mappings = List.of(MappingSpec.expression("createdAt", "java.time.Instant.now()"));

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, mappings, List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"createdAt\"")
                .contains("expression = \"java(java.time.Instant.now())\"");
    }

    @Test
    void generate_shouldBePublicInterface() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode).contains("public interface");
    }

    @Test
    void generate_shouldIncludeJavadoc() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then
        assertThat(generatedCode)
                .contains("MapStruct mapper for converting")
                .contains("bidirectional mapping")
                .contains("toEntity()")
                .contains("toDomain()");
    }

    @Test
    void generate_shouldHandleNullSpec() {
        // When/Then
        assertThatThrownBy(() -> JpaMapperCodegen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MapperSpec cannot be null");
    }

    @Test
    void generateFile_shouldCreateJavaFile() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        JavaFile javaFile = JpaMapperCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(javaFile).isNotNull();
        assertThat(generatedCode).startsWith("package " + TEST_PACKAGE + ";").contains("interface " + MAPPER_NAME);
    }

    @Test
    void generateFile_shouldHaveCorrectPackage() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        JavaFile javaFile = JpaMapperCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(generatedCode).startsWith("package " + TEST_PACKAGE + ";");
    }

    @Test
    void generateFile_shouldIncludeNecessaryImports() {
        // Given
        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE, MAPPER_NAME, DOMAIN_TYPE, ENTITY_TYPE, List.of(), List.of(), null, List.of(), List.of());

        // When
        JavaFile javaFile = JpaMapperCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then
        assertThat(generatedCode)
                .contains("import javax.annotation.processing.Generated")
                .contains("import org.mapstruct.Mapper");
    }

    @Test
    void generateFile_shouldHandleNullSpec() {
        // When/Then
        assertThatThrownBy(() -> JpaMapperCodegen.generateFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MapperSpec cannot be null");
    }

    @Test
    void generateFile_shouldProduceValidCode() {
        // Given
        List<MappingSpec> toEntityMappings = List.of(MappingSpec.direct("id", "orderId.value"));
        List<MappingSpec> toDomainMappings = List.of(MappingSpec.direct("orderId", "id"));

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                toEntityMappings,
                toDomainMappings,
                null,
                List.of(),
                List.of());

        // When
        JavaFile javaFile = JpaMapperCodegen.generateFile(spec);
        String generatedCode = javaFile.toString();

        // Then - Verify the complete structure
        assertThat(generatedCode)
                .contains("package " + TEST_PACKAGE + ";")
                .contains("public interface " + MAPPER_NAME)
                .contains("@Generated")
                .contains("@Mapper")
                .contains("componentModel = \"spring\"")
                .contains("toEntity(")
                .contains("toDomain(")
                .contains("@Mapping")
                .contains("target = \"id\"")
                .contains("source = \"orderId.value\"")
                .contains("target = \"orderId\"")
                .contains("source = \"id\"");
    }

    @Test
    void generate_shouldHandleMixedMappingTypes() {
        // Given - Mix direct, ignore, and expression mappings
        List<MappingSpec> toEntityMappings = List.of(
                MappingSpec.direct("id", "orderId.value"),
                MappingSpec.ignore("version"),
                MappingSpec.expression("createdAt", "java.time.Instant.now()"));

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                toEntityMappings,
                List.of(),
                null,
                List.of(),
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then - All mapping types should be present
        assertThat(generatedCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"id\"")
                .contains("source = \"orderId.value\"")
                .contains("target = \"version\"")
                .contains("ignore = true")
                .contains("target = \"createdAt\"")
                .contains("expression = \"java(java.time.Instant.now())\"");
    }

    // =====================================================================
    // Value Object conversion methods tests
    // =====================================================================

    @Test
    void generate_shouldIncludeValueObjectConversionMethods() {
        // Given - A mapper with a Value Object mapping
        MapperSpec.ValueObjectMappingSpec emailVoSpec =
                new MapperSpec.ValueObjectMappingSpec("com.example.Email", "Email", "java.lang.String", "value", true);

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(emailVoSpec),
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then - Should contain unwrap method: String map(Email)
        assertThat(generatedCode)
                .contains("default java.lang.String map(com.example.Email vo)")
                .contains("return vo != null ? vo.value() : null");

        // And - Should contain wrap method: Email mapToEmail(String)
        assertThat(generatedCode)
                .contains("default com.example.Email mapToEmail(java.lang.String value)")
                .contains("return value != null ? new com.example.Email(value) : null");
    }

    @Test
    void generate_shouldHandleMultipleValueObjects() {
        // Given - A mapper with multiple Value Object mappings
        MapperSpec.ValueObjectMappingSpec emailVoSpec =
                new MapperSpec.ValueObjectMappingSpec("com.example.Email", "Email", "java.lang.String", "value", true);
        MapperSpec.ValueObjectMappingSpec phoneVoSpec =
                new MapperSpec.ValueObjectMappingSpec("com.example.Phone", "Phone", "java.lang.String", "number", true);

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(emailVoSpec, phoneVoSpec),
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then - Should contain methods for both Value Objects
        assertThat(generatedCode)
                .contains("default java.lang.String map(com.example.Email vo)")
                .contains("default com.example.Email mapToEmail(java.lang.String value)")
                .contains("default java.lang.String map(com.example.Phone vo)")
                .contains("default com.example.Phone mapToPhone(java.lang.String value)");
    }

    @Test
    void generate_shouldHandleClassBasedValueObject() {
        // Given - A Value Object that is a class (not a record)
        MapperSpec.ValueObjectMappingSpec classVoSpec = new MapperSpec.ValueObjectMappingSpec(
                "com.example.Money", "Money", "java.math.BigDecimal", "getAmount", false);

        MapperSpec spec = new MapperSpec(
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(classVoSpec),
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then - Should use getAmount() accessor for class-based VO
        assertThat(generatedCode)
                .contains("return vo != null ? vo.getAmount() : null")
                .contains("default com.example.Money mapToMoney(java.math.BigDecimal value)");
    }
}
