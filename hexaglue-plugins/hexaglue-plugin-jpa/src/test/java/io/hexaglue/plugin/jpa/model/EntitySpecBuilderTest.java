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

package io.hexaglue.plugin.jpa.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ir.CascadeType;
import io.hexaglue.arch.model.ir.FetchType;
import io.hexaglue.arch.model.ir.IdentityStrategy;
import io.hexaglue.arch.model.ir.IdentityWrapperKind;
import io.hexaglue.arch.model.ir.Nullability;
import io.hexaglue.arch.model.ir.RelationKind;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EntitySpec.Builder} validation.
 *
 * <p>These tests validate that the EntitySpec builder correctly assembles
 * complete entity specifications and enforces required field constraints.
 */
class EntitySpecBuilderTest {

    @Test
    void build_shouldCreateCompleteSpec_whenAllFieldsProvided() {
        // Given: A fully configured entity builder
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(java.util.UUID.class),
                TypeName.get(java.util.UUID.class),
                IdentityStrategy.UUID,
                IdentityWrapperKind.NONE);

        PropertyFieldSpec property1 = new PropertyFieldSpec(
                "firstName",
                ClassName.get(String.class),
                Nullability.NON_NULL,
                "first_name",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                java.util.List.of());

        PropertyFieldSpec property2 = new PropertyFieldSpec(
                "email",
                ClassName.get(String.class),
                Nullability.NULLABLE,
                "email",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                java.util.List.of());

        RelationFieldSpec relation = new RelationFieldSpec(
                "address",
                ClassName.get("com.example", "Address"),
                RelationKind.EMBEDDED,
                ElementKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.EAGER,
                false);

        // When: Building the entity spec
        EntitySpec spec = EntitySpec.builder()
                .packageName("com.example.infrastructure.jpa")
                .className("CustomerEntity")
                .tableName("customers")
                .domainQualifiedName("com.example.domain.Customer")
                .idField(idField)
                .addProperty(property1)
                .addProperty(property2)
                .addRelation(relation)
                .enableAuditing(true)
                .enableOptimisticLocking(false)
                .build();

        // Then: All fields should be correctly set
        assertThat(spec.packageName()).isEqualTo("com.example.infrastructure.jpa");
        assertThat(spec.className()).isEqualTo("CustomerEntity");
        assertThat(spec.tableName()).isEqualTo("customers");
        assertThat(spec.domainQualifiedName()).isEqualTo("com.example.domain.Customer");
        assertThat(spec.idField()).isEqualTo(idField);
        assertThat(spec.properties()).hasSize(2);
        assertThat(spec.relations()).hasSize(1);
        assertThat(spec.enableAuditing()).isTrue();
        assertThat(spec.enableOptimisticLocking()).isFalse();

        // And: Helper methods should work correctly
        assertThat(spec.fullyQualifiedClassName()).isEqualTo("com.example.infrastructure.jpa.CustomerEntity");
        assertThat(spec.domainSimpleName()).isEqualTo("Customer");
        assertThat(spec.hasProperties()).isTrue();
        assertThat(spec.hasRelations()).isTrue();
    }

    @Test
    void build_shouldRequirePackageName() {
        // Given: Builder without package name
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        EntitySpec.Builder builder = EntitySpec.builder()
                // .packageName missing
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName("com.example.Order")
                .idField(idField);

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("packageName is required");
    }

    @Test
    void build_shouldRequireClassName() {
        // Given: Builder without class name
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        EntitySpec.Builder builder = EntitySpec.builder()
                .packageName("com.example.jpa")
                // .className missing
                .tableName("orders")
                .domainQualifiedName("com.example.Order")
                .idField(idField);

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("className is required");
    }

    @Test
    void build_shouldRequireTableName() {
        // Given: Builder without table name
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        EntitySpec.Builder builder = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("OrderEntity")
                // .tableName missing
                .domainQualifiedName("com.example.Order")
                .idField(idField);

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tableName is required");
    }

    @Test
    void build_shouldRequireDomainQualifiedName() {
        // Given: Builder without domain qualified name
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        EntitySpec.Builder builder = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("OrderEntity")
                .tableName("orders")
                // .domainQualifiedName missing
                .idField(idField);

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("domainQualifiedName is required");
    }

    @Test
    void build_shouldRequireIdField() {
        // Given: Builder without ID field
        EntitySpec.Builder builder = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName("com.example.Order");
        // .idField missing

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("idField is required");
    }

    @Test
    void build_shouldAllowEmptyPropertiesAndRelations() {
        // Given: Builder with only required fields (no properties/relations)
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        // When: Building minimal spec
        EntitySpec spec = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("SimpleEntity")
                .tableName("simple")
                .domainQualifiedName("com.example.Simple")
                .idField(idField)
                .build();

        // Then: Properties and relations should be empty but not null
        assertThat(spec.properties()).isEmpty();
        assertThat(spec.relations()).isEmpty();
        assertThat(spec.hasProperties()).isFalse();
        assertThat(spec.hasRelations()).isFalse();
    }

    @Test
    void build_shouldDefaultAuditingAndLockingToFalse() {
        // Given: Builder without explicit auditing/locking settings
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        // When: Building spec with defaults
        EntitySpec spec = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName("com.example.Order")
                .idField(idField)
                .build();

        // Then: Auditing and locking should be false by default
        assertThat(spec.enableAuditing()).isFalse();
        assertThat(spec.enableOptimisticLocking()).isFalse();
    }

    @Test
    void build_shouldCreateImmutableLists() {
        // Given: Builder with mutable lists
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        PropertyFieldSpec property = new PropertyFieldSpec(
                "name",
                ClassName.get(String.class),
                Nullability.NON_NULL,
                "name",
                false,
                false,
                false,
                "java.lang.String",
                false,
                null,
                null,
                java.util.List.of());

        // When: Building spec
        EntitySpec spec = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName("com.example.Order")
                .idField(idField)
                .addProperty(property)
                .build();

        // Then: Lists should be immutable
        assertThatThrownBy(() -> spec.properties().add(property)).isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> spec.relations().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void domainSimpleName_shouldExtractCorrectly_fromFullyQualifiedName() {
        // Given: Entity with fully qualified domain name
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);

        EntitySpec spec = EntitySpec.builder()
                .packageName("com.example.jpa")
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName("com.example.domain.model.Order")
                .idField(idField)
                .build();

        // When/Then: Should extract simple name correctly
        assertThat(spec.domainSimpleName()).isEqualTo("Order");
    }
}
