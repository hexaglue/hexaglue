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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PropertyFieldSpec} handling of domain types in JPA entities.
 *
 * <p>These tests validate that domain types (Identifiers, Enums) are properly
 * converted to JPA-compatible types to avoid coupling between infrastructure
 * and domain layers.
 *
 * @since 5.0.0
 */
class PropertyFieldSpecTest {

    // =====================================================================
    // Test fixtures
    // =====================================================================

    private static TypeStructure createRecordStructure(List<Field> fields) {
        return TypeStructure.builder(TypeNature.RECORD)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(fields)
                .build();
    }

    private static TypeStructure createEnumStructure() {
        return TypeStructure.builder(TypeNature.ENUM)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
    }

    private static Field createField(String name, String typeFqn, Set<FieldRole> roles) {
        return new Field(
                name,
                TypeRef.of(typeFqn),
                Set.of(Modifier.PRIVATE),
                List.of(),        // annotations
                Optional.empty(), // documentation
                Optional.empty(), // wrappedType
                Optional.empty(), // elementType
                roles
        );
    }

    private static ClassificationTrace createTrace(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    private static ArchitecturalModel createModelWithDomainIndex(DomainIndex domainIndex) {
        ProjectContext project = ProjectContext.forTesting("test-project", "com.example");
        return ArchitecturalModel.builder(project)
                .domainIndex(domainIndex)
                .build();
    }

    // =====================================================================
    // C2 Bug: Cross-aggregate Identifier types in JPA entities
    // =====================================================================

    @Nested
    @DisplayName("C2 Bug: Cross-aggregate Identifier handling")
    class CrossAggregateIdentifierTests {

        /**
         * Reproduces C2 bug: CustomerId (cross-aggregate Identifier) should be
         * unwrapped to UUID, not used with @Embedded.
         *
         * <p>When an OrderEntity has a field `customerId: CustomerId`, and CustomerId
         * is an Identifier record wrapping UUID, the generated JPA entity should use:
         * {@code @Column private UUID customerId;}
         * instead of:
         * {@code @Embedded private CustomerId customerId;}
         */
        @Test
        @DisplayName("Cross-aggregate Identifier should be unwrapped to primitive type")
        void crossAggregateIdentifier_shouldBeUnwrappedToUuid() {
            // Given: A field with type CustomerId (an Identifier wrapping UUID)
            Field customerIdField = createField(
                    "customerId",
                    "com.ecommerce.domain.customer.CustomerId",
                    Set.of() // No special role - it's just a reference
            );

            // Create the CustomerId Identifier in registry
            Field valueField = new Field(
                    "value",
                    TypeRef.of("java.util.UUID"),
                    Set.of(Modifier.PRIVATE, Modifier.FINAL),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of()
            );
            TypeStructure customerIdStructure = createRecordStructure(List.of(valueField));
            Identifier customerId = Identifier.of(
                    TypeId.of("com.ecommerce.domain.customer.CustomerId"),
                    customerIdStructure,
                    createTrace(ElementKind.IDENTIFIER),
                    TypeRef.of("java.util.UUID")
            );

            // Create registry and domain index with CustomerId
            TypeRegistry registry = TypeRegistry.builder()
                    .add(customerId)
                    .build();
            DomainIndex domainIndex = DomainIndex.from(registry);
            ArchitecturalModel model = createModelWithDomainIndex(domainIndex);

            // When: Converting to PropertyFieldSpec
            PropertyFieldSpec spec = PropertyFieldSpec.fromV5(customerIdField, model, Map.of(), null);

            // Then: Should unwrap to UUID, not be embedded
            assertThat(spec.unwrappedType())
                    .as("Cross-aggregate Identifier should have unwrappedType set to UUID")
                    .isNotNull()
                    .isEqualTo(TypeName.get(UUID.class));

            assertThat(spec.effectiveJpaType())
                    .as("effectiveJpaType should return UUID, not CustomerId")
                    .isEqualTo(TypeName.get(UUID.class));

            assertThat(spec.shouldBeEmbedded())
                    .as("Cross-aggregate Identifier should NOT be embedded")
                    .isFalse();

            assertThat(spec.isWrappedForeignKey())
                    .as("Cross-aggregate Identifier ending in 'Id' should be marked as wrapped foreign key")
                    .isTrue();
        }

        @Test
        @DisplayName("Cross-aggregate Identifier should have correct accessor method")
        void crossAggregateIdentifier_shouldHaveCorrectAccessorMethod() {
            // Given: A field with type ProductId (an Identifier wrapping UUID)
            Field productIdField = createField(
                    "productId",
                    "com.example.ProductId",
                    Set.of()
            );

            // Create the ProductId Identifier
            Field valueField = new Field(
                    "value",
                    TypeRef.of("java.util.UUID"),
                    Set.of(Modifier.PRIVATE, Modifier.FINAL),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of()
            );
            TypeStructure productIdStructure = createRecordStructure(List.of(valueField));
            Identifier productId = Identifier.of(
                    TypeId.of("com.example.ProductId"),
                    productIdStructure,
                    createTrace(ElementKind.IDENTIFIER),
                    TypeRef.of("java.util.UUID")
            );

            TypeRegistry registry = TypeRegistry.builder()
                    .add(productId)
                    .build();
            DomainIndex domainIndex = DomainIndex.from(registry);
            ArchitecturalModel model = createModelWithDomainIndex(domainIndex);

            // When
            PropertyFieldSpec spec = PropertyFieldSpec.fromV5(productIdField, model, Map.of(), null);

            // Then: Accessor method should be "value" for records
            assertThat(spec.wrapperAccessorMethod())
                    .as("Record-based Identifier should use 'value' accessor")
                    .isEqualTo("value");
        }
    }

    // =====================================================================
    // C2 Bug: Domain Enum types in JPA entities
    // =====================================================================

    @Nested
    @DisplayName("C2 Bug: Domain Enum handling")
    class DomainEnumTests {

        /**
         * Reproduces C2 bug: OrderStatus (domain enum) should be detected as enum
         * even when classified as VALUE_OBJECT.
         *
         * <p>When an OrderEntity has a field `status: OrderStatus`, the generated
         * JPA entity should use:
         * {@code @Enumerated(EnumType.STRING) @Column private OrderStatus status;}
         * and NOT:
         * {@code @Column private OrderStatus status;} (without @Enumerated)
         */
        @Test
        @DisplayName("Domain enum classified as VALUE_OBJECT should be detected as enum")
        void domainEnum_classifiedAsValueObject_shouldBeDetectedAsEnum() {
            // Given: A field with type OrderStatus (an enum classified as VALUE_OBJECT)
            Field statusField = createField(
                    "status",
                    "com.ecommerce.domain.order.OrderStatus",
                    Set.of()
            );

            // Create OrderStatus as a VALUE_OBJECT with TypeNature.ENUM
            TypeStructure enumStructure = createEnumStructure();
            ValueObject orderStatus = ValueObject.of(
                    TypeId.of("com.ecommerce.domain.order.OrderStatus"),
                    enumStructure,
                    createTrace(ElementKind.VALUE_OBJECT)
            );

            TypeRegistry registry = TypeRegistry.builder()
                    .add(orderStatus)
                    .build();
            DomainIndex domainIndex = DomainIndex.from(registry);
            ArchitecturalModel model = createModelWithDomainIndex(domainIndex);

            // When
            PropertyFieldSpec spec = PropertyFieldSpec.fromV5(statusField, model, Map.of(), null);

            // Then: Should be detected as enum
            assertThat(spec.isEnum())
                    .as("Enum type should be detected via TypeNature.ENUM")
                    .isTrue();

            assertThat(spec.shouldBeEmbedded())
                    .as("Enums should never be embedded")
                    .isFalse();
        }

        /**
         * Tests that enums NOT classified as VALUE_OBJECT are still handled correctly.
         *
         * <p>This validates that the code doesn't fail when an enum is not in the
         * domainIndex (e.g., not classified or classified differently).
         */
        @Test
        @DisplayName("Enum not in domainIndex should not cause errors")
        void enumNotInDomainIndex_shouldBeHandledGracefully() {
            // Given: A field with enum type not in domainIndex
            Field statusField = createField(
                    "status",
                    "com.example.UnclassifiedStatus",
                    Set.of()
            );

            // Empty registry and domain index
            TypeRegistry registry = TypeRegistry.builder().build();
            DomainIndex domainIndex = DomainIndex.from(registry);
            ArchitecturalModel model = createModelWithDomainIndex(domainIndex);

            // When
            PropertyFieldSpec spec = PropertyFieldSpec.fromV5(statusField, model, Map.of(), null);

            // Then: Should not throw, and should be treated as simple property
            assertThat(spec.isEnum())
                    .as("Unclassified enum should not be detected as enum without structure info")
                    .isFalse();

            assertThat(spec.shouldBeEmbedded())
                    .as("Unclassified type should not be embedded by default")
                    .isFalse();
        }
    }

    // =====================================================================
    // Integration tests for embeddable mapping
    // =====================================================================

    @Nested
    @DisplayName("Embeddable mapping integration")
    class EmbeddableMappingTests {

        @Test
        @DisplayName("Complex VALUE_OBJECT should use embeddable mapping")
        void complexValueObject_shouldUseEmbeddableMapping() {
            // Given: A field with type Address (complex VALUE_OBJECT)
            Field addressField = createField(
                    "shippingAddress",
                    "com.example.Address",
                    Set.of()
            );

            // Create Address as a VALUE_OBJECT with multiple fields
            Field streetField = createField("street", "java.lang.String", Set.of());
            Field cityField = createField("city", "java.lang.String", Set.of());
            TypeStructure addressStructure = createRecordStructure(List.of(streetField, cityField));
            ValueObject address = ValueObject.of(
                    TypeId.of("com.example.Address"),
                    addressStructure,
                    createTrace(ElementKind.VALUE_OBJECT)
            );

            TypeRegistry registry = TypeRegistry.builder()
                    .add(address)
                    .build();
            DomainIndex domainIndex = DomainIndex.from(registry);
            ArchitecturalModel model = createModelWithDomainIndex(domainIndex);

            // Embeddable mapping provided
            Map<String, String> embeddableMapping = Map.of(
                    "com.example.Address", "com.example.infrastructure.AddressEmbeddable"
            );

            // When
            PropertyFieldSpec spec = PropertyFieldSpec.fromV5(addressField, model, embeddableMapping, null);

            // Then: Should use embeddable type
            assertThat(spec.shouldBeEmbedded())
                    .as("Complex VALUE_OBJECT with embeddable mapping should be embedded")
                    .isTrue();

            assertThat(spec.effectiveJpaType())
                    .as("Should use embeddable type from mapping")
                    .isEqualTo(ClassName.bestGuess("com.example.infrastructure.AddressEmbeddable"));
        }

        @Test
        @DisplayName("Simple wrapper VALUE_OBJECT should NOT use embeddable mapping")
        void simpleWrapperValueObject_shouldNotUseEmbeddableMapping() {
            // Given: A field with type Quantity (simple wrapper VALUE_OBJECT)
            Field quantityField = createField(
                    "quantity",
                    "com.example.Quantity",
                    Set.of()
            );

            // Create Quantity as a single-field record VALUE_OBJECT
            // The wrappedType is determined by Field.wrappedType() in the VO's structure
            Field valueField = new Field(
                    "value",
                    TypeRef.of("int"),
                    Set.of(Modifier.PRIVATE, Modifier.FINAL),
                    List.of(),
                    Optional.empty(), // documentation
                    Optional.of(TypeRef.of("int")), // wrappedType
                    Optional.empty(), // elementType
                    Set.of()
            );
            TypeStructure quantityStructure = createRecordStructure(List.of(valueField));
            ValueObject quantity = ValueObject.of(
                    TypeId.of("com.example.Quantity"),
                    quantityStructure,
                    createTrace(ElementKind.VALUE_OBJECT)
            );

            TypeRegistry registry = TypeRegistry.builder()
                    .add(quantity)
                    .build();
            DomainIndex domainIndex = DomainIndex.from(registry);
            ArchitecturalModel model = createModelWithDomainIndex(domainIndex);

            // Embeddable mapping provided (but should not be used for simple wrappers)
            Map<String, String> embeddableMapping = Map.of(
                    "com.example.Quantity", "com.example.infrastructure.QuantityEmbeddable"
            );

            // When
            PropertyFieldSpec spec = PropertyFieldSpec.fromV5(quantityField, model, embeddableMapping, null);

            // Then: Should be unwrapped, not embedded
            assertThat(spec.shouldBeEmbedded())
                    .as("Simple wrapper VALUE_OBJECT should NOT be embedded")
                    .isFalse();
        }
    }
}
