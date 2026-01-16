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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.domain.Identifier;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DomainContentSelector using v4 ArchitecturalModel API.
 *
 * @since 4.0.0
 */
@DisplayName("DomainContentSelector")
class DomainContentSelectorTest {

    private static final String PKG = "com.example.domain";

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("AggregateRoot Selection")
    class AggregateRootSelection {

        @Test
        @DisplayName("should select aggregate roots")
        void shouldSelectAggregateRoots() {
            // Given
            DomainEntity order = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(order)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectAggregateRoots();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("Order");
            assertThat(doc.packageName()).isEqualTo(PKG);
            assertThat(doc.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should transform identity correctly when present")
        void shouldTransformIdentityCorrectly() {
            // Given: Aggregate root with identity
            DomainEntity order = new DomainEntity(
                    io.hexaglue.arch.ElementId.of(PKG + ".Order"),
                    ElementKind.AGGREGATE_ROOT,
                    "id",
                    TypeRef.of(PKG + ".OrderId"),
                    java.util.Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.AGGREGATE_ROOT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(order)
                    .build();

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
            DomainEntity lineItem = DomainEntity.entity(PKG + ".OrderLineItem", highConfidence(ElementKind.ENTITY));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(lineItem)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectEntities();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderLineItem");
            assertThat(doc.kind()).isEqualTo(DomainKind.ENTITY);
        }

        @Test
        @DisplayName("should not include aggregate roots in entity selection")
        void shouldNotIncludeAggregateRootsInEntitySelection() {
            // Given
            DomainEntity aggRoot = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            DomainEntity entity = DomainEntity.entity(PKG + ".OrderLine", highConfidence(ElementKind.ENTITY));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(aggRoot)
                    .add(entity)
                    .build();

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
            ValueObject money = ValueObject.of(PKG + ".Money", List.of("amount", "currency"), highConfidence(ElementKind.VALUE_OBJECT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(money)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectValueObjects();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("Money");
            assertThat(doc.kind()).isEqualTo(DomainKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should have no identity for value objects")
        void shouldHaveNoIdentityForValueObjects() {
            // Given
            ValueObject money = ValueObject.of(PKG + ".Money", List.of("amount"), highConfidence(ElementKind.VALUE_OBJECT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(money)
                    .build();

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
            Identifier orderId = Identifier.of(PKG + ".OrderId", highConfidence(ElementKind.IDENTIFIER));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderId)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectIdentifiers();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderId");
            assertThat(doc.kind()).isEqualTo(DomainKind.IDENTIFIER);
        }
    }

    @Nested
    @DisplayName("DomainEvent Selection")
    class DomainEventSelection {

        @Test
        @DisplayName("should select domain events")
        void shouldSelectDomainEvents() {
            // Given
            DomainEvent orderPlaced = DomainEvent.of(PKG + ".OrderPlaced", highConfidence(ElementKind.DOMAIN_EVENT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderPlaced)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectDomainEvents();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderPlaced");
            assertThat(doc.kind()).isEqualTo(DomainKind.DOMAIN_EVENT);
        }
    }

    @Nested
    @DisplayName("DomainService Selection")
    class DomainServiceSelection {

        @Test
        @DisplayName("should select domain services")
        void shouldSelectDomainServices() {
            // Given
            DomainService pricingService = DomainService.of(PKG + ".PricingService", highConfidence(ElementKind.DOMAIN_SERVICE));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(pricingService)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectDomainServices();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("PricingService");
            assertThat(doc.kind()).isEqualTo(DomainKind.DOMAIN_SERVICE);
        }
    }

    @Nested
    @DisplayName("ApplicationService Selection")
    class ApplicationServiceSelection {

        @Test
        @DisplayName("should select application services")
        void shouldSelectApplicationServices() {
            // Given
            ApplicationService orderService = ApplicationService.of(PKG + ".OrderService", highConfidence(ElementKind.APPLICATION_SERVICE));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderService)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectApplicationServices();

            // Then
            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderService");
            assertThat(doc.kind()).isEqualTo(DomainKind.APPLICATION_SERVICE);
        }
    }

    @Nested
    @DisplayName("selectAllTypes")
    class SelectAllTypes {

        @Test
        @DisplayName("should select all types from model")
        void shouldSelectAllTypes() {
            // Given
            DomainEntity aggRoot = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            DomainEntity entity = DomainEntity.entity(PKG + ".OrderLine", highConfidence(ElementKind.ENTITY));
            ValueObject money = ValueObject.of(PKG + ".Money", List.of(), highConfidence(ElementKind.VALUE_OBJECT));
            Identifier orderId = Identifier.of(PKG + ".OrderId", highConfidence(ElementKind.IDENTIFIER));
            DomainEvent event = DomainEvent.of(PKG + ".OrderPlaced", highConfidence(ElementKind.DOMAIN_EVENT));
            DomainService svc = DomainService.of(PKG + ".PricingService", highConfidence(ElementKind.DOMAIN_SERVICE));
            ApplicationService appSvc = ApplicationService.of(PKG + ".OrderAppService", highConfidence(ElementKind.APPLICATION_SERVICE));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(aggRoot)
                    .add(entity)
                    .add(money)
                    .add(orderId)
                    .add(event)
                    .add(svc)
                    .add(appSvc)
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // When
            List<DomainTypeDoc> results = selector.selectAllTypes();

            // Then
            assertThat(results).hasSize(7);
        }
    }

    @Nested
    @DisplayName("Empty Selections")
    class EmptySelections {

        @Test
        @DisplayName("should return empty list when no aggregate roots")
        void shouldReturnEmptyListWhenNoAggregateRoots() {
            // Given
            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectAggregateRoots()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no entities")
        void shouldReturnEmptyListWhenNoEntities() {
            // Given
            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectEntities()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no value objects")
        void shouldReturnEmptyListWhenNoValueObjects() {
            // Given
            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectValueObjects()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no identifiers")
        void shouldReturnEmptyListWhenNoIdentifiers() {
            // Given
            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .build();

            DomainContentSelector selector = new DomainContentSelector(model);

            // Then
            assertThat(selector.selectIdentifiers()).isEmpty();
        }
    }
}
