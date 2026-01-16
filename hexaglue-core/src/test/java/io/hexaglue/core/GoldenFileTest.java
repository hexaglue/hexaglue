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

package io.hexaglue.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Golden file tests for IR non-regression.
 * Compares generated IR JSON against expected golden files.
 *
 * @deprecated v4.0.0 - IrSnapshot has been removed. These tests need to be rewritten
 *             to use ArchitecturalModel serialization.
 */
@Disabled("v4.0.0 - IrSnapshot removed, needs rewrite for ArchitecturalModel")
class GoldenFileTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("hexaglue-golden-test-");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir).sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    // ignore
                }
            });
        }
    }

    @Test
    void coffeeshopDomain_shouldMatchGoldenFile() throws IOException {
        // Given: Coffee shop domain sources
        writeSource("com/coffeeshop/domain/order/OrderId.java", """
                package com.coffeeshop.domain.order;
                import java.util.UUID;
                public record OrderId(UUID value) {}
                """);

        writeSource("com/coffeeshop/domain/order/Location.java", """
                package com.coffeeshop.domain.order;
                public enum Location { IN_STORE, TAKE_AWAY }
                """);

        // Explicit @ValueObject annotation (implicit heuristics removed)
        writeSource("com/coffeeshop/domain/order/LineItem.java", """
                package com.coffeeshop.domain.order;
                import java.math.BigDecimal;
                import org.jmolecules.ddd.annotation.ValueObject;
                @ValueObject
                public record LineItem(String productName, int quantity, BigDecimal unitPrice) {}
                """);

        writeSource("com/coffeeshop/domain/order/Order.java", """
                package com.coffeeshop.domain.order;
                import java.util.List;
                public class Order {
                    private final OrderId id;
                    private final String customerName;
                    private final Location location;
                    private final List<LineItem> items;
                    public Order(OrderId id, String customerName, Location location, List<LineItem> items) {
                        this.id = id;
                        this.customerName = customerName;
                        this.location = location;
                        this.items = items;
                    }
                    public OrderId getId() { return id; }
                    public String getCustomerName() { return customerName; }
                    public Location getLocation() { return location; }
                    public List<LineItem> getItems() { return items; }
                }
                """);

        // Explicit @PrimaryPort annotation (naming criteria removed)
        writeSource("com/coffeeshop/ports/in/OrderingCoffee.java", """
                package com.coffeeshop.ports.in;
                import com.coffeeshop.domain.order.*;
                import java.util.Optional;
                import org.jmolecules.architecture.hexagonal.PrimaryPort;
                @PrimaryPort
                public interface OrderingCoffee {
                    Order createOrder(String customerName, Location location);
                    Optional<Order> findOrder(OrderId id);
                }
                """);

        // Explicit @Repository annotation
        writeSource("com/coffeeshop/ports/out/OrderRepository.java", """
                package com.coffeeshop.ports.out;
                import com.coffeeshop.domain.order.*;
                import java.util.Optional;
                import org.jmolecules.ddd.annotation.Repository;
                @Repository
                public interface OrderRepository {
                    Order save(Order order);
                    Optional<Order> findById(OrderId id);
                }
                """);

        // When: Analyze
        EngineConfig config = EngineConfig.minimal(tempDir, "com.coffeeshop");
        HexaGlueEngine engine = HexaGlueEngine.create();
        EngineResult result = engine.analyze(config);

        // Then: v4 uses ArchitecturalModel instead of IrSnapshot
        // Golden file comparison is disabled until a new serializer is implemented
        assertThat(result.model()).isNotNull();
        assertThat(result.model().size()).isGreaterThan(0);
    }

    private void writeSource(String relativePath, String content) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}
