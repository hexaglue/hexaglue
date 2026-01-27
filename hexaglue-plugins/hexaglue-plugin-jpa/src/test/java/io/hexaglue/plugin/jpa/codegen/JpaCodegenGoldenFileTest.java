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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ir.CascadeType;
import io.hexaglue.arch.model.ir.FetchType;
import io.hexaglue.arch.model.ir.IdentityStrategy;
import io.hexaglue.arch.model.ir.IdentityWrapperKind;
import io.hexaglue.arch.model.ir.Nullability;
import io.hexaglue.arch.model.ir.RelationKind;
import io.hexaglue.plugin.jpa.model.EmbeddableSpec;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.IdFieldSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Golden file tests for JPA code generation.
 *
 * <p>These tests validate that the JPA code generators produce stable, expected output
 * across different configurations. Any change to the code generation that modifies the
 * output will cause these tests to fail, signaling a potential regression.
 *
 * <h2>Configurations Tested</h2>
 * <ul>
 *   <li>Default entity configuration</li>
 *   <li>Entity with auditing enabled</li>
 *   <li>Entity with optimistic locking enabled</li>
 *   <li>Entity with table prefix</li>
 *   <li>Full entity configuration (all options enabled)</li>
 *   <li>Entity with embedded value object</li>
 *   <li>Entity with relationships</li>
 *   <li>Embeddable value objects</li>
 *   <li>Repository interfaces</li>
 * </ul>
 *
 * @since 5.0.0
 */
class JpaCodegenGoldenFileTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");
    private static final String INFRA_PACKAGE = "com.example.infrastructure.jpa";
    private static final String DOMAIN_PACKAGE = "com.example.domain";

    // Pattern to normalize @Generated date for comparison
    private static final Pattern GENERATED_DATE_PATTERN = Pattern.compile("date = \"[^\"]+\"");

    // =========================================================================
    // Entity Golden Files
    // =========================================================================

    @Nested
    @DisplayName("Entity Generation")
    class EntityGenerationTest {

        @Test
        @DisplayName("Default configuration")
        void defaultConfiguration() throws IOException {
            EntitySpec spec = createOrderEntitySpec(false, false, "");
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-default.java.txt");
        }

        @Test
        @DisplayName("With auditing enabled")
        void withAuditingEnabled() throws IOException {
            EntitySpec spec = createOrderEntitySpec(true, false, "");
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-with-auditing.java.txt");
        }

        @Test
        @DisplayName("With optimistic locking enabled")
        void withOptimisticLockingEnabled() throws IOException {
            EntitySpec spec = createOrderEntitySpec(false, true, "");
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-with-locking.java.txt");
        }

        @Test
        @DisplayName("With table prefix")
        void withTablePrefix() throws IOException {
            EntitySpec spec = createOrderEntitySpec(false, false, "app_");
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-with-prefix.java.txt");
        }

        @Test
        @DisplayName("Full configuration")
        void fullConfiguration() throws IOException {
            EntitySpec spec = createOrderEntitySpec(true, true, "app_");
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-full-config.java.txt");
        }

        @Test
        @DisplayName("With embedded value object")
        void withEmbeddedValueObject() throws IOException {
            EntitySpec spec = createEntityWithEmbedded();
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-with-embedded.java.txt");
        }

        @Test
        @DisplayName("With one-to-many relation")
        void withOneToManyRelation() throws IOException {
            EntitySpec spec = createEntityWithRelations();
            assertGoldenFile(JpaEntityCodegen.generate(spec), INFRA_PACKAGE, "entity-with-relations.java.txt");
        }
    }

    // =========================================================================
    // Embeddable Golden Files
    // =========================================================================

    @Nested
    @DisplayName("Embeddable Generation")
    class EmbeddableGenerationTest {

        @Test
        @DisplayName("Money value object")
        void moneyValueObject() throws IOException {
            EmbeddableSpec spec = createMoneyEmbeddableSpec();
            assertGoldenFile(JpaEmbeddableCodegen.generate(spec), INFRA_PACKAGE, "embeddable-money.java.txt");
        }

        @Test
        @DisplayName("Address value object")
        void addressValueObject() throws IOException {
            EmbeddableSpec spec = createAddressEmbeddableSpec();
            assertGoldenFile(JpaEmbeddableCodegen.generate(spec), INFRA_PACKAGE, "embeddable-address.java.txt");
        }
    }

    // =========================================================================
    // Repository Golden Files
    // =========================================================================

    @Nested
    @DisplayName("Repository Generation")
    class RepositoryGenerationTest {

        @Test
        @DisplayName("Basic repository")
        void basicRepository() throws IOException {
            RepositorySpec spec = createOrderRepositorySpec();
            assertGoldenFile(JpaRepositoryCodegen.generate(spec), INFRA_PACKAGE, "repository-order.java.txt");
        }
    }

    // =========================================================================
    // Golden File Assertion
    // =========================================================================

    private void assertGoldenFile(TypeSpec typeSpec, String packageName, String goldenFileName) throws IOException {
        // Generate code
        JavaFile javaFile =
                JavaFile.builder(packageName, typeSpec).indent("    ").build();
        String actualCode = normalizeGeneratedCode(javaFile.toString());

        // Compare with golden file
        Path goldenPath =
                Path.of(System.getProperty("user.dir")).resolve(GOLDEN_DIR).resolve(goldenFileName);
        if (Files.exists(goldenPath)) {
            String expectedCode = Files.readString(goldenPath, StandardCharsets.UTF_8);
            assertThat(actualCode)
                    .as("Generated code should match golden file: %s", goldenFileName)
                    .isEqualTo(expectedCode);
        } else {
            // First run: create golden file
            Files.createDirectories(goldenPath.getParent());
            Files.writeString(goldenPath, actualCode, StandardCharsets.UTF_8);
            System.out.println("Golden file created: " + goldenPath);
            System.out.println("Please review and commit the golden file.");
        }
    }

    /**
     * Normalizes generated code for stable comparison.
     * Replaces dynamic parts (like dates) with fixed placeholders.
     */
    private String normalizeGeneratedCode(String code) {
        // Normalize @Generated date to a fixed value
        return GENERATED_DATE_PATTERN.matcher(code).replaceAll("date = \"2026-01-01T00:00:00Z\"");
    }

    // =========================================================================
    // Entity Spec Builders
    // =========================================================================

    private EntitySpec createOrderEntitySpec(
            boolean enableAuditing, boolean enableOptimisticLocking, String tablePrefix) {
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(UUID.class),
                TypeName.get(UUID.class),
                IdentityStrategy.AUTO,
                IdentityWrapperKind.NONE);

        PropertyFieldSpec customerNameField = new PropertyFieldSpec(
                "customerName",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "customer_name",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec totalAmountField = new PropertyFieldSpec(
                "totalAmount",
                TypeName.get(BigDecimal.class),
                Nullability.NON_NULL,
                "total_amount",
                false,
                false,
                false,
                "java.math.BigDecimal",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec statusField = new PropertyFieldSpec(
                "status",
                ClassName.get("com.example.domain", "OrderStatus"),
                Nullability.NON_NULL,
                "status",
                false,
                true,
                false,
                "com.example.domain.OrderStatus",
                false,
                null,
                null,
                List.of());

        return EntitySpec.builder()
                .packageName(INFRA_PACKAGE)
                .className("OrderEntity")
                .tableName(tablePrefix + "orders")
                .domainQualifiedName(DOMAIN_PACKAGE + ".Order")
                .idField(idField)
                .addProperty(customerNameField)
                .addProperty(totalAmountField)
                .addProperty(statusField)
                .enableAuditing(enableAuditing)
                .enableOptimisticLocking(enableOptimisticLocking)
                .build();
    }

    private EntitySpec createEntityWithEmbedded() {
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(UUID.class),
                TypeName.get(UUID.class),
                IdentityStrategy.AUTO,
                IdentityWrapperKind.NONE);

        PropertyFieldSpec nameField = new PropertyFieldSpec(
                "name",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "name",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec priceField = new PropertyFieldSpec(
                "price",
                ClassName.get(INFRA_PACKAGE, "MoneyEmbeddable"),
                Nullability.NON_NULL,
                "price",
                true,
                false,
                false,
                "com.example.domain.Money",
                false,
                null,
                null,
                List.of());

        return EntitySpec.builder()
                .packageName(INFRA_PACKAGE)
                .className("ProductEntity")
                .tableName("products")
                .domainQualifiedName(DOMAIN_PACKAGE + ".Product")
                .idField(idField)
                .addProperty(nameField)
                .addProperty(priceField)
                .build();
    }

    private EntitySpec createEntityWithRelations() {
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(UUID.class),
                TypeName.get(UUID.class),
                IdentityStrategy.AUTO,
                IdentityWrapperKind.NONE);

        PropertyFieldSpec customerNameField = new PropertyFieldSpec(
                "customerName",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "customer_name",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        RelationFieldSpec itemsRelation = new RelationFieldSpec(
                "items",
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(INFRA_PACKAGE, "OrderItemEntity")),
                RelationKind.ONE_TO_MANY,
                ElementKind.ENTITY,
                "order",
                CascadeType.ALL,
                FetchType.LAZY,
                true);

        return EntitySpec.builder()
                .packageName(INFRA_PACKAGE)
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName(DOMAIN_PACKAGE + ".Order")
                .idField(idField)
                .addProperty(customerNameField)
                .addRelation(itemsRelation)
                .build();
    }

    // =========================================================================
    // Embeddable Spec Builders
    // =========================================================================

    private EmbeddableSpec createMoneyEmbeddableSpec() {
        PropertyFieldSpec amountField = new PropertyFieldSpec(
                "amount",
                TypeName.get(BigDecimal.class),
                Nullability.NON_NULL,
                "amount",
                false,
                false,
                false,
                "java.math.BigDecimal",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec currencyField = new PropertyFieldSpec(
                "currency",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "currency",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        return EmbeddableSpec.builder()
                .packageName(INFRA_PACKAGE)
                .className("MoneyEmbeddable")
                .domainQualifiedName(DOMAIN_PACKAGE + ".Money")
                .addProperty(amountField)
                .addProperty(currencyField)
                .build();
    }

    private EmbeddableSpec createAddressEmbeddableSpec() {
        PropertyFieldSpec streetField = new PropertyFieldSpec(
                "street",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "street",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec cityField = new PropertyFieldSpec(
                "city",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "city",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec zipCodeField = new PropertyFieldSpec(
                "zipCode",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "zip_code",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        PropertyFieldSpec countryField = new PropertyFieldSpec(
                "country",
                TypeName.get(String.class),
                Nullability.NON_NULL,
                "country",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                List.of());

        return EmbeddableSpec.builder()
                .packageName(INFRA_PACKAGE)
                .className("AddressEmbeddable")
                .domainQualifiedName(DOMAIN_PACKAGE + ".Address")
                .addProperty(streetField)
                .addProperty(cityField)
                .addProperty(zipCodeField)
                .addProperty(countryField)
                .build();
    }

    // =========================================================================
    // Repository Spec Builder
    // =========================================================================

    private RepositorySpec createOrderRepositorySpec() {
        return new RepositorySpec(
                INFRA_PACKAGE,
                "OrderJpaRepository",
                ClassName.get(INFRA_PACKAGE, "OrderEntity"),
                TypeName.get(UUID.class),
                DOMAIN_PACKAGE + ".Order",
                List.of());
    }
}
