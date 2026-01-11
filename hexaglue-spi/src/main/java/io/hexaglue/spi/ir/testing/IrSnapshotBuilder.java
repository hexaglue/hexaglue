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

import io.hexaglue.spi.ir.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating {@link IrSnapshot} instances in tests.
 *
 * <p>Example usage:
 * <pre>{@code
 * IrSnapshot ir = IrSnapshotBuilder.create("com.example")
 *     .withDomainType(
 *         DomainTypeBuilder.aggregateRoot("com.example.Order")
 *             .withIdentity("id", "com.example.OrderId", "java.util.UUID")
 *             .build())
 *     .withPort(
 *         PortBuilder.repository("com.example.OrderRepository")
 *             .managing("com.example.Order")
 *             .build())
 *     .build();
 * }</pre>
 */
public final class IrSnapshotBuilder {

    private String basePackage = "com.example";
    private String projectName = null;
    private String projectVersion = null;
    private String engineVersion = "2.0.0-SNAPSHOT";
    private Instant timestamp = Instant.now();
    private final List<DomainType> domainTypes = new ArrayList<>();
    private final List<Port> ports = new ArrayList<>();

    private IrSnapshotBuilder() {}

    /**
     * Creates a new builder with the specified base package.
     */
    public static IrSnapshotBuilder create(String basePackage) {
        IrSnapshotBuilder builder = new IrSnapshotBuilder();
        builder.basePackage = basePackage;
        return builder;
    }

    /**
     * Creates a new builder with default base package "com.example".
     */
    public static IrSnapshotBuilder create() {
        return new IrSnapshotBuilder();
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Sets the base package.
     */
    public IrSnapshotBuilder basePackage(String basePackage) {
        this.basePackage = basePackage;
        return this;
    }

    /**
     * Sets the project name.
     *
     * @since 3.0.0
     */
    public IrSnapshotBuilder projectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    /**
     * Sets the project version.
     *
     * @since 3.0.0
     */
    public IrSnapshotBuilder projectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
        return this;
    }

    /**
     * Sets the engine version.
     */
    public IrSnapshotBuilder engineVersion(String version) {
        this.engineVersion = version;
        return this;
    }

    /**
     * Sets the timestamp.
     */
    public IrSnapshotBuilder timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    // =========================================================================
    // Domain types
    // =========================================================================

    /**
     * Adds a domain type to the snapshot.
     */
    public IrSnapshotBuilder withDomainType(DomainType type) {
        domainTypes.add(type);
        return this;
    }

    /**
     * Adds multiple domain types to the snapshot.
     */
    public IrSnapshotBuilder withDomainTypes(DomainType... types) {
        domainTypes.addAll(List.of(types));
        return this;
    }

    /**
     * Creates an aggregate root using a nested builder.
     *
     * <p>Example:
     * <pre>{@code
     * .withAggregateRoot("com.example.Order", builder -> builder
     *     .withIdentity("id", "com.example.OrderId", "java.util.UUID")
     *     .withProperty("status", "com.example.OrderStatus"))
     * }</pre>
     */
    public IrSnapshotBuilder withAggregateRoot(
            String qualifiedName, java.util.function.Consumer<DomainTypeBuilder> config) {
        DomainTypeBuilder builder = DomainTypeBuilder.aggregateRoot(qualifiedName);
        config.accept(builder);
        domainTypes.add(builder.build());
        return this;
    }

    /**
     * Creates an entity using a nested builder.
     */
    public IrSnapshotBuilder withEntity(String qualifiedName, java.util.function.Consumer<DomainTypeBuilder> config) {
        DomainTypeBuilder builder = DomainTypeBuilder.entity(qualifiedName);
        config.accept(builder);
        domainTypes.add(builder.build());
        return this;
    }

    /**
     * Creates a value object using a nested builder.
     */
    public IrSnapshotBuilder withValueObject(
            String qualifiedName, java.util.function.Consumer<DomainTypeBuilder> config) {
        DomainTypeBuilder builder = DomainTypeBuilder.valueObject(qualifiedName);
        config.accept(builder);
        domainTypes.add(builder.build());
        return this;
    }

    /**
     * Creates an identifier type using a nested builder.
     */
    public IrSnapshotBuilder withIdentifier(
            String qualifiedName, java.util.function.Consumer<DomainTypeBuilder> config) {
        DomainTypeBuilder builder = DomainTypeBuilder.identifier(qualifiedName);
        config.accept(builder);
        domainTypes.add(builder.build());
        return this;
    }

    // =========================================================================
    // Ports
    // =========================================================================

    /**
     * Adds a port to the snapshot.
     */
    public IrSnapshotBuilder withPort(Port port) {
        ports.add(port);
        return this;
    }

    /**
     * Adds multiple ports to the snapshot.
     */
    public IrSnapshotBuilder withPorts(Port... portsList) {
        ports.addAll(List.of(portsList));
        return this;
    }

    /**
     * Creates a repository port using a nested builder.
     */
    public IrSnapshotBuilder withRepository(String qualifiedName, java.util.function.Consumer<PortBuilder> config) {
        PortBuilder builder = PortBuilder.repository(qualifiedName);
        config.accept(builder);
        ports.add(builder.build());
        return this;
    }

    /**
     * Creates a use case port using a nested builder.
     */
    public IrSnapshotBuilder withUseCase(String qualifiedName, java.util.function.Consumer<PortBuilder> config) {
        PortBuilder builder = PortBuilder.useCase(qualifiedName);
        config.accept(builder);
        ports.add(builder.build());
        return this;
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Builds the IrSnapshot.
     */
    public IrSnapshot build() {
        DomainModel domain = new DomainModel(List.copyOf(domainTypes));
        PortModel portModel = new PortModel(List.copyOf(ports));
        IrMetadata metadata = new IrMetadata(
                basePackage, projectName, projectVersion, timestamp, engineVersion, domainTypes.size(), ports.size());

        return new IrSnapshot(domain, portModel, metadata);
    }
}
