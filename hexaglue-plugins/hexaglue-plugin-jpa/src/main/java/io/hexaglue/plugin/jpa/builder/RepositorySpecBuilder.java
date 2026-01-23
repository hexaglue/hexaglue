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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.DerivedMethodSpec;
import io.hexaglue.plugin.jpa.model.JpaModelUtils;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builder for transforming domain entities to RepositorySpec model.
 *
 * <p>This builder handles the transformation of domain aggregate roots into
 * Spring Data JPA repository interface specifications. It resolves entity
 * and ID types, applies naming conventions, and prepares the specification
 * for JavaPoet code generation.
 *
 * <h3>Generated Repository Example:</h3>
 * <pre>{@code
 * public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
 *     // Spring Data provides CRUD implementations automatically
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
public final class RepositorySpecBuilder {

    private AggregateRoot aggregateRoot;
    private Entity entity;
    private List<io.hexaglue.arch.model.DrivenPort> drivenPorts = List.of();
    private JpaConfig config;
    private String infrastructurePackage;
    private ArchitecturalModel model;

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
     * @param infrastructurePackage the package for generated JPA classes
     * @return this builder
     */
    public RepositorySpecBuilder infrastructurePackage(String infrastructurePackage) {
        this.infrastructurePackage = infrastructurePackage;
        return this;
    }

    /**
     * Sets the architectural model for type resolution.
     *
     * <p>The model is used to resolve Identifier types to their wrapped types
     * in derived method parameters (C4 fix).
     *
     * @param model the architectural model
     * @return this builder
     * @since 5.0.0
     */
    public RepositorySpecBuilder model(ArchitecturalModel model) {
        this.model = model;
        return this;
    }

    /**
     * Sets the aggregate root to transform.
     *
     * @param aggregateRoot the aggregate root from the model
     * @return this builder
     * @since 5.0.0
     */
    public RepositorySpecBuilder aggregateRoot(AggregateRoot aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        this.entity = null;
        return this;
    }

    /**
     * Sets the entity to transform.
     *
     * @param entity the entity from the model
     * @return this builder
     * @since 5.0.0
     */
    public RepositorySpecBuilder entity(Entity entity) {
        this.entity = entity;
        this.aggregateRoot = null;
        return this;
    }

    /**
     * Sets the driven ports for derived method extraction.
     *
     * @param ports the driven ports from the model
     * @return this builder
     * @since 5.0.0
     */
    public RepositorySpecBuilder drivenPorts(List<io.hexaglue.arch.model.DrivenPort> ports) {
        this.drivenPorts = ports != null ? ports : List.of();
        return this;
    }

    /**
     * Builds the RepositorySpec from the provided configuration.
     *
     * @return an immutable RepositorySpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if the entity has no identity
     */
    public RepositorySpec build() {
        validateRequiredFields();

        // Determine source
        String simpleName;
        String qualifiedName;
        TypeName idType;

        if (aggregateRoot != null) {
            simpleName = aggregateRoot.id().simpleName();
            qualifiedName = aggregateRoot.id().qualifiedName();
            idType = resolveIdTypeFromField(aggregateRoot.identityField());
        } else {
            simpleName = entity.id().simpleName();
            qualifiedName = entity.id().qualifiedName();
            var identityFieldOpt = entity.identityField();
            if (identityFieldOpt.isEmpty()) {
                throw new IllegalArgumentException(
                        "Entity " + qualifiedName + " has no identity. Cannot generate JPA repository.");
            }
            idType = resolveIdTypeFromField(identityFieldOpt.get());
        }

        String interfaceName = simpleName + config.repositorySuffix();
        String entityClassName = simpleName + config.entitySuffix();
        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);

        // Collect derived methods from driven ports
        List<DerivedMethodSpec> derivedMethods = collectDerivedMethods(entityType);

        return new RepositorySpec(
                infrastructurePackage, interfaceName, entityType, idType, qualifiedName, derivedMethods);
    }

    /**
     * Resolves the ID type from a Field.
     *
     * @param identityField the identity field
     * @return the JavaPoet TypeName of the unwrapped ID type
     * @since 5.0.0
     */
    private TypeName resolveIdTypeFromField(Field identityField) {
        // If the field has a wrappedType, use it (the unwrapped type)
        if (identityField.wrappedType().isPresent()) {
            return JpaModelUtils.resolveTypeName(
                    identityField.wrappedType().get().qualifiedName());
        }
        return JpaModelUtils.resolveTypeName(identityField.type().qualifiedName());
    }

    /**
     * Collects derived query methods from driven ports.
     *
     * <p>Uses the port's structure.methods() to extract property-based query methods.
     * C4 fix: Uses DomainIndex to resolve Identifier types to their wrapped types.
     *
     * @param entityType the entity type for return types
     * @return list of derived method specifications
     * @since 5.0.0
     */
    private List<DerivedMethodSpec> collectDerivedMethods(TypeName entityType) {
        Map<String, DerivedMethodSpec> methodsBySignature = new LinkedHashMap<>();

        // C4 fix: Get DomainIndex for Identifier type resolution
        Optional<DomainIndex> domainIndexOpt = model != null ? model.domainIndex() : Optional.empty();

        for (io.hexaglue.arch.model.DrivenPort port : drivenPorts) {
            for (var method : port.structure().methods()) {
                // Skip static methods
                if (method.isStatic()) {
                    continue;
                }
                DerivedMethodSpec spec = DerivedMethodSpec.fromV5(method, entityType, domainIndexOpt);
                if (spec != null) {
                    String signature = computeMethodSignature(spec);
                    methodsBySignature.putIfAbsent(signature, spec);
                }
            }
        }

        return new ArrayList<>(methodsBySignature.values());
    }

    /**
     * Computes a unique signature for a method based on name and parameter types.
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
        if (aggregateRoot == null && entity == null) {
            throw new IllegalStateException("Either aggregateRoot or entity is required");
        }
        if (config == null) {
            throw new IllegalStateException("config is required");
        }
        if (infrastructurePackage == null || infrastructurePackage.isEmpty()) {
            throw new IllegalStateException("infrastructurePackage is required");
        }
    }
}
