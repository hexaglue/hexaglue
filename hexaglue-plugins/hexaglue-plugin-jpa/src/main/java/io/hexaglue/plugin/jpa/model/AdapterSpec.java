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

import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.strategy.AdapterContext;
import java.util.List;

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
 * @param idInfo the identity information for ID handling (may be null)
 * @param isDomainRecord true if the domain class is a Java record
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
        List<AdapterMethodSpec> methods,
        AdapterContext.IdInfo idInfo,
        boolean isDomainRecord) {

    public AdapterSpec {
        implementedPorts = implementedPorts != null ? List.copyOf(implementedPorts) : null;
        methods = methods != null ? List.copyOf(methods) : List.of();
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
     * Returns the count of methods by kind.
     *
     * @param kind the method kind to count
     * @return the number of methods matching the kind
     */
    public long methodCountByKind(io.hexaglue.arch.model.ir.MethodKind kind) {
        return methods.stream().filter(m -> m.kind() == kind).count();
    }
}
