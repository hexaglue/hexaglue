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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.RelationKind;
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
}
