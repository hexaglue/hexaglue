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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.ir.CascadeType;
import io.hexaglue.arch.model.ir.DomainRelation;
import io.hexaglue.arch.model.ir.FetchType;
import io.hexaglue.arch.model.ir.RelationKind;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RelationFieldSpec} SPI mapping validation.
 *
 * <p>These tests validate that RelationFieldSpec correctly maps SPI DomainRelation
 * to the intermediate representation needed for JPA relationship annotations.
 */
class RelationFieldSpecTest {

    @Test
    void from_shouldMapOneToManyRelation_withCascadeAndOrphanRemoval() {
        // Given: A one-to-many relation with cascade ALL and orphan removal
        DomainRelation relation = new DomainRelation(
                "items",
                RelationKind.ONE_TO_MANY,
                "com.example.LineItem",
                ElementKind.ENTITY,
                null,
                CascadeType.ALL,
                FetchType.LAZY,
                true);

        // When: Converting to RelationFieldSpec
        RelationFieldSpec spec = RelationFieldSpec.from(relation);

        // Then: All relationship metadata should be preserved
        assertThat(spec.fieldName()).isEqualTo("items");
        assertThat(spec.targetType().toString()).isEqualTo("java.util.List<com.example.LineItem>");
        assertThat(spec.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
        assertThat(spec.targetKind()).isEqualTo(ElementKind.ENTITY);
        assertThat(spec.cascade()).isEqualTo(CascadeType.ALL);
        assertThat(spec.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(spec.orphanRemoval()).isTrue();
        assertThat(spec.isOwning()).isTrue();
        assertThat(spec.isBidirectional()).isFalse();
        assertThat(spec.isCollection()).isTrue();
        assertThat(spec.targetsEntity()).isTrue();
    }

    @Test
    void from_shouldMapManyToOneRelation_withFetchLazy() {
        // Given: A many-to-one relation with lazy fetch
        DomainRelation relation = new DomainRelation(
                "order",
                RelationKind.MANY_TO_ONE,
                "com.example.Order",
                ElementKind.AGGREGATE_ROOT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        // When: Converting to RelationFieldSpec
        RelationFieldSpec spec = RelationFieldSpec.from(relation);

        // Then: Should be a non-collection reference with lazy loading
        assertThat(spec.fieldName()).isEqualTo("order");
        assertThat(spec.targetType().toString()).isEqualTo("com.example.Order");
        assertThat(spec.kind()).isEqualTo(RelationKind.MANY_TO_ONE);
        assertThat(spec.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(spec.isCollection()).isFalse();
        assertThat(spec.isOwning()).isTrue();
        assertThat(spec.targetsEntity()).isTrue();
    }

    @Test
    void from_shouldMapEmbeddedRelation_whenValueObject() {
        // Given: An embedded value object relation
        DomainRelation relation = new DomainRelation(
                "address",
                RelationKind.EMBEDDED,
                "com.example.Address",
                ElementKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.EAGER,
                false);

        // When: Converting to RelationFieldSpec
        RelationFieldSpec spec = RelationFieldSpec.from(relation);

        // Then: Should be marked as embedded
        assertThat(spec.fieldName()).isEqualTo("address");
        assertThat(spec.targetType().toString()).isEqualTo("com.example.Address");
        assertThat(spec.kind()).isEqualTo(RelationKind.EMBEDDED);
        assertThat(spec.targetKind()).isEqualTo(ElementKind.VALUE_OBJECT);
        assertThat(spec.isEmbedded()).isTrue();
        assertThat(spec.targetsValueObject()).isTrue();
        assertThat(spec.fetch()).isEqualTo(FetchType.EAGER);
    }

    @Test
    void isEmbedded_shouldReturnTrue_whenEmbeddedOrElementCollection() {
        // Given: Embedded and element collection relations
        DomainRelation embedded = DomainRelation.embedded("address", "com.example.Address");
        DomainRelation elementCollection = new DomainRelation(
                "tags",
                RelationKind.ELEMENT_COLLECTION,
                "com.example.Tag",
                ElementKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        // When: Converting to specs
        RelationFieldSpec embeddedSpec = RelationFieldSpec.from(embedded);
        RelationFieldSpec elementSpec = RelationFieldSpec.from(elementCollection);

        // Then: Both should be marked as embedded
        assertThat(embeddedSpec.isEmbedded()).isTrue();
        assertThat(elementSpec.isEmbedded()).isTrue();
    }

    @Test
    void isOwning_shouldReturnTrue_whenNoMappedBy() {
        // Given: An owning side relation (mappedBy is null)
        DomainRelation owningRelation = new DomainRelation(
                "items",
                RelationKind.ONE_TO_MANY,
                "com.example.LineItem",
                ElementKind.ENTITY,
                null,
                CascadeType.ALL,
                FetchType.LAZY,
                true);

        // When: Converting to spec
        RelationFieldSpec spec = RelationFieldSpec.from(owningRelation);

        // Then: Should be owning side
        assertThat(spec.isOwning()).isTrue();
        assertThat(spec.isBidirectional()).isFalse();
        assertThat(spec.mappedBy()).isNull();
    }

    @Test
    void isOwning_shouldReturnFalse_whenMappedByPresent() {
        // Given: An inverse side relation (mappedBy is set)
        DomainRelation inverseRelation = new DomainRelation(
                "order",
                RelationKind.MANY_TO_ONE,
                "com.example.Order",
                ElementKind.AGGREGATE_ROOT,
                "items",
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        // When: Converting to spec
        RelationFieldSpec spec = RelationFieldSpec.from(inverseRelation);

        // Then: Should be inverse side
        assertThat(spec.isOwning()).isFalse();
        assertThat(spec.isBidirectional()).isTrue();
        assertThat(spec.mappedBy()).isEqualTo("items");
    }

    @Test
    void from_shouldWrapInSet_whenManyToMany() {
        // Given: A many-to-many relation
        DomainRelation relation = new DomainRelation(
                "categories",
                RelationKind.MANY_TO_MANY,
                "com.example.Category",
                ElementKind.ENTITY,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        // When: Converting to spec
        RelationFieldSpec spec = RelationFieldSpec.from(relation);

        // Then: Should be wrapped in Set to avoid duplicates
        assertThat(spec.targetType().toString()).isEqualTo("java.util.Set<com.example.Category>");
        assertThat(spec.isCollection()).isTrue();
    }

    @Test
    void from_shouldWrapInList_whenOneToManyOrElementCollection() {
        // Given: One-to-many and element collection relations
        DomainRelation oneToMany = DomainRelation.oneToMany("items", "com.example.LineItem", ElementKind.ENTITY);

        DomainRelation elementCollection = new DomainRelation(
                "tags",
                RelationKind.ELEMENT_COLLECTION,
                "java.lang.String",
                ElementKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        // When: Converting to specs
        RelationFieldSpec oneToManySpec = RelationFieldSpec.from(oneToMany);
        RelationFieldSpec elementSpec = RelationFieldSpec.from(elementCollection);

        // Then: Both should be wrapped in List
        assertThat(oneToManySpec.targetType().toString()).isEqualTo("java.util.List<com.example.LineItem>");
        assertThat(elementSpec.targetType().toString()).isEqualTo("java.util.List<java.lang.String>");
    }

    /**
     * Tests for {@link RelationFieldSpec#fromV5(Field, ArchitecturalModel, Map, Map)}
     * element type extraction and relation kind detection.
     *
     * <p>These tests validate the fix for Issue 4: when {@code field.elementType()} is empty
     * (generics not resolved by the parser), the method should fall back to
     * {@code field.type().typeArguments()} to extract the element type instead of
     * using the raw collection type FQN (e.g., "java.util.List").
     *
     * @since 5.0.0
     */
    @Nested
    @DisplayName("fromV5 element type extraction")
    class FromV5Tests {

        private static ClassificationTrace highConfidence(ElementKind kind) {
            return ClassificationTrace.highConfidence(kind, "test", "Test classification");
        }

        private static ArchitecturalModel modelWithEntity(String entityFqn) {
            Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .modifiers(Set.of(Modifier.PUBLIC))
                    .fields(List.of(idField))
                    .build();

            Entity entity = Entity.of(TypeId.of(entityFqn), structure, highConfidence(ElementKind.ENTITY), idField);

            TypeRegistry registry = TypeRegistry.builder().add(entity).build();
            DomainIndex domainIndex = DomainIndex.from(registry);

            ProjectContext project = ProjectContext.forTesting("test-project", "com.example");
            return ArchitecturalModel.builder(project)
                    .domainIndex(domainIndex)
                    .typeRegistry(registry)
                    .build();
        }

        private static ArchitecturalModel modelWithValueObject(String voFqn) {
            Field valueField =
                    Field.builder("amount", TypeRef.of("java.math.BigDecimal")).build();

            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                    .modifiers(Set.of(Modifier.PUBLIC))
                    .fields(List.of(valueField))
                    .build();

            ValueObject vo = ValueObject.of(TypeId.of(voFqn), structure, highConfidence(ElementKind.VALUE_OBJECT));

            TypeRegistry registry = TypeRegistry.builder().add(vo).build();
            DomainIndex domainIndex = DomainIndex.from(registry);

            ProjectContext project = ProjectContext.forTesting("test-project", "com.example");
            return ArchitecturalModel.builder(project)
                    .domainIndex(domainIndex)
                    .typeRegistry(registry)
                    .build();
        }

        @Test
        @DisplayName("should extract element type when elementType is present")
        void fromV5_shouldExtractElementType_whenElementTypePresent() {
            // Given: A collection field with elementType properly resolved
            Field field = Field.builder(
                            "lines",
                            TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.OrderLine"))))
                    .elementType(TypeRef.of("com.example.OrderLine"))
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            ArchitecturalModel model = modelWithEntity("com.example.OrderLine");

            // When
            RelationFieldSpec spec = RelationFieldSpec.fromV5(field, model, Map.of(), Map.of());

            // Then: Should correctly identify OrderLine as the target
            assertThat(spec.targetKind()).isEqualTo(ElementKind.ENTITY);
            assertThat(spec.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
        }

        @Test
        @DisplayName("should fallback to typeArguments when elementType is empty")
        void fromV5_shouldFallbackToTypeArguments_whenElementTypeEmpty() {
            // Given: A collection field WITHOUT elementType but WITH typeArguments
            Field field = Field.builder(
                            "lines",
                            TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.OrderLine"))))
                    // No .elementType() — simulates parser not resolving generics
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            ArchitecturalModel model = modelWithEntity("com.example.OrderLine");

            // When
            RelationFieldSpec spec = RelationFieldSpec.fromV5(field, model, Map.of(), Map.of());

            // Then: Should fallback to typeArguments and find OrderLine as Entity
            assertThat(spec.targetKind()).isEqualTo(ElementKind.ENTITY);
            assertThat(spec.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
        }

        @Test
        @DisplayName("should produce ONE_TO_MANY for collection of entities")
        void fromV5_shouldProduceOneToMany_forCollectionOfEntities() {
            // Given: A collection of entities (List<OrderLine> where OrderLine is Entity)
            Field field = Field.builder(
                            "lines",
                            TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.OrderLine"))))
                    .elementType(TypeRef.of("com.example.OrderLine"))
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            ArchitecturalModel model = modelWithEntity("com.example.OrderLine");

            // When
            RelationFieldSpec spec = RelationFieldSpec.fromV5(field, model, Map.of(), Map.of());

            // Then
            assertThat(spec.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
            assertThat(spec.targetKind()).isEqualTo(ElementKind.ENTITY);
        }

        @Test
        @DisplayName("should produce ELEMENT_COLLECTION for collection of value objects")
        void fromV5_shouldProduceElementCollection_forCollectionOfValueObjects() {
            // Given: A collection of value objects (List<Money> where Money is ValueObject)
            Field field = Field.builder(
                            "amounts",
                            TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.Money"))))
                    .elementType(TypeRef.of("com.example.Money"))
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            ArchitecturalModel model = modelWithValueObject("com.example.Money");

            // When
            RelationFieldSpec spec = RelationFieldSpec.fromV5(field, model, Map.of(), Map.of());

            // Then
            assertThat(spec.kind()).isEqualTo(RelationKind.ELEMENT_COLLECTION);
            assertThat(spec.targetKind()).isEqualTo(ElementKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should apply entityMapping for ONE_TO_MANY target")
        void fromV5_shouldApplyEntityMapping_forOneToManyTarget() {
            // Given: A collection of entities with entityMapping
            Field field = Field.builder(
                            "lines",
                            TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.OrderLine"))))
                    .elementType(TypeRef.of("com.example.OrderLine"))
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            ArchitecturalModel model = modelWithEntity("com.example.OrderLine");
            Map<String, String> entityMapping =
                    Map.of("com.example.OrderLine", "com.example.infrastructure.jpa.OrderLineEntity");

            // When
            RelationFieldSpec spec = RelationFieldSpec.fromV5(field, model, Map.of(), entityMapping);

            // Then: Target type should be the JPA entity, not the domain type
            assertThat(spec.targetType().toString())
                    .isEqualTo("java.util.List<com.example.infrastructure.jpa.OrderLineEntity>");
        }

        @Test
        @DisplayName("should not fallback to VALUE_OBJECT when entity is in domainIndex")
        void fromV5_shouldNotFallbackToValueObject_whenEntityInDomainIndex() {
            // Given: A field referencing an entity that IS in the domain index
            // but elementType is empty (would previously fallback to "java.util.List" FQN)
            Field field = Field.builder(
                            "lines",
                            TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.OrderLine"))))
                    // No elementType — this is the bug scenario
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            ArchitecturalModel model = modelWithEntity("com.example.OrderLine");

            // When
            RelationFieldSpec spec = RelationFieldSpec.fromV5(field, model, Map.of(), Map.of());

            // Then: Should NOT be VALUE_OBJECT (which was the bug behavior)
            assertThat(spec.targetKind()).isNotEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(spec.targetKind()).isEqualTo(ElementKind.ENTITY);
            assertThat(spec.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
        }
    }
}
