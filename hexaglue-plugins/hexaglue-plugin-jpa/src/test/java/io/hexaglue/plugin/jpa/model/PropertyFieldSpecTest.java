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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.spi.ir.Cardinality;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.Nullability;
import io.hexaglue.spi.ir.RelationInfo;
import io.hexaglue.spi.ir.RelationKind;
import io.hexaglue.spi.ir.SourceRef;
import io.hexaglue.spi.ir.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PropertyFieldSpec} SPI mapping validation.
 *
 * <p>These tests validate that PropertyFieldSpec correctly maps SPI DomainProperty
 * to the intermediate representation needed for JavaPoet code generation.
 */
class PropertyFieldSpecTest {

    @Test
    void from_shouldMapSimpleProperty_whenNonNullable() {
        // Given: A simple non-nullable property
        DomainProperty property = new DomainProperty(
                "firstName", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        // When: Converting to PropertyFieldSpec
        PropertyFieldSpec spec = PropertyFieldSpec.from(property);

        // Then: All fields should be correctly mapped
        assertThat(spec.fieldName()).isEqualTo("firstName");
        assertThat(spec.javaType().toString()).isEqualTo("java.lang.String");
        assertThat(spec.nullability()).isEqualTo(Nullability.NON_NULL);
        assertThat(spec.columnName()).isEqualTo("first_name");
        assertThat(spec.isEmbedded()).isFalse();
        assertThat(spec.isValueObject()).isFalse();
        assertThat(spec.shouldBeEmbedded()).isFalse();
        assertThat(spec.isRequired()).isTrue();
        assertThat(spec.isNullable()).isFalse();
    }

    @Test
    void from_shouldMapNullableProperty_whenNullable() {
        // Given: A nullable property
        DomainProperty property = new DomainProperty(
                "middleName", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.NULLABLE, false);

        // When: Converting to PropertyFieldSpec
        PropertyFieldSpec spec = PropertyFieldSpec.from(property);

        // Then: Nullability should be preserved
        assertThat(spec.nullability()).isEqualTo(Nullability.NULLABLE);
        assertThat(spec.isNullable()).isTrue();
        assertThat(spec.isRequired()).isFalse();
    }

    @Test
    void from_shouldMapEmbeddedProperty_whenValueObjectEmbeddedWithoutRelation() {
        // Given: An embedded value object property WITHOUT explicit relation
        // (some value objects might be embedded but not have RelationInfo)
        DomainProperty property = new DomainProperty(
                "shippingAddress",
                TypeRef.of("com.example.Address"),
                Cardinality.SINGLE,
                Nullability.NON_NULL,
                false,
                true,
                null // No explicit relation info - just marked as embedded
                );

        // When: Converting to PropertyFieldSpec
        PropertyFieldSpec spec = PropertyFieldSpec.from(property);

        // Then: Embedded flag should be preserved
        assertThat(spec.isEmbedded()).isTrue();
        assertThat(spec.fieldName()).isEqualTo("shippingAddress");
        assertThat(spec.javaType().toString()).isEqualTo("com.example.Address");
    }

    @Test
    void from_shouldConvertToSnakeCase_whenCamelCase() {
        // Given: Properties with various camelCase names
        DomainProperty simpleCase =
                new DomainProperty("id", TypeRef.of("java.lang.Long"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        DomainProperty multiWordCase = new DomainProperty(
                "totalAmountInCents", TypeRef.of("java.lang.Long"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        // When: Converting to PropertyFieldSpec
        PropertyFieldSpec simpleSpec = PropertyFieldSpec.from(simpleCase);
        PropertyFieldSpec multiSpec = PropertyFieldSpec.from(multiWordCase);

        // Then: Column names should be in snake_case
        assertThat(simpleSpec.columnName()).isEqualTo("id");
        assertThat(multiSpec.columnName()).isEqualTo("total_amount_in_cents");
    }

    @Test
    void from_shouldRejectRelationProperty_whenHasRelation() {
        // Given: A property with a relation (should use RelationFieldSpec instead)
        RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.ONE_TO_MANY, "com.example.LineItem");
        DomainProperty relationProperty = new DomainProperty(
                "items",
                TypeRef.parameterized("java.util.List", TypeRef.of("com.example.LineItem")),
                Cardinality.COLLECTION,
                Nullability.NON_NULL,
                false,
                false,
                relationInfo);

        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> PropertyFieldSpec.from(relationProperty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has a relation")
                .hasMessageContaining("RelationFieldSpec");
    }

    @Test
    void from_shouldRejectIdentityProperty_whenIsIdentity() {
        // Given: An identity property (should use IdFieldSpec instead)
        DomainProperty identityProperty =
                new DomainProperty("id", TypeRef.of("java.util.UUID"), Cardinality.SINGLE, Nullability.NON_NULL, true);

        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> PropertyFieldSpec.from(identityProperty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is an identity")
                .hasMessageContaining("IdFieldSpec");
    }

    // =====================================================================
    // Value Object detection tests
    // =====================================================================

    @Test
    void from_shouldDetectValueObject_whenTypeIsValueObjectInAllTypes() {
        // Given: A property whose type is a Value Object
        DomainProperty emailProperty = new DomainProperty(
                "email", TypeRef.of("com.example.Email"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        // And: A list of all types containing Email as a Value Object
        DomainType emailValueObject = new DomainType(
                "com.example.Email",
                "Email",
                "com.example",
                ElementKind.VALUE_OBJECT,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(new DomainProperty(
                        "value", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.NON_NULL, false)),
                List.of(),
                SourceRef.unknown());

        List<DomainType> allTypes = List.of(emailValueObject);

        // When: Converting to PropertyFieldSpec with allTypes
        PropertyFieldSpec spec = PropertyFieldSpec.from(emailProperty, allTypes);

        // Then: Single-property VALUE_OBJECT records are treated as simple wrappers
        // They are unwrapped to their primitive type instead of being embedded
        assertThat(spec.isValueObject()).isTrue();
        assertThat(spec.isEmbedded()).isFalse();
        assertThat(spec.shouldBeEmbedded()).isFalse(); // Simple wrappers are unwrapped, not embedded
        assertThat(spec.typeQualifiedName()).isEqualTo("com.example.Email");
        assertThat(spec.unwrappedType()).isNotNull();
        assertThat(spec.unwrappedType().toString()).isEqualTo("java.lang.String");
        assertThat(spec.wrapperAccessorMethod()).isEqualTo("value");
    }

    @Test
    void from_shouldDetectValueObjectAsEmbedded_whenMultipleProperties() {
        // Given: A property whose type is a Value Object with multiple properties
        DomainProperty moneyProperty = new DomainProperty(
                "totalPrice", TypeRef.of("com.example.Money"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        // And: A list of all types containing Money as a Value Object with 2 properties
        DomainType moneyValueObject = new DomainType(
                "com.example.Money",
                "Money",
                "com.example",
                ElementKind.VALUE_OBJECT,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(
                        new DomainProperty(
                                "amount",
                                TypeRef.of("java.math.BigDecimal"),
                                Cardinality.SINGLE,
                                Nullability.NON_NULL,
                                false),
                        new DomainProperty(
                                "currency",
                                TypeRef.of("java.util.Currency"),
                                Cardinality.SINGLE,
                                Nullability.NON_NULL,
                                false)),
                List.of(),
                SourceRef.unknown());

        List<DomainType> allTypes = List.of(moneyValueObject);

        // When: Converting to PropertyFieldSpec with allTypes
        PropertyFieldSpec spec = PropertyFieldSpec.from(moneyProperty, allTypes);

        // Then: Multi-property VALUE_OBJECTs should be embedded
        assertThat(spec.isValueObject()).isTrue();
        assertThat(spec.isEmbedded()).isFalse();
        assertThat(spec.shouldBeEmbedded()).isTrue(); // Multi-property VALUE_OBJECTs are embedded
        assertThat(spec.typeQualifiedName()).isEqualTo("com.example.Money");
        assertThat(spec.unwrappedType()).isNull(); // Not a simple wrapper
        assertThat(spec.wrapperAccessorMethod()).isNull();
    }

    @Test
    void from_shouldNotDetectValueObject_whenTypeIsNotInAllTypes() {
        // Given: A property whose type is not in allTypes
        DomainProperty emailProperty = new DomainProperty(
                "email", TypeRef.of("com.example.Email"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        // And: An empty list of allTypes
        List<DomainType> allTypes = List.of();

        // When: Converting to PropertyFieldSpec with empty allTypes
        PropertyFieldSpec spec = PropertyFieldSpec.from(emailProperty, allTypes);

        // Then: isValueObject should be false
        assertThat(spec.isValueObject()).isFalse();
        assertThat(spec.shouldBeEmbedded()).isFalse();
    }

    @Test
    void from_shouldNotDetectValueObject_whenTypeIsEntity() {
        // Given: A property whose type is an Entity (not a Value Object)
        DomainProperty itemProperty = new DomainProperty(
                "owner", TypeRef.of("com.example.Person"), Cardinality.SINGLE, Nullability.NON_NULL, false);

        // And: A list of all types containing Person as an Entity
        DomainType personEntity = new DomainType(
                "com.example.Person",
                "Person",
                "com.example",
                ElementKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.empty(),
                List.of(),
                List.of(),
                SourceRef.unknown());

        List<DomainType> allTypes = List.of(personEntity);

        // When: Converting to PropertyFieldSpec with allTypes
        PropertyFieldSpec spec = PropertyFieldSpec.from(itemProperty, allTypes);

        // Then: isValueObject should be false
        assertThat(spec.isValueObject()).isFalse();
        assertThat(spec.shouldBeEmbedded()).isFalse();
    }

    @Test
    void shouldBeEmbedded_shouldReturnTrue_whenEmbeddedEvenIfNotValueObject() {
        // Given: A property marked as embedded but not a Value Object type
        DomainProperty addressProperty = new DomainProperty(
                "address",
                TypeRef.of("com.example.Address"),
                Cardinality.SINGLE,
                Nullability.NON_NULL,
                false,
                true, // isEmbedded = true
                null);

        // When: Converting to PropertyFieldSpec (with empty allTypes)
        PropertyFieldSpec spec = PropertyFieldSpec.from(addressProperty, List.of());

        // Then: shouldBeEmbedded should be true (from isEmbedded flag)
        assertThat(spec.isEmbedded()).isTrue();
        assertThat(spec.isValueObject()).isFalse();
        assertThat(spec.shouldBeEmbedded()).isTrue();
    }
}
