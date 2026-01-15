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

package io.hexaglue.arch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ElementRegistry}.
 *
 * @since 4.0.0
 */
@DisplayName("ElementRegistry")
class ElementRegistryTest {

    private TestArchElement order;
    private TestArchElement customer;
    private TestArchElement orderRepository;

    @BeforeEach
    void setUp() {
        order = TestArchElement.aggregate("com.example.domain.Order");
        customer = TestArchElement.entity("com.example.domain.Customer");
        orderRepository = TestArchElement.drivenPort("com.example.ports.OrderRepository");
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create empty registry")
        void shouldCreateEmptyRegistry() {
            // when
            ElementRegistry registry = ElementRegistry.builder().build();

            // then
            assertThat(registry.size()).isZero();
        }

        @Test
        @DisplayName("should add single element")
        void shouldAddSingleElement() {
            // when
            ElementRegistry registry = ElementRegistry.builder().add(order).build();

            // then
            assertThat(registry.size()).isEqualTo(1);
            assertThat(registry.contains(order.id())).isTrue();
        }

        @Test
        @DisplayName("should add multiple elements")
        void shouldAddMultipleElements() {
            // when
            ElementRegistry registry = ElementRegistry.builder()
                    .add(order)
                    .add(customer)
                    .add(orderRepository)
                    .build();

            // then
            assertThat(registry.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("should add all from collection")
        void shouldAddAllFromCollection() {
            // when
            ElementRegistry registry =
                    ElementRegistry.builder().addAll(List.of(order, customer)).build();

            // then
            assertThat(registry.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should reject duplicate elements")
        void shouldRejectDuplicates() {
            // given
            TestArchElement duplicate = TestArchElement.entity("com.example.domain.Order");

            // when/then
            assertThatThrownBy(() ->
                            ElementRegistry.builder().add(order).add(duplicate).build())
                    .isInstanceOf(DuplicateElementException.class)
                    .hasMessageContaining("com.example.domain.Order");
        }
    }

    @Nested
    @DisplayName("Access")
    class AccessTests {

        private ElementRegistry registry;

        @BeforeEach
        void setUp() {
            registry = ElementRegistry.builder()
                    .add(order)
                    .add(customer)
                    .add(orderRepository)
                    .build();
        }

        @Test
        @DisplayName("get() should return element when exists")
        void getShouldReturnWhenExists() {
            assertThat(registry.get(order.id())).contains(order);
        }

        @Test
        @DisplayName("get() should return empty when not exists")
        void getShouldReturnEmptyWhenNotExists() {
            ElementId unknown = ElementId.of("com.example.Unknown");
            assertThat(registry.get(unknown)).isEmpty();
        }

        @Test
        @DisplayName("get() with type should return when type matches")
        void getWithTypeShouldReturnWhenMatches() {
            assertThat(registry.get(order.id(), TestArchElement.class)).contains(order);
        }

        @Test
        @DisplayName("get() with type should return empty when type doesn't match")
        void getWithTypeShouldReturnEmptyWhenNoMatch() {
            // TestArchElement is not a SpecificType
            assertThat(registry.get(order.id(), SpecificType.class)).isEmpty();
        }

        @Test
        @DisplayName("contains() should return true when element exists")
        void containsShouldReturnTrueWhenExists() {
            assertThat(registry.contains(order.id())).isTrue();
        }

        @Test
        @DisplayName("contains() should return false when element not exists")
        void containsShouldReturnFalseWhenNotExists() {
            ElementId unknown = ElementId.of("com.example.Unknown");
            assertThat(registry.contains(unknown)).isFalse();
        }
    }

    @Nested
    @DisplayName("Iteration")
    class IterationTests {

        private ElementRegistry registry;

        @BeforeEach
        void setUp() {
            registry = ElementRegistry.builder()
                    .add(order)
                    .add(customer)
                    .add(orderRepository)
                    .build();
        }

        @Test
        @DisplayName("all() should return stream of all elements")
        void allShouldReturnAllElements() {
            assertThat(registry.all()).containsExactlyInAnyOrder(order, customer, orderRepository);
        }

        @Test
        @DisplayName("all() with type should filter by type")
        void allWithTypeShouldFilter() {
            assertThat(registry.all(TestArchElement.class)).containsExactlyInAnyOrder(order, customer, orderRepository);
        }

        @Test
        @DisplayName("ofKind() should return elements with matching kind")
        void ofKindShouldReturnMatchingElements() {
            assertThat(registry.ofKind(ElementKind.AGGREGATE)).containsExactly(order);
            assertThat(registry.ofKind(ElementKind.ENTITY)).containsExactly(customer);
            assertThat(registry.ofKind(ElementKind.DRIVEN_PORT)).containsExactly(orderRepository);
        }

        @Test
        @DisplayName("ofKind() should return empty stream when no matches")
        void ofKindShouldReturnEmptyWhenNoMatches() {
            assertThat(registry.ofKind(ElementKind.VALUE_OBJECT)).isEmpty();
        }

        @Test
        @DisplayName("inPackage() should return elements in package")
        void inPackageShouldReturnMatchingElements() {
            assertThat(registry.inPackage("com.example.domain")).containsExactlyInAnyOrder(order, customer);
            assertThat(registry.inPackage("com.example.ports")).containsExactly(orderRepository);
        }

        @Test
        @DisplayName("inPackage() should return empty when no matches")
        void inPackageShouldReturnEmptyWhenNoMatches() {
            assertThat(registry.inPackage("com.other")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("countByKind() should count elements by kind")
        void countByKindShouldCount() {
            // given
            ElementRegistry registry = ElementRegistry.builder()
                    .add(order)
                    .add(customer)
                    .add(TestArchElement.entity("com.example.domain.Product"))
                    .add(orderRepository)
                    .build();

            // when
            var counts = registry.countByKind();

            // then
            assertThat(counts.get(ElementKind.AGGREGATE)).isEqualTo(1L);
            assertThat(counts.get(ElementKind.ENTITY)).isEqualTo(2L);
            assertThat(counts.get(ElementKind.DRIVEN_PORT)).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("registry should be immutable after build")
        void registryShouldBeImmutable() {
            // given
            ElementRegistry.Builder builder = ElementRegistry.builder().add(order);
            ElementRegistry registry = builder.build();

            // when - add more to builder after build
            builder.add(customer);

            // then - registry should not be affected
            assertThat(registry.size()).isEqualTo(1);
            assertThat(registry.contains(customer.id())).isFalse();
        }
    }

    /**
     * Test type for verifying type filtering.
     */
    interface SpecificType extends ArchElement.Marker {}
}
