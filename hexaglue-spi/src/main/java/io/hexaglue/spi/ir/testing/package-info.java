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

/**
 * Test utilities for building IR structures in plugin tests.
 *
 * <p>This package provides fluent builders for creating test fixtures:
 * <ul>
 *   <li>{@link io.hexaglue.spi.ir.testing.DomainTypeBuilder} - for creating
 *       {@link io.hexaglue.spi.ir.DomainType} instances with various configurations</li>
 *   <li>{@link io.hexaglue.spi.ir.testing.PortBuilder} - for creating
 *       {@link io.hexaglue.spi.ir.Port} instances</li>
 *   <li>{@link io.hexaglue.spi.ir.testing.IrSnapshotBuilder} - for creating complete
 *       {@link io.hexaglue.spi.ir.IrSnapshot} instances</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainType order = DomainTypeBuilder.aggregateRoot("com.example.Order")
 *     .withIdentity("id", "com.example.OrderId", "java.util.UUID")
 *     .withProperty("status", "com.example.OrderStatus")
 *     .withCollectionProperty("items", "com.example.LineItem")
 *     .build();
 *
 * IrSnapshot snapshot = IrSnapshotBuilder.create()
 *     .withDomainType(order)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Builders are mutable and not thread-safe. Each test should create its own
 * builder instance.
 *
 * @since 2.0.0
 */
package io.hexaglue.spi.ir.testing;
