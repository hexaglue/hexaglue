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

package io.hexaglue.plugin.audit.adapter.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.plugin.audit.domain.model.BoundedContext;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainModel;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.SourceRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BoundedContextDetector")
class BoundedContextDetectorTest {

    private BoundedContextDetector detector;

    @BeforeEach
    void setUp() {
        detector = new BoundedContextDetector();
    }

    @Test
    @DisplayName("should detect single bounded context with all domain type kinds")
    void shouldDetectSingleContextWithAllTypes() {
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType orderLineEntity = createDomainType("com.example.order", "OrderLine", DomainKind.ENTITY);
        DomainType moneyValueObject = createDomainType("com.example.order", "Money", DomainKind.VALUE_OBJECT);
        DomainType orderPlacedEvent = createDomainType("com.example.order", "OrderPlaced", DomainKind.DOMAIN_EVENT);
        DomainType pricingService = createDomainType("com.example.order", "PricingService", DomainKind.DOMAIN_SERVICE);

        DomainModel domainModel = new DomainModel(
                List.of(orderAggregate, orderLineEntity, moneyValueObject, orderPlacedEvent, pricingService));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(1);

        BoundedContext orderContext = contexts.get(0);
        assertThat(orderContext.name()).isEqualTo("order");
        assertThat(orderContext.aggregateRoots()).containsExactly(orderAggregate);
        assertThat(orderContext.entities()).containsExactly(orderLineEntity);
        assertThat(orderContext.valueObjects()).containsExactly(moneyValueObject);
        assertThat(orderContext.domainEvents()).containsExactly(orderPlacedEvent);
        assertThat(orderContext.domainServices()).containsExactly(pricingService);
        assertThat(orderContext.totalTypes()).isEqualTo(5);
        assertThat(orderContext.isEmpty()).isFalse();
        assertThat(orderContext.hasAggregates()).isTrue();
    }

    @Test
    @DisplayName("should detect multiple bounded contexts")
    void shouldDetectMultipleContexts() {
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType orderLineEntity = createDomainType("com.example.order", "OrderLine", DomainKind.ENTITY);

        DomainType productAggregate = createDomainType("com.example.inventory", "Product", DomainKind.AGGREGATE_ROOT);
        DomainType stockEntity = createDomainType("com.example.inventory", "Stock", DomainKind.ENTITY);

        DomainModel domainModel =
                new DomainModel(List.of(orderAggregate, orderLineEntity, productAggregate, stockEntity));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(2);

        // Verify order context
        BoundedContext orderContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("order"))
                .findFirst()
                .orElseThrow();

        assertThat(orderContext.aggregateRoots()).containsExactly(orderAggregate);
        assertThat(orderContext.entities()).containsExactly(orderLineEntity);
        assertThat(orderContext.totalTypes()).isEqualTo(2);

        // Verify inventory context
        BoundedContext inventoryContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("inventory"))
                .findFirst()
                .orElseThrow();

        assertThat(inventoryContext.aggregateRoots()).containsExactly(productAggregate);
        assertThat(inventoryContext.entities()).containsExactly(stockEntity);
        assertThat(inventoryContext.totalTypes()).isEqualTo(2);
    }

    @Test
    @DisplayName("should handle nested package structures")
    void shouldHandleNestedPackages() {
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType shippingAggregate =
                createDomainType("com.example.order.shipping", "Shipment", DomainKind.AGGREGATE_ROOT);

        DomainType orderLineEntity = createDomainType("com.example.order", "OrderLine", DomainKind.ENTITY);
        DomainType trackingEntity = createDomainType("com.example.order.shipping", "Tracking", DomainKind.ENTITY);

        DomainModel domainModel =
                new DomainModel(List.of(orderAggregate, shippingAggregate, orderLineEntity, trackingEntity));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(2);

        // Verify that nested packages create separate contexts
        BoundedContext orderContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("order"))
                .findFirst()
                .orElseThrow();

        assertThat(orderContext.aggregateRoots()).containsExactly(orderAggregate);
        assertThat(orderContext.entities()).containsExactly(orderLineEntity);

        BoundedContext shippingContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("shipping"))
                .findFirst()
                .orElseThrow();

        assertThat(shippingContext.aggregateRoots()).containsExactly(shippingAggregate);
        assertThat(shippingContext.entities()).containsExactly(trackingEntity);
    }

    @Test
    @DisplayName("should include sub-package types in parent context when no aggregate in sub-package")
    void shouldIncludeSubPackageTypesInParentContext() {
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType orderLineEntity = createDomainType("com.example.order", "OrderLine", DomainKind.ENTITY);

        // Value object in sub-package, but no aggregate root there
        DomainType moneyValueObject =
                createDomainType("com.example.order.valueobjects", "Money", DomainKind.VALUE_OBJECT);

        DomainModel domainModel = new DomainModel(List.of(orderAggregate, orderLineEntity, moneyValueObject));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(1);

        BoundedContext orderContext = contexts.get(0);
        assertThat(orderContext.name()).isEqualTo("order");
        assertThat(orderContext.aggregateRoots()).containsExactly(orderAggregate);
        assertThat(orderContext.entities()).containsExactly(orderLineEntity);
        assertThat(orderContext.valueObjects()).containsExactly(moneyValueObject);
        assertThat(orderContext.totalTypes()).isEqualTo(3);
    }

    @Test
    @DisplayName("should return empty list when no aggregate roots found")
    void shouldReturnEmptyWhenNoAggregates() {
        DomainType entity = createDomainType("com.example.domain", "SomeEntity", DomainKind.ENTITY);
        DomainType valueObject = createDomainType("com.example.domain", "SomeValue", DomainKind.VALUE_OBJECT);

        DomainModel domainModel = new DomainModel(List.of(entity, valueObject));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for empty domain model")
    void shouldReturnEmptyForEmptyDomainModel() {
        DomainModel domainModel = new DomainModel(List.of());

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).isEmpty();
    }

    @Test
    @DisplayName("should handle flat package structure")
    void shouldHandleFlatPackageStructure() {
        DomainType orderAggregate = createDomainType("com.example", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType productAggregate = createDomainType("com.example", "Product", DomainKind.AGGREGATE_ROOT);
        DomainType customerAggregate = createDomainType("com.example", "Customer", DomainKind.AGGREGATE_ROOT);

        DomainModel domainModel = new DomainModel(List.of(orderAggregate, productAggregate, customerAggregate));

        List<BoundedContext> contexts = detector.detect(domainModel);

        // All aggregates in same package creates single context
        assertThat(contexts).hasSize(1);

        BoundedContext context = contexts.get(0);
        assertThat(context.name()).isEqualTo("example");
        assertThat(context.aggregateRoots()).hasSize(3);
        assertThat(context.aggregateRoots())
                .containsExactlyInAnyOrder(orderAggregate, productAggregate, customerAggregate);
    }

    @Test
    @DisplayName("should handle context with only aggregate root")
    void shouldHandleContextWithOnlyAggregate() {
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);

        DomainModel domainModel = new DomainModel(List.of(orderAggregate));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(1);

        BoundedContext orderContext = contexts.get(0);
        assertThat(orderContext.name()).isEqualTo("order");
        assertThat(orderContext.aggregateRoots()).containsExactly(orderAggregate);
        assertThat(orderContext.entities()).isEmpty();
        assertThat(orderContext.valueObjects()).isEmpty();
        assertThat(orderContext.domainEvents()).isEmpty();
        assertThat(orderContext.domainServices()).isEmpty();
        assertThat(orderContext.totalTypes()).isEqualTo(1);
    }

    @Test
    @DisplayName("should derive context name from simple package")
    void shouldDeriveContextNameFromSimplePackage() {
        DomainType aggregate = createDomainType("order", "Order", DomainKind.AGGREGATE_ROOT);

        DomainModel domainModel = new DomainModel(List.of(aggregate));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.get(0).name()).isEqualTo("order");
    }

    @Test
    @DisplayName("should handle mixed bounded contexts with varying compositions")
    void shouldHandleMixedContexts() {
        // Order context: full featured with all types
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType orderLineEntity = createDomainType("com.example.order", "OrderLine", DomainKind.ENTITY);
        DomainType moneyValueObject = createDomainType("com.example.order", "Money", DomainKind.VALUE_OBJECT);
        DomainType orderPlacedEvent = createDomainType("com.example.order", "OrderPlaced", DomainKind.DOMAIN_EVENT);

        // Inventory context: only aggregate and entity
        DomainType productAggregate = createDomainType("com.example.inventory", "Product", DomainKind.AGGREGATE_ROOT);
        DomainType stockEntity = createDomainType("com.example.inventory", "Stock", DomainKind.ENTITY);

        // Payment context: only aggregate
        DomainType paymentAggregate = createDomainType("com.example.payment", "Payment", DomainKind.AGGREGATE_ROOT);

        DomainModel domainModel = new DomainModel(List.of(
                orderAggregate,
                orderLineEntity,
                moneyValueObject,
                orderPlacedEvent,
                productAggregate,
                stockEntity,
                paymentAggregate));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(3);

        // Verify order context
        BoundedContext orderContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("order"))
                .findFirst()
                .orElseThrow();
        assertThat(orderContext.totalTypes()).isEqualTo(4);

        // Verify inventory context
        BoundedContext inventoryContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("inventory"))
                .findFirst()
                .orElseThrow();
        assertThat(inventoryContext.totalTypes()).isEqualTo(2);
        assertThat(inventoryContext.valueObjects()).isEmpty();
        assertThat(inventoryContext.domainEvents()).isEmpty();

        // Verify payment context
        BoundedContext paymentContext = contexts.stream()
                .filter(ctx -> ctx.name().equals("payment"))
                .findFirst()
                .orElseThrow();
        assertThat(paymentContext.totalTypes()).isEqualTo(1);
        assertThat(paymentContext.entities()).isEmpty();
    }

    @Test
    @DisplayName("should throw NullPointerException when domain model is null")
    void shouldThrowWhenDomainModelIsNull() {
        assertThatThrownBy(() -> detector.detect(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("domainModel required");
    }

    @Test
    @DisplayName("should not include application services in bounded contexts")
    void shouldNotIncludeApplicationServices() {
        DomainType orderAggregate = createDomainType("com.example.order", "Order", DomainKind.AGGREGATE_ROOT);
        DomainType orderService =
                createDomainType("com.example.order", "OrderApplicationService", DomainKind.APPLICATION_SERVICE);

        DomainModel domainModel = new DomainModel(List.of(orderAggregate, orderService));

        List<BoundedContext> contexts = detector.detect(domainModel);

        assertThat(contexts).hasSize(1);

        BoundedContext orderContext = contexts.get(0);
        assertThat(orderContext.aggregateRoots()).containsExactly(orderAggregate);
        // Application services should not be included in bounded context domain types
        assertThat(orderContext.domainServices()).isEmpty();
        assertThat(orderContext.totalTypes()).isEqualTo(1);
    }

    // === Helper Methods ===

    /**
     * Creates a test domain type with the given parameters.
     *
     * @param packageName the package name
     * @param simpleName  the simple class name
     * @param kind        the domain kind
     * @return a new DomainType instance
     */
    private DomainType createDomainType(String packageName, String simpleName, DomainKind kind) {
        String qualifiedName = packageName + "." + simpleName;

        return new DomainType(
                qualifiedName,
                simpleName,
                packageName,
                kind,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                new SourceRef(qualifiedName + ".java", 1, 1));
    }
}
