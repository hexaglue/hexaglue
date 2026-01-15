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

package io.hexaglue.plugin.jpa.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import io.hexaglue.spi.ir.TypeRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TypeMappings}.
 *
 * <p>Tests validate the behavior of type conversion from domain types to JPA types,
 * ensuring correct handling of primitives, collections, optionals, maps, and custom types.
 */
class TypeMappingsTest {

    // =====================================================================
    // toJpaType(String) tests
    // =====================================================================

    @Test
    void toJpaType_shouldMapUUID() {
        TypeName result = TypeMappings.toJpaType("java.util.UUID");

        assertThat(result).isEqualTo(TypeName.get(UUID.class));
    }

    @Test
    void toJpaType_shouldMapString() {
        TypeName result = TypeMappings.toJpaType("java.lang.String");

        assertThat(result).isEqualTo(TypeName.get(String.class));
    }

    @Test
    void toJpaType_shouldMapBigDecimal() {
        TypeName result = TypeMappings.toJpaType("java.math.BigDecimal");

        assertThat(result).isEqualTo(TypeName.get(BigDecimal.class));
    }

    @Test
    void toJpaType_shouldMapLocalDate() {
        TypeName result = TypeMappings.toJpaType("java.time.LocalDate");

        assertThat(result).isEqualTo(TypeName.get(LocalDate.class));
    }

    @Test
    void toJpaType_shouldMapLocalDateTime() {
        TypeName result = TypeMappings.toJpaType("java.time.LocalDateTime");

        assertThat(result).isEqualTo(TypeName.get(LocalDateTime.class));
    }

    @Test
    void toJpaType_shouldMapInstant() {
        TypeName result = TypeMappings.toJpaType("java.time.Instant");

        assertThat(result).isEqualTo(TypeName.get(Instant.class));
    }

    @Test
    void toJpaType_shouldMapCustomType() {
        TypeName result = TypeMappings.toJpaType("com.example.Order");

        assertThat(result).isEqualTo(ClassName.bestGuess("com.example.Order"));
    }

    @Test
    void toJpaType_shouldThrowExceptionForNullQualifiedName() {
        assertThatThrownBy(() -> TypeMappings.toJpaType((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Qualified name cannot be null or empty");
    }

    @Test
    void toJpaType_shouldThrowExceptionForEmptyQualifiedName() {
        assertThatThrownBy(() -> TypeMappings.toJpaType(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Qualified name cannot be null or empty");
    }

    // =====================================================================
    // toJpaType(TypeRef) - Primitive types
    // =====================================================================

    @Test
    void toJpaType_shouldMapPrimitiveInt() {
        TypeRef typeRef = TypeRef.primitive("int");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.INT);
    }

    @Test
    void toJpaType_shouldMapPrimitiveLong() {
        TypeRef typeRef = TypeRef.primitive("long");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.LONG);
    }

    @Test
    void toJpaType_shouldMapPrimitiveBoolean() {
        TypeRef typeRef = TypeRef.primitive("boolean");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.BOOLEAN);
    }

    @Test
    void toJpaType_shouldMapPrimitiveDouble() {
        TypeRef typeRef = TypeRef.primitive("double");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.DOUBLE);
    }

    @Test
    void toJpaType_shouldMapPrimitiveFloat() {
        TypeRef typeRef = TypeRef.primitive("float");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.FLOAT);
    }

    @Test
    void toJpaType_shouldMapPrimitiveShort() {
        TypeRef typeRef = TypeRef.primitive("short");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.SHORT);
    }

    @Test
    void toJpaType_shouldMapPrimitiveByte() {
        TypeRef typeRef = TypeRef.primitive("byte");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.BYTE);
    }

    @Test
    void toJpaType_shouldMapPrimitiveChar() {
        TypeRef typeRef = TypeRef.primitive("char");

        TypeName result = TypeMappings.toJpaType(typeRef);

        assertThat(result).isEqualTo(TypeName.CHAR);
    }

    // =====================================================================
    // toJpaType(TypeRef) - Collections
    // =====================================================================

    @Test
    void toJpaType_shouldMapListOfCustomTypes() {
        TypeRef elementType = TypeRef.of("com.example.Order");
        TypeRef listType = TypeRef.parameterized("java.util.List", elementType);

        TypeName result = TypeMappings.toJpaType(listType);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.List<com.example.Order>");
    }

    @Test
    void toJpaType_shouldMapSetOfCustomTypes() {
        TypeRef elementType = TypeRef.of("com.example.Tag");
        TypeRef setType = TypeRef.parameterized("java.util.Set", elementType);

        TypeName result = TypeMappings.toJpaType(setType);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.Set<com.example.Tag>");
    }

    @Test
    void toJpaType_shouldMapCollectionToList() {
        TypeRef elementType = TypeRef.of("com.example.Item");
        TypeRef collectionType = TypeRef.parameterized("java.util.Collection", elementType);

        TypeName result = TypeMappings.toJpaType(collectionType);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.List<com.example.Item>");
    }

    @Test
    void toJpaType_shouldMapListOfStrings() {
        TypeRef elementType = TypeRef.of("java.lang.String");
        TypeRef listType = TypeRef.parameterized("java.util.List", elementType);

        TypeName result = TypeMappings.toJpaType(listType);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.List<java.lang.String>");
    }

    // =====================================================================
    // toJpaType(TypeRef) - Optionals
    // =====================================================================

    @Test
    void toJpaType_shouldUnwrapOptionalOfCustomType() {
        TypeRef elementType = TypeRef.of("com.example.Order");
        TypeRef optionalType = TypeRef.parameterized("java.util.Optional", elementType);

        TypeName result = TypeMappings.toJpaType(optionalType);

        // Optional should be unwrapped for JPA
        assertThat(result).isEqualTo(ClassName.bestGuess("com.example.Order"));
    }

    @Test
    void toJpaType_shouldUnwrapOptionalOfString() {
        TypeRef elementType = TypeRef.of("java.lang.String");
        TypeRef optionalType = TypeRef.parameterized("java.util.Optional", elementType);

        TypeName result = TypeMappings.toJpaType(optionalType);

        assertThat(result).isEqualTo(TypeName.get(String.class));
    }

    @Test
    void toJpaType_shouldUnwrapOptionalOfUUID() {
        TypeRef elementType = TypeRef.of("java.util.UUID");
        TypeRef optionalType = TypeRef.parameterized("java.util.Optional", elementType);

        TypeName result = TypeMappings.toJpaType(optionalType);

        assertThat(result).isEqualTo(TypeName.get(UUID.class));
    }

    // =====================================================================
    // toJpaType(TypeRef) - Nested generics
    // =====================================================================

    @Test
    void toJpaType_shouldUnwrapOptionalInsideList() {
        // List<Optional<Order>> → List<Order>
        TypeRef orderType = TypeRef.of("com.example.Order");
        TypeRef optionalOrder = TypeRef.parameterized("java.util.Optional", orderType);
        TypeRef listOfOptionalOrder = TypeRef.parameterized("java.util.List", optionalOrder);

        TypeName result = TypeMappings.toJpaType(listOfOptionalOrder);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.List<com.example.Order>");
    }

    @Test
    void toJpaType_shouldUnwrapOptionalAroundList() {
        // Optional<List<Order>> → List<Order>
        TypeRef orderType = TypeRef.of("com.example.Order");
        TypeRef listOfOrder = TypeRef.parameterized("java.util.List", orderType);
        TypeRef optionalList = TypeRef.parameterized("java.util.Optional", listOfOrder);

        TypeName result = TypeMappings.toJpaType(optionalList);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.List<com.example.Order>");
    }

    @Test
    void toJpaType_shouldUnwrapOptionalInsideSet() {
        // Set<Optional<Tag>> → Set<Tag>
        TypeRef tagType = TypeRef.of("com.example.Tag");
        TypeRef optionalTag = TypeRef.parameterized("java.util.Optional", tagType);
        TypeRef setOfOptionalTag = TypeRef.parameterized("java.util.Set", optionalTag);

        TypeName result = TypeMappings.toJpaType(setOfOptionalTag);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.Set<com.example.Tag>");
    }

    @Test
    void toJpaType_shouldUnwrapOptionalAroundSet() {
        // Optional<Set<Tag>> → Set<Tag>
        TypeRef tagType = TypeRef.of("com.example.Tag");
        TypeRef setOfTag = TypeRef.parameterized("java.util.Set", tagType);
        TypeRef optionalSet = TypeRef.parameterized("java.util.Optional", setOfTag);

        TypeName result = TypeMappings.toJpaType(optionalSet);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.Set<com.example.Tag>");
    }

    // =====================================================================
    // toJpaType(TypeRef) - Maps
    // =====================================================================

    @Test
    void toJpaType_shouldMapMapOfStringToOrder() {
        TypeRef keyType = TypeRef.of("java.lang.String");
        TypeRef valueType = TypeRef.of("com.example.Order");
        TypeRef mapType = TypeRef.parameterized("java.util.Map", keyType, valueType);

        TypeName result = TypeMappings.toJpaType(mapType);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.Map<java.lang.String, com.example.Order>");
    }

    @Test
    void toJpaType_shouldMapMapOfLongToString() {
        TypeRef keyType = TypeRef.of("java.lang.Long");
        TypeRef valueType = TypeRef.of("java.lang.String");
        TypeRef mapType = TypeRef.parameterized("java.util.Map", keyType, valueType);

        TypeName result = TypeMappings.toJpaType(mapType);

        assertThat(result).isInstanceOf(ParameterizedTypeName.class);
        String typeString = result.toString();
        assertThat(typeString).isEqualTo("java.util.Map<java.lang.Long, java.lang.String>");
    }

    // =====================================================================
    // toJpaType(TypeRef) - Validation
    // =====================================================================

    @Test
    void toJpaType_shouldThrowExceptionForNullTypeRef() {
        assertThatThrownBy(() -> TypeMappings.toJpaType((TypeRef) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TypeRef cannot be null");
    }

    // =====================================================================
    // unwrapIdentifier tests
    // =====================================================================

    @Test
    void unwrapIdentifier_shouldReturnUnwrappedTypeForWrappedIdentity() {
        TypeRef wrappedType = TypeRef.of("com.example.OrderId");
        TypeRef unwrappedType = TypeRef.of("java.util.UUID");
        Identity identity = Identity.wrapped(
                "id", wrappedType, unwrappedType, IdentityStrategy.UUID, IdentityWrapperKind.RECORD, "value");

        TypeName result = TypeMappings.unwrapIdentifier(identity);

        assertThat(result).isEqualTo(TypeName.get(UUID.class));
    }

    @Test
    void unwrapIdentifier_shouldReturnTypeWhenNotWrapped() {
        TypeRef uuidType = TypeRef.of("java.util.UUID");
        Identity identity = Identity.unwrapped("id", uuidType, IdentityStrategy.UUID);

        TypeName result = TypeMappings.unwrapIdentifier(identity);

        assertThat(result).isEqualTo(TypeName.get(UUID.class));
    }

    @Test
    void unwrapIdentifier_shouldHandleLongUnwrappedType() {
        TypeRef wrappedType = TypeRef.of("com.example.TaskId");
        TypeRef unwrappedType = TypeRef.of("java.lang.Long");
        Identity identity = Identity.wrapped(
                "id", wrappedType, unwrappedType, IdentityStrategy.IDENTITY, IdentityWrapperKind.CLASS, "getValue");

        TypeName result = TypeMappings.unwrapIdentifier(identity);

        assertThat(result).isEqualTo(TypeName.get(Long.class));
    }

    @Test
    void unwrapIdentifier_shouldThrowExceptionForNullIdentity() {
        assertThatThrownBy(() -> TypeMappings.unwrapIdentifier(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Identity cannot be null");
    }

    // =====================================================================
    // requiresImport tests
    // =====================================================================

    @Test
    void requiresImport_shouldReturnTrueForCustomTypes() {
        TypeRef typeRef = TypeRef.of("com.example.Order");

        boolean result = TypeMappings.requiresImport(typeRef);

        assertThat(result).isTrue();
    }

    @Test
    void requiresImport_shouldReturnTrueForJavaUtilTypes() {
        TypeRef typeRef = TypeRef.of("java.util.UUID");

        boolean result = TypeMappings.requiresImport(typeRef);

        assertThat(result).isTrue();
    }

    @Test
    void requiresImport_shouldReturnFalseForJavaLangString() {
        TypeRef typeRef = TypeRef.of("java.lang.String");

        boolean result = TypeMappings.requiresImport(typeRef);

        assertThat(result).isFalse();
    }

    @Test
    void requiresImport_shouldReturnFalseForJavaLangInteger() {
        TypeRef typeRef = TypeRef.of("java.lang.Integer");

        boolean result = TypeMappings.requiresImport(typeRef);

        assertThat(result).isFalse();
    }

    @Test
    void requiresImport_shouldReturnFalseForPrimitives() {
        TypeRef typeRef = TypeRef.primitive("int");

        boolean result = TypeMappings.requiresImport(typeRef);

        assertThat(result).isFalse();
    }

    @Test
    void requiresImport_shouldThrowExceptionForNullTypeRef() {
        assertThatThrownBy(() -> TypeMappings.requiresImport(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TypeRef cannot be null");
    }

    // =====================================================================
    // packageName tests
    // =====================================================================

    @Test
    void packageName_shouldExtractPackageFromQualifiedName() {
        TypeRef typeRef = TypeRef.of("com.example.domain.Order");

        String result = TypeMappings.packageName(typeRef);

        assertThat(result).isEqualTo("com.example.domain");
    }

    @Test
    void packageName_shouldReturnEmptyStringForPrimitives() {
        TypeRef typeRef = TypeRef.primitive("int");

        String result = TypeMappings.packageName(typeRef);

        assertThat(result).isEmpty();
    }

    @Test
    void packageName_shouldExtractJavaUtilPackage() {
        TypeRef typeRef = TypeRef.of("java.util.UUID");

        String result = TypeMappings.packageName(typeRef);

        assertThat(result).isEqualTo("java.util");
    }

    @Test
    void packageName_shouldThrowExceptionForNullTypeRef() {
        assertThatThrownBy(() -> TypeMappings.packageName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TypeRef cannot be null");
    }

    // =====================================================================
    // unwrapElement tests
    // =====================================================================

    @Test
    void unwrapElement_shouldExtractElementTypeFromList() {
        TypeRef elementType = TypeRef.of("com.example.Order");
        TypeRef listType = TypeRef.parameterized("java.util.List", elementType);

        TypeName result = TypeMappings.unwrapElement(listType);

        assertThat(result).isEqualTo(ClassName.bestGuess("com.example.Order"));
    }

    @Test
    void unwrapElement_shouldExtractElementTypeFromOptional() {
        TypeRef elementType = TypeRef.of("com.example.Customer");
        TypeRef optionalType = TypeRef.parameterized("java.util.Optional", elementType);

        TypeName result = TypeMappings.unwrapElement(optionalType);

        assertThat(result).isEqualTo(ClassName.bestGuess("com.example.Customer"));
    }

    @Test
    void unwrapElement_shouldReturnUnchangedForSimpleType() {
        TypeRef typeRef = TypeRef.of("java.lang.String");

        TypeName result = TypeMappings.unwrapElement(typeRef);

        assertThat(result).isEqualTo(TypeName.get(String.class));
    }

    @Test
    void unwrapElement_shouldThrowExceptionForNullTypeRef() {
        assertThatThrownBy(() -> TypeMappings.unwrapElement(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TypeRef cannot be null");
    }
}
