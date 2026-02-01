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
import io.hexaglue.plugin.jpa.model.MapperSpec.ChildEntityConversionSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ConversionKind;
import io.hexaglue.plugin.jpa.model.MapperSpec.MappingSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ReconstitutionParameterSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ReconstitutionSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ValueObjectMappingSpec;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                List.of(),
                List.of(),
                null,
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
                List.of(),
                List.of(),
                null,
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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                mappings,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                mappings,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                TEST_PACKAGE,
                MAPPER_NAME,
                DOMAIN_TYPE,
                ENTITY_TYPE,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of());

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
                List.of(),
                List.of(),
                null,
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
                List.of(),
                List.of(),
                null,
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
                List.of(),
                List.of(),
                null,
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
                List.of(),
                List.of(),
                null,
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
                List.of(),
                List.of(),
                null,
                List.of());

        // When
        TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
        String generatedCode = mapperInterface.toString();

        // Then - Should use getAmount() accessor for class-based VO
        assertThat(generatedCode)
                .contains("return vo != null ? vo.getAmount() : null")
                .contains("default com.example.Money mapToMoney(java.math.BigDecimal value)");
    }

    // =====================================================================
    // Reconstitution default method tests (Issue-3)
    // =====================================================================

    @Nested
    @DisplayName("Reconstitution Default Method")
    class ReconstitutionDefaultMethod {

        @Test
        @DisplayName("should generate default method when reconstitution spec is present")
        void should_generateDefaultMethod_whenReconstitutionSpecPresent() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "id", "com.example.domain.CustomerId", "id", ConversionKind.WRAPPED_IDENTITY),
                            new ReconstitutionParameterSpec(
                                    "firstName", "java.lang.String", "firstName", ConversionKind.DIRECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: toDomain should be a default method, not abstract
            assertThat(generatedCode).contains("default");
            assertThat(generatedCode).doesNotContain("abstract com.example.domain.Order toDomain");
        }

        @Test
        @DisplayName("should include null check in generated method")
        void should_includeNullCheck_inGeneratedMethod() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(new ReconstitutionParameterSpec(
                            "firstName", "java.lang.String", "firstName", ConversionKind.DIRECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then
            assertThat(generatedCode).contains("if (entity == null)");
            assertThat(generatedCode).contains("return null");
        }

        @Test
        @DisplayName("should call factory method with correct class name")
        void should_callFactoryMethod_withCorrectClassName() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(new ReconstitutionParameterSpec(
                            "firstName", "java.lang.String", "firstName", ConversionKind.DIRECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should call Customer.reconstitute(...)
            assertThat(generatedCode).contains("Customer.reconstitute(");
        }

        @Test
        @DisplayName("should generate direct getter calls for DIRECT parameters")
        void should_generateDirectGetterCalls_forDirectParameters() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "firstName", "java.lang.String", "firstName", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec(
                                    "phone", "java.lang.String", "phone", ConversionKind.DIRECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then
            assertThat(generatedCode).contains("entity.getFirstName()");
            assertThat(generatedCode).contains("entity.getPhone()");
        }

        @Test
        @DisplayName("should generate map() calls for WRAPPED_IDENTITY parameters")
        void should_generateMapCalls_forWrappedIdentityParameters() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(new ReconstitutionParameterSpec(
                            "id", "com.example.domain.CustomerId", "id", ConversionKind.WRAPPED_IDENTITY)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then
            assertThat(generatedCode).contains("map(entity.getId())");
        }

        @Test
        @DisplayName("should generate mapToXxx() calls for VALUE_OBJECT parameters")
        void should_generateMapToXxxCalls_forValueObjectParameters() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(new ReconstitutionParameterSpec(
                            "email", "com.example.domain.Email", "email", ConversionKind.VALUE_OBJECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then
            assertThat(generatedCode).contains("mapToEmail(entity.getEmail())");
        }

        @Test
        @DisplayName("should generate mapToXxx() calls for foreign key identity parameters")
        void should_generateMapToXxxCalls_forForeignKeyIdentityParameters() {
            // Given: Foreign key identity uses VALUE_OBJECT conversion
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Order",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "id", "com.example.domain.OrderId", "id", ConversionKind.WRAPPED_IDENTITY),
                            new ReconstitutionParameterSpec(
                                    "customerId",
                                    "com.example.domain.CustomerId",
                                    "customerId",
                                    ConversionKind.VALUE_OBJECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Own identity uses map(), foreign key uses mapToCustomerId()
            assertThat(generatedCode).contains("map(entity.getId())");
            assertThat(generatedCode).contains("mapToCustomerId(entity.getCustomerId())");
        }

        @Test
        @DisplayName("should use isX() getter for primitive boolean fields")
        void should_useIsGetter_forPrimitiveBooleanFields() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(
                            new ReconstitutionParameterSpec("active", "boolean", "active", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec(
                                    "verified", "java.lang.Boolean", "verified", ConversionKind.DIRECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: primitive boolean should use isActive(), not getActive()
            assertThat(generatedCode).contains("entity.isActive()");
            assertThat(generatedCode).doesNotContain("entity.getActive()");

            // And: wrapper Boolean should still use getVerified()
            assertThat(generatedCode).contains("entity.getVerified()");
            assertThat(generatedCode).doesNotContain("entity.isVerified()");
        }

        @Test
        @DisplayName("should generate toDomain() calls for EMBEDDED_VALUE_OBJECT parameters")
        void should_generateToDomainCalls_forEmbeddedValueObjectParameters() {
            // Given: A reconstitution spec with an EMBEDDED_VALUE_OBJECT parameter (e.g., Money)
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "id", "com.example.domain.CustomerId", "id", ConversionKind.WRAPPED_IDENTITY),
                            new ReconstitutionParameterSpec(
                                    "amount",
                                    "com.example.domain.Money",
                                    "amount",
                                    ConversionKind.EMBEDDED_VALUE_OBJECT)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should call toDomain(entity.getAmount()) for embedded VO
            assertThat(generatedCode).contains("toDomain(entity.getAmount())");
            // And: Should still use map() for wrapped identity
            assertThat(generatedCode).contains("map(entity.getId())");
        }

        @Test
        @DisplayName("should generate toLocalDateTime() calls for AUDIT_TEMPORAL parameters")
        void should_generateToLocalDateTimeCalls_forAuditTemporalParameters() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "id", "com.example.domain.CustomerId", "id", ConversionKind.WRAPPED_IDENTITY),
                            new ReconstitutionParameterSpec(
                                    "createdAt", "java.time.LocalDateTime", "createdAt", ConversionKind.AUDIT_TEMPORAL),
                            new ReconstitutionParameterSpec(
                                    "updatedAt",
                                    "java.time.LocalDateTime",
                                    "updatedAt",
                                    ConversionKind.AUDIT_TEMPORAL)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should call toLocalDateTime() for audit temporal parameters
            assertThat(generatedCode).contains("toLocalDateTime(entity.getCreatedAt())");
            assertThat(generatedCode).contains("toLocalDateTime(entity.getUpdatedAt())");
        }

        @Test
        @DisplayName("should generate toLocalDateTime helper method when AUDIT_TEMPORAL used")
        void should_generateToLocalDateTimeHelperMethod_whenAuditTemporalUsed() {
            // Given
            ReconstitutionSpec reconstitutionSpec = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Customer",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "firstName", "java.lang.String", "firstName", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec(
                                    "createdAt",
                                    "java.time.LocalDateTime",
                                    "createdAt",
                                    ConversionKind.AUDIT_TEMPORAL)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    reconstitutionSpec,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should contain the toLocalDateTime helper method
            assertThat(generatedCode)
                    .contains("default java.time.LocalDateTime toLocalDateTime(java.time.Instant instant)");
            assertThat(generatedCode)
                    .contains("java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())");
        }

        @Test
        @DisplayName("should fallback to abstract method when no reconstitution spec")
        void should_fallbackToAbstractMethod_whenNoReconstitutionSpec() {
            // Given
            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: toDomain should NOT be a default method (implicitly abstract in interface)
            assertThat(generatedCode).contains("toDomain(");
            // No "default" keyword before toDomain, no null check, no reconstitute call
            assertThat(generatedCode).doesNotContain("default com.example.domain.Order toDomain");
            assertThat(generatedCode).doesNotContain("reconstitute");
        }
    }

    // =====================================================================
    // Child Entity Conversion tests
    // =====================================================================

    @Nested
    @DisplayName("Child Entity Conversion")
    class ChildEntityConversion {

        @Test
        @DisplayName("should generate toEntity method for child entity")
        void should_generateToEntityMethod_forChildEntity() {
            // Given: A mapper with a ChildEntityConversionSpec (no reconstitution, no VOs)
            ChildEntityConversionSpec childSpec = new ChildEntityConversionSpec(
                    "com.example.domain.OrderLine",
                    "com.example.infrastructure.jpa.OrderLineJpaEntity",
                    "OrderLine",
                    "OrderLineJpaEntity",
                    null,
                    List.of(),
                    List.of(),
                    List.of());

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of(childSpec));

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should contain abstract toEntity method for OrderLine
            assertThat(generatedCode)
                    .contains("toEntity(com.example.domain.OrderLine")
                    .contains("com.example.infrastructure.jpa.OrderLineJpaEntity toEntity");
        }

        @Test
        @DisplayName("should generate abstract toDomain method for child entity when no reconstitution")
        void should_generateToDomainMethod_forChildEntity_abstract() {
            // Given: A mapper with a ChildEntityConversionSpec without reconstitution
            ChildEntityConversionSpec childSpec = new ChildEntityConversionSpec(
                    "com.example.domain.OrderLine",
                    "com.example.infrastructure.jpa.OrderLineJpaEntity",
                    "OrderLine",
                    "OrderLineJpaEntity",
                    null, // No reconstitution
                    List.of(),
                    List.of(),
                    List.of());

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of(childSpec));

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should contain abstract toDomain method (no "default" keyword)
            assertThat(generatedCode)
                    .contains("toDomain(com.example.infrastructure.jpa.OrderLineJpaEntity")
                    .contains("com.example.domain.OrderLine toDomain");
            // And: Should NOT be a default method
            assertThat(generatedCode).doesNotContain("default com.example.domain.OrderLine toDomain");
        }

        @Test
        @DisplayName("should generate default toDomain method for child entity with reconstitution")
        void should_generateToDomainDefaultMethod_forChildEntity() {
            // Given: A ChildEntityConversionSpec WITH a ReconstitutionSpec
            ReconstitutionSpec childReconstitution = new ReconstitutionSpec(
                    "of",
                    "com.example.domain.OrderLine",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "productId",
                                    "com.example.domain.ProductId",
                                    "productId",
                                    ConversionKind.VALUE_OBJECT),
                            new ReconstitutionParameterSpec(
                                    "productName", "java.lang.String", "productName", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec("quantity", "int", "quantity", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec(
                                    "unitPrice",
                                    "com.example.domain.Money",
                                    "unitPrice",
                                    ConversionKind.EMBEDDED_VALUE_OBJECT)));

            ChildEntityConversionSpec childSpec = new ChildEntityConversionSpec(
                    "com.example.domain.OrderLine",
                    "com.example.infrastructure.jpa.OrderLineJpaEntity",
                    "OrderLine",
                    "OrderLineJpaEntity",
                    childReconstitution,
                    List.of(),
                    List.of(),
                    List.of());

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of(childSpec));

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should be a default method
            assertThat(generatedCode).contains("default com.example.domain.OrderLine toDomain");
            // And: Should have null check
            assertThat(generatedCode).contains("if (entity == null)");
            assertThat(generatedCode).contains("return null");
            // And: Should call factory method
            assertThat(generatedCode).contains("OrderLine.of(");
            // And: Should include the parameter conversions
            assertThat(generatedCode).contains("mapToProductId(entity.getProductId())");
            assertThat(generatedCode).contains("entity.getProductName()");
            assertThat(generatedCode).contains("entity.getQuantity()");
            assertThat(generatedCode).contains("toDomain(entity.getUnitPrice())");
        }

        @Test
        @DisplayName("should generate constructor-based toDomain for child entity without factory method")
        void should_generateConstructorBasedToDomain_forChildEntity() {
            // Given: A ChildEntityConversionSpec with constructor-based reconstitution (null factoryMethodName)
            ReconstitutionSpec childReconstitution = new ReconstitutionSpec(
                    null, // null = constructor-based reconstitution
                    "com.example.domain.OrderLine",
                    List.of(
                            new ReconstitutionParameterSpec("id", "java.lang.Long", "id", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec(
                                    "productId",
                                    "com.example.domain.ProductId",
                                    "productId",
                                    ConversionKind.VALUE_OBJECT),
                            new ReconstitutionParameterSpec(
                                    "productName", "java.lang.String", "productName", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec("quantity", "int", "quantity", ConversionKind.DIRECT),
                            new ReconstitutionParameterSpec(
                                    "unitPrice",
                                    "com.example.domain.Money",
                                    "unitPrice",
                                    ConversionKind.EMBEDDED_VALUE_OBJECT)));

            ChildEntityConversionSpec childSpec = new ChildEntityConversionSpec(
                    "com.example.domain.OrderLine",
                    "com.example.infrastructure.jpa.OrderLineJpaEntity",
                    "OrderLine",
                    "OrderLineJpaEntity",
                    childReconstitution,
                    List.of(),
                    List.of(),
                    List.of());

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of(childSpec));

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should be a default method
            assertThat(generatedCode).contains("default com.example.domain.OrderLine toDomain");
            // And: Should have null check
            assertThat(generatedCode).contains("if (entity == null)");
            assertThat(generatedCode).contains("return null");
            // And: Should call constructor with 'new' (not a static factory)
            assertThat(generatedCode).contains("new com.example.domain.OrderLine(");
            // And: Should NOT contain a factory method call pattern
            assertThat(generatedCode).doesNotContain("OrderLine.reconstitute(");
            assertThat(generatedCode).doesNotContain("OrderLine.of(");
            // And: Should include the parameter conversions
            assertThat(generatedCode).contains("entity.getId()");
            assertThat(generatedCode).contains("mapToProductId(entity.getProductId())");
            assertThat(generatedCode).contains("entity.getProductName()");
            assertThat(generatedCode).contains("entity.getQuantity()");
            assertThat(generatedCode).contains("toDomain(entity.getUnitPrice())");
        }

        @Test
        @DisplayName("should generate stream map conversion for child entity collection")
        void should_generateStreamMapConversion_forChildEntityCollection() {
            // Given: Parent reconstitution spec with a CHILD_ENTITY_COLLECTION parameter
            ReconstitutionSpec parentReconstitution = new ReconstitutionSpec(
                    "reconstitute",
                    "com.example.domain.Order",
                    List.of(
                            new ReconstitutionParameterSpec(
                                    "id", "com.example.domain.OrderId", "id", ConversionKind.WRAPPED_IDENTITY),
                            new ReconstitutionParameterSpec(
                                    "lines", "java.util.List", "lines", ConversionKind.CHILD_ENTITY_COLLECTION)));

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    parentReconstitution,
                    List.of());

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should generate stream map conversion
            assertThat(generatedCode).contains(".stream().map(this::toDomain).collect(");
            // And: Should call the parent factory method
            assertThat(generatedCode).contains("Order.reconstitute(");
        }

        @Test
        @DisplayName("should generate child-specific VO conversions")
        void should_generateChildSpecificVoConversions() {
            // Given: A ChildEntityConversionSpec with a ValueObjectMappingSpec
            ValueObjectMappingSpec productIdVo = new ValueObjectMappingSpec(
                    "com.example.domain.ProductId", "ProductId", "java.util.UUID", "value", true);

            ChildEntityConversionSpec childSpec = new ChildEntityConversionSpec(
                    "com.example.domain.OrderLine",
                    "com.example.infrastructure.jpa.OrderLineJpaEntity",
                    "OrderLine",
                    "OrderLineJpaEntity",
                    null,
                    List.of(),
                    List.of(productIdVo),
                    List.of());

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of(childSpec));

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Should generate mapToProductId method
            assertThat(generatedCode)
                    .contains("mapToProductId(java.util.UUID value)")
                    .contains("com.example.domain.ProductId");
        }

        @Test
        @DisplayName("should generate child toEntity mapping annotations")
        void should_generateChildToEntityMappingAnnotations() {
            // Given: A ChildEntityConversionSpec with toEntityMappings for audit ignores
            ChildEntityConversionSpec childSpec = new ChildEntityConversionSpec(
                    "com.example.domain.OrderLine",
                    "com.example.infrastructure.jpa.OrderLineJpaEntity",
                    "OrderLine",
                    "OrderLineJpaEntity",
                    null,
                    List.of(MappingSpec.ignore("createdAt"), MappingSpec.ignore("updatedAt")),
                    List.of(),
                    List.of());

            MapperSpec spec = new MapperSpec(
                    TEST_PACKAGE,
                    MAPPER_NAME,
                    DOMAIN_TYPE,
                    ENTITY_TYPE,
                    List.of(),
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of(childSpec));

            // When
            TypeSpec mapperInterface = JpaMapperCodegen.generate(spec);
            String generatedCode = mapperInterface.toString();

            // Then: Child toEntity method should have @Mapping annotations
            assertThat(generatedCode)
                    .contains("@org.mapstruct.Mapping")
                    .contains("target = \"createdAt\"")
                    .contains("ignore = true")
                    .contains("target = \"updatedAt\"");
        }
    }
}
