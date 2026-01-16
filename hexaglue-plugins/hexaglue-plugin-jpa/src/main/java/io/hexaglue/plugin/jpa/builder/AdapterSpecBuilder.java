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
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.plugin.jpa.model.JpaModelUtils;
import io.hexaglue.plugin.jpa.strategy.AdapterContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder for transforming v4 DrivenPort to AdapterSpec model.
 *
 * <p>This builder creates adapter class specifications that implement port
 * interfaces (driven ports in Hexagonal Architecture) using JPA repositories
 * and MapStruct mappers. It handles:
 * <ul>
 *   <li>Single port implementation (one port → one adapter)</li>
 *   <li>Multiple port implementation (merged adapter pattern)</li>
 *   <li>Port method transformation to adapter method specifications</li>
 *   <li>Method deduplication (important for merged ports)</li>
 *   <li>Dependency resolution (repository, mapper references)</li>
 *   <li>Type mapping between domain and infrastructure layers</li>
 * </ul>
 *
 * <p>Design decision: Adapters bridge the domain and infrastructure layers.
 * They implement port interfaces defined in the domain layer and delegate to
 * infrastructure components (JPA repositories, MapStruct mappers) to perform
 * actual persistence operations.
 *
 * <h3>Method Deduplication:</h3>
 * <p>When multiple ports are merged into a single adapter, they may declare the
 * same methods (e.g., both ports have {@code findById}). This builder deduplicates
 * methods by their signature to avoid generating duplicate implementations.
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
 *         var entity = mapper.toEntity(order);
 *         var saved = repository.save(entity);
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
 * @since 4.0.0
 */
public final class AdapterSpecBuilder {

    private List<DrivenPort> drivenPorts;
    private DomainEntity domainEntity;
    private ArchitecturalModel architecturalModel;
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
     * Sets the v4 driven ports to implement.
     *
     * @param ports the driven ports
     * @return this builder
     * @since 4.0.0
     */
    public AdapterSpecBuilder drivenPorts(List<DrivenPort> ports) {
        this.drivenPorts = ports;
        return this;
    }

    /**
     * Sets the v4 domain entity managed by this adapter.
     *
     * @param entity the domain entity
     * @return this builder
     * @since 4.0.0
     */
    public AdapterSpecBuilder domainEntity(DomainEntity entity) {
        this.domainEntity = entity;
        return this;
    }

    /**
     * Sets the v4 architectural model.
     *
     * @param model the architectural model
     * @return this builder
     * @since 4.0.0
     */
    public AdapterSpecBuilder model(ArchitecturalModel model) {
        this.architecturalModel = model;
        return this;
    }

    /**
     * Builds the AdapterSpec from the provided configuration.
     *
     * @return an immutable AdapterSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if ports list is empty
     */
    public AdapterSpec build() {
        validateRequiredFields();

        if (drivenPorts.isEmpty()) {
            throw new IllegalArgumentException("At least one driven port is required to generate an adapter");
        }

        String simpleName = domainEntity.id().simpleName();

        // Derive class name based on number of ports
        String className;
        if (drivenPorts.size() == 1) {
            className = drivenPorts.get(0).id().simpleName() + config.adapterSuffix();
        } else {
            className = simpleName + config.adapterSuffix();
        }

        // Convert all ports to TypeNames for implements clause
        List<TypeName> implementedPorts = drivenPorts.stream()
                .map(port -> ClassName.bestGuess(port.id().qualifiedName()))
                .collect(Collectors.toList());

        // Resolve domain, entity, repository, and mapper types
        TypeName domainClass = ClassName.bestGuess(domainEntity.id().qualifiedName());
        String entityClassName = simpleName + config.entitySuffix();
        TypeName entityClass = ClassName.get(infrastructurePackage, entityClassName);
        TypeName repositoryClass = ClassName.get(infrastructurePackage, simpleName + config.repositorySuffix());
        TypeName mapperClass = ClassName.get(infrastructurePackage, simpleName + config.mapperSuffix());

        // Collect all methods from all driven ports with deduplication
        List<AdapterMethodSpec> methods = buildAdapterMethodSpecs();

        // Build IdInfo from domain entity's identity
        AdapterContext.IdInfo idInfo = buildIdInfo();

        return new AdapterSpec(
                infrastructurePackage,
                className,
                implementedPorts,
                domainClass,
                entityClass,
                repositoryClass,
                mapperClass,
                methods,
                idInfo);
    }

    /**
     * Builds IdInfo from v4 domain entity.
     */
    private AdapterContext.IdInfo buildIdInfo() {
        if (!domainEntity.hasIdentity()) {
            return null;
        }

        io.hexaglue.syntax.TypeRef idType = domainEntity.identityType();

        // Check if it's a wrapped identity
        TypeName wrapperType = JpaModelUtils.resolveTypeName(idType.qualifiedName());
        TypeName unwrappedType = wrapperType;
        boolean isWrapped = false;

        if (architecturalModel != null) {
            var voOpt = architecturalModel
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(idType.qualifiedName()))
                    .filter(vo -> vo.componentFields().size() == 1)
                    .filter(vo -> vo.syntax() != null)
                    .findFirst();

            if (voOpt.isPresent()) {
                var vo = voOpt.get();
                var wrappedField = vo.syntax().fields().get(0);
                unwrappedType =
                        JpaModelUtils.resolveTypeName(wrappedField.type().qualifiedName());
                isWrapped = true;
            }
        }

        return new AdapterContext.IdInfo(isWrapped ? wrapperType : null, unwrappedType, isWrapped);
    }

    /**
     * Builds adapter method specifications from v4 driven ports.
     */
    private List<AdapterMethodSpec> buildAdapterMethodSpecs() {
        Map<String, AdapterMethodSpec> methodsBySignature = new LinkedHashMap<>();

        for (DrivenPort port : drivenPorts) {
            for (var operation : port.operations()) {
                AdapterMethodSpec spec = AdapterMethodSpec.fromV4(operation);
                String signature = computeSignature(spec);
                methodsBySignature.putIfAbsent(signature, spec);
            }
        }

        return new ArrayList<>(methodsBySignature.values());
    }

    /**
     * Computes a unique signature for a method based on name and parameter types.
     *
     * <p>This signature is used for deduplication. Two methods with the same name
     * and parameter types are considered duplicates, even if they come from
     * different port interfaces.
     *
     * @param spec the method specification
     * @return a signature string like "findById(java.util.UUID)"
     */
    private String computeSignature(AdapterMethodSpec spec) {
        String params = spec.parameters().stream().map(p -> p.type().toString()).collect(Collectors.joining(","));
        return spec.name() + "(" + params + ")";
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
        if (drivenPorts == null) {
            throw new IllegalStateException("drivenPorts is required");
        }
        if (domainEntity == null) {
            throw new IllegalStateException("domainEntity is required");
        }
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }
    }
}
