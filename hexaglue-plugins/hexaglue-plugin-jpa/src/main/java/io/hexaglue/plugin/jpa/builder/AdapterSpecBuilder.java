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
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Port;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * @since 3.0.0
 */
public final class AdapterSpecBuilder {

    // Legacy SPI fields
    private List<Port> ports;
    private DomainType domainType;

    // v4 model fields
    private List<DrivenPort> drivenPorts;
    private DomainEntity domainEntity;
    private ArchitecturalModel architecturalModel;

    // Common fields
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
     * @deprecated Use {@link #drivenPorts(List)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
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
     * @deprecated Use {@link #domainEntity(DomainEntity)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
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
     * <p>If v4 model is available (drivenPorts and domainEntity set),
     * uses v4 model. Otherwise falls back to legacy SPI.
     *
     * @return an immutable AdapterSpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if ports list is empty
     */
    public AdapterSpec build() {
        validateRequiredFields();

        // Use v4 model if available
        if (drivenPorts != null && domainEntity != null) {
            return buildFromV4Model();
        }

        // Fall back to legacy SPI
        return buildFromLegacyModel();
    }

    /**
     * Builds AdapterSpec using v4 ArchitecturalModel.
     *
     * @return the built AdapterSpec
     * @since 4.0.0
     */
    private AdapterSpec buildFromV4Model() {
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
        List<AdapterMethodSpec> methods = buildAdapterMethodSpecsV4();

        // Build IdInfo from domain entity's identity
        AdapterContext.IdInfo idInfo = buildIdInfoV4();

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
    private AdapterContext.IdInfo buildIdInfoV4() {
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
    private List<AdapterMethodSpec> buildAdapterMethodSpecsV4() {
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
     * Builds AdapterSpec using legacy SPI model.
     *
     * @return the built AdapterSpec
     */
    private AdapterSpec buildFromLegacyModel() {
        if (ports.isEmpty()) {
            throw new IllegalArgumentException("At least one port is required to generate an adapter");
        }

        // Derive class name based on number of ports
        // Single port: use port name (e.g., PokemonRepositoryFetcherAdapter)
        // Multiple ports: use domain type name for backward compatibility
        String className;
        if (ports.size() == 1) {
            className = ports.get(0).simpleName() + config.adapterSuffix();
        } else {
            className = domainType.simpleName() + config.adapterSuffix();
        }

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

        // Collect all methods from all ports with deduplication
        List<AdapterMethodSpec> methods = buildAdapterMethodSpecs();

        // Build IdInfo from domain type's identity
        AdapterContext.IdInfo idInfo =
                domainType.identity().map(AdapterContext.IdInfo::from).orElse(null);

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
     * Builds the AdapterContext for method generation.
     *
     * <p>The AdapterContext provides type information and field names needed
     * by method generation strategies. It includes IdInfo from the domain type's
     * identity for proper ID handling.
     *
     * @return the adapter context
     */
    public AdapterContext buildContext() {
        validateRequiredFields();

        TypeName domainClass = ClassName.bestGuess(domainType.qualifiedName());
        String entityClassName = domainType.simpleName() + config.entitySuffix();
        TypeName entityClass = ClassName.get(infrastructurePackage, entityClassName);

        // Build IdInfo from domain type's identity
        AdapterContext.IdInfo idInfo =
                domainType.identity().map(AdapterContext.IdInfo::from).orElse(null);

        return new AdapterContext(domainClass, entityClass, "repository", "mapper", idInfo);
    }

    /**
     * Builds adapter method specifications from all port methods with deduplication.
     *
     * <p>This method collects all methods from all provided ports and transforms
     * them into AdapterMethodSpec instances. Methods with identical signatures
     * are deduplicated to prevent generating duplicate implementations.
     *
     * <p>Deduplication is essential for merged adapters where multiple ports may
     * declare the same standard repository methods (findById, save, etc.).
     *
     * @return list of deduplicated adapter method specifications
     */
    private List<AdapterMethodSpec> buildAdapterMethodSpecs() {
        Map<String, AdapterMethodSpec> methodsBySignature = new LinkedHashMap<>();

        for (Port port : ports) {
            for (var method : port.methods()) {
                AdapterMethodSpec spec = AdapterMethodSpec.from(method);
                String signature = computeSignature(spec);

                // Keep the first occurrence only (deduplication)
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
