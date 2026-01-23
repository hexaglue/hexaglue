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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
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
 * Builder for transforming DrivenPort to AdapterSpec model.
 *
 * <p>This builder creates adapter class specifications that implement port
 * interfaces (driven ports in Hexagonal Architecture) using JPA repositories
 * and MapStruct mappers.
 *
 * @since 4.0.0
 */
public final class AdapterSpecBuilder {

    private AggregateRoot aggregateRoot;
    private Entity entity;
    private List<io.hexaglue.arch.model.DrivenPort> drivenPorts;

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
     * @param infrastructurePackage the package for generated JPA classes
     * @return this builder
     */
    public AdapterSpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the driven ports to implement.
     *
     * @param ports the driven ports from the model
     * @return this builder
     * @since 5.0.0
     */
    public AdapterSpecBuilder drivenPorts(List<io.hexaglue.arch.model.DrivenPort> ports) {
        this.drivenPorts = ports != null ? List.copyOf(ports) : null;
        return this;
    }

    /**
     * Sets the aggregate root managed by this adapter.
     *
     * @param aggregateRoot the aggregate root from the model
     * @return this builder
     * @since 5.0.0
     */
    public AdapterSpecBuilder aggregateRoot(AggregateRoot aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        this.entity = null;
        return this;
    }

    /**
     * Sets the entity managed by this adapter.
     *
     * @param entity the entity from the model
     * @return this builder
     * @since 5.0.0
     */
    public AdapterSpecBuilder entity(Entity entity) {
        this.entity = entity;
        this.aggregateRoot = null;
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

        // Determine source
        String simpleName;
        String qualifiedName;

        if (aggregateRoot != null) {
            simpleName = aggregateRoot.id().simpleName();
            qualifiedName = aggregateRoot.id().qualifiedName();
        } else {
            simpleName = entity.id().simpleName();
            qualifiedName = entity.id().qualifiedName();
        }

        // Validate ports
        if (drivenPorts == null || drivenPorts.isEmpty()) {
            throw new IllegalArgumentException("At least one driven port is required to generate an adapter");
        }

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
        TypeName domainClass = ClassName.bestGuess(qualifiedName);
        String entityClassName = simpleName + config.entitySuffix();
        TypeName entityClass = ClassName.get(infrastructurePackage, entityClassName);
        TypeName repositoryClass = ClassName.get(infrastructurePackage, simpleName + config.repositorySuffix());
        TypeName mapperClass = ClassName.get(infrastructurePackage, simpleName + config.mapperSuffix());

        // Collect all methods from all driven ports with deduplication
        List<AdapterMethodSpec> methods = buildAdapterMethodSpecs();

        // Build IdInfo
        AdapterContext.IdInfo idInfo = buildIdInfo();

        // Determine if domain class is a record
        boolean isDomainRecord = isDomainRecord();

        return new AdapterSpec(
                infrastructurePackage,
                className,
                implementedPorts,
                domainClass,
                entityClass,
                repositoryClass,
                mapperClass,
                methods,
                idInfo,
                isDomainRecord);
    }

    /**
     * Determines if the domain class is a Java record.
     *
     * @return true if the domain class is a record, false for regular classes
     * @since 3.0.0
     */
    private boolean isDomainRecord() {
        if (aggregateRoot != null) {
            return aggregateRoot.structure().isRecord();
        } else if (entity != null) {
            return entity.structure().isRecord();
        }
        return false;
    }

    /**
     * Builds IdInfo from domain source.
     */
    private AdapterContext.IdInfo buildIdInfo() {
        if (aggregateRoot != null) {
            return buildIdInfoFromField(aggregateRoot.identityField());
        } else {
            var identityFieldOpt = entity.identityField();
            if (identityFieldOpt.isEmpty()) {
                return null;
            }
            return buildIdInfoFromField(identityFieldOpt.get());
        }
    }

    /**
     * Builds IdInfo from a Field.
     *
     * @since 5.0.0
     */
    private AdapterContext.IdInfo buildIdInfoFromField(Field identityField) {
        TypeName wrapperType =
                JpaModelUtils.resolveTypeName(identityField.type().qualifiedName());
        TypeName unwrappedType = wrapperType;
        boolean isWrapped = identityField.wrappedType().isPresent();

        if (isWrapped) {
            unwrappedType = JpaModelUtils.resolveTypeName(
                    identityField.wrappedType().get().qualifiedName());
            return new AdapterContext.IdInfo(wrapperType, unwrappedType, true);
        }

        return new AdapterContext.IdInfo(null, unwrappedType, false);
    }

    /**
     * Builds adapter method specifications from driven ports.
     *
     * <p>Uses the port's structure.methods() to extract all interface methods.
     *
     * @since 5.0.0
     */
    private List<AdapterMethodSpec> buildAdapterMethodSpecs() {
        Map<String, AdapterMethodSpec> methodsBySignature = new LinkedHashMap<>();

        for (io.hexaglue.arch.model.DrivenPort port : drivenPorts) {
            for (var method : port.structure().methods()) {
                // Skip static and default methods - only abstract interface methods need implementation
                if (method.isStatic()) {
                    continue;
                }
                AdapterMethodSpec spec = AdapterMethodSpec.fromV5(method);
                String signature = computeSignature(spec);
                methodsBySignature.putIfAbsent(signature, spec);
            }
        }

        return new ArrayList<>(methodsBySignature.values());
    }

    /**
     * Computes a unique signature for a method based on name and parameter types.
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
        if (aggregateRoot == null && entity == null) {
            throw new IllegalStateException("Either aggregateRoot or entity is required");
        }
        if (drivenPorts == null || drivenPorts.isEmpty()) {
            throw new IllegalStateException("drivenPorts is required");
        }
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }
    }
}
