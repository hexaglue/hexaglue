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

package io.hexaglue.arch.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ElementRegistry;
import io.hexaglue.arch.RelationshipStore;
import io.hexaglue.arch.UnclassifiedType;
import io.hexaglue.arch.adapters.DrivenAdapter;
import io.hexaglue.arch.adapters.DrivingAdapter;
import io.hexaglue.arch.domain.Aggregate;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Query API")
class QueryApiTest {

    private static final String PKG = "com.example";

    private ElementRegistry registry;
    private RelationshipStore relationships;
    private ModelQuery query;

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @BeforeEach
    void setUp() {
        // Build test registry with various elements
        DomainEntity orderRoot =
                DomainEntity.aggregateRoot(PKG + ".domain.Order", highConfidence(ElementKind.AGGREGATE_ROOT));
        DomainEntity productRoot =
                DomainEntity.aggregateRoot(PKG + ".domain.Product", highConfidence(ElementKind.AGGREGATE_ROOT));

        Aggregate orderAggregate = Aggregate.of(
                PKG + ".domain.OrderAggregate",
                ElementRef.of(orderRoot.id(), DomainEntity.class),
                highConfidence(ElementKind.AGGREGATE));
        Aggregate productAggregate = Aggregate.of(
                PKG + ".domain.ProductAggregate",
                ElementRef.of(productRoot.id(), DomainEntity.class),
                highConfidence(ElementKind.AGGREGATE));

        ValueObject money = ValueObject.of(
                PKG + ".domain.Money", List.of("amount", "currency"), highConfidence(ElementKind.VALUE_OBJECT));
        DomainEvent orderPlaced = DomainEvent.of(PKG + ".domain.OrderPlaced", highConfidence(ElementKind.DOMAIN_EVENT));
        DomainService pricing =
                DomainService.of(PKG + ".domain.PricingService", highConfidence(ElementKind.DOMAIN_SERVICE));

        DrivingPort placeOrderUseCase =
                DrivingPort.of(PKG + ".ports.PlaceOrderUseCase", highConfidence(ElementKind.DRIVING_PORT));
        DrivingPort cancelOrderUseCase =
                DrivingPort.of(PKG + ".ports.CancelOrderUseCase", highConfidence(ElementKind.DRIVING_PORT));
        DrivenPort orderRepository =
                DrivenPort.of(PKG + ".ports.OrderRepository", highConfidence(ElementKind.DRIVEN_PORT));
        DrivenPort productRepository =
                DrivenPort.of(PKG + ".ports.ProductRepository", highConfidence(ElementKind.DRIVEN_PORT));
        DrivenPort paymentGateway =
                DrivenPort.of(PKG + ".ports.PaymentGateway", highConfidence(ElementKind.DRIVEN_PORT));
        ApplicationService orderService = ApplicationService.of(
                PKG + ".services.OrderApplicationService", highConfidence(ElementKind.APPLICATION_SERVICE));

        DrivingAdapter orderController =
                DrivingAdapter.of(PKG + ".adapters.OrderController", highConfidence(ElementKind.DRIVING_ADAPTER));
        DrivenAdapter jpaOrderRepository =
                DrivenAdapter.of(PKG + ".adapters.JpaOrderRepository", highConfidence(ElementKind.DRIVEN_ADAPTER));

        UnclassifiedType unknown = UnclassifiedType.of(PKG + ".utils.Helper", highConfidence(ElementKind.UNCLASSIFIED));

        registry = ElementRegistry.builder()
                .add(orderRoot)
                .add(productRoot)
                .add(orderAggregate)
                .add(productAggregate)
                .add(money)
                .add(orderPlaced)
                .add(pricing)
                .add(placeOrderUseCase)
                .add(cancelOrderUseCase)
                .add(orderRepository)
                .add(productRepository)
                .add(paymentGateway)
                .add(orderService)
                .add(orderController)
                .add(jpaOrderRepository)
                .add(unknown)
                .build();

        // Build relationships
        RelationshipStore.Builder relBuilder = RelationshipStore.builder();
        relBuilder.addManages(orderRepository.id(), orderAggregate.id());
        relBuilder.addManages(productRepository.id(), productAggregate.id());
        relationships = relBuilder.build();

        query = new DefaultModelQuery(registry, relationships);
    }

    @Nested
    @DisplayName("ModelQuery")
    class ModelQueryTest {

        @Test
        @DisplayName("should access aggregates")
        void shouldAccessAggregates() {
            // when
            AggregateQuery aggregates = query.aggregates();

            // then
            assertThat(aggregates.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("should access driving ports")
        void shouldAccessDrivingPorts() {
            // when
            long count = query.drivingPorts().count();

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should access driven ports")
        void shouldAccessDrivenPorts() {
            // when
            long count = query.drivenPorts().count();

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should access application services")
        void shouldAccessApplicationServices() {
            // when
            long count = query.applicationServices().count();

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should access domain services")
        void shouldAccessDomainServices() {
            // when
            long count = query.domainServices().count();

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should access driving adapters")
        void shouldAccessDrivingAdapters() {
            // when
            long count = query.drivingAdapters().count();

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should access driven adapters")
        void shouldAccessDrivenAdapters() {
            // when
            long count = query.drivenAdapters().count();

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should access all elements")
        void shouldAccessAllElements() {
            // when
            long count = query.elements().count();

            // then
            assertThat(count).isEqualTo(16);
        }

        @Test
        @DisplayName("should access elements by type")
        void shouldAccessElementsByType() {
            // when
            long count = query.elements(ValueObject.class).count();

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should find element by reference")
        void shouldFindByReference() {
            // given
            ElementRef<DrivingPort> ref =
                    ElementRef.of(ElementId.of(PKG + ".ports.PlaceOrderUseCase"), DrivingPort.class);

            // when
            var result = query.find(ref);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().simpleName()).isEqualTo("PlaceOrderUseCase");
        }

        @Test
        @DisplayName("should get element by reference")
        void shouldGetByReference() {
            // given
            ElementRef<DrivingPort> ref =
                    ElementRef.of(ElementId.of(PKG + ".ports.PlaceOrderUseCase"), DrivingPort.class);

            // when
            DrivingPort port = query.get(ref);

            // then
            assertThat(port.simpleName()).isEqualTo("PlaceOrderUseCase");
        }

        @Test
        @DisplayName("should return empty for missing reference")
        void shouldReturnEmptyForMissing() {
            // given
            ElementRef<DrivingPort> ref = ElementRef.of(ElementId.of(PKG + ".ports.NotExist"), DrivingPort.class);

            // when
            var result = query.find(ref);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("AggregateQuery")
    class AggregateQueryTest {

        @Test
        @DisplayName("should filter aggregates with repository")
        void shouldFilterWithRepository() {
            // when
            List<Aggregate> withRepo = query.aggregates().withRepository().toList();

            // then
            assertThat(withRepo).hasSize(2);
        }

        @Test
        @DisplayName("should filter aggregates without repository")
        void shouldFilterWithoutRepository() {
            // given - Add an aggregate without repository
            DomainEntity root =
                    DomainEntity.aggregateRoot(PKG + ".domain.Orphan", highConfidence(ElementKind.AGGREGATE_ROOT));
            Aggregate orphan = Aggregate.of(
                    PKG + ".domain.OrphanAggregate",
                    ElementRef.of(root.id(), DomainEntity.class),
                    highConfidence(ElementKind.AGGREGATE));

            ElementRegistry newRegistry =
                    ElementRegistry.builder().add(root).add(orphan).build();
            RelationshipStore emptyRel = RelationshipStore.builder().build();
            ModelQuery newQuery = new DefaultModelQuery(newRegistry, emptyRel);

            // when
            List<Aggregate> withoutRepo =
                    newQuery.aggregates().withoutRepository().toList();

            // then
            assertThat(withoutRepo).hasSize(1);
            assertThat(withoutRepo.get(0).simpleName()).isEqualTo("OrphanAggregate");
        }

        @Test
        @DisplayName("should filter by package")
        void shouldFilterByPackage() {
            // when
            List<Aggregate> inDomainPkg =
                    query.aggregates().inPackage(PKG + ".domain").toList();

            // then
            assertThat(inDomainPkg).hasSize(2);
        }

        @Test
        @DisplayName("should be reusable (immutable)")
        void shouldBeReusable() {
            // given
            AggregateQuery baseQuery = query.aggregates();

            // when - use the query twice
            long count1 = baseQuery.count();
            long count2 = baseQuery.count();

            // then - same result each time (query is immutable)
            assertThat(count1).isEqualTo(count2).isEqualTo(2);
        }

        @Test
        @DisplayName("should chain filters")
        void shouldChainFilters() {
            // when
            List<Aggregate> result = query.aggregates()
                    .withRepository()
                    .inPackage(PKG + ".domain")
                    .toList();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should stream results")
        void shouldStreamResults() {
            // when
            List<String> names = query.aggregates().stream()
                    .map(ArchElement::simpleName)
                    .sorted()
                    .toList();

            // then
            assertThat(names).containsExactly("OrderAggregate", "ProductAggregate");
        }

        @Test
        @DisplayName("should get first result")
        void shouldGetFirst() {
            // when
            var first = query.aggregates().first();

            // then
            assertThat(first).isPresent();
        }

        @Test
        @DisplayName("should check existence")
        void shouldCheckExists() {
            // then
            assertThat(query.aggregates().exists()).isTrue();
            assertThat(query.aggregates().isEmpty()).isFalse();
        }

        @Test
        @DisplayName("should apply forEach")
        void shouldApplyForEach() {
            // given
            StringBuilder sb = new StringBuilder();

            // when
            query.aggregates().forEach(agg -> sb.append(agg.simpleName()).append(","));

            // then
            assertThat(sb.toString()).contains("OrderAggregate").contains("ProductAggregate");
        }

        @Test
        @DisplayName("should map results")
        void shouldMapResults() {
            // when
            List<String> names = query.aggregates().map(ArchElement::simpleName);

            // then
            assertThat(names).hasSize(2).contains("OrderAggregate", "ProductAggregate");
        }
    }

    @Nested
    @DisplayName("ElementQuery")
    class ElementQueryTest {

        @Test
        @DisplayName("should filter with predicate")
        void shouldFilterWithPredicate() {
            // when
            long count = query.elements()
                    .filter(e -> e.simpleName().startsWith("Order"))
                    .count();

            // then
            assertThat(count).isGreaterThan(0);
        }

        @Test
        @DisplayName("should filter by package")
        void shouldFilterByPackage() {
            // when
            List<ArchElement> domainElements =
                    query.elements().inPackage(PKG + ".domain").toList();

            // then
            assertThat(domainElements).hasSizeGreaterThan(0);
            assertThat(domainElements).allMatch(e -> e.packageName().equals(PKG + ".domain"));
        }

        @Test
        @DisplayName("should filter by confidence level")
        void shouldFilterByConfidence() {
            // when
            List<ArchElement> highConfidenceElements =
                    query.elements().withConfidence(ConfidenceLevel.HIGH).toList();

            // then
            assertThat(highConfidenceElements).hasSize(16); // All test elements are HIGH confidence
        }

        @Test
        @DisplayName("should get single result when only one")
        void shouldGetSingleWhenOne() {
            // when
            var single = query.elements(DomainService.class).single();

            // then
            assertThat(single).isPresent();
            assertThat(single.get().simpleName()).isEqualTo("PricingService");
        }

        @Test
        @DisplayName("should throw on single with multiple results")
        void shouldThrowOnSingleWithMultiple() {
            // when/then
            assertThatThrownBy(() -> query.aggregates().single()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("PortQuery")
    class PortQueryTest {

        @Test
        @DisplayName("should access all ports")
        void shouldAccessAllPorts() {
            // when
            long total = query.allPorts().count();

            // then
            assertThat(total).isEqualTo(5); // 2 driving + 3 driven
        }
    }

    @Nested
    @DisplayName("ServiceQuery")
    class ServiceQueryTest {

        @Test
        @DisplayName("should access application services")
        void shouldAccessAppServices() {
            // when
            List<ApplicationService> services = query.applicationServices().toList();

            // then
            assertThat(services).hasSize(1);
            assertThat(services.get(0).simpleName()).isEqualTo("OrderApplicationService");
        }

        @Test
        @DisplayName("should access domain services")
        void shouldAccessDomainServices() {
            // when
            List<DomainService> services = query.domainServices().toList();

            // then
            assertThat(services).hasSize(1);
            assertThat(services.get(0).simpleName()).isEqualTo("PricingService");
        }
    }

    @Nested
    @DisplayName("AdapterQuery")
    class AdapterQueryTest {

        @Test
        @DisplayName("should access driving adapters")
        void shouldAccessDrivingAdapters() {
            // when
            List<DrivingAdapter> adapters = query.drivingAdapters().toList();

            // then
            assertThat(adapters).hasSize(1);
            assertThat(adapters.get(0).simpleName()).isEqualTo("OrderController");
        }

        @Test
        @DisplayName("should access driven adapters")
        void shouldAccessDrivenAdapters() {
            // when
            List<DrivenAdapter> adapters = query.drivenAdapters().toList();

            // then
            assertThat(adapters).hasSize(1);
            assertThat(adapters.get(0).simpleName()).isEqualTo("JpaOrderRepository");
        }
    }
}
