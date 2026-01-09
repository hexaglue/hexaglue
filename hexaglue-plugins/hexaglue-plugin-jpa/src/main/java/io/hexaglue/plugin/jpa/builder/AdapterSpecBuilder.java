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

package io.hexaglue.plugin.jpa.builder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Port;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for transforming SPI Port to AdapterSpec model.
 *
 * <p>This builder creates adapter class specifications that implement port
 * interfaces (driven ports in Hexagonal Architecture) using JPA repositories
 * and MapStruct mappers. It handles:
 * <ul>
 *   <li>Single port implementation (one port → one adapter)</li>
 *   <li>Multiple port implementation (merged adapter pattern)</li>
 *   <li>Port method transformation to adapter method specifications</li>
 *   <li>Dependency resolution (repository, mapper references)</li>
 *   <li>Type mapping between domain and infrastructure layers</li>
 * </ul>
 *
 * <p>Design decision: Adapters bridge the domain and infrastructure layers.
 * They implement port interfaces defined in the domain layer and delegate to
 * infrastructure components (JPA repositories, MapStruct mappers) to perform
 * actual persistence operations. This keeps the domain layer independent of
 * infrastructure concerns while providing a clean integration point.
 *
 * <h3>Generated Adapter Example:</h3>
 * <pre>{@code
 * @Component
 * public class OrderRepositoryAdapter implements OrderRepository {
 *     private final OrderJpaRepository repository;
 *     private final OrderMapper mapper;
 *
 *     public OrderRepositoryAdapter(OrderJpaRepository repository, OrderMapper mapper) {
 *         this.repository = repository;
 *         this.mapper = mapper;
 *     }
 *
 *     @Override
 *     public Order save(Order order) {
 *         OrderEntity entity = mapper.toEntity(order);
 *         OrderEntity saved = repository.save(entity);
 *         return mapper.toDomain(saved);
 *     }
 *
 *     @Override
 *     public Optional<Order> findById(UUID id) {
 *         return repository.findById(id).map(mapper::toDomain);
 *     }
 * }
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * AdapterSpec spec = AdapterSpecBuilder.builder()
 *     .ports(List.of(orderRepositoryPort))
 *     .domainType(orderType)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 2.0.0
 */
public final class AdapterSpecBuilder {

    private List<Port> ports;
    private DomainType domainType;
    private JpaConfig config;
    private String infrastructurePackage;

    private AdapterSpecBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new AdapterSpecBuilder instance.
     *
     * @return a new builder instance
     */
    public static AdapterSpecBuilder builder() {
        return new AdapterSpecBuilder();
    }

    /**
     * Sets the port interfaces to implement.
     *
     * <p>For single port adapters, provide a list with one port. For merged
     * adapters that implement multiple repository interfaces for the same
     * aggregate, provide multiple ports.
     *
     * @param ports the driven port interfaces
     * @return this builder
     */
    public AdapterSpecBuilder ports(List<Port> ports) {
        this.ports = ports;
        return this;
    }

    /**
     * Sets the domain type managed by this adapter.
     *
     * <p>This should be the aggregate root or entity that the adapter's
     * repository and mapper work with.
     *
     * @param domainType the domain aggregate root or entity
     * @return this builder
     */
    public AdapterSpecBuilder domainType(DomainType domainType) {
        this.domainType = domainType;
        return this;
    }

    /**
     * Sets the JPA plugin configuration.
     *
     * @param config the JPA configuration
     * @return this builder
     */
    public AdapterSpecBuilder config(JpaConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Sets the infrastructure package name.
     *
     * <p>This is typically derived from the domain package by replacing
     * "domain" with "infrastructure.jpa".
     *
     * @param infrastructurePackage the package for generated JPA classes
     * @return this builder
     */
    public AdapterSpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Builds the AdapterSpec from the provided configuration.
     *
     * <p>This method performs the transformation from SPI Port(s) and DomainType
     * to the AdapterSpec model. It resolves all type references, generates method
     * specifications, and prepares the complete adapter specification for code
     * generation.
     *
     * <p>For merged adapters (multiple ports), the class name is derived from the
     * domain type rather than the port name. For single port adapters, the class
     * name can be derived from either the port or domain type based on preference.
     *
     * @return an immutable AdapterSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if ports list is empty
     */
    public AdapterSpec build() {
        validateRequiredFields();

        if (ports.isEmpty()) {
            throw new IllegalArgumentException("At least one port is required to generate an adapter");
        }

        // Derive class name from domain type for consistency
        String className = domainType.simpleName() + config.adapterSuffix();

        // Convert all ports to TypeNames for implements clause
        List<TypeName> implementedPorts = ports.stream()
                .map(port -> ClassName.bestGuess(port.qualifiedName()))
                .collect(Collectors.toList());

        // Resolve domain, entity, repository, and mapper types
        TypeName domainClass = ClassName.bestGuess(domainType.qualifiedName());
        String entityClassName = domainType.simpleName() + config.entitySuffix();
        TypeName entityClass = ClassName.get(infrastructurePackage, entityClassName);
        TypeName repositoryClass =
                ClassName.get(infrastructurePackage, domainType.simpleName() + config.repositorySuffix());
        TypeName mapperClass = ClassName.get(infrastructurePackage, domainType.simpleName() + config.mapperSuffix());

        // Collect all methods from all ports
        List<AdapterMethodSpec> methods = buildAdapterMethodSpecs();

        return new AdapterSpec(
                infrastructurePackage,
                className,
                implementedPorts,
                domainClass,
                entityClass,
                repositoryClass,
                mapperClass,
                methods);
    }

    /**
     * Builds adapter method specifications from all port methods.
     *
     * <p>This method collects all methods from all provided ports and transforms
     * them into AdapterMethodSpec instances. Each method spec contains the
     * signature information and inferred pattern needed for implementation
     * generation.
     *
     * <p>Method patterns are inferred from the method name (save, findById, etc.)
     * to determine the appropriate implementation strategy in the code generator.
     *
     * @return list of adapter method specifications
     */
    private List<AdapterMethodSpec> buildAdapterMethodSpecs() {
        List<AdapterMethodSpec> methods = new ArrayList<>();

        for (Port port : ports) {
            List<AdapterMethodSpec> portMethods =
                    port.methods().stream().map(AdapterMethodSpec::from).collect(Collectors.toList());
            methods.addAll(portMethods);
        }

        return methods;
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        if (ports == null) {
            throw new IllegalStateException("ports is required");
        }
        if (domainType == null) {
            throw new IllegalStateException("domainType is required");
        }
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }
    }
}
