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

package io.hexaglue.plugin.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.SourceRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BoundedContext")
class BoundedContextTest {

    @Test
    @DisplayName("should create bounded context with all domain type kinds")
    void shouldCreateWithAllTypes() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        DomainType entity = createType("OrderLine", DomainKind.ENTITY);
        DomainType valueObject = createType("Money", DomainKind.VALUE_OBJECT);
        DomainType event = createType("OrderPlaced", DomainKind.DOMAIN_EVENT);
        DomainType service = createType("PricingService", DomainKind.DOMAIN_SERVICE);

        BoundedContext context = new BoundedContext(
                "order", List.of(aggregate), List.of(entity), List.of(valueObject), List.of(event), List.of(service));

        assertThat(context.name()).isEqualTo("order");
        assertThat(context.aggregateRoots()).containsExactly(aggregate);
        assertThat(context.entities()).containsExactly(entity);
        assertThat(context.valueObjects()).containsExactly(valueObject);
        assertThat(context.domainEvents()).containsExactly(event);
        assertThat(context.domainServices()).containsExactly(service);
    }

    @Test
    @DisplayName("should calculate total types correctly")
    void shouldCalculateTotalTypes() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        DomainType entity = createType("OrderLine", DomainKind.ENTITY);
        DomainType valueObject = createType("Money", DomainKind.VALUE_OBJECT);

        BoundedContext context = new BoundedContext(
                "order", List.of(aggregate), List.of(entity), List.of(valueObject), List.of(), List.of());

        assertThat(context.totalTypes()).isEqualTo(3);
    }

    @Test
    @DisplayName("should return true when empty")
    void shouldBeEmpty() {
        BoundedContext context = BoundedContext.empty("order");

        assertThat(context.isEmpty()).isTrue();
        assertThat(context.totalTypes()).isZero();
        assertThat(context.hasAggregates()).isFalse();
    }

    @Test
    @DisplayName("should return false when not empty")
    void shouldNotBeEmpty() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        BoundedContext context = BoundedContext.withAggregates("order", List.of(aggregate));

        assertThat(context.isEmpty()).isFalse();
        assertThat(context.hasAggregates()).isTrue();
    }

    @Test
    @DisplayName("should return all types as single list")
    void shouldReturnAllTypesAsList() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        DomainType entity = createType("OrderLine", DomainKind.ENTITY);
        DomainType valueObject = createType("Money", DomainKind.VALUE_OBJECT);

        BoundedContext context = new BoundedContext(
                "order", List.of(aggregate), List.of(entity), List.of(valueObject), List.of(), List.of());

        List<DomainType> allTypes = context.allTypes();

        assertThat(allTypes).hasSize(3);
        assertThat(allTypes).containsExactlyInAnyOrder(aggregate, entity, valueObject);
    }

    @Test
    @DisplayName("should create bounded context with only aggregates using factory method")
    void shouldCreateWithAggregatesOnly() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        BoundedContext context = BoundedContext.withAggregates("order", List.of(aggregate));

        assertThat(context.name()).isEqualTo("order");
        assertThat(context.aggregateRoots()).containsExactly(aggregate);
        assertThat(context.entities()).isEmpty();
        assertThat(context.valueObjects()).isEmpty();
        assertThat(context.domainEvents()).isEmpty();
        assertThat(context.domainServices()).isEmpty();
    }

    @Test
    @DisplayName("should create empty bounded context using factory method")
    void shouldCreateEmpty() {
        BoundedContext context = BoundedContext.empty("order");

        assertThat(context.name()).isEqualTo("order");
        assertThat(context.isEmpty()).isTrue();
        assertThat(context.allTypes()).isEmpty();
    }

    @Test
    @DisplayName("should create defensive copies of lists")
    void shouldCreateDefensiveCopies() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        List<DomainType> aggregates = new java.util.ArrayList<>();
        aggregates.add(aggregate);

        BoundedContext context = BoundedContext.withAggregates("order", aggregates);

        // Modify original list
        aggregates.clear();

        // Context should still have the aggregate
        assertThat(context.aggregateRoots()).containsExactly(aggregate);
    }

    @Test
    @DisplayName("should handle null lists by using empty lists")
    void shouldHandleNullLists() {
        BoundedContext context = new BoundedContext("order", null, null, null, null, null);

        assertThat(context.aggregateRoots()).isEmpty();
        assertThat(context.entities()).isEmpty();
        assertThat(context.valueObjects()).isEmpty();
        assertThat(context.domainEvents()).isEmpty();
        assertThat(context.domainServices()).isEmpty();
        assertThat(context.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("should throw NullPointerException when name is null")
    void shouldThrowWhenNameIsNull() {
        assertThatThrownBy(() -> new BoundedContext(null, List.of(), List.of(), List.of(), List.of(), List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name required");
    }

    @Test
    @DisplayName("should return immutable lists")
    void shouldReturnImmutableLists() {
        DomainType aggregate = createType("Order", DomainKind.AGGREGATE_ROOT);
        BoundedContext context = BoundedContext.withAggregates("order", List.of(aggregate));

        assertThatThrownBy(() -> context.aggregateRoots().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    // === Helper Methods ===

    private DomainType createType(String simpleName, DomainKind kind) {
        String qualifiedName = "com.example." + simpleName;
        return new DomainType(
                qualifiedName,
                simpleName,
                "com.example",
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
