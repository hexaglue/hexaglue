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

package io.hexaglue.spi.ir.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.ir.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IrSnapshotBuilder")
class IrSnapshotBuilderTest {

    @Nested
    @DisplayName("Basic usage")
    class BasicUsageTest {

        @Test
        @DisplayName("should create empty snapshot")
        void shouldCreateEmptySnapshot() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.example").build();

            assertThat(ir.isEmpty()).isTrue();
            assertThat(ir.metadata().basePackage()).isEqualTo("com.example");
            assertThat(ir.domain().types()).isEmpty();
            assertThat(ir.ports().ports()).isEmpty();
        }

        @Test
        @DisplayName("should create snapshot with default base package")
        void shouldCreateSnapshotWithDefaultBasePackage() {
            IrSnapshot ir = IrSnapshotBuilder.create().build();

            assertThat(ir.metadata().basePackage()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Domain types")
    class DomainTypesTest {

        @Test
        @DisplayName("should add domain type")
        void shouldAddDomainType() {
            DomainType order =
                    DomainTypeBuilder.aggregateRoot("com.example.Order").build();

            IrSnapshot ir = IrSnapshotBuilder.create("com.example")
                    .withDomainType(order)
                    .build();

            assertThat(ir.domain().types()).hasSize(1);
            assertThat(ir.domain().types().get(0).qualifiedName()).isEqualTo("com.example.Order");
            assertThat(ir.metadata().typeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should add aggregate root with nested builder")
        void shouldAddAggregateRootWithNestedBuilder() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.example")
                    .withAggregateRoot(
                            "com.example.Order", b -> b.withIdentity("id", "com.example.OrderId", "java.util.UUID")
                                    .withProperty("status", "com.example.OrderStatus"))
                    .build();

            assertThat(ir.domain().types()).hasSize(1);
            DomainType order = ir.domain().types().get(0);
            assertThat(order.isAggregateRoot()).isTrue();
            assertThat(order.hasIdentity()).isTrue();
            assertThat(order.properties()).hasSize(1);
        }

        @Test
        @DisplayName("should add multiple domain types")
        void shouldAddMultipleDomainTypes() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.example")
                    .withAggregateRoot("com.example.Order", b -> {})
                    .withEntity("com.example.LineItem", b -> {})
                    .withValueObject("com.example.Address", b -> {})
                    .withIdentifier("com.example.OrderId", b -> {})
                    .build();

            assertThat(ir.domain().types()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Ports")
    class PortsTest {

        @Test
        @DisplayName("should add port")
        void shouldAddPort() {
            Port repo = PortBuilder.repository("com.example.OrderRepository").build();

            IrSnapshot ir =
                    IrSnapshotBuilder.create("com.example").withPort(repo).build();

            assertThat(ir.ports().ports()).hasSize(1);
            assertThat(ir.ports().ports().get(0).qualifiedName()).isEqualTo("com.example.OrderRepository");
            assertThat(ir.metadata().portCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should add repository with nested builder")
        void shouldAddRepositoryWithNestedBuilder() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.example")
                    .withRepository("com.example.OrderRepository", b -> b.managing("com.example.Order")
                            .withSaveMethod("com.example.Order")
                            .withFindByIdMethod("com.example.Order", "com.example.OrderId"))
                    .build();

            assertThat(ir.ports().ports()).hasSize(1);
            Port repo = ir.ports().ports().get(0);
            assertThat(repo.isRepository()).isTrue();
            assertThat(repo.managedTypes()).containsExactly("com.example.Order");
            assertThat(repo.methods()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Full scenario")
    class FullScenarioTest {

        @Test
        @DisplayName("should build complete IR snapshot")
        void shouldBuildCompleteIrSnapshot() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.example")
                    .withIdentifier("com.example.OrderId", b -> b.withProperty("value", "java.util.UUID"))
                    .withValueObject("com.example.Address", b -> b.withProperty("street", "java.lang.String")
                            .withProperty("city", "java.lang.String"))
                    .withAggregateRoot(
                            "com.example.Order", b -> b.withIdentity("id", "com.example.OrderId", "java.util.UUID")
                                    .withEmbeddedProperty("shippingAddress", "com.example.Address")
                                    .withJMoleculesAggregateRoot())
                    .withRepository("com.example.OrderRepository", b -> b.managing("com.example.Order")
                            .withJMoleculesRepository())
                    .build();

            // Verify domain
            assertThat(ir.domain().types()).hasSize(3);
            assertThat(ir.domain().types()).extracting("simpleName").containsExactly("OrderId", "Address", "Order");

            // Verify ports
            assertThat(ir.ports().ports()).hasSize(1);
            assertThat(ir.ports().ports().get(0).isRepository()).isTrue();

            // Verify metadata
            assertThat(ir.metadata().typeCount()).isEqualTo(3);
            assertThat(ir.metadata().portCount()).isEqualTo(1);
            assertThat(ir.isEmpty()).isFalse();
        }
    }
}
