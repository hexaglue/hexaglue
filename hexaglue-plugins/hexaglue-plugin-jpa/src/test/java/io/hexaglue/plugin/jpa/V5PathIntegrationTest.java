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

package io.hexaglue.plugin.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the v5 model path in JPA generation.
 *
 * <p>These tests verify that the v5 enriched model types (AggregateRoot, Entity, ValueObject)
 * work correctly with their enrichments (FieldRole, isSingleValue(), wrappedField(), etc.)
 *
 * <p>These tests validate that the v5 model types are suitable for JPA code generation
 * without requiring a full ArchitecturalModel setup.
 *
 * @since 5.0.0
 */
@DisplayName("V5 Path Integration Tests")
class V5PathIntegrationTest {

    @Nested
    @DisplayName("AggregateRoot v5 enrichments")
    class AggregateRootV5Enrichments {

        @Test
        @DisplayName("should have required identity field (non-null)")
        void shouldHaveRequiredIdentityField() {
            // Given: A v5 AggregateRoot with proper identity field
            Field identityField = createIdentityField("id", "java.util.UUID");
            TypeStructure structure = createStructureWithFields(
                    TypeNature.RECORD, List.of(identityField, createSimpleField("name", "java.lang.String")));
            ClassificationTrace trace = createTrace(ElementKind.AGGREGATE_ROOT);

            // When: Creating the AggregateRoot
            AggregateRoot order = AggregateRoot.builder(
                            TypeId.of("com.example.domain.Order"), structure, trace, identityField)
                    .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                    .build();

            // Then: Identity field should be accessible (REQUIRED - never null)
            assertThat(order.identityField()).isNotNull();
            assertThat(order.identityField().name()).isEqualTo("id");
            assertThat(order.identityField().hasRole(FieldRole.IDENTITY)).isTrue();
        }

        @Test
        @DisplayName("should have structure with fields accessible")
        void shouldHaveStructureWithFieldsAccessible() {
            // Given: A v5 AggregateRoot with multiple fields
            Field idField = createIdentityField("id", "java.util.UUID");
            Field nameField = createSimpleField("name", "java.lang.String");
            Field statusField = createSimpleField("status", "java.lang.String");

            TypeStructure structure =
                    createStructureWithFields(TypeNature.RECORD, List.of(idField, nameField, statusField));
            ClassificationTrace trace = createTrace(ElementKind.AGGREGATE_ROOT);

            AggregateRoot order = AggregateRoot.builder(
                            TypeId.of("com.example.domain.Order"), structure, trace, idField)
                    .build();

            // Then: Structure fields should be accessible for JPA generation
            assertThat(order.structure().fields()).hasSize(3);
            assertThat(order.structure().getFieldsWithRole(FieldRole.IDENTITY)).hasSize(1);
        }

        @Test
        @DisplayName("should track entities and value objects in aggregate")
        void shouldTrackEntitiesAndValueObjectsInAggregate() {
            // Given: An AggregateRoot with referenced entities and value objects
            Field idField = createIdentityField("id", "java.util.UUID");
            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(idField));
            ClassificationTrace trace = createTrace(ElementKind.AGGREGATE_ROOT);

            AggregateRoot order = AggregateRoot.builder(
                            TypeId.of("com.example.domain.Order"), structure, trace, idField)
                    .entities(List.of(TypeRef.of("com.example.domain.OrderLine")))
                    .valueObjects(
                            List.of(TypeRef.of("com.example.domain.Money"), TypeRef.of("com.example.domain.Address")))
                    .build();

            // Then: Aggregate should track its components
            assertThat(order.entities()).containsExactly(TypeRef.of("com.example.domain.OrderLine"));
            assertThat(order.valueObjects()).hasSize(2);
        }

        @Test
        @DisplayName("should track driven port (repository)")
        void shouldTrackDrivenPort() {
            // Given: An AggregateRoot with associated repository
            Field idField = createIdentityField("id", "java.util.UUID");
            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(idField));
            ClassificationTrace trace = createTrace(ElementKind.AGGREGATE_ROOT);

            AggregateRoot order = AggregateRoot.builder(
                            TypeId.of("com.example.domain.Order"), structure, trace, idField)
                    .drivenPort(TypeRef.of("com.example.domain.OrderRepository"))
                    .build();

            // Then: Should track the repository
            assertThat(order.hasDrivenPort()).isTrue();
            assertThat(order.drivenPort()).isPresent();
            assertThat(order.drivenPort().get().qualifiedName()).isEqualTo("com.example.domain.OrderRepository");
        }
    }

    @Nested
    @DisplayName("Entity v5 enrichments")
    class EntityV5Enrichments {

        @Test
        @DisplayName("should have optional identity field")
        void shouldHaveOptionalIdentityField() {
            // Given: A v5 Entity with identity field
            Field idField = createIdentityField("id", "java.lang.Long");
            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(idField));
            ClassificationTrace trace = createTrace(ElementKind.ENTITY);

            Entity orderLine = Entity.of(TypeId.of("com.example.domain.OrderLine"), structure, trace, idField);

            // Then: Identity field should be accessible
            assertThat(orderLine.identityField()).isPresent();
            assertThat(orderLine.identityField().get().name()).isEqualTo("id");
        }

        @Test
        @DisplayName("should track owning aggregate")
        void shouldTrackOwningAggregate() {
            // Given: An Entity that belongs to an aggregate
            Field idField = createIdentityField("id", "java.lang.Long");
            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(idField));
            ClassificationTrace trace = createTrace(ElementKind.ENTITY);

            Entity orderLine = Entity.of(
                    TypeId.of("com.example.domain.OrderLine"),
                    structure,
                    trace,
                    Optional.of(idField),
                    Optional.of(TypeRef.of("com.example.domain.Order")));

            // Then: Should track the owning aggregate
            assertThat(orderLine.owningAggregate()).isPresent();
            assertThat(orderLine.owningAggregate().get().qualifiedName()).isEqualTo("com.example.domain.Order");
        }
    }

    @Nested
    @DisplayName("ValueObject isSingleValue() enrichment")
    class ValueObjectSingleValueEnrichment {

        @Test
        @DisplayName("should detect single-value ValueObject with one field")
        void shouldDetectSingleValueWithOneField() {
            // Given: A ValueObject with exactly one field
            Field valueField = Field.builder("value", TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                    .build();

            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(valueField));
            ClassificationTrace trace = createTrace(ElementKind.VALUE_OBJECT);

            ValueObject orderId = ValueObject.of(TypeId.of("com.example.domain.OrderId"), structure, trace);

            // Then: Should be detected as single-value
            assertThat(orderId.isSingleValue()).isTrue();
            assertThat(orderId.wrappedField()).isPresent();
            assertThat(orderId.wrappedField().get().name()).isEqualTo("value");
            assertThat(orderId.wrappedField().get().type().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should not detect multi-value ValueObject as single-value")
        void shouldNotDetectMultiValueAsSingleValue() {
            // Given: A ValueObject with multiple fields
            Field amountField = Field.builder("amount", TypeRef.of("java.math.BigDecimal"))
                    .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                    .build();
            Field currencyField = Field.builder("currency", TypeRef.of("java.util.Currency"))
                    .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                    .build();

            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(amountField, currencyField));
            ClassificationTrace trace = createTrace(ElementKind.VALUE_OBJECT);

            ValueObject money = ValueObject.of(TypeId.of("com.example.domain.Money"), structure, trace);

            // Then: Should not be detected as single-value
            assertThat(money.isSingleValue()).isFalse();
            assertThat(money.wrappedField()).isEmpty();
        }

        @Test
        @DisplayName("should return wrapped field type for JPA mapping")
        void shouldReturnWrappedFieldTypeForJpaMapping() {
            // Given: A single-value ValueObject wrapping UUID
            Field valueField =
                    Field.builder("value", TypeRef.of("java.util.UUID")).build();
            TypeStructure structure = createStructureWithFields(TypeNature.RECORD, List.of(valueField));
            ClassificationTrace trace = createTrace(ElementKind.VALUE_OBJECT);

            ValueObject orderId = ValueObject.of(TypeId.of("com.example.domain.OrderId"), structure, trace);

            // Then: Wrapped field type should be UUID for JPA @Id mapping
            assertThat(orderId.wrappedField().get().type().qualifiedName()).isEqualTo("java.util.UUID");
        }
    }

    @Nested
    @DisplayName("Field FieldRole enrichments")
    class FieldRoleEnrichments {

        @Test
        @DisplayName("should detect IDENTITY role")
        void shouldDetectIdentityRole() {
            // Given: A field with IDENTITY role
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            // Then: Field should have IDENTITY role
            assertThat(idField.hasRole(FieldRole.IDENTITY)).isTrue();
        }

        @Test
        @DisplayName("should detect COLLECTION role with element type")
        void shouldDetectCollectionRoleWithElementType() {
            // Given: A field with COLLECTION role and element type
            Field itemsField = Field.builder("items", TypeRef.of("java.util.List"))
                    .modifiers(Set.of(Modifier.PRIVATE))
                    .elementType(TypeRef.of("com.example.domain.OrderItem"))
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            // Then: Field should have COLLECTION role and element type
            assertThat(itemsField.hasRole(FieldRole.COLLECTION)).isTrue();
            assertThat(itemsField.elementType()).isPresent();
            assertThat(itemsField.elementType().get().qualifiedName()).isEqualTo("com.example.domain.OrderItem");
        }

        @Test
        @DisplayName("should detect EMBEDDED role for value objects")
        void shouldDetectEmbeddedRoleForValueObjects() {
            // Given: A field with EMBEDDED role
            Field addressField = Field.builder("address", TypeRef.of("com.example.domain.Address"))
                    .modifiers(Set.of(Modifier.PRIVATE))
                    .roles(Set.of(FieldRole.EMBEDDED))
                    .build();

            // Then: Field should have EMBEDDED role for JPA @Embedded mapping
            assertThat(addressField.hasRole(FieldRole.EMBEDDED)).isTrue();
        }

        @Test
        @DisplayName("should detect AUDIT role for timestamp fields")
        void shouldDetectAuditRoleForTimestampFields() {
            // Given: A field with AUDIT role
            Field createdAtField = Field.builder("createdAt", TypeRef.of("java.time.Instant"))
                    .modifiers(Set.of(Modifier.PRIVATE))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();

            // Then: Field should have AUDIT role for JPA auditing
            assertThat(createdAtField.hasRole(FieldRole.AUDIT)).isTrue();
        }

        @Test
        @DisplayName("should support multiple roles on same field")
        void shouldSupportMultipleRoles() {
            // Given: A field with multiple roles
            Field complexField = Field.builder("data", TypeRef.of("java.lang.Object"))
                    .modifiers(Set.of(Modifier.PRIVATE))
                    .roles(Set.of(FieldRole.EMBEDDED, FieldRole.TECHNICAL))
                    .build();

            // Then: Field should have all specified roles
            assertThat(complexField.hasRole(FieldRole.EMBEDDED)).isTrue();
            assertThat(complexField.hasRole(FieldRole.TECHNICAL)).isTrue();
            assertThat(complexField.roles()).containsExactlyInAnyOrder(FieldRole.EMBEDDED, FieldRole.TECHNICAL);
        }

        @Test
        @DisplayName("should detect AGGREGATE_REFERENCE role for cross-aggregate relations")
        void shouldDetectAggregateReferenceRoleForCrossAggregateRelations() {
            // Given: A field referencing another aggregate
            Field customerField = Field.builder("customerId", TypeRef.of("com.example.domain.CustomerId"))
                    .modifiers(Set.of(Modifier.PRIVATE))
                    .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                    .build();

            // Then: Field should have AGGREGATE_REFERENCE role for cross-aggregate references
            assertThat(customerField.hasRole(FieldRole.AGGREGATE_REFERENCE)).isTrue();
        }
    }

    // Helper methods for creating test fixtures

    private Field createIdentityField(String name, String type) {
        return Field.builder(name, TypeRef.of(type))
                .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
    }

    private Field createSimpleField(String name, String type) {
        return Field.builder(name, TypeRef.of(type))
                .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                .build();
    }

    private TypeStructure createStructureWithFields(TypeNature nature, List<Field> fields) {
        return TypeStructure.builder(nature)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(fields)
                .build();
    }

    private ClassificationTrace createTrace(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test-criterion", "Test classification for " + kind);
    }
}
