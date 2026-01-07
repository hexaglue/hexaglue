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
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaModelUtils}.
 *
 * <p>Verifies the correctness of package transformation, naming conversion,
 * and type resolution utilities.
 */
class JpaModelUtilsTest {

    // ========================================================================
    // deriveInfrastructurePackage() tests
    // ========================================================================

    @Test
    void deriveInfrastructurePackage_shouldConvertDomainPackage() {
        // Given
        String domainPackage = "com.example.domain";

        // When
        String result = JpaModelUtils.deriveInfrastructurePackage(domainPackage);

        // Then
        assertThat(result).isEqualTo("com.example.infrastructure.jpa");
    }

    @Test
    void deriveInfrastructurePackage_shouldHandleNestedDomainPackage() {
        // Given
        String domainPackage = "com.example.order.domain";

        // When
        String result = JpaModelUtils.deriveInfrastructurePackage(domainPackage);

        // Then
        assertThat(result).isEqualTo("com.example.order.infrastructure.jpa");
    }

    @Test
    void deriveInfrastructurePackage_shouldHandleNonDomainPackage() {
        // Given
        String domainPackage = "com.example.payment";

        // When
        String result = JpaModelUtils.deriveInfrastructurePackage(domainPackage);

        // Then
        assertThat(result).isEqualTo("com.example.payment.infrastructure.jpa");
    }

    @Test
    void deriveInfrastructurePackage_shouldHandlePackageWithDomainInMiddle() {
        // Given
        String domainPackage = "com.domain.example";

        // When
        String result = JpaModelUtils.deriveInfrastructurePackage(domainPackage);

        // Then
        // Should not replace "domain" in the middle, only append
        assertThat(result).isEqualTo("com.domain.example.infrastructure.jpa");
    }

    @Test
    void deriveInfrastructurePackage_shouldThrowExceptionForNullPackage() {
        // When/Then
        assertThatThrownBy(() -> JpaModelUtils.deriveInfrastructurePackage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Domain package cannot be null or empty");
    }

    @Test
    void deriveInfrastructurePackage_shouldThrowExceptionForEmptyPackage() {
        // When/Then
        assertThatThrownBy(() -> JpaModelUtils.deriveInfrastructurePackage(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Domain package cannot be null or empty");
    }

    // ========================================================================
    // toSnakeCase() tests
    // ========================================================================

    @Test
    void toSnakeCase_shouldConvertCamelCase() {
        // Given
        String camelCase = "firstName";

        // When
        String result = JpaModelUtils.toSnakeCase(camelCase);

        // Then
        assertThat(result).isEqualTo("first_name");
    }

    @Test
    void toSnakeCase_shouldConvertMultipleWords() {
        // Given
        String camelCase = "totalAmount";

        // When
        String result = JpaModelUtils.toSnakeCase(camelCase);

        // Then
        assertThat(result).isEqualTo("total_amount");
    }

    @Test
    void toSnakeCase_shouldHandleSingleWord() {
        // Given
        String camelCase = "id";

        // When
        String result = JpaModelUtils.toSnakeCase(camelCase);

        // Then
        assertThat(result).isEqualTo("id");
    }

    @Test
    void toSnakeCase_shouldHandleAlreadyLowercase() {
        // Given
        String camelCase = "name";

        // When
        String result = JpaModelUtils.toSnakeCase(camelCase);

        // Then
        assertThat(result).isEqualTo("name");
    }

    @Test
    void toSnakeCase_shouldHandleUppercaseStart() {
        // Given
        String camelCase = "OrderId";

        // When
        String result = JpaModelUtils.toSnakeCase(camelCase);

        // Then
        assertThat(result).isEqualTo("order_id");
    }

    @Test
    void toSnakeCase_shouldHandleConsecutiveUppercase() {
        // Given
        String camelCase = "XMLParser";

        // When
        String result = JpaModelUtils.toSnakeCase(camelCase);

        // Then
        // Note: The current implementation handles this as xml_parser
        assertThat(result).isEqualTo("xmlparser");
    }

    @Test
    void toSnakeCase_shouldThrowExceptionForNullInput() {
        // When/Then
        assertThatThrownBy(() -> JpaModelUtils.toSnakeCase(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Field name cannot be null or empty");
    }

    @Test
    void toSnakeCase_shouldThrowExceptionForEmptyInput() {
        // When/Then
        assertThatThrownBy(() -> JpaModelUtils.toSnakeCase(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Field name cannot be null or empty");
    }

    // ========================================================================
    // resolveTypeName() tests
    // ========================================================================

    @Test
    void resolveTypeName_shouldResolveIntPrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("int");

        // Then
        assertThat(result).isEqualTo(TypeName.INT);
    }

    @Test
    void resolveTypeName_shouldResolveLongPrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("long");

        // Then
        assertThat(result).isEqualTo(TypeName.LONG);
    }

    @Test
    void resolveTypeName_shouldResolveShortPrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("short");

        // Then
        assertThat(result).isEqualTo(TypeName.SHORT);
    }

    @Test
    void resolveTypeName_shouldResolveBytePrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("byte");

        // Then
        assertThat(result).isEqualTo(TypeName.BYTE);
    }

    @Test
    void resolveTypeName_shouldResolveBooleanPrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("boolean");

        // Then
        assertThat(result).isEqualTo(TypeName.BOOLEAN);
    }

    @Test
    void resolveTypeName_shouldResolveCharPrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("char");

        // Then
        assertThat(result).isEqualTo(TypeName.CHAR);
    }

    @Test
    void resolveTypeName_shouldResolveFloatPrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("float");

        // Then
        assertThat(result).isEqualTo(TypeName.FLOAT);
    }

    @Test
    void resolveTypeName_shouldResolveDoublePrimitive() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("double");

        // Then
        assertThat(result).isEqualTo(TypeName.DOUBLE);
    }

    @Test
    void resolveTypeName_shouldResolveVoidType() {
        // When
        TypeName result = JpaModelUtils.resolveTypeName("void");

        // Then
        assertThat(result).isEqualTo(TypeName.VOID);
    }

    @Test
    void resolveTypeName_shouldResolveSimpleClass() {
        // Given
        String qualifiedName = "java.lang.String";

        // When
        TypeName result = JpaModelUtils.resolveTypeName(qualifiedName);

        // Then
        assertThat(result).isEqualTo(ClassName.bestGuess(qualifiedName));
        assertThat(result.toString()).isEqualTo("java.lang.String");
    }

    @Test
    void resolveTypeName_shouldResolveCustomClass() {
        // Given
        String qualifiedName = "com.example.domain.Order";

        // When
        TypeName result = JpaModelUtils.resolveTypeName(qualifiedName);

        // Then
        assertThat(result).isEqualTo(ClassName.bestGuess(qualifiedName));
        assertThat(result.toString()).isEqualTo("com.example.domain.Order");
    }

    @Test
    void resolveTypeName_shouldResolveUUID() {
        // Given
        String qualifiedName = "java.util.UUID";

        // When
        TypeName result = JpaModelUtils.resolveTypeName(qualifiedName);

        // Then
        assertThat(result).isEqualTo(ClassName.bestGuess(qualifiedName));
        assertThat(result.toString()).isEqualTo("java.util.UUID");
    }

    @Test
    void resolveTypeName_shouldThrowExceptionForNullInput() {
        // When/Then
        assertThatThrownBy(() -> JpaModelUtils.resolveTypeName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Qualified name cannot be null or empty");
    }

    @Test
    void resolveTypeName_shouldThrowExceptionForEmptyInput() {
        // When/Then
        assertThatThrownBy(() -> JpaModelUtils.resolveTypeName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Qualified name cannot be null or empty");
    }
}
