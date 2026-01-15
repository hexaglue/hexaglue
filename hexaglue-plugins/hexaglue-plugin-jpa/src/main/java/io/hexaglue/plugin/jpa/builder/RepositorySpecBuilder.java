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
import io.hexaglue.plugin.jpa.model.DerivedMethodSpec;
import io.hexaglue.plugin.jpa.model.JpaModelUtils;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortMethod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for transforming SPI DomainType to RepositorySpec model.
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
 *     .domainType(orderType)
 *     .config(jpaConfig)
 *     .infrastructurePackage("com.example.infrastructure.jpa")
 *     .build();
 * }</pre>
 *
 * @since 2.0.0
 */
public final class RepositorySpecBuilder {

    private DomainType domainType;
    private JpaConfig config;
    private String infrastructurePackage;
    private List<Port> ports = List.of();

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
     * Sets the domain type to transform.
     *
     * @param domainType the domain aggregate root
     * @return this builder
     */
    public RepositorySpecBuilder domainType(DomainType domainType) {
        this.domainType = domainType;
        return this;
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
     * Sets the ports associated with this domain type.
     *
     * <p>The ports are used to extract derived query methods (findByProperty,
     * existsByProperty, etc.) that need to be declared in the repository interface.
     *
     * @param ports the repository ports for this domain type
     * @return this builder
     */
    public RepositorySpecBuilder ports(List<Port> ports) {
        this.ports = ports != null ? ports : List.of();
        return this;
    }

    /**
     * Builds the RepositorySpec from the provided configuration.
     *
     * <p>This method performs the transformation from SPI DomainType to the
     * RepositorySpec model. It validates that all required fields are set
     * and derives the repository interface name and type parameters.
     *
     * <p>The ID type is unwrapped for JPA compatibility. For example, a wrapped
     * identifier {@code record OrderId(UUID value)} is unwrapped to {@code UUID}
     * for use in the {@code JpaRepository<OrderEntity, UUID>} generic parameter.
     *
     * <p>Derived methods are collected from the associated ports. Methods like
     * {@code findByEmail}, {@code existsByStatus} are extracted and will be
     * declared in the generated repository interface.
     *
     * @return an immutable RepositorySpec ready for code generation
     * @throws IllegalStateException if required fields are missing
     * @throws IllegalArgumentException if the domain type has no identity
     */
    public RepositorySpec build() {
        validateRequiredFields();

        if (!domainType.hasIdentity()) {
            throw new IllegalArgumentException(
                    "Domain type " + domainType.qualifiedName() + " has no identity. Cannot generate JPA repository.");
        }

        String interfaceName = domainType.simpleName() + config.repositorySuffix();
        String entityClassName = domainType.simpleName() + config.entitySuffix();

        TypeName entityType = ClassName.get(infrastructurePackage, entityClassName);
        TypeName idType = resolveIdType();

        // Collect derived methods from ports
        List<DerivedMethodSpec> derivedMethods = collectDerivedMethods(entityType);

        return new RepositorySpec(
                infrastructurePackage, interfaceName, entityType, idType, domainType.qualifiedName(), derivedMethods);
    }

    /**
     * Collects derived query methods from the associated ports.
     *
     * <p>This method scans all ports for property-based query methods
     * (findByProperty, existsByProperty, countByProperty, etc.) and
     * transforms them into DerivedMethodSpec instances.
     *
     * <p>Method deduplication is performed using method signatures to avoid
     * generating duplicate methods when multiple ports declare the same query.
     *
     * @param entityType the entity type to use in return types
     * @return list of derived method specifications
     */
    private List<DerivedMethodSpec> collectDerivedMethods(TypeName entityType) {
        if (ports.isEmpty()) {
            return List.of();
        }

        // Use LinkedHashMap to preserve insertion order and deduplicate by signature
        Map<String, DerivedMethodSpec> methodsBySignature = new LinkedHashMap<>();

        for (Port port : ports) {
            for (PortMethod method : port.methods()) {
                DerivedMethodSpec spec = DerivedMethodSpec.from(method, entityType);
                if (spec != null) {
                    String signature = computeMethodSignature(spec);
                    // Keep the first occurrence only (deduplication)
                    methodsBySignature.putIfAbsent(signature, spec);
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
     * Resolves the ID type from the domain type's identity.
     *
     * <p>Uses the unwrapped type for JPA compatibility. Wrapped identity types
     * (e.g., {@code record OrderId(UUID value)}) are unwrapped to their underlying
     * type (e.g., {@code UUID}) for use in the JpaRepository generic parameter.
     *
     * <p>This ensures that the repository can work with standard Java types that
     * JPA understands natively, rather than custom wrapper types.
     *
     * @return the JavaPoet TypeName of the unwrapped ID type
     */
    private TypeName resolveIdType() {
        Identity identity =
                domainType.identity().orElseThrow(() -> new IllegalArgumentException("Domain type has no identity"));

        String idTypeName = identity.unwrappedType().qualifiedName();
        return JpaModelUtils.resolveTypeName(idTypeName);
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if any required field is missing
     */
    private void validateRequiredFields() {
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
