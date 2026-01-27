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

package io.hexaglue.plugin.livingdoc.content;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DomainContentSelector using v5 ArchitecturalModel API.
 *
 * @since 4.0.0
 * @since 5.0.0 - Migrated to v5 ArchType API
 */
@DisplayName("DomainContentSelector")
class DomainContentSelectorTest {

    private static final String PKG = "com.example.domain";

    @Nested
    @DisplayName("AggregateRoot Selection")
    class AggregateRootSelection {

        @Test
        @DisplayName("should select aggregate roots")
        void shouldSelectAggregateRoots() {
            // Given
            AggregateRoot order = aggregateRoot(PKG + ".Order", "id", TypeRef.of(PKG + ".OrderId"));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), order);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectAggregateRoots();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("Order");
            assertThat(doc.packageName()).isEqualTo(PKG);
            assertThat(doc.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should transform identity correctly when present")
        void shouldTransformIdentityCorrectly() {
            // Given: Aggregate root with identity
            AggregateRoot order = aggregateRoot(PKG + ".Order", "id", TypeRef.of(PKG + ".OrderId"));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), order);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            DomainTypeDoc doc = results.get(0);

            // Then
            assertThat(doc.identity()).isNotNull();
            assertThat(doc.identity().fieldName()).isEqualTo("id");
            assertThat(doc.identity().type()).isEqualTo("OrderId");
        }
    }

    @Nested
    @DisplayName("Entity Selection")
    class EntitySelection {

        @Test
        @DisplayName("should select entities (non-aggregate roots)")
        void shouldSelectEntities() {
            // Given
            Entity lineItem = entity(PKG + ".OrderLineItem");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), lineItem);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectEntities();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderLineItem");
            assertThat(doc.kind()).isEqualTo(ElementKind.ENTITY);
        }

        @Test
        @DisplayName("should not include aggregate roots in entity selection")
        void shouldNotIncludeAggregateRootsInEntitySelection() {
            // Given
            AggregateRoot aggRoot = aggregateRoot(PKG + ".Order");
            Entity entityItem = entity(PKG + ".OrderLine");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), aggRoot, entityItem);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectEntities();

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("OrderLine");
        }
    }

    @Nested
    @DisplayName("ValueObject Selection")
    class ValueObjectSelection {

        @Test
        @DisplayName("should select value objects")
        void shouldSelectValueObjects() {
            // Given
            ValueObject money = valueObject(PKG + ".Money", List.of("amount", "currency"));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), money);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("Money");
            assertThat(doc.kind()).isEqualTo(ElementKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should have no identity for value objects")
        void shouldHaveNoIdentityForValueObjects() {
            // Given
            ValueObject money = valueObject(PKG + ".Money", List.of("amount"));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), money);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            // Then
            assertThat(doc.identity()).isNull();
        }
    }

    @Nested
    @DisplayName("Identifier Selection")
    class IdentifierSelection {

        @Test
        @DisplayName("should select identifiers")
        void shouldSelectIdentifiers() {
            // Given
            Identifier orderId = identifier(PKG + ".OrderId");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderId);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectIdentifiers();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderId");
            assertThat(doc.kind()).isEqualTo(ElementKind.IDENTIFIER);
        }
    }

    @Nested
    @DisplayName("DomainEvent Selection")
    class DomainEventSelection {

        @Test
        @DisplayName("should select domain events")
        void shouldSelectDomainEvents() {
            // Given
            DomainEvent orderPlaced = domainEvent(PKG + ".OrderPlaced");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderPlaced);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectDomainEvents();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderPlaced");
            assertThat(doc.kind()).isEqualTo(ElementKind.DOMAIN_EVENT);
        }
    }

    @Nested
    @DisplayName("DomainService Selection")
    class DomainServiceSelection {

        @Test
        @DisplayName("should select domain services")
        void shouldSelectDomainServices() {
            // Given
            DomainService pricingService = domainService(PKG + ".PricingService");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), pricingService);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectDomainServices();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("PricingService");
            assertThat(doc.kind()).isEqualTo(ElementKind.DOMAIN_SERVICE);
        }
    }

    @Nested
    @DisplayName("ApplicationService Selection")
    class ApplicationServiceSelection {

        @Test
        @DisplayName("should select application services")
        void shouldSelectApplicationServices() {
            // Given
            ApplicationService orderService = applicationService(PKG + ".OrderService");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderService);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectApplicationServices();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderService");
            assertThat(doc.kind()).isEqualTo(ElementKind.APPLICATION_SERVICE);
        }
    }

    @Nested
    @DisplayName("selectAllTypes")
    class SelectAllTypes {

        @Test
        @DisplayName("should select all types from model")
        void shouldSelectAllTypes() {
            // Given
            AggregateRoot aggRoot = aggregateRoot(PKG + ".Order");
            Entity entityItem = entity(PKG + ".OrderLine");
            ValueObject money = valueObject(PKG + ".Money", List.of());
            Identifier orderId = identifier(PKG + ".OrderId");
            DomainEvent event = domainEvent(PKG + ".OrderPlaced");
            DomainService svc = domainService(PKG + ".PricingService");
            ApplicationService appSvc = applicationService(PKG + ".OrderAppService");

            ArchitecturalModel model = createModel(
                    ProjectContext.forTesting("app", PKG), aggRoot, entityItem, money, orderId, event, svc, appSvc);

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectAllTypes();

            // Then
            assertThat(results).hasSize(7);
        }
    }

    @Nested
    @DisplayName("Property Extraction")
    class PropertyExtraction {

        @Test
        @DisplayName("should mark primitive types as NON_NULL")
        void shouldMarkPrimitiveTypesAsNonNull() {
            // Given: ValueObject with primitive fields
            Field booleanField = Field.of("completed", TypeRef.primitive("boolean"));
            Field intField = Field.of("quantity", TypeRef.primitive("int"));
            ValueObject vo = valueObject(PKG + ".Task", booleanField, intField);

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), vo);
            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            // Then
            assertThat(doc.properties()).hasSize(2);
            assertThat(doc.properties())
                    .extracting("name", "nullability")
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple("completed", "NON_NULL"),
                            org.assertj.core.groups.Tuple.tuple("quantity", "NON_NULL"));
        }

        @Test
        @DisplayName("should mark reference types as UNKNOWN by default")
        void shouldMarkReferenceTypesAsUnknown() {
            // Given: ValueObject with reference field
            Field stringField = Field.of("name", TypeRef.of("java.lang.String"));
            ValueObject vo = valueObject(PKG + ".Person", stringField);

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), vo);
            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            // Then
            assertThat(doc.properties()).hasSize(1);
            assertThat(doc.properties().get(0).nullability()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("should detect collection cardinality")
        void shouldDetectCollectionCardinality() {
            // Given: ValueObject with a List field
            Field listField =
                    Field.of("items", TypeRef.parameterized("java.util.List", List.of(TypeRef.of("java.lang.String"))));
            ValueObject vo = valueObject(PKG + ".Order", listField);

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), vo);
            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            // Then
            assertThat(doc.properties()).hasSize(1);
            assertThat(doc.properties().get(0).cardinality()).isEqualTo("COLLECTION");
        }

        @Test
        @DisplayName("should detect single cardinality for non-collection types")
        void shouldDetectSingleCardinalityForNonCollectionTypes() {
            // Given: ValueObject with a non-collection field
            Field stringField = Field.of("name", TypeRef.of("java.lang.String"));
            ValueObject vo = valueObject(PKG + ".Person", stringField);

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), vo);
            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            // Then
            assertThat(doc.properties()).hasSize(1);
            assertThat(doc.properties().get(0).cardinality()).isEqualTo("SINGLE");
        }
    }

    @Nested
    @DisplayName("Empty Selections")
    class EmptySelections {

        @Test
        @DisplayName("should return empty list when no aggregate roots")
        void shouldReturnEmptyListWhenNoAggregateRoots() {
            // Given
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG));

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectAggregateRoots()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no entities")
        void shouldReturnEmptyListWhenNoEntities() {
            // Given
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG));

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectEntities()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no value objects")
        void shouldReturnEmptyListWhenNoValueObjects() {
            // Given
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG));

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectValueObjects()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no identifiers")
        void shouldReturnEmptyListWhenNoIdentifiers() {
            // Given
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG));

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectIdentifiers()).isEmpty();
        }
    }
}
