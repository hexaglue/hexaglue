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

package io.hexaglue.plugin.jpa.model;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.spi.ir.DomainModel;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Port;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Complete specification for generating a JPA adapter implementation.
 *
 * <p>This record aggregates all information needed to generate an adapter class
 * that implements one or more port interfaces. The adapter bridges the domain
 * and persistence layers using the repository and mapper.
 *
 * <p>Design decision: Adapters implement Hexagonal Architecture's port interfaces,
 * delegating to Spring Data JPA repositories and MapStruct mappers for the actual
 * persistence and mapping logic. This keeps the adapter focused on coordination.
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * package com.example.infrastructure.jpa;
 *
 * import org.springframework.stereotype.Component;
 *
 * @Component
 * public class OrderAdapter implements OrderRepository {
 *     private final OrderJpaRepository repository;
 *     private final OrderMapper mapper;
 *
 *     public OrderAdapter(OrderJpaRepository repository, OrderMapper mapper) {
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
 *     // ... other methods
 * }
 * }</pre>
 *
 * @param packageName the package for the generated adapter class
 * @param className the simple class name (e.g., "OrderAdapter")
 * @param implementedPorts the list of port interfaces to implement
 * @param domainClass the JavaPoet type of the domain aggregate
 * @param entityClass the JavaPoet type of the JPA entity
 * @param repositoryClass the JavaPoet type of the Spring Data repository
 * @param mapperClass the JavaPoet type of the MapStruct mapper
 * @param methods the list of methods to implement
 * @since 2.0.0
 */
public record AdapterSpec(
        String packageName,
        String className,
        List<TypeName> implementedPorts,
        TypeName domainClass,
        TypeName entityClass,
        TypeName repositoryClass,
        TypeName mapperClass,
        List<AdapterMethodSpec> methods) {

    /**
     * Creates an AdapterSpec from a SPI Port, DomainModel, and JpaConfig.
     *
     * <p>This factory method derives the adapter metadata from the port interface
     * and domain model, applying naming conventions from the configuration.
     *
     * <p>Naming convention:
     * <ul>
     *   <li>Class name: {PortSimpleName} + {adapterSuffix}</li>
     *   <li>Package: {entityPackage} (same as entity)</li>
     * </ul>
     *
     * <p>The adapter will:
     * <ul>
     *   <li>Implement the port interface</li>
     *   <li>Depend on the generated JPA repository</li>
     *   <li>Depend on the generated mapper</li>
     *   <li>Implement all port methods by delegating to repository/mapper</li>
     * </ul>
     *
     * @param port the driven port interface to implement
     * @param domain the domain model containing the aggregate root
     * @param config the JPA plugin configuration
     * @return an AdapterSpec ready for code generation
     * @throws IllegalArgumentException if the port has no primary managed type
     */
    public static AdapterSpec from(Port port, DomainModel domain, JpaConfig config) {
        if (port.primaryManagedType() == null) {
            throw new IllegalArgumentException(
                    "Port " + port.qualifiedName() + " has no primary managed type. Cannot generate adapter.");
        }

        // Find the domain type managed by this port
        DomainType domainType = domain.findByQualifiedName(port.primaryManagedType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Domain type " + port.primaryManagedType() + " not found in model"));

        // Derive package and names
        String entityPackage = JpaModelUtils.deriveInfrastructurePackage(domainType.packageName());
        String className = port.simpleName() + config.adapterSuffix();

        // Resolve types
        TypeName portType = ClassName.bestGuess(port.qualifiedName());
        TypeName domainClass = ClassName.bestGuess(domainType.qualifiedName());
        TypeName entityClass = ClassName.get(entityPackage, domainType.simpleName() + config.entitySuffix());
        TypeName repositoryClass = ClassName.get(entityPackage, domainType.simpleName() + config.repositorySuffix());
        TypeName mapperClass = ClassName.get(entityPackage, domainType.simpleName() + config.mapperSuffix());

        // Convert port methods to adapter method specs
        List<AdapterMethodSpec> methods =
                port.methods().stream().map(AdapterMethodSpec::from).collect(Collectors.toList());

        return new AdapterSpec(
                entityPackage,
                className,
                List.of(portType),
                domainClass,
                entityClass,
                repositoryClass,
                mapperClass,
                methods);
    }

    /**
     * Returns the fully qualified class name.
     *
     * @return packageName + "." + className
     */
    public String fullyQualifiedClassName() {
        return packageName + "." + className;
    }

    /**
     * Returns true if this adapter implements multiple ports.
     *
     * @return true if implementedPorts has more than one element
     */
    public boolean implementsMultiplePorts() {
        return implementedPorts.size() > 1;
    }

    /**
     * Returns true if this adapter has any methods to implement.
     *
     * @return true if methods list is not empty
     */
    public boolean hasMethods() {
        return !methods.isEmpty();
    }

    /**
     * Returns the count of methods by pattern.
     *
     * @param pattern the method pattern to count
     * @return the number of methods matching the pattern
     */
    public long methodCountByPattern(MethodPattern pattern) {
        return methods.stream().filter(m -> m.pattern() == pattern).count();
    }
}
