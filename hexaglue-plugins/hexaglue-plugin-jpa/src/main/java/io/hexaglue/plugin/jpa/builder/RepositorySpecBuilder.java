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
import io.hexaglue.plugin.jpa.model.DerivedMethodSpec;
import io.hexaglue.plugin.jpa.model.JpaModelUtils;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for transforming v4 DomainEntity to RepositorySpec model.
 *
 * <p>This builder handles the transformation of domain aggregate roots into
 * Spring Data JPA repository interface specifications. It resolves entity
 * and ID types, applies naming conventions, and prepares the specification
 * for JavaPoet code generation.
 *
 * <p>Design decision: Repository generation is straightforward since Spring Data
 * repositories are purely declarative interfaces. The builder's main responsibility
 * is determining the correct entity and ID type parameters for the JpaRepository
 * generic interface.
 *
 * <h3>Generated Repository Example:</h3>
 * <pre>{@code
 * public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
 *     // Spring Data provides CRUD implementations automatically
 * }
 * }</pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * RepositorySpec spec = RepositorySpecBuilder.builder()
 *     .domainEntity(orderEntity)
 *     .model(architecturalModel)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 4.0.0
 */
public final class RepositorySpecBuilder {

    private DomainEntity domainEntity;
    private List<DrivenPort> drivenPorts = List.of();
    private ArchitecturalModel architecturalModel;
    private JpaConfig config;
    private String infrastructurePackage;

    private RepositorySpecBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new RepositorySpecBuilder instance.
     *
     * @return a new builder instance
     */
    public static RepositorySpecBuilder builder() {
        return new RepositorySpecBuilder();
    }

    /**
     * Sets the JPA plugin configuration.
     *
     * @param config the JPA configuration
     * @return this builder
     */
    public RepositorySpecBuilder config(JpaConfig config) {
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
    public RepositorySpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the v4 domain entity to transform.
     *
     * @param entity the domain entity from ArchitecturalModel
     * @return this builder
     * @since 4.0.0
     */
    public RepositorySpecBuilder domainEntity(DomainEntity entity) {
        this.domainEntity = entity;
        return this;
    }

    /**
     * Sets the v4 driven ports for derived method extraction.
     *
     * @param ports the driven ports for this domain entity
     * @return this builder
     * @since 4.0.0
     */
    public RepositorySpecBuilder drivenPorts(List<DrivenPort> ports) {
        this.drivenPorts = ports != null ? ports : List.of();
        return this;
    }

    /**
     * Sets the v4 architectural model.
     *
     * @param model the architectural model
     * @return this builder
     * @since 4.0.0
     */
    public RepositorySpecBuilder model(ArchitecturalModel model) {
        this.architecturalModel = model;
        return this;
    }

    /**
     * Builds the RepositorySpec from the provided configuration.
     *
     * @return an immutable RepositorySpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if the domain entity has no identity
     */
    public RepositorySpec build() {
        validateRequiredFields();

        if (!domainEntity.hasIdentity()) {
            throw new IllegalArgumentException("Domain entity "
                    + domainEntity.id().qualifiedName() + " has no identity. Cannot generate JPA repository.");
        }

        String simpleName = domainEntity.id().simpleName();
        String interfaceName = simpleName + config.repositorySuffix();
        String entityClassName = simpleName + config.entitySuffix();

        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);
        TypeName idType = resolveIdType();

        // Collect derived methods from v4 driven ports
        List<DerivedMethodSpec> derivedMethods = collectDerivedMethods(entityType);

        return new RepositorySpec(
                infrastructurePackage,
                interfaceName,
                entityType,
                idType,
                domainEntity.id().qualifiedName(),
                derivedMethods);
    }

    /**
     * Resolves the ID type from the v4 domain entity's identity.
     *
     * @return the JavaPoet TypeName of the unwrapped ID type
     */
    private TypeName resolveIdType() {
        TypeRef idType = domainEntity.identityType();

        // Check if it's a value object wrapper (needs unwrapping)
        if (architecturalModel != null) {
            var voOpt = architecturalModel
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(idType.qualifiedName()))
                    .filter(vo -> vo.componentFields().size() == 1)
                    .filter(vo -> vo.syntax() != null)
                    .findFirst();

            if (voOpt.isPresent()) {
                // Unwrap the value object
                var wrappedField = voOpt.get().syntax().fields().get(0);
                return JpaModelUtils.resolveTypeName(wrappedField.type().qualifiedName());
            }
        }

        return JpaModelUtils.resolveTypeName(idType.qualifiedName());
    }

    /**
     * Collects derived query methods from v4 driven ports.
     *
     * @param entityType the entity type for return types
     * @return list of derived method specifications
     */
    private List<DerivedMethodSpec> collectDerivedMethods(TypeName entityType) {
        if (drivenPorts.isEmpty()) {
            return List.of();
        }

        Map<String, DerivedMethodSpec> methodsBySignature = new LinkedHashMap<>();

        for (DrivenPort port : drivenPorts) {
            for (var operation : port.operations()) {
                // Use method syntax if available
                if (operation.syntax() != null) {
                    DerivedMethodSpec spec = DerivedMethodSpec.fromV4(operation, entityType);
                    if (spec != null) {
                        String signature = computeMethodSignature(spec);
                        methodsBySignature.putIfAbsent(signature, spec);
                    }
                }
            }
        }

        return new ArrayList<>(methodsBySignature.values());
    }

    /**
     * Computes a unique signature for a method based on name and parameter types.
     *
     * <p>This is used for deduplication when multiple ports declare the same method.
     *
     * @param spec the derived method specification
     * @return a signature string in the format "methodName(Type1,Type2,...)"
     */
    private String computeMethodSignature(DerivedMethodSpec spec) {
        String paramTypes = spec.parameters().stream()
                .map(p -> p.type().toString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return spec.methodName() + "(" + paramTypes + ")";
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
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
